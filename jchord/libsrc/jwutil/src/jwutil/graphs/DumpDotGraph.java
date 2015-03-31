// DumpDotGraph.java, created May 17, 2004 2:51:42 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import jwutil.collections.Filter;
import jwutil.collections.HashWorklist;
import jwutil.collections.IndexMap;

/**
 * DumpDotGraph
 * 
 * @author jwhaley
 * @version $Id: DumpDotGraph.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class DumpDotGraph {
    static boolean TRACE = false;
    static PrintStream out = System.out;
    // Graph nodes/edges
    Set nodes;
    Navigator navigator;
    // Graph labels
    Filter nodeLabels;
    EdgeLabeler edgeLabels;
    // Graph colors
    Filter nodeColors;
    EdgeLabeler edgeColors;
    // Graph styles
    Filter nodeStyles;
    EdgeLabeler edgeStyles;
    // Graph options
    boolean concentrate;
    // Clusters
    Filter containingCluster;
    Set clusterRoots;
    Navigator clusterNavigator;

    public DumpDotGraph() {
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void setNodeLabels(Filter nodeLabels) {
        this.nodeLabels = nodeLabels;
    }

    public void setEdgeLabels(EdgeLabeler edgeLabels) {
        this.edgeLabels = edgeLabels;
    }

    public void setNodeColors(Filter nodeColors) {
        this.nodeColors = nodeColors;
    }

    public void setEdgeColors(EdgeLabeler edgeColors) {
        this.edgeColors = edgeColors;
    }

    public void setNodeStyles(Filter nodeStyles) {
        this.nodeStyles = nodeStyles;
    }

    public void setEdgeStyles(EdgeLabeler edgeStyles) {
        this.edgeStyles = edgeStyles;
    }

    public void setClusters(Filter clusters) {
        this.containingCluster = clusters;
    }

    public void setClusterNesting(Navigator nesting) {
        this.clusterNavigator = nesting;
    }

    public void setNodeSet(Set nodes) {
        this.nodes = nodes;
    }

    public Set computeTransitiveClosure(Collection roots) {
        HashWorklist w = new HashWorklist(true);
        w.addAll(roots);
        while (!w.isEmpty()) {
            Object o = w.pull();
            w.addAll(navigator.next(o));
        }
        nodes = w.getVisitedSet();
        return nodes;
    }

    public Set computeBidirTransitiveClosure(Collection roots) {
        HashWorklist w = new HashWorklist(true);
        w.addAll(roots);
        while (!w.isEmpty()) {
            Object o = w.pull();
            w.addAll(navigator.next(o));
            w.addAll(navigator.prev(o));
        }
        nodes = w.getVisitedSet();
        return nodes;
    }

    void computeClusters() {
        if (containingCluster == null) return;
        HashWorklist w = new HashWorklist(true);
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            Object o = i.next();
            Object c = containingCluster.map(o);
            if (c != null) w.add(c);
        }
        if (clusterNavigator != null) {
            clusterRoots = new HashSet();
            while (!w.isEmpty()) {
                Object o = w.pull();
                if (TRACE) out.println("Cluster: " + o);
                Collection c;
                c = clusterNavigator.next(o);
                if (TRACE) out.println("Successors: " + c);
                w.addAll(c);
                c = clusterNavigator.prev(o);
                if (TRACE) out.println("Predecessors: " + c);
                w.addAll(c);
                if (c.isEmpty()) clusterRoots.add(o);
            }
        } else {
            clusterRoots = w.getVisitedSet();
        }
        if (TRACE) out.println("Cluster roots: " + clusterRoots);
    }

    public void dump(String filename) throws IOException {
        BufferedWriter dos = null;
        try {
            dos = new BufferedWriter(new FileWriter(filename));
            dump(dos);
        } finally {
            if (dos != null) dos.close();
        }
    }

    void dumpNodes(BufferedWriter dos, IndexMap m, Object cluster)
        throws IOException {
        if (TRACE) out.println("Dumping nodes for cluster " + cluster);
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            Object o = i.next();
            if (containingCluster != null) {
                Object c = containingCluster.map(o);
                if (c != cluster) continue;
                if (c == null) continue;
                if (!c.equals(cluster)) continue;
            }
            Object nodeid = (m != null) ? ("n" + m.get(o)) : "\"" + o + "\"";
            dos.write("  " + nodeid);
            boolean open = false;
            if (nodeLabels != null) {
                Object label = nodeLabels.map(o);
                if (label != null) {
                    open = true;
                    dos.write(" [label=\"" + label + "\"");
                }
            }
            if (nodeStyles != null) {
                Object label = nodeStyles.map(o);
                if (label != null) {
                    if (!open) dos.write(" [");
                    else dos.write(",");
                    open = true;
                    dos.write("style=" + label);
                }
            }
            if (nodeColors != null) {
                Object label = nodeColors.map(o);
                if (label != null) {
                    if (!open) dos.write(" [");
                    else dos.write(",");
                    open = true;
                    dos.write("color=" + label);
                }
            }
            if (open) dos.write("]");
            dos.write(";\n");
        }
    }

    void dumpCluster(BufferedWriter dos, IndexMap m, Set visitedClusters, Object c)
        throws IOException {
        if (!visitedClusters.add(c)) return;
        dos.write("  subgraph cluster" + visitedClusters.size() + " {\n");
        dumpNodes(dos, m, c);
        if (clusterNavigator != null) {
            Collection subClusters = clusterNavigator.next(c);
            if (TRACE) out.println("Subclusters: " + subClusters);
            for (Iterator i = subClusters.iterator(); i.hasNext();) {
                Object subC = i.next();
                dumpCluster(dos, m, visitedClusters, subC);
            }
        }
        dos.write("  }\n");
    }

    public void dump(BufferedWriter dos) throws IOException {
        computeClusters();
        dos.write("digraph {\n");
        dos.write("  size=\"10,7.5\";\n");
        dos.write("  rotate=90;\n");
        if (concentrate) dos.write("  concentrate=true;\n");
        dos.write("  ratio=fill;\n");
        dos.write("\n");
        IndexMap m;
        if (nodeLabels != null) {
            m = new IndexMap("NodeID");
        } else {
            m = null;
        }
        if (clusterRoots != null) {
            Set visitedClusters = new HashSet();
            for (Iterator i = clusterRoots.iterator(); i.hasNext();) {
                Object c = i.next();
                dumpCluster(dos, m, visitedClusters, c);
            }
        }
        dumpNodes(dos, m, null);
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            Object n1 = i.next();
            Object node1id = (m != null) ? ("n" + m.get(n1)) : "\"" + n1 + "\"";
            Collection succ = navigator.next(n1);
            for (Iterator j = succ.iterator(); j.hasNext();) {
                Object n2 = j.next();
                if (!nodes.contains(n2)) continue;
                Object node2id = (m != null) ? ("n" + m.get(n2)) : "\"" + n2
                    + "\"";
                dos.write("  " + node1id + " -> " + node2id);
                boolean open = false;
                if (edgeLabels != null) {
                    Object label = edgeLabels.getLabel(n1, n2);
                    if (label != null) {
                        open = true;
                        dos.write(" [label=\"" + label + "\"]");
                    }
                }
                if (edgeStyles != null) {
                    Object label = edgeStyles.getLabel(n1, n2);
                    if (label != null) {
                        if (!open) dos.write(" [");
                        else dos.write(",");
                        open = true;
                        dos.write("style=" + label);
                    }
                }
                if (edgeColors != null) {
                    Object label = edgeColors.getLabel(n1, n2);
                    if (label != null) {
                        if (!open) dos.write(" [");
                        else dos.write(",");
                        open = true;
                        dos.write("color=" + label);
                    }
                }
                dos.write(";\n");
            }
        }
        dos.write("}\n");
    }
}
