// VarToAttribTranslator.java, created Oct 27, 2004 2:26:29 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Variable;

/**
 * VarToAttribTranslator
 * 
 * @author jwhaley
 * @version $Id: VarToAttribTranslator.java 364 2004-10-31 14:13:49Z joewhaley $
 */
public class VarToAttribTranslator implements OrderTranslator {
    
    InferenceRule ir;
    
    public VarToAttribTranslator(InferenceRule ir) {
        this.ir = ir;
    }
    
    public Order translate(Order o) {
        if (FindBestDomainOrder.TRACE > 3) System.out.print("Translating "+o);
        LinkedList result = new LinkedList();
        for (Iterator i = o.iterator(); i.hasNext(); ) {
            Object a = i.next();
            if (a instanceof Collection) {
                Collection result2 = new LinkedList();
                for (Iterator j = ((Collection) a).iterator(); j.hasNext(); ) {
                    Variable a2 = (Variable) j.next();
                    Attribute b2 = ir.getAttribute(a2);
                    if (b2 != null) result2.add(b2);
                }
                if (result2.size() > 1) {
                    result.add(result2);
                } else if (!result2.isEmpty()) {
                    result.add(result2.iterator().next());
                }
            } else {
                Attribute b = ir.getAttribute((Variable) a);
                if (b != null) result.add(b);
            }
        }
        if (FindBestDomainOrder.TRACE > 3) System.out.println(" -> "+result);
        return new Order(result);
    }
    
}
