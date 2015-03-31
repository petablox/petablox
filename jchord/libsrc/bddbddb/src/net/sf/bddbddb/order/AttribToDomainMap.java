// AttribToDomainMap.java, created Oct 31, 2004 1:57:38 AM by joewhaley
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
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;

/**
 * AttribToDomainMap
 * 
 * @author jwhaley
 * @version $Id: AttribToDomainMap.java 502 2005-04-13 00:26:23Z joewhaley $
 */
public class AttribToDomainMap extends AbstractMap {
    
    public static Collection convert(Collection attribs) {
        Collection result = new LinkedList();
        for (Iterator i = attribs.iterator(); i.hasNext(); ) {
            Attribute a = (Attribute) i.next();
            result.add(a.getDomain());
        }
        return result;
    }
    
    Solver s;
    transient Collection allAttribs;
    
    /**
     * 
     */
    public AttribToDomainMap(Solver s) {
        this.s = s;
    }

    public Object get(Object o) {
        Attribute a = (Attribute) o;
        return a.getDomain();
    }
    
    private void buildAttribs() {
        allAttribs = new LinkedList();
        for (Iterator i = s.getRelations().iterator(); i.hasNext(); ) {
            Relation r = (Relation) i.next();
            allAttribs.addAll(r.getAttributes());
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        if (allAttribs == null) buildAttribs();
        return new AbstractSet() {
            public Iterator iterator() {
                final Iterator i = allAttribs.iterator();
                return new UnmodifiableIterator() {
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public Object next() {
                        Attribute a = (Attribute) i.next();
                        return a.getDomain();
                    }
                };
            }
            public int size() {
                return allAttribs.size();
            }
        };
    }
}
