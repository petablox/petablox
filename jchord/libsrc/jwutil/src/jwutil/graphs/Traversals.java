// Traversals.java, created Thu May 26 23:09:37 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jwutil.collections.Pair;
import jwutil.util.Assert;

/**
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Traversals.java,v 1.1 2004/09/27 22:42:31 joewhaley Exp $
 */
public abstract class Traversals {
    
    public static void test(Navigator nav, Collection roots) {
        HashSet visitedNodes = new HashSet();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            test_helper1(nav, o, visitedNodes);
        }
        visitedNodes.clear();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            test_helper2(nav, o, visitedNodes);
        }
    }
    public static void test_helper1(Navigator nav, Object o, Set m) {
        for (Iterator j=nav.next(o).iterator(); j.hasNext(); ) {
            Object p = j.next();
            if (!nav.prev(p).contains(o)) {
                Assert.UNREACHABLE(o+"->"+p);
            }
            if (m.add(p))
                test_helper1(nav, o, m);
        }
    }
    public static void test_helper2(Navigator nav, Object o, Set m) {
        for (Iterator j=nav.prev(o).iterator(); j.hasNext(); ) {
            Object p = j.next();
            if (!nav.next(p).contains(o)) {
                Assert.UNREACHABLE(p+"<-"+o);
            }
            if (m.add(p))
                test_helper2(nav, o, m);
        }
    }
    
    public static Set getAllEdges(Navigator nav, Collection roots) {
        HashSet visitedNodes = new HashSet();
        HashSet visitedEdges = new HashSet();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            getAllEdges_helper(nav, o, visitedNodes, visitedEdges);
        }
        return visitedEdges;
    }
    private static void getAllEdges_helper(Navigator nav, Object o, HashSet visitedNodes, HashSet visitedEdges) {
        if (visitedNodes.contains(o)) return;
        visitedNodes.add(o);
        for (Iterator j=nav.next(o).iterator(); j.hasNext(); ) {
            Object p = j.next();
            visitedEdges.add(new Pair(o, p));
            getAllEdges_helper(nav, p, visitedNodes, visitedEdges);
        }
    }
    
    public static Map buildPredecessorMap(Navigator nav, Collection roots) {
        HashMap m = new HashMap();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (m.containsKey(o)) continue; // if "roots" has a repeated element
            m.put(o, new HashSet());
            buildPredecessorMap_helper(nav, o, m);
        }
        return m;
    }
    public static void buildPredecessorMap_helper(Navigator nav, Object o, Map m) {
        for (Iterator j=nav.next(o).iterator(); j.hasNext(); ) {
            Object p = j.next();
            HashSet s = (HashSet) m.get(p);
            boolean visited;
            if (s == null) {
                m.put(p, s = new HashSet());
                visited = false;
            } else {
                visited = true;
            }
            s.add(o);
            if (!visited)
                buildPredecessorMap_helper(nav, p, m);
        }
    }
    
    public static Map buildSuccessorMap(Navigator nav, Collection roots) {
        HashMap m = new HashMap();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (m.containsKey(o)) continue; // if "roots" has a repeated element
            HashSet s;
            m.put(o, s = new HashSet());
            buildSuccessorMap_helper(nav, o, s, m);
        }
        return m;
    }
    public static void buildSuccessorMap_helper(Navigator nav, Object o, HashSet s, Map m) {
        for (Iterator j=nav.next(o).iterator(); j.hasNext(); ) {
            Object p = j.next();
            s.add(p);
            HashSet s2 = (HashSet) m.get(p);
            if (s2 != null) continue;
            m.put(p, s2 = new HashSet());
            buildSuccessorMap_helper(nav, p, s2, m);
        }
    }
    
    public static List preOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), PREORDER);
    }
    public static List preOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, PREORDER);
    }
    
    public static List reversePreOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), REVERSE_PREORDER);
    }
    public static List reversePreOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, REVERSE_PREORDER);
    }
    
    public static List inOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), INORDER);
    }
    public static List inOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, INORDER);
    }
    
    public static List reverseInOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), REVERSE_INORDER);
    }
    public static List reverseInOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, REVERSE_INORDER);
    }
    
    public static List postOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), POSTORDER);
    }
    public static List postOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, POSTORDER);
    }
    
    public static List reversePostOrder(Navigator nav, Object root) {
        return traversal_helper(nav, Collections.singleton(root), REVERSE_POSTORDER);
    }
    public static List reversePostOrder(Navigator nav, Collection roots) {
        return traversal_helper(nav, roots, REVERSE_POSTORDER);
    }
    
    private static final byte PREORDER          = 1;
    private static final byte REVERSE_PREORDER  = 2;
    private static final byte INORDER           = 3;
    private static final byte REVERSE_INORDER   = 4;
    private static final byte POSTORDER         = 5;
    private static final byte REVERSE_POSTORDER = 6;
    
    private static final List traversal_helper(Navigator nav, Collection roots,
                                               byte type) {
        HashSet visited = new HashSet();
        LinkedList result = new LinkedList();
        for (Iterator i=roots.iterator(); i.hasNext(); ) {
            Object root = i.next();
            traversal_helper(nav, root, visited, result, type);
        }
        return result;
    }
    
    /** Helper function to compute reverse post order. */
    private static final void traversal_helper(
        Navigator nav,
        Object node,
        HashSet visited,
        LinkedList result,
        byte type) {
        if (visited.contains(node)) return; visited.add(node);
        if (type == PREORDER) result.add(node);
        else if (type == REVERSE_PREORDER) result.addFirst(node);
        Collection bbs = nav.next(node);
        Iterator bbi = bbs.iterator();
        while (bbi.hasNext()) {
            Object node2 = bbi.next();
            traversal_helper(nav, node2, visited, result, type);
            if (type == INORDER) {
                result.add(node);
                type = 0;
            } else if (type == REVERSE_INORDER) {
                result.addFirst(node);
                type = 0;
            }
        }
        if (type == POSTORDER) result.add(node);
        else if (type == REVERSE_POSTORDER) result.addFirst(node);
    }
    
}
