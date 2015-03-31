// ReverseNavigator.java, created Aug 16, 2003 1:14:47 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;

/**
 * ReverseNavigator uses a supplied navigator and traverses the graph in reverse.
 * 
 * @author John Whaley
 * @version $Id: ReverseNavigator.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class ReverseNavigator implements Navigator {

    private final Navigator navigator;

    public ReverseNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Navigator#next(java.lang.Object)
     */
    public Collection next(Object node) {
        return navigator.prev(node);
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Navigator#prev(java.lang.Object)
     */
    public Collection prev(Object node) {
        return navigator.next(node);
    }

}
