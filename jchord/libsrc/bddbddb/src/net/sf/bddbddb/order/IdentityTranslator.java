// IdentityTranslator.java, created Oct 24, 2004 12:19:15 AM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.LinkedList;

/**
 * The identity translation.
 * 
 * @author jwhaley
 * @version $Id: IdentityTranslator.java 435 2005-02-13 03:24:59Z cs343 $
 */
public class IdentityTranslator implements OrderTranslator {
    
    /**
     * Singleton instance.
     */
    public static final IdentityTranslator INSTANCE = new IdentityTranslator();
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.FindBestDomainOrder.OrderTranslator#translate(net.sf.bddbddb.FindBestDomainOrder.Order)
     */
    public Order translate(Order o) { return new Order(new LinkedList(o)); }
    
}
