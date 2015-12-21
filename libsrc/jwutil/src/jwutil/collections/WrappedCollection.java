// WrappedCollection.java, created Jun 15, 2003 3:08:14 PM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * WrappedCollection
 * 
 * @author John Whaley
 * @version $Id: WrappedCollection.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class WrappedCollection extends AbstractCollection {

    protected Collection c;
    protected Filter in;
    protected Filter out;

    public WrappedCollection(Collection c, Filter in, Filter out) {
        this.c = c; this.in = in; this.out = out;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        return c.size();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#clear()
     */
    public void clear() {
        c.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        return c.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        // TODO Auto-generated method stub
        return super.toArray();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o) {
        return c.add(in.map(o));
    }

    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return c.contains(in.map(o));
    }

    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return c.remove(in.map(o));
    }

    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c) {
        // TODO Auto-generated method stub
        return super.addAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        // TODO Auto-generated method stub
        return super.containsAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c) {
        // TODO Auto-generated method stub
        return super.removeAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c) {
        // TODO Auto-generated method stub
        return super.retainAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        return new FilterIterator(c.iterator(), out);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] a) {
        // TODO Auto-generated method stub
        return super.toArray(a);
    }

}
