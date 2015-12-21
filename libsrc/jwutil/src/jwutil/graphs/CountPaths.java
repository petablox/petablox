// CountPaths.java, created Jul 13, 2003 1:23:33 PM by John Whaley
// Copyright (C) 2003 John Whaley
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import jwutil.util.Assert;

/**
 * CountPaths
 * 
 * @author John Whaley
 * @version $Id: CountPaths.java,v 1.2 2005/05/05 19:00:43 joewhaley Exp $
 */
public class CountPaths {

    public static long countPaths(Graph g) {
        return countPaths(g.getNavigator(), g.getRoots());
    }

    public static long countPaths(Navigator nav, Collection roots) {
        HashMap counts = new HashMap();
        List list = Traversals.reversePostOrder(nav, roots);
        long max = 0L;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            long myValue = 0L;
            for (Iterator j = nav.prev(o).iterator(); j.hasNext(); ) {
                Object p = j.next();
                long[] d = (long[]) counts.get(p);
                Assert._assert(list.contains(p));
                if (d == null) continue;
                myValue += d[0];
            }
            if (myValue == 0L) myValue = 1L;
            counts.put(o, new long[] { myValue });
            max = Math.max(max, myValue);
        }
        return max;
    }
    
    public static long countPaths(Graph g, int k) {
        return countPaths(g.getNavigator(), g.getRoots(), k);
    }
    
    public static long countPaths(Navigator nav, Collection roots, int k) {
        HashMap counts = new HashMap();
        List list = Traversals.reversePostOrder(nav, roots);
        long max = 0L;
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            Object o = i.next();
            long[] myValue = new long[k+1];
            Collection prev = nav.prev(o);
            max = Math.max(max, myValue[0]);
            for (Iterator j = prev.iterator(); j.hasNext(); ) {
                Object p = j.next();
                long[] d = (long[]) counts.get(p);
                Assert._assert(list.contains(p));
                if (d == null) {
                    continue;
                }
                for (int a=1; a<k+1; ++a) {
                    myValue[a] += d[a-1];
                }
            }
            for (int a=0; a<k+1; ++a) {
                if (myValue[a] == 0L) myValue[a] = 1L;
                max = Math.max(max, myValue[a]);
            }
            counts.put(o, myValue);
        }
        return max;
    }
    
}
