// Dot.java, created May 10, 2004 by cunkel
// Copyright (C) 2004 cunkel
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigInteger;
import jwutil.collections.HashWorklist;
import jwutil.collections.Worklist;
import jwutil.graphs.Graph;
import jwutil.strings.MyStringTokenizer;
import jwutil.util.Assert;
import net.sf.bddbddb.RelationGraph.GraphNode;

/**
 * Dot
 * 
 * @author cunkel
 * @version $Id: Dot.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class Dot {
    private Solver solver;
    private Collection/* EdgeSource */edgeSources;
    private Collection/* NodeAttributeModifier */nodeModifiers;
    private Worklist worklist;
    private Collection/* String */nodes;
    private Collection/* String */edges;
    private Collection/* Relation */usedRelations;
    private String outputFileName;

    Dot() {
        edgeSources = new LinkedList();
        nodeModifiers = new LinkedList();
        worklist = new HashWorklist(true);
        nodes = new LinkedList();
        edges = new LinkedList();
        usedRelations = new HashSet();
    }

    void outputGraph() throws IOException {
        Iterator i = edgeSources.iterator();
        Set allRoots = new HashSet();
        while (i.hasNext()) {
            allRoots.addAll(((EdgeSource) i.next()).roots());
        }
        i = allRoots.iterator();
        while (i.hasNext()) {
            worklist.push(i.next());
        }
        while (!worklist.isEmpty()) {
            GraphNode n = (GraphNode) worklist.pull();
            visitNode(n);
        }
        BufferedWriter dos = null;
        try {
            dos = new BufferedWriter(new FileWriter(solver.basedir + outputFileName));
            dos.write("digraph {\n");
            dos.write("  size=\"7.5,10\";\n");
            //dos.write(" rotate=90;\n");
            dos.write("  concentrate=true;\n");
            dos.write("  ratio=fill;\n");
            dos.write("  rankdir=LR;");
            dos.write("\n");
            i = nodes.iterator();
            while (i.hasNext()) {
                dos.write("  ");
                dos.write((String) i.next());
            }
            dos.write("\n");
            i = edges.iterator();
            while (i.hasNext()) {
                dos.write("  ");
                dos.write((String) i.next());
            }
            dos.write("}\n");
        } finally {
            if (dos != null) {
                dos.close();
            }
        }
    }

    void parseInput(Solver s, LineNumberReader in) throws IOException {
        solver = s;
        String currentLine = in.readLine();
        while (currentLine != null) {
            solver.out.println("Parsing " + currentLine);
            MyStringTokenizer st = new MyStringTokenizer(currentLine, " \t,()");
            parseLine(st);
            currentLine = in.readLine();
        }
    }

    private void parseLine(MyStringTokenizer st) {
        if (!st.hasMoreTokens()) {
            return;
        }
        String s = st.nextToken();
        if (s.startsWith("#")) {
            return;
        }
        if (s.equals("edge")) {
            String relationName = st.nextToken();
            String roots = st.nextToken();
            String rootsName = st.nextToken();
            if (!roots.equals("roots")) {
                throw new IllegalArgumentException();
            }
            Relation edgeRelation = solver.getRelation(relationName);
            Relation rootsRelation = solver.getRelation(rootsName);
            usedRelations.add(edgeRelation);
            usedRelations.add(rootsRelation);
            EdgeSource es = new EdgeSource(edgeRelation, rootsRelation);
            if (st.hasMoreTokens()) {
                String label = st.nextToken();
                if (!label.equals("label")) {
                    throw new IllegalArgumentException();
                }
                relationName = st.nextToken();
                String[] e = new String[3];
                e[0] = st.nextToken();
                e[1] = st.nextToken();
                e[2] = st.nextToken();
                int sourceIndex = -1;
                int labelIndex = -1;
                int sinkIndex = -1;
                for (int i = 0; i < 3; i++) {
                    if (e[i].equals("source")) {
                        sourceIndex = i;
                    } else if (e[i].equals("sink")) {
                        sinkIndex = i;
                    } else if (e[i].equals("label")) {
                        labelIndex = i;
                    }
                }
                if (sourceIndex == -1) throw new IllegalArgumentException();
                if (sinkIndex == -1) throw new IllegalArgumentException();
                if (labelIndex == -1) throw new IllegalArgumentException();
                Relation labelRelation = solver.getRelation(relationName);
                usedRelations.add(labelRelation);
                es.setLabelSource(new LabelSource(labelRelation, sourceIndex, sinkIndex, labelIndex));
            }
            edgeSources.add(es);
        } else if (s.equals("domain")) {
            String domainName = st.nextToken();
            String attribute = st.nextToken();
            String value = st.nextToken();
            nodeModifiers.add(new DomainModifier(attribute, value, solver.getDomain(domainName)));
        } else if (s.equals("default")) {
            String attribute = st.nextToken();
            String value = st.nextToken();
            nodeModifiers.add(new DefaultModifier(attribute, value));
        } else if (s.equals("relation")) {
            String relationName = st.nextToken();
            String attribute = st.nextToken();
            String value = st.nextToken();
            BDDRelation relation = (BDDRelation) solver.getRelation(relationName);
            usedRelations.add(relation);
            nodeModifiers.add(new InRelationModifier(attribute, value, relation));
        } else if (s.equals("output")) {
            outputFileName = st.nextToken();
        } else {
            throw new IllegalArgumentException();
        }
    }
    private static class LabelSource {
        Relation relation;
        int sourceIndex;
        int sinkIndex;
        int labelIndex;
        Domain labelDomain;

        LabelSource(Relation r, int sourceI, int sinkI, int labelI) {
            relation = r;
            sourceIndex = sourceI;
            sinkIndex = sinkI;
            labelIndex = labelI;
            Attribute a = (Attribute) relation.attributes.get(labelIndex);
            labelDomain = a.attributeDomain;
        }

        String getLabel(RelationGraph.GraphNode source, RelationGraph.GraphNode sink) {
            BigInteger[] restriction = new BigInteger[3];
            restriction[0] = restriction[1] = restriction[2] = BigInteger.valueOf(-1);
            restriction[sourceIndex] = source.number;
            restriction[sinkIndex] = sink.number;
            TupleIterator i = relation.iterator(restriction);
            String label = null;
            while (i.hasNext()) {
                BigInteger[] labelTuple = i.nextTuple();
                BigInteger labelNumber = labelTuple[labelIndex];
                String l = labelDomain.toString(labelNumber);
                if (label == null) {
                    label = l;
                } else {
                    label = label + ", " + l;
                }
            }
            return label;
        }
    }
    private static class EdgeSource {
        Relation relation;
        Relation roots;
        LabelSource labelSource;
        Graph g;

        EdgeSource(Relation rel, Relation rts) {
            relation = rel;
            roots = rts;
            labelSource = null;
            g = null;
        }

        public void setLabelSource(LabelSource source) {
            labelSource = source;
        }

        public Collection roots() {
            if (g == null) {
                g = new RelationGraph(roots, relation);
            }
            return g.getRoots();
        }

        public void visitSources(Dot dot, RelationGraph.GraphNode sink, boolean addEdges) {
            if (g == null) {
                g = new RelationGraph(roots, relation);
            }
            Collection c = g.getNavigator().prev(sink);
            Iterator i = c.iterator();
            while (i.hasNext()) {
                RelationGraph.GraphNode source = (RelationGraph.GraphNode) i.next();
                dot.enqueue(source);
                if (addEdges) {
                    String label = null;
                    if (labelSource != null) {
                        label = labelSource.getLabel(source, sink);
                    }
                    if (label != null) {
                        dot.addEdge(dot.nodeName(source) + " -> " + dot.nodeName(sink) + " [label=\"" + label + "\"];\n");
                    } else {
                        dot.addEdge(dot.nodeName(source) + " -> " + dot.nodeName(sink) + ";\n");
                    }
                }
            }
        }

        public void visitSinks(Dot dot, RelationGraph.GraphNode source, boolean addEdges) {
            if (g == null) {
                g = new RelationGraph(roots, relation);
            }
            Collection c = g.getNavigator().next(source);
            Iterator i = c.iterator();
            while (i.hasNext()) {
                RelationGraph.GraphNode sink = (RelationGraph.GraphNode) i.next();
                dot.enqueue(sink);
                if (addEdges) {
                    String label = null;
                    if (labelSource != null) {
                        label = labelSource.getLabel(source, sink);
                    }
                    if (label != null) {
                        dot.addEdge(dot.nodeName(source) + " -> " + dot.nodeName(sink) + " [label=\"" + label + "\"];\n");
                    } else {
                        dot.addEdge(dot.nodeName(source) + " -> " + dot.nodeName(sink) + ";\n");
                    }
                }
            }
        }
    }
    private static abstract class NodeAttributeModifier {
        abstract boolean match(RelationGraph.GraphNode n, Map a);
    }
    private class DefaultModifier extends NodeAttributeModifier {
        String property;
        String value;

        DefaultModifier(String p, String v) {
            property = p;
            value = v;
        }

        boolean match(GraphNode n, Map a) {
            a.put(property, value);
            return true;
        }
    }
    private class DomainModifier extends NodeAttributeModifier {
        Domain fd;
        String property;
        String value;

        DomainModifier(String p, String v, Domain f) {
            property = p;
            value = v;
            fd = f;
        }

        boolean match(GraphNode n, Map a) {
            Domain f = n.v.getDomain();
            if (f.equals(fd)) {
                a.put(property, value);
                return true;
            } else {
                return false;
            }
        }
    }
    private class InRelationModifier extends NodeAttributeModifier {
        BDDRelation relation;
        String property;
        String value;

        InRelationModifier(String p, String v, BDDRelation r) {
            property = p;
            value = v;
            relation = r;
            Assert._assert(r.attributes.size() == 1);
        }

        boolean match(GraphNode n, Map a) {
            Attribute attr = (Attribute) relation.attributes.iterator().next();
            Domain f = attr.attributeDomain;
            if (n.v.getDomain().equals(f)) {
                if (relation.contains(0, n.number)) {
                    a.put(property, value);
                    return true;
                }
            }
            return false;
        }
    }

    public void enqueue(GraphNode x) {
        worklist.push(x);
    }

    public void addEdge(String edge) {
        edges.add(edge);
    }

    public String nodeName(GraphNode n) {
        return "\"" + n.toString() + "\"";
    }

    private void visitNode(GraphNode x) {
        Map attributes = new HashMap();
        String nodeName = (String) x.v.getDomain().map.get(x.number.intValue());
        if (nodeName != null) {
            attributes.put("label", nodeName);
        }
        Iterator i = nodeModifiers.iterator();
        while (i.hasNext()) {
            NodeAttributeModifier m = (NodeAttributeModifier) i.next();
            m.match(x, attributes);
        }
        String node = nodeName(x) + " [";
        i = attributes.keySet().iterator();
        boolean firstAttribute = true;
        while (i.hasNext()) {
            String attribute = (String) i.next();
            String value = (String) attributes.get(attribute);
            if (!firstAttribute) {
                node += ", ";
            }
            node += attribute + "=" + "\"" + value + "\"";
            firstAttribute = false;
        }
        node += "];\n";
        nodes.add(node);
        i = edgeSources.iterator();
        while (i.hasNext()) {
            EdgeSource es = (EdgeSource) i.next();
            es.visitSinks(this, x, true);
            //es.visitSources(this, x, false);
        }
    }

    /**
     * @return  the collection of used relations
     */
    public Collection getUsedRelations() {
        return usedRelations;
    }
}
