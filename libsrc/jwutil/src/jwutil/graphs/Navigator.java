// Navigator.java, created Thu Mar 27 17:49:37 2003 by joewhaley
// Copyright (C) 2001-3 Alexandru SALCIANU <salcianu@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;

/**
 * The <code>Navigator</code> interface allows graph algorithms to detect (and
 * use) the arcs from and to a certain node. This allows the use of many graph
 * algorithms (eg construction of strongly connected components) even for very
 * general graphs where the arcs model only a subtle semantic relation (eg
 * caller-callee) that is not directly stored in the structure of the nodes.
 * 
 * @author Alexandru SALCIANU <salcianu@alum.mit.edu>
 * @version $Id: Navigator.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public interface Navigator {
    /** Returns the successors of <code>node</code>. */
    public Collection next(Object node);

    /** Returns the predecessors of <code>node</code>. */
    public Collection prev(Object node);
}
