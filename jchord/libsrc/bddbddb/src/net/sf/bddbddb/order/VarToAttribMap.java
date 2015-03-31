// VarToAttribMap.java, created Oct 31, 2004 1:43:04 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.order;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import jwutil.collections.UnmodifiableIterator;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.Variable;

/**
 * VarToAttribMap
 * 
 * @author jwhaley
 * @version $Id: VarToAttribMap.java 446 2005-02-24 01:03:29Z cs343 $
 */
public class VarToAttribMap extends AbstractMap {
    
    public static Collection convert(Collection vars, InferenceRule ir) {
        Collection result = new LinkedList();
        for (Iterator i = vars.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            Attribute a = (Attribute) ir.getAttribute(v);
            if (a != null) result.add(a);
        }
        return result;
    }
    
    InferenceRule ir;
    
    /**
     * 
     */
    public VarToAttribMap(InferenceRule ir) {
        this.ir = ir;
    }

    public Object get(Object key) {
        Variable v = (Variable) key;
        Attribute a = ir.getAttribute(v);
        return a;
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        return new MapEntrySet(ir);
    }
    
    static class MapEntrySet extends AbstractSet {

        InferenceRule ir;
        
        MapEntrySet(InferenceRule ir) {
            this.ir = ir;
        }
        
        public Iterator iterator() {
            return new UnmodifiableIterator() {
                Iterator i = ir.getNecessaryVariables().iterator();
                Iterator j = ir.getUnnecessaryVariables().iterator();
                
                public boolean hasNext() {
                    return i.hasNext() || j.hasNext();
                }

                public Object next() {
                    Variable v;
                    if (i.hasNext()) v = (Variable) i.next();
                    else v = (Variable) j.next();
                    return new MapEntry(v, ir);
                }
                
            };
        }

        public int size() {
            return ir.numberOfVariables();
        }
        
    };

    
    static class MapEntry implements AbstractMap.Entry {

        Variable v;
        InferenceRule ir;
        
        MapEntry(Variable v, InferenceRule ir) {
            this.v = v;
            this.ir = ir;
        }
        
        /* (non-Javadoc)
         * @see java.util.Map.Entry#getKey()
         */
        public Object getKey() {
            return v;
        }

        /* (non-Javadoc)
         * @see java.util.Map.Entry#getValue()
         */
        public Object getValue() {
            return ir.getAttribute(v);
        }

        /* (non-Javadoc)
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        public Object setValue(Object arg0) {
            throw new UnsupportedOperationException();
        }
        
    }
}
