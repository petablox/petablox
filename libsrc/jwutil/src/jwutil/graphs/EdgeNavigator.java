// EdgeNavigator.java, created Jun 15, 2003 3:55:42 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.Collections;

import jwutil.collections.Filter;
import jwutil.collections.Pair;
import jwutil.collections.WrappedCollection;

/**
 * This navigator is used as a wrapper for another navigator.  It adds an
 * "edge" node for each edge in the original graph.  The edge nodes are
 * Pair objects.
 * 
 * @author John Whaley
 * @version $Id: EdgeNavigator.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class EdgeNavigator implements Navigator {

    protected final Navigator navigator;

    /** Construct a new EdgeNavigator for the given navigator.
     * 
     * @param n navigator
     */
    public EdgeNavigator(Navigator n) {
        this.navigator = n;
    }

    public static class AddLeftFilter extends Filter {
        Object left;
        public AddLeftFilter(Object o) { this.left = o; }
        public Object map(Object o) {
            return new Pair(left, o);
        }
    }

    public static class AddRightFilter extends Filter {
        Object right;
        public AddRightFilter(Object o) { this.right = o; }
        public Object map(Object o) {
            return new Pair(o, right);
        }
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Navigator#next(java.lang.Object)
     */
    public Collection next(Object node) {
        if (node instanceof Pair) {
            Pair p = (Pair) node;
            return Collections.singleton(p.right);
        } else {
            return new WrappedCollection(navigator.next(node), null, new AddLeftFilter(node));
        }
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Navigator#prev(java.lang.Object)
     */
    public Collection prev(Object node) {
        if (node instanceof Pair) {
            Pair p = (Pair) node;
            return Collections.singleton(p.left);
        } else {
            return new WrappedCollection(navigator.prev(node), null, new AddRightFilter(node));
        }
    }

}
