// GlobalPathNumbering.java, created Aug 4, 2004 8:56:01 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.math.BigInteger;
import jwutil.collections.Pair;
import jwutil.util.Assert;

/**
 * GlobalPathNumbering
 * 
 * @author jwhaley
 * @version $Id: GlobalPathNumbering.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public class GlobalPathNumbering extends PathNumbering {
    
    public GlobalPathNumbering() {
    }
    
    public GlobalPathNumbering(Selector s) {
        this.selector = s;
    }
    
    Selector selector;
    
    /** Navigator for the graph. */
    Navigator navigator;
    
    /** Map from nodes to numbers. */
    Map nodeNumbering = new HashMap();
    
    /** Map from edges to ranges. */
    Map edgeNumbering = new HashMap();
    
    /* (non-Javadoc)
     * @see jwutil.graphs.PathNumbering#countPaths(java.util.Collection, jwutil.graphs.Navigator, java.util.Map)
     */
    public BigInteger countPaths(Collection roots, Navigator navigator, Map initialMap) {
        for (Iterator i = roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            BigInteger total = BigInteger.ONE;
            if (initialMap != null)
                total = toBigInt((Number) initialMap.get(o));
            nodeNumbering.put(o, total);
            Assert._assert(!total.equals(BigInteger.ZERO), o.toString());
        }
        BigInteger max = BigInteger.ZERO;
        Iterator rpo = Traversals.reversePostOrder(navigator, roots).iterator();
        while (rpo.hasNext()) {
            Object o = rpo.next();
            BigInteger pathsToNode = (BigInteger) nodeNumbering.get(o);
            if (pathsToNode == null) {
                // This node is not a root.
                pathsToNode = BigInteger.ONE;
            }
            Collection prev = navigator.prev(o);
            for (Iterator i = prev.iterator(); i.hasNext(); ) {
                Object p = i.next();
                BigInteger pathsToPred = (BigInteger) nodeNumbering.get(p);
                if (pathsToPred == null) {
                    // We haven't visited this predecessor yet.
                    // Because this is topological, it must be a loop edge.
                    //System.out.println("Loop edge: "+p+" -> "+o+" current target num="+val);
                    pathsToPred = BigInteger.ONE;
                }
                BigInteger newPathsToNode = pathsToNode.add(pathsToPred);
                Object edge = new Pair(p, o);
                if (!isImportant(p, o, newPathsToNode)) {
                    // Unimportant edge.
                    Range range = new Range(pathsToNode.subtract(BigInteger.ONE),
                                            pathsToNode.subtract(BigInteger.ONE));
                    edgeNumbering.put(edge, range);
                    //System.out.println("Putting unimportant Edge ("+edge+") = "+range);
                } else {
                    Range range = new Range(pathsToNode.subtract(BigInteger.ONE),
                                            newPathsToNode.subtract(BigInteger.valueOf(2)));
                    edgeNumbering.put(edge, range);
                    //System.out.println("Putting important Edge ("+edge+") = "+range);
                    pathsToNode = newPathsToNode;
                }
            }
            //Assert._assert(!val.equals(BigInteger.ZERO), o.toString());
            if (!prev.isEmpty()) pathsToNode = pathsToNode.subtract(BigInteger.ONE);
            nodeNumbering.put(o, pathsToNode);
            if (pathsToNode.compareTo(max) > 0) max = pathsToNode;
        }
        return max;
    }

    public boolean isImportant(Object a, Object b, BigInteger num) {
        if (selector == null) return true;
        return selector.isImportant(a, b, num);
    }
    
    /* (non-Javadoc)
     * @see jwutil.graphs.PathNumbering#getRange(java.lang.Object)
     */
    public Range getRange(Object o) {
        BigInteger b = (BigInteger) nodeNumbering.get(o);
        if (b == null) return null;
        return new Range(BigInteger.ZERO, b.subtract(BigInteger.ONE));
    }

    /* (non-Javadoc)
     * @see jwutil.graphs.PathNumbering#getEdge(java.lang.Object, java.lang.Object)
     */
    public Range getEdge(Object from, Object to) {
        Object key = new Pair(from, to);
        Range r = (Range) edgeNumbering.get(key);
        //System.out.println("Edge ("+key+") = "+r);
        return r;
    }
    
}
