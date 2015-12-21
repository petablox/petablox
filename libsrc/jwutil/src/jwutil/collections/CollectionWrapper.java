// CollectionWrapper.java, created Fri Nov 12 18:06:32 1999 by pnkfelix
// Copyright (C) 1999 Felix S. Klock II <pnkfelix@mit.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.Iterator;

/**
 * <code>CollectionWrapper</code> is a class that acts as a wrapper
 * around another Collection, using it as its backing store.  This
 * class isn't meant for direct usage, but rather provides for an easy
 * way for developers to quickly add extra independent behavior to
 * their own specific Collections without having to reimplement all of
 * AbstractCollection's interface
 * 
 * @author  Felix S. Klock II <pnkfelix@mit.edu>
 * @version $Id: CollectionWrapper.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class CollectionWrapper implements Collection {
    
    /** Collection backing <code>this</code>. */
    protected final Collection b; 

    /** Creates a <code>CollectionWrapper</code>. */
    public CollectionWrapper(Collection c) {
        this.b = c;
    }
    
    public boolean add(Object o) {
        return b.add(o);
    }

    public boolean addAll(Collection c) {
        return b.addAll(c);
    }

    public void clear() {
        b.clear();
    }
    
    public boolean contains(Object o) {
        return b.contains(o);
    }
    
    public boolean containsAll(Collection c) {
        return b.containsAll(c);
    }

    public boolean isEmpty() {
        return b.isEmpty();
    }

    public Iterator iterator() {
        return b.iterator();
    }

    public boolean remove(Object o) {
        return b.remove(o);
    }
    
    public boolean removeAll(Collection c) {
        return b.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return b.retainAll(c);
    }

    public int size() {
        return b.size();
    }

    public Object[] toArray() {
        return b.toArray();
    }
    
    public Object[] toArray(Object[] a) {
        return b.toArray(a);
    }
    
    public String toString() {
        return b.toString();
    }

    public boolean equals(Object o) {
        return b.equals(o);
    }

    public int hashCode() {
        return b.hashCode();
    }
}
