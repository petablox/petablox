/*
 * Created on Nov 25, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package jwutil.graphs;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author jwhaley
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RootPathNumbering extends PathNumbering {
    
    public static final boolean TRACE_NUMBERING = true;

    Range global;
    Map iMap;
    
    public BigInteger countPaths(Collection roots, Navigator navigator, Map initialMap) {
        this.iMap = new HashMap();
        int num_roots = 1;
        for (Iterator i = initialMap.entrySet().iterator(); i.hasNext(); ) {
            Object o = i.next();
            Map.Entry e = (Map.Entry) o;
            Number a = (Number) e.getValue();
            int val = a.intValue();
            Range r = new Range(num_roots, num_roots + val);
            this.iMap.put(e.getKey(), r);
            if (TRACE_NUMBERING) System.out.println(e.getKey()+" has range "+r);
            num_roots += val+1;
        }
        
        this.global = new Range(0, num_roots-1);
        if (TRACE_NUMBERING) System.out.println("Others have range "+global);
        
        BigInteger max_paths = BigInteger.valueOf(num_roots);
        return max_paths;
    }
    
    public Range getRange(Object o) {
        if (iMap.containsKey(o)) {
            return (Range) iMap.get(o);
        }
        return global;
    }
    
    public Range getEdge(Object from, Object to) {
        //if (iMap.containsKey(from)) {
        //    return (Range) iMap.get(from);
        //}
        return global;
    }
    
}
