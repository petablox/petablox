// PathNumbering.java, created Aug 16, 2003 1:49:33 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import jwutil.collections.IndexMap;
import jwutil.collections.Pair;
import jwutil.io.Textualizable;
import jwutil.io.Textualizer;

/**
 * PathNumbering
 * 
 * @author John Whaley
 * @version $Id: PathNumbering.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public abstract class PathNumbering {

    public abstract BigInteger countPaths(Collection roots, Navigator navigator, Map initialMap);
    public abstract Range getRange(Object o);
    public abstract Range getEdge(Object from, Object to);
    
    public BigInteger countPaths(Graph graph) {
        return countPaths(graph.getRoots(), graph.getNavigator(), null);
    }
    
    public Range getEdge(Pair edge) {
        return getEdge(edge.left, edge.right);
    }

    public void dotGraph(BufferedWriter out, Collection roots, Navigator navigator) throws IOException {
        out.write("digraph \"PathNumbering\" {\n");
        out.write("  concentrate=true; node[fontsize=7];\n");
        LinkedList toVisit = new LinkedList();
        toVisit.addAll(roots);
        IndexMap m = new IndexMap("NodeMap");
        while (!toVisit.isEmpty()) {
            Object source = toVisit.removeFirst();
            int j = m.get(source);
            out.write("  n"+j+" [label=\""+source+"\"];\n");
            for (Iterator i = navigator.next(source).iterator(); i.hasNext(); ) {
                Object target = i.next();
                if (!m.contains(target)) {
                    toVisit.add(target);
                }
                int k = m.get(target);
                Range r = getEdge(source, target);
                out.write("  n"+j+" -> n"+k+" [label=\""+r+"\"];\n");
            }
        }
        out.write("}\n");
    }
    
    public static class Range implements Textualizable {
        public Number low, high;
        public Range(int l, int h) {
            this.low = new Integer(l); this.high = new Integer(h);
        }
        public Range(Number l, Number h) {
            this.low = l; this.high = h;
        }
        public Range(Number l, BigInteger h) {
            this.low = l; this.high = fromBigInt(h);
        }
        public Range(BigInteger l, Number h) {
            this.low = fromBigInt(l); this.high = h;
        }
        public Range(BigInteger l, BigInteger h) {
            this.low = fromBigInt(l); this.high = fromBigInt(h);
        }
        public String toString() {
            return "<"+low+','+high+'>';
        }
        public boolean equals(Range r) {
            return low.equals(r.low) && high.equals(r.high);
        }
        public boolean equals(Object o) {
            try {
                return equals((Range) o);
            } catch (ClassCastException x) {
                return false;
            }
        }
        public int hashCode() {
            return low.hashCode() ^ high.hashCode();
        }
        public void write(Textualizer t) throws IOException {
            t.writeString(low+" "+high);
        }
        public void writeEdges(Textualizer t) throws IOException { }
        public void addEdge(String s, Textualizable t) { }
        public static Range read(StringTokenizer st) {
            long lo = Long.parseLong(st.nextToken());
            long hi = Long.parseLong(st.nextToken());
            return new Range(new Long(lo), new Long(hi));
        }
    }
    
    /** Converts the given Number to BigInteger representation. */
    public static BigInteger toBigInt(Number n) {
        if (n instanceof BigInteger) return (BigInteger) n;
        else return BigInteger.valueOf(n.longValue());
    }

    /** Converts the given BigInteger to a potentially smaller Number representation. */
    public static Number fromBigInt(BigInteger n) {
        int bits = n.bitLength();
        if (bits < 32) return new Integer(n.intValue());
        if (bits < 64) return new Long(n.longValue());
        return n;
    }

    public interface Selector {
        /**
         * Return true if the edge scc1->scc2 is important.
         */
        boolean isImportant(SCComponent scc1, SCComponent scc2, BigInteger num);
        
        /**
         * Return true if the edge a->b is important.
         */
        boolean isImportant(Object a, Object b, BigInteger num);
    }
    
}
