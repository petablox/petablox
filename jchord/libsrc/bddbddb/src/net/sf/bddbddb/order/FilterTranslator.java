// FilterTranslator.java, created Oct 31, 2004 3:04:05 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * FilterTranslator
 * 
 * @author jwhaley
 * @version $Id: FilterTranslator.java 446 2005-02-24 01:03:29Z cs343 $
 */
public class FilterTranslator implements OrderTranslator {
    
    Collection c;
    
    /**
     * 
     */
    public FilterTranslator(Collection c) {
        this.c = c;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.order.OrderTranslator#translate(net.sf.bddbddb.order.Order)
     */
    public Order translate(Order o) {
        LinkedList result = new LinkedList();
        Collection cCopy = new LinkedList(c);
        for (Iterator i = o.iterator(); i.hasNext(); ) {
            Object a = i.next();
            if (a instanceof Collection) {
                Collection result2 = new LinkedList();
                for (Iterator j = ((Collection) a).iterator(); j.hasNext(); ) {
                    Object a2 = j.next();
                    if (cCopy.contains(a2)){
                        result2.add(a2);
                        cCopy.remove(a2);
                    }
                }
                if (result2.size() > 1) {
                    result.add(result2);
                } else if (!result2.isEmpty()) {
                    result.add(result2.iterator().next());
                }
            } else {
                if (cCopy.contains(a)){
                    result.add(a);
                    cCopy.remove(a);
                }
            }
        }
        return new Order(result);
    }
    public String toString(){
       return "FilterTranslator: " + c;
    }
}
