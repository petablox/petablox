// EdgeGraph.java, created Jun 15, 2003 7:14:10 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;

/**
 * EdgeGraph
 * 
 * @author John Whaley
 * @version $Id: EdgeGraph.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class EdgeGraph implements Graph {

    Graph g;

    public EdgeGraph(Graph g) { this.g = g; }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getRoots()
     */
    public Collection getRoots() {
        return g.getRoots();
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.Graph#getNavigator()
     */
    public Navigator getNavigator() {
        return new EdgeNavigator(g.getNavigator());
    }

    public Graph getGraph() { return g; }

}
