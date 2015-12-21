// DominanceFrontier.java, created Aug 18, 2003 12:27:30 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * DominanceFrontier computes and manages dominance frontiers.
 * Based on some code originally due to the Scale compiler group at UMass.
 * 
 * @author John Whaley
 * @version $Id: DominanceFrontier.java,v 1.3 2005/05/28 10:24:54 joewhaley Exp $
 */
public class DominanceFrontier {

    /**
     * The actual dominance frontiers.
     */
    private Map/*<Object,Set>*/ frontiers;
    
    /**
     * The dominator relation.
     */
    private Dominators dom;
    
    /**
     * The root of the graph.
     */
    private Object begin;

    /**
     * The navigator for the graph.
     */
    private Navigator navigator;

    /**
     * @param begin  root node of the graph (or end node for post-dominators)
     * @param dom  (post-)dominators
     */
    public DominanceFrontier(Object begin, Navigator nav, Dominators dom) {
        this.dom = dom;
        this.begin = begin;
        this.navigator = dom.isPostDominators()?new ReverseNavigator(nav):nav;
        this.frontiers = new HashMap();

        computeDominanceFrontier();
    }

    /**
     * Return the set of all of the nodes on the dominance frontier of the given node.
     * @param bb the node for which the dominance frontier is requested.
     */
    public Set getDominanceFrontier(Object bb) {
        Object df = frontiers.get(bb);
        if (df == null)
            return Collections.EMPTY_SET;

        if (df instanceof Set)
            return (Set) df;

        return Collections.singleton(df);
    }
    
    /**
     * Return the iterated dominance frontier of the given node.
     */
    public Set getIteratedDominanceFrontier(Object node) {
        Set idf = new HashSet();
        //idf.add(node);
        Set diff = new HashSet();
        diff.add(node);
        
        do {
            Set newDiff = new HashSet(); 
            for(Iterator iter = diff.iterator(); iter.hasNext();){
                Object diffNode = iter.next();
                for(Iterator newIter = getDominanceFrontier(diffNode).iterator(); newIter.hasNext();){
                    Object newNode = newIter.next();
                    if(!idf.contains(newNode)){
                        newDiff.add(newNode);
                        idf.add(newNode);
                    }
                }
            }
            diff = newDiff;
        } while(!diff.isEmpty());
        
        return idf;
    }

    /**
     * Return true if b2 is in b1's dominance frontier.
     */
    public boolean inDominanceFrontier(Object b1, Object b2) {
        Object df = frontiers.get(b1);
        if (df == null)
            return false;
        if (df instanceof Set)
            return ((Set) df).contains(b2);
        return (df == b2);
    }

    private Object addDf(Object set, Object newmem) {
        if (set == null)
            return newmem;
        if (set instanceof Set) {
            ((Set) set).add(newmem);
            return set;
        }
        Set ns = new HashSet(3);
        ns.add(set);
        ns.add(newmem);
        return ns;
    }

    /**
     * Compute the dominance frontier of the graph.  The dominators must be
     * computed first.  The algorithm first goes depth-first on the dominator tree,
     * pushing children on a stack.  This insures that children in the dominator
     * tree are popped off the stack before their parents.  Then, once this stack is
     * created, dominators are popped off and processed.  Since the dominance frontier of
     * a dominator is the union of the dominance frontiers of its children and those 
     * immediate children in the graph that it doesn't dominate, the first
     * have been calculated already and the last can be computed directly.
     * @see jwutil.graphs.Dominators
     */
    private void computeDominanceFrontier() {
        LinkedList wl = new LinkedList();
        LinkedList tree = new LinkedList();

        wl.add(begin);

        while (!wl.isEmpty()) {
            // Make sure children are processed before parents
            Object n = wl.removeLast();
            tree.add(n);
            Iterator ed = dom.getDominatees(n);
            while (ed.hasNext()) {
                Object c = ed.next();
                tree.add(c);
                wl.add(c);
            }
        }

        while (!tree.isEmpty()) {
            Object n = tree.removeLast();
            Object S = null;

            // Determine those immediate children in the CFG who are not dominated by n.
            for (Iterator i=navigator.next(n).iterator(); i.hasNext(); ) {
                Object y = i.next();
                if (n != dom.getDominatorOf(y))
                    S = addDf(S, y);
            }

            // Union in the dominance frontiers of n's children in the dominator tree.
            Iterator ed = dom.getDominatees(n);
            while (ed.hasNext()) {
                Object c = ed.next();
                Iterator ec = getDominanceFrontier(c).iterator();
                // The child is done first!
                while (ec.hasNext()) {
                    Object w = ec.next();
                    if (n != dom.getDominatorOf(w)) {
                        S = addDf(S, w);
                    }
                }
            }

            if (S != null)
                frontiers.put(n, S);
        }

    }

}
