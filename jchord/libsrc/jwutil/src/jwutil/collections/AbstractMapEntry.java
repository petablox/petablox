// AbstractMapEntry.java, created Tue Feb 23 16:34:46 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Map;
/**
 * An <code>AbstractMapEntry</code> takes care of most of the grunge
 * work involved in subclassing <code>java.util.Map.Entry</code>.  For
 * an immutable entry, you need only implement <code>getKey()</code>
 * and <code>getValue()</code>.  For a modifiable entry, you must also
 * implement <code>setValue()</code>; the default implementation throws
 * an <code>UnsupportedOperationException</code>.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: AbstractMapEntry.java,v 1.2 2005/04/08 21:38:09 joewhaley Exp $ */
public abstract class AbstractMapEntry/*<K,V>*/ implements Map.Entry/*<K,V>*/ {
    
    /** Returns the key corresponding to this entry. */
    public abstract Object/*K*/ getKey();
    
    /** Returns the value corresponding to this entry.  If the mapping
     *  has been removed from the backing map (by the iterator's
     *  <code>remove()</code> operation), the results of this call are
     *  undefined. */
    public abstract Object/*V*/ getValue();
    
    /** Replaces the value corresponding to this entry with the specified
     *  value (optional operation).  (Writes through to the map.)  The
     *  behavior of this call is undefined if the mapping has already been
     *  removed from the map (by the iterator's <code>remove()</code>
     *  operation).
     *  @return old value corresponding to entry.
     */
    public Object/*V*/ setValue(Object/*V*/ value) {
        throw new UnsupportedOperationException();
    }
    
    /** Returns a human-readable representation of this map entry. */
    public String toString() {
        return 
            ((getKey()  ==null)?"null":getKey()  .toString()) + "=" +
            ((getValue()==null)?"null":getValue().toString());
    }
    
    /** Compares the specified object with this entry for equality.
     *  Returns <code>true</code> if the given object is also a map
     *  entry and the two entries represent the same mapping. */
    public boolean equals(Object o) {
        if (o instanceof Map.Entry) return equals((Map.Entry) o);
        return false;
    }
    
    public boolean equals(Map.Entry e) {
        if (this == e) return true;
        Object k1 = getKey();
        Object k2 = e.getKey();
        if (k1 == k2 || (k1 != null && k1.equals(k2))) {
            Object v1 = getValue();
            Object v2 = e.getValue();
            if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                return true;
        }
        return false;
    }
    
    /** Returns the hash code value for this map entry. */
    public int hashCode() {
        Object key = getKey();
        Object value = getValue();
        return (key==null ? 0 : key.hashCode()) ^
               (value==null ? 0 : value.hashCode());
    }
}
