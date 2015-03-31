// SCComponent.java, created Thu Mar 27 17:49:37 2003 by joewhaley
// Copyright (C) 2001-3 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.Serializable;
import jwutil.util.Assert;

/**
 * <code>SCComponent</code> models a <i>Strongly connected component </i> of a
 * graph. The only way to split a graph into <code>SCComponent</code> s is
 * though <code>buildSCC</code>. That method is quite flexible: all it needs
 * is a root node (or a set of root nodes) and a <i>Navigator </i>: an object
 * implementing the <code>Navigator</code> interface that provides the edges
 * coming into/going out of a given <code>Object</code>. So, it can build
 * strongly connected components even for graphs that are not built up from
 * <code>CFGraphable</code> nodes, a good example being the set of methods
 * where the edges represent the caller-callee relation (in this case, the
 * strongly connected components group together sets of mutually recursive
 * methods).
 * 
 * @author Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: SCComponent.java,v 1.5 2005/05/23 21:13:33 joewhaley Exp $
 */
public final class SCComponent implements Comparable, Serializable {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3257844385516501305L;
    
    /**
     * Default navigator through a component graph (a dag of strongly connected
     * components).
     */
    public static final Navigator SCC_NAVIGATOR = new Navigator() {
        public Collection next(Object node) {
            return Arrays.asList(((SCComponent) node).next);
        }

        public Collection prev(Object node) {
            return Arrays.asList(((SCComponent) node).prev);
        }
    };
    // THE FIRST PART CONTAINS JUST SOME STATIC METHODS & FIELDS
    // The internal version of a SCC: basically the same as the external
    // one, but using Sets instead of arrays; the idea is to use the most
    // convenient format during the generation and next to convert everything
    // to the small one to save some memory ...
    private static class SCComponentInt {
        // the nodes of this SCC
        public ArrayList nodes = new ArrayList();
        // the successors; kept as both a set (for quick membership testing)
        // and a vector for uniquely ordered and fast iterations.
        public Set next = new HashSet();
        public ArrayList next_vec = new ArrayList();
        // the predecessors; similar to next, next_vec
        public Set prev = new HashSet();
        public ArrayList prev_vec = new ArrayList();
        // the "economic format" component
        public SCComponent comp = new SCComponent();
        // is there any edge to itself?
        public boolean loop = false;
    }
    // the set of nodes that are reachable from the root object
    private static Set reachable_nodes;
    // the set of reached nodes (to avoid reanalyzing them "ad infinitum")
    private static Set analyzed_nodes;
    // Mapping node (Object) -> Strongly Connected Component (SCComponentInt)
    private static HashMap node2scc;
    // The vector of the reached nodes, in the order DFS finished them
    private static ArrayList nodes_vector;
    private static SCComponentInt current_scc_int;
    // the navigator used in the DFS algorithm
    private static Navigator nav;
    // vector to put the generated SCCs in.
    private static ArrayList scc_vector;

    /**
     * Convenient version of <code>buildSCC(Object[],Navigator)</code>
     * 
     * @param graph
     *            directed graph
     * @return set of <code>graph</code>'s strongly connected components
     */
    public static final Set/* SCComponent <Node> */buildSCC(Graph /* Node */graph) {
        return buildSCC(graph.getRoots(), graph.getNavigator());
    }

    /**
     * Convenient version for the single root case (see the other
     * <code>buildSCC</code> for details). Returns the single element of the
     * set of top level SCCs.
     */
    public static final SCComponent buildSCC(final Object root,
        final Navigator navigator) {
        Set set = buildSCC(Collections.singleton(root), navigator);
        if ((set == null) || set.isEmpty()) return null;
        Assert._assert(set.size() <= 1,
            "More than one root SCComponent in a call with a a single root");
        // return the single element of the set of root SCComponents.
        return (SCComponent) (set.iterator().next());
    }

