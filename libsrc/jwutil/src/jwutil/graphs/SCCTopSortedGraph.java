// SCCTopSortedGraph.java, created Thu Mar 27 17:49:37 2003 by joewhaley
// Copyright (C) 2001-3 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.Serializable;
import jwutil.util.Assert;

/**
 * <code>SCCTopSortedGraph</code> represents a graph of strongly connected
 * components topologically sorted in decreasing order. To obtain such a graph,
 * use the <code>topSort</code> static method. It uses a Depth First Search to
 * do the sortting in linear time (see <code>Section 23.4</code> in Cormen and
 * co for the exact algorithm).
 * 
 * @author Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: SCCTopSortedGraph.java,v 1.3 2005/05/23 21:13:18 joewhaley Exp $
 */
public class SCCTopSortedGraph implements Graph, Serializable {
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3689912890060846388L;
    
    private SCComponent first;
    private SCComponent last;

    // the only way to obtain an object of this class is through topSort
    private SCCTopSortedGraph(SCComponent first, SCComponent last) {
        this.first = first;
        this.last = last;
    }

    /**
     * Returns the first (i.e. one of the topologically biggest)
     * <code>SCComponent</code>
     */
    public final SCComponent getFirst() {
        return first;
    }

    /**
     * Returns the last (i.e. one of the topologically smallest)
     * <code>SCComponent</code>
     */
    public final SCComponent getLast() {
        return last;
    }
    // data for the static method topSort
    private static Set reached_sccs;
    private static SCComponent first_scc;
    private static SCComponent last_scc;

    /**
     * Sorts all the strongly connected component reachable from
     * <code>root</code> in decreasing topological order. This method sets the
     * <code>nextTopSort</code> and <code>prevTopSort</code> fields of the
     * <code>SCComponent</code>s, arranging then in a double linked list
     * according to the aforementioned order. <br>
     * It returns a <code>SCCTopSortedGraph</code> containing the first and
     * the last elements of this list. <b>Note: </b> This is just a convenient
     * function, for more than one root, please use the more general
     * <code>topSort(Set)</code>.
     */
    public static SCCTopSortedGraph topSort(SCComponent root) {
        // sorting an empty component graph is really easy!
        if (root == null) return new SCCTopSortedGraph(null, null);
        return topSort(Collections.singleton(root));
    }

    /**
     * Sorts all the strongly connected component reachable from one of the
     * <code>SCComponent</code> s from <code>roots</code> in decreasing
     * topological order. This method sets the <code>nextTopSort</code> and
     * <code>prevTopSort</code> fields of the <code>SCComponent</code>s,
     * arranging then in a double linked list according to the aforementioned
     * order. <br>
     * It returns a <code>SCCTopSortedGraph</code> containing the first and
     * the last elements of this list. <b>Note: </b> the <code>roots</code>
     * parameter must contain only root <code>Sccomponent</code> s (ie
     * <code>SCComponent</code> s without any entering edge.
     */
    public static SCCTopSortedGraph topSort(Set roots) {
        // sorting an empty component graph is really easy!
        if (roots.isEmpty()) return new SCCTopSortedGraph(null, null);
        reached_sccs = new HashSet();
        // to facilitate insertions into the double linked list of SCCs,
        // a dummy node is created (now, we don't worry about
        // first_scc == null)
        last_scc = new SCComponent();
        first_scc = last_scc;
        // Depth First Search to sort the SCCs topologically
        Iterator it_sccs = roots.iterator();
        while (it_sccs.hasNext()) {
            SCComponent scc = (SCComponent) it_sccs.next();
            // TODO: eliminate this paranoic debug when the code is known
            // to be stable.
            Assert._assert(!reached_sccs.contains(scc),
                "The roots argument contains no-root sccs.");
            DFS_topsort(scc);
        }
        // get rid of the dummy node
        last_scc = last_scc.prevTopSort;
        last_scc.nextTopSort = null;
        SCCTopSortedGraph G = new SCCTopSortedGraph(first_scc, last_scc);
        reached_sccs = null; // enable the GC
        first_scc = null;
        last_scc = null;
        return G;
    }

    // the DFS used by the topological sort algorithm
    private static void DFS_topsort(SCComponent scc) {
        if (reached_sccs.contains(scc)) return;
        reached_sccs.add(scc);
        int nb_next = scc.nextLength();
        for (int i = 0; i < nb_next; i++)
            DFS_topsort(scc.next(i));
        // add scc at the front of the list of sorted SCC
        scc.nextTopSort = first_scc;
        first_scc.prevTopSort = scc;
        first_scc = scc;
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getRoots()
     */
    public Collection getRoots() {
        // Assumes single root.
        return Collections.singleton(first);
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getNavigator()
     */
    public Navigator getNavigator() {
        return SCComponent.SCC_NAVIGATOR;
    }

    public List list() {
        return first.listTopSort();
    }
}
