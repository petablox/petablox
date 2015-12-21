// MapBasedTranslator.java, created Oct 24, 2004 12:19:42 AM by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.FindBestDomainOrder;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.RuleTerm;
import net.sf.bddbddb.Variable;

/**
 * Translator based on a map.
 * 
 * @author jwhaley
 * @version $Id: MapBasedTranslator.java 448 2005-03-07 06:58:48Z cs343 $
 */
public class MapBasedTranslator implements OrderTranslator {
    
    Map m;
    
    public MapBasedTranslator(Map m) {
        this.m = m;
    }
    
    /**
     * direction == true means map from rule to relation.
     * direction == false means map from relation to rule.
     * 
     * @param ir  rule
     * @param r  relation
     * @param direction  true=rule->relation, false=relation->rule
     */
    public MapBasedTranslator(InferenceRule ir, Relation r, boolean direction) {
        m = new HashMap();
        for (Iterator i = ir.getSubgoals().iterator(); i.hasNext(); ) {
            RuleTerm rt = (RuleTerm) i.next();
            if (rt.getRelation() != r) continue;
            int rsize = r.getAttributes().size();
            Assert._assert(rsize == rt.getVariables().size());
            for (int j = 0; j < rsize; ++j) {
                Attribute a = (Attribute) r.getAttribute(j);
                Variable v = (Variable) rt.getVariable(j);
                // Note: this doesn't match exactly if a relation appears
                // twice in a rule.
                if (direction)
                    m.put(v, a);
                else
                    m.put(a, v);
            }
        }
    }
    
    /**
     * direction == true means map from ruleterm to relation.
     * direction == false means map from relation to ruleterm.
     * 
     * @param rt  ruleterm
     * @param direction  true=ruleterm->relation, false=relation->ruleterm
     */
    public MapBasedTranslator(RuleTerm rt, boolean direction) {
        m = new HashMap();
        Relation r = rt.getRelation();
        int rsize = r.getAttributes().size();
        Assert._assert(rsize == rt.getVariables().size());
        for (int j = 0; j < rsize; ++j) {
            Attribute a = (Attribute) r.getAttribute(j);
            Variable v = (Variable) rt.getVariable(j);
            if (direction)
                m.put(v, a);
            else
                m.put(a, v);
        }
    }
    
    /* (non-Javadoc)
     * @see net.sf.bddbddb.FindBestDomainOrder.OrderTranslator#translate(net.sf.bddbddb.FindBestDomainOrder.Order)
     */
    public Order translate(Order o) {
        if (FindBestDomainOrder.TRACE > 3) System.out.print("Translating "+o);
        LinkedList result = new LinkedList();
        for (Iterator i = o.iterator(); i.hasNext(); ) {
            Object a = i.next();
            if (a instanceof Collection) {
                Collection result2 = new LinkedList();
                for (Iterator j = ((Collection) a).iterator(); j.hasNext(); ) {
                    Object a2 = j.next();
                    Object b2 = m.get(a2);
                    if (b2 != null) result2.add(b2);
                }
                if (result2.size() > 1) {
                    result.add(result2);
                } else if (!result2.isEmpty()) {
                    result.add(result2.iterator().next());
                }
            } else {
                Object b = m.get(a);
                if (b != null) result.add(b);
            }
        }
        if (FindBestDomainOrder.TRACE > 3) System.out.println(" -> "+result);
        return new Order(result);
    }
    
}