    /**
     * Constructs the strongly connected components of the graph containing all
     * the nodes reachable on paths that originate in nodes from
     * <code>roots</code>. The edges are indicated by <code>navigator</code>.
     * Returns the set of the root <code>SCComponent</code>s, the components
     * that are not pointed by any other component. This constraint is actively
     * used in the topological sorting agorithm (see
     * <code>SCCTopSortedGraph</code>).
     */
    public static final Set buildSCC(final Collection roots,
        final Navigator navigator) {
        scc_vector = new ArrayList();
        // STEP 1: compute the finished time of each node in a DFS exploration.
        // At the end of this step, nodes_vector will contain all the reached
        // nodes, in the order of their "finished" time.
        nav = navigator;
        analyzed_nodes = new HashSet();
        nodes_vector = new ArrayList();
        for (Iterator i = roots.iterator(); i.hasNext();) {
            Object root = i.next();
            // avoid reanalyzing nodes
            if (!analyzed_nodes.contains(root)) DFS_first(root);
        }
        // STEP 2. build the SCCs by doing a DFS in the reverse graph.
        node2scc = new HashMap();
        // "in reverse" navigator
        nav = new ReverseNavigator(navigator);
        // Explore the nodes in the decreasing order of their finishing time.
        // This phase will create the SCCs (big format) and initialize the
        // node2scc mapping (but it won't set the inter-SCC edges). Also,
        // the SCCs are put in scc_vector.
        // only the nodes reachable from root nodes count! :
        // we make sure that navigator.prev cannot take us to strange places!
        reachable_nodes = analyzed_nodes;
        analyzed_nodes = new HashSet();
        for (int i = nodes_vector.size() - 1; i >= 0; i--) {
            Object node = nodes_vector.get(i);
            // explore nodes that are still unanalyzed
            if (node2scc.get(node) == null) {
                current_scc_int = new SCComponentInt();
                scc_vector.add(current_scc_int);
                DFS_second(node);
            }
        }
        // Put the edges between the SCCs.
        put_the_edges(navigator);
        // Convert the big format SCCs into the compressed format SCCs.
        build_compressed_format();
        // Save the root SCComponents somewhere before activating the GC.
        Set root_sccs = new HashSet();
        for (Iterator i = roots.iterator(); i.hasNext();) {
            Object root = i.next();
            SCComponent root_scc = ((SCComponentInt) node2scc.get(root)).comp;
            if (root_scc.prevLength() == 0) {
                root_sccs.add(root_scc);
            }
        }
        nav = null; // enable the GC
        nodes_vector = null;
        reachable_nodes = null;
        analyzed_nodes = null;
        node2scc = null;
        current_scc_int = null;
        scc_vector = null;
        return root_sccs;
    }

    // DFS for the first step: the "forward" navigation
    private static final void DFS_first(Object node) {
        // do not analyze nodes already reached
        if (analyzed_nodes.contains(node)) return;
        analyzed_nodes.add(node);
        Collection next = nav.next(node);
        for (Iterator i = next.iterator(); i.hasNext();)
            DFS_first(i.next());
        nodes_vector.add(node);
    }

    // DFS for the second step: the "backward" navigation.
    private static final void DFS_second(Object node) {
        if (analyzed_nodes.contains(node) || !reachable_nodes.contains(node)) return;
        analyzed_nodes.add(node);
        node2scc.put(node, current_scc_int);
        current_scc_int.nodes.add(node);
        Collection next = nav.next(node);
        for (Iterator i = next.iterator(); i.hasNext();)
            DFS_second(i.next());
    }

    // put the edges between the SCCs: there is an edge from scc1 to scc2 iff
    // there is at least one pair of nodes n1 in scc1 and n2 in scc2 such that
    // there exists an edge from n1 to n2.
    private static final void put_the_edges(final Navigator navigator) {
        for (int i = 0; i < scc_vector.size(); i++) {
            SCComponentInt compi = (SCComponentInt) scc_vector.get(i);
            for (Iterator it = compi.nodes.iterator(); it.hasNext();) {
                Object node = it.next();
                Collection edges = navigator.next(node);
                for (Iterator j = edges.iterator(); j.hasNext();) {
                    Object node2 = j.next();
                    SCComponentInt compi2 = (SCComponentInt) node2scc
                        .get(node2);
                    if (compi2 == null) {
                        System.out.println("Error! " + node
                            + " has unknown successor " + node2);
                        continue;
                    }
                    if (compi2 == compi) compi.loop = true;
                    else {
                        if (compi.next.add(compi2.comp)) compi.next_vec
                            .add(compi2.comp);
                        if (compi2.prev.add(compi.comp)) compi2.prev_vec
                            .add(compi.comp);
                    }
                }
            }
        }
    }

