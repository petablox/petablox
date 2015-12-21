// LoopStructureGraph.java, created Mar 25, 2004 8:06:41 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.collections.UnionFind;

/**
 * LoopStructureGraph
 * 
 * Implementation still incomplete.
 * 
 * @author jwhaley
 * @version $Id: LoopStructureGraph.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class LoopStructureGraph {
    
    UnionFind LP;
    UnionFind RLH;
    Map loopParent;
    MultiMap crossFwdEdges;
    
    Set irreducibleLoopHeader;
    
    void findloop(Object potentialHeader) {
        Set loopBody = new HashSet();
        List worklist = new LinkedList();
        
    }
    
    void markIrreducibleLoops(Object z) {
        irreducibleLoopHeader = new HashSet();
        Object t = loopParent.get(z);
        while (t != null) {
            Object u = RLH.find(t);
            irreducibleLoopHeader.add(u);
            t = loopParent.get(u);
            if (t != null) RLH.union(u, t);
        }
    }
    
    void processCrossFwdEdges(Object x) {
        for (Iterator i = crossFwdEdges.getValues(x).iterator(); i.hasNext(); ) {
            Pair p = (Pair) i.next();
            Object y = p.left;
            Object z = p.right;
            Object find_y = LP.find(y);
            Object find_z = LP.find(z);
            // add edge find_y -> find_z to graph
            markIrreducibleLoops(z);
        }
    }
    
    void ModifiedHavlakAlgorithm(Collection vertices, Navigator nav) {
        LP = new UnionFind(vertices.size());
        RLH = new UnionFind(vertices.size());
        loopParent = new HashMap();
        crossFwdEdges = new GenericMultiMap();
        for (Iterator i = vertices.iterator(); i.hasNext(); ) {
            Object x = i.next();
            LP.add(x); RLH.add(x);
        }
        // for every forward edge and cross edge y -> x of G do
        //    remove y -> x from G and add it to crossFwdEdges[least-common-ancestor(y,x)]
    
        // for every vertex x of G in reverse-DFS-order do
        //    processCrossFwdEdges(x);
        //    findloop(x);
        
    }
    
}
