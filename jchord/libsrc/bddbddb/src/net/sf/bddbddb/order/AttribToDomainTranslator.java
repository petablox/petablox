// AttribToDomainTranslator.java, created Oct 31, 2004 1:39:40 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import net.sf.bddbddb.Attribute;

/**
 * AttribToDomainTranslator
 * 
 * @author jwhaley
 * @version $Id: AttribToDomainTranslator.java 364 2004-10-31 14:13:49Z joewhaley $
 */
public class AttribToDomainTranslator implements OrderTranslator {
    
    public static final AttribToDomainTranslator INSTANCE =
        new AttribToDomainTranslator();
    
    /**
     * 
     */
    private AttribToDomainTranslator() { }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.order.OrderTranslator#translate(net.sf.bddbddb.order.Order)
     */
    public Order translate(Order o) {
        LinkedList result = new LinkedList();
        for (Iterator i = o.iterator(); i.hasNext(); ) {
            Object a = i.next();
            if (a instanceof Collection) {
                Collection result2 = new LinkedList();
                for (Iterator j = ((Collection) a).iterator(); j.hasNext(); ) {
                    Attribute a2 = (Attribute) j.next();
                    result2.add(a2.getDomain());
                }
                if (result2.size() > 1) {
                    result.add(result2);
                } else if (!result2.isEmpty()) {
                    result.add(result2.iterator().next());
                }
            } else {
                Attribute b = (Attribute) a;
                result.add(b.getDomain());
            }
        }
        return new Order(result);
    }
}