    // Build the compressed format attached to each "fat" SCComponentInt.
    // This requires converting some sets to arrays (and sorting them in
    // the deterministic case).
    private static final void build_compressed_format() {
        for (int i = 0; i < scc_vector.size(); i++) {
            SCComponentInt compInt = (SCComponentInt) scc_vector.get(i);
            SCComponent comp = compInt.comp;
            comp.loop = compInt.loop;
            comp.nodes = new HashSet(compInt.nodes);
            comp.nodes_array = compInt.nodes.toArray(new Object[compInt.nodes
                .size()]);
            comp.next = (SCComponent[]) compInt.next_vec
                .toArray(new SCComponent[compInt.next_vec.size()]);
            comp.prev = (SCComponent[]) compInt.prev_vec
                .toArray(new SCComponent[compInt.prev_vec.size()]);
        }
    }

    public final void fillEntriesAndExits(Navigator nav) {
        int size = this.size();
        boolean isEntry[] = new boolean[size];
        boolean isExit[] = new boolean[size];
        int nb_entries = 0;
        int nb_exits = 0;
        for (int i = 0; i < size; i++) {
            Object node = nodes_array[i];
            Collection prev = nav.prev(node);
            for (Iterator j = prev.iterator(); j.hasNext();) {
                Object jnext = j.next();
                if (!nodes.contains(jnext)) {
                    isEntry[i] = true;
                    nb_entries++;
                    break;
                }
            }
            Collection next = nav.next(node);
            for (Iterator j = prev.iterator(); j.hasNext();) {
                Object jnext = j.next();
                if (!nodes.contains(jnext)) {
                    isExit[i] = true;
                    nb_exits++;
                    break;
                }
            }
        }
        entries = new Object[nb_entries];
        exits = new Object[nb_exits];
        for (int i = 0, pentries = 0, pexits = 0; i < size; i++) {
            if (isEntry[i]) entries[pentries++] = nodes_array[i];
            if (isExit[i]) exits[pexits++] = nodes_array[i];
        }
    }
    // HERE STARTS THE REAL (i.e. NON STATIC) CLASS
    private static int count = 0;
    private int id;

    /**
     * Returns the numeric ID of <code>this</code> <code>SCComponent</code>.
     * Just for debug purposes ...
     */
    public int getId() {
        return id;
    }
    // The nodes of this SCC (Strongly Connected Component).
    private Set nodes;
    private Object[] nodes_array;
    // The successors.
    private SCComponent[] next;
    // The predecessors.
    private SCComponent[] prev;
    // entries into this SCC
    private Object[] entries;
    private Object[] exits;
    // is there any arc to itself?
    private boolean loop;

    /**
     * Checks whether <code>this</code> strongly connected component
     * corresponds to a loop, <i>ie </i> it has at least one arc to itself.
     */
    public final boolean isLoop() {
        return loop;
    }

    //The only way to produce SCCs is through SCComponent.buildSCC !
    SCComponent() {
        id = count++;
    }

    SCComponent(int id) {
        this.id = id;
    }

    public int compareTo(Object o) {
        SCComponent scc2 = (SCComponent) o;
        int id2 = scc2.id;
        if (id < id2) return -1;
        if (id == id2) return 0;
        return 1;
    }

    /** Returns the number of successors. */
    public final int nextLength() {
        return next.length;
    }

    /** Returns the <code>i</code> th successor. */
    public final SCComponent next(int i) {
        return next[i];
    }

    public final SCComponent[] next() {
        return next;
    }

    /** Returns the number of predecessors. */
    public final int prevLength() {
        return prev.length;
    }

