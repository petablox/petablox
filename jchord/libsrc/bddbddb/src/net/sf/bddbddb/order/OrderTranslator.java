// OrderTranslator.java, created Oct 24, 2004 12:17:57 AM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

/**
 * Translate from one order to another.  Used when orders have different names.
 * 
 * @author jwhaley
 * @version $Id: OrderTranslator.java 364 2004-10-31 14:13:49Z joewhaley $
 */
public interface OrderTranslator {
    
    /**
     * Translate the given order.  Always generates a new Order object, even if
     * the order does not change.
     * 
     * @param o  order
     * @return  translated order
     */
    Order translate(Order o);
    
    public static class Compose implements OrderTranslator {

        OrderTranslator t1, t2;
        
        public Compose(OrderTranslator t1, OrderTranslator t2) {
            this.t1 = t1; this.t2 = t2;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.order.OrderTranslator#translate(net.sf.bddbddb.order.Order)
         */
        public Order translate(Order o) {
            return t2.translate(t1.translate(o));
        }
        
    }
    
}
