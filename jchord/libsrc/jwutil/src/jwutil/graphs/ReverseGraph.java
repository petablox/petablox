// ReverseGraph.java, created Mar 22, 2004 1:57:53 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;

/**
 * ReverseGraph
 * 
 * @author jwhaley
 * @version $Id: ReverseGraph.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class ReverseGraph implements Graph {
    
    public ReverseGraph(Graph g, Collection newRoots) {
        _graph = g;
        _nav = new ReverseNavigator(g.getNavigator());
        _roots = newRoots;
    }
    
    private Graph _graph;
    private Navigator _nav;
    private Collection _roots;
    
    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getRoots()
     */
    public Collection getRoots() {
        return _roots;
    }
    
    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getNavigator()
     */
    public Navigator getNavigator() {
        return _nav;
    }
    
    public Graph getGraph() { return _graph; }
    
}