    /** Returns the <code>i</code> th predecessor. */
    public final SCComponent prev(int i) {
        return prev[i];
    }

    public final SCComponent[] prev() {
        return prev;
    }

    /**
     * Returns the nodes of <code>this</code> strongly connected component
     * (set version).
     */
    public final Set nodeSet() {
        return nodes;
    }

    /**
     * Returns the nodes of <code>this</code> strongly connected component;
     * array version - good for iterating over the elements of the SCC.
     */
    public final Object[] nodes() {
        return nodes_array;
    }

    /**
     * Returns the number of nodes in <code>this</code> strongly connected
     * component.
     */
    public final int size() {
        return nodes_array.length;
    }

    /**
     * Checks whether <code>node</code> belongs to <code>this</code>\
     * strongly connected component.
     */
    public final boolean contains(Object node) {
        return nodes.contains(node);
    }

    /**
     * Returns the entry nodes of <code>this</code> strongly connected
     * component. These are the nodes taht are reachable from outside the
     * component.
     */
    public final Object[] entries() {
        return entries;
    }

    /**
     * Returns the exit nodes of <code>this</code> strongly connected
     * component. These are the nodes that have arcs toward nodes outside the
     * component.
     */
    public final Object[] exits() {
        return exits;
    }
    // the next and prev links in the double linked list of SCCs in
    // decreasing topological order
    SCComponent nextTopSort = null;
    SCComponent prevTopSort = null;

    /**
     * Returns the next <code>SCComponent</code> according to the decreasing
     * topological order
     */
    public final SCComponent nextTopSort() {
        return nextTopSort;
    }

    /**
     * Returns the previous <code>SCComponent</code> according to the
     * decreasing topological order
     */
    public final SCComponent prevTopSort() {
        return prevTopSort;
    }

    /**
     * Returns the list of <code>SCComponent</code> s in topologically-sorted
     * order.
     */
    public final List/* <SCComponent> */listTopSort() {
        int n = 1;
        SCComponent c = this;
        while ((c = c.nextTopSort()) != null)
            ++n;
        SCComponent[] a = new SCComponent[n];
        c = this;
        for (int i = 0; i < n; ++i) {
            a[i] = c;
            c = c.nextTopSort();
        }
        Assert._assert(c == null);
        return Arrays.asList(a);
    }

    /** Pretty print debug function. */
    public final String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("SCC" + id + " (size " + size() + ") {");
        if (true) {
            for (int i = 0; i < nodes_array.length; i++) {
                if (i > 0) buffer.append(", ");
                buffer.append(nodes_array[i]);
            }
        }
        buffer.append("}");
        //buffer.append(prevStringRepr());
        //buffer.append(nextStringRepr());
        return buffer.toString();
    }

    // Returns a string representation of the "prev" links.
    private String prevStringRepr() {
        StringBuffer buffer = new StringBuffer();
        int nb_prev = prevLength();
        if (nb_prev > 0) {
            buffer.append("Prev:");
            for (int i = 0; i < nb_prev; i++) {
                buffer.append(" SCC" + prev(i).id);
            }
            buffer.append("\n");
        }
        return buffer.toString();
    }

    // Returns a string representation of the "next" links.
    private String nextStringRepr() {
        StringBuffer buffer = new StringBuffer();
        int nb_next = nextLength();
        if (nb_next > 0) {
            buffer.append("Next:");
            for (int i = 0; i < nb_next; i++) {
                buffer.append(" SCC" + next(i).id);
            }
            buffer.append("\n");
        }
        return buffer.toString();
    }
    public static final boolean DETERMINISTIC = true;

    public int hashCode() {
        if (false) {
            if (DETERMINISTIC) {
                int h = 0;
                if (nodes_array != null) {
                    for (int i = 0; i < nodes_array.length; ++i) {
                        Object o = nodes_array[i];
                        if (o != null) h ^= o.hashCode();
                    }
                }
                return h;
            } else {
                return System.identityHashCode(this);
            }
        } else {
            return id;
        }
    }
    
    public boolean equals(Object o) {
        return this == o;
    }
}
