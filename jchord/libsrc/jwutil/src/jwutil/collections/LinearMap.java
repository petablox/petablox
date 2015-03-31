// LinearMap.java, created May 5, 2004 8:41:20 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import jwutil.util.Assert;

/**
 * LinearMap
 * 
 * @author jwhaley
 * @version $Id: LinearMap.java,v 1.3 2005/04/29 02:32:24 joewhaley Exp $
 */
public class LinearMap extends AbstractMap {

    protected final List keys;
    protected final List values;
    
    public LinearMap(List k, List v) {
        this.keys = k;
        this.values = v;
        Assert._assert(k.size() == v.size());
    }
    
    public LinearMap(int size) {
        this(new ArrayList(size), new ArrayList(size));
    }
    
    public LinearMap() {
        this(16);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        keys.clear();
        values.clear();
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object arg0) {
        return keys.contains(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object arg0) {
        return values.contains(arg0);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object arg0) {
        int i = keys.indexOf(arg0);
        if (i == -1) return null;
        return values.get(i);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        return new LinearSet(ListFactory.arrayListFactory, keys);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object arg0, Object arg1) {
        int i = keys.indexOf(arg0);
        if (i != -1) return values.set(i, arg1); 
        keys.add(arg0);
        values.add(arg1);
        return null;
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object arg0) {
        int i = keys.indexOf(arg0);
        if (i == -1) return null;
        keys.remove(i);
        return values.remove(i);
    }
    
    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection values() {
        return values;
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractMap#entrySet()
     */
    public Set entrySet() {
        return new EntrySet();
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        return keys.size();
    }
    
    class EntrySet extends AbstractSet {

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#iterator()
         */
        public Iterator iterator() {
            return new EntryIterator();
        }

        /* (non-Javadoc)
         * @see java.util.AbstractCollection#size()
         */
        public int size() {
            return LinearMap.this.size();
        }
        
    }
    
    class EntryIterator implements Iterator {

        int k = 0;
        
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return k < size();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return new Entry(k++);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            --k;
            keys.remove(k);
            values.remove(k);
        }
        
    }
    
    class Entry extends AbstractMapEntry {
        int k;

        Entry(int k) {
            this.k = k;
        }
        
        /* (non-Javadoc)
         * @see java.util.Map.Entry#getKey()
         */
        public Object getKey() {
            return keys.get(k);
        }

        /* (non-Javadoc)
         * @see java.util.Map.Entry#getValue()
         */
        public Object getValue() {
            return values.get(k);
        }

        /* (non-Javadoc)
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        public Object setValue(Object arg0) {
            return values.set(k, arg0);
        }
        
    }
    
}
