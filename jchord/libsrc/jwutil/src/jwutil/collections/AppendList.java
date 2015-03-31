// AppendList.java, created Fri May 26 23:58:36 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A list which is two lists appended together.
 * 
 * @author John Whaley
 * @version $Id: AppendList.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class AppendList implements List {

    protected List l1, l2;

    public AppendList(List l1, List l2) {
        this.l1 = l1; this.l2 = l2;
    }

    /* (non-Javadoc)
     * @see java.util.List#size()
     */
    public int size() {
        return l1.size() + l2.size();
    }

    /* (non-Javadoc)
     * @see java.util.List#clear()
     */
    public void clear() {
        l1.clear(); l2.clear();
    }

    /* (non-Javadoc)
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return l1.isEmpty() && l2.isEmpty();
    }

    private static final boolean USE_ARRAYCOPY = false;
    /* (non-Javadoc)
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        int n = size();
        Object[] result = new Object[n];
        l1.toArray(result); n = l1.size();
        if (USE_ARRAYCOPY) {
            System.arraycopy(l2.toArray(), 0, result, n, l2.size());
        } else {
            for (Iterator i=l2.iterator(); i.hasNext(); ) {
                result[n++] = i.next();
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        int n = l1.size();
        if (index < n) return l1.get(index);
        else return l2.get(index-n);
    }

    /* (non-Javadoc)
     * @see java.util.List#remove(int)
     */
    public Object remove(int index) {
        int n = l1.size();
        if (index < n) return l1.remove(index);
        else return l2.remove(index-n);
    }

    /* (non-Javadoc)
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object element) {
        int n = l1.size();
        if (index < n) l1.add(index, element);
        else l2.add(index-n, element);
    }

    /* (non-Javadoc)
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        int n = l1.indexOf(o);
        if (n != -1) return n;
        n = l2.indexOf(o);
        if (n != -1) return l1.size()+n;
        return -1;
    }

    /* (non-Javadoc)
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        int n = l2.lastIndexOf(o);
        if (n != -1) return n+l1.size();
        return l1.lastIndexOf(o);
    }

    /* (non-Javadoc)
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Object o) {
        return l2.add(o);
    }

    /* (non-Javadoc)
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return l1.contains(o) || l2.contains(o);
    }

    /* (non-Javadoc)
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        boolean result = l1.remove(o);
        if (result == true) return true;
        return l2.remove(o);
    }

    /* (non-Javadoc)
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c) {
        int n = l1.size();
        if (index < n) return l1.addAll(index, c);
        else return l2.addAll(index-n, c);
    }

    /* (non-Javadoc)
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c) {
        return l2.addAll(c);
    }

    /* (non-Javadoc)
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        for (Iterator i=c.iterator(); i.hasNext(); ) {
            if (!this.contains(i.next()))
                return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c) {
        boolean result = false;
        if (l1.removeAll(c)) result = true;
        if (l2.removeAll(c)) result = true;
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c) {
        boolean result = false;
        if (l1.retainAll(c)) result = true;
        if (l2.retainAll(c)) result = true;
        return result;
    }

    /* (non-Javadoc)
     * @see java.util.List#iterator()
     */
    public Iterator iterator() {
        return new AppendIterator(l1.iterator(), l2.iterator());
    }

    /* (non-Javadoc)
     * @see java.util.List#subList(int, int)
     */
    public List subList(final int fromIndex, final int toIndex) {
        return new SubList(this, fromIndex, toIndex);
    }
    
    static class SubList extends AbstractList {
        private List l;
        private int offset;
        private int size;

        SubList(List list, int fromIndex, int toIndex) {
            if (fromIndex < 0)
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > list.size())
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            if (fromIndex > toIndex)
                throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                                   ") > toIndex(" + toIndex + ")");
            l = list;
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        public Object set(int index, Object element) {
            rangeCheck(index);
            return l.set(index+offset, element);
        }

        public Object get(int index) {
            rangeCheck(index);
            return l.get(index+offset);
        }

        public int size() {
            return size;
        }

        public void add(int index, Object element) {
            if (index<0 || index>size)
                throw new IndexOutOfBoundsException();
            l.add(index+offset, element);
            size++;
        }

        public Object remove(int index) {
            rangeCheck(index);
            Object result = l.remove(index+offset);
            size--;
            return result;
        }

        public boolean addAll(Collection c) {
            return addAll(size, c);
        }

        public boolean addAll(int index, Collection c) {
            if (index<0 || index>size)
                throw new IndexOutOfBoundsException(
                    "Index: "+index+", Size: "+size);
            int cSize = c.size();
            if (cSize==0)
                return false;

            l.addAll(offset+index, c);
            size += cSize;
            return true;
        }

        public Iterator iterator() {
            return SubList.this.listIterator();
        }

        public ListIterator listIterator(final int index) {
            if (index<0 || index>size)
                throw new IndexOutOfBoundsException(
                    "Index: "+index+", Size: "+size);

            return new ListIterator() {
                private ListIterator i = l.listIterator(index+offset);

                public boolean hasNext() {
                    return nextIndex() < size;
                }

                public Object next() {
                    if (hasNext())
                        return i.next();
                    else
                        throw new NoSuchElementException();
                }

                public boolean hasPrevious() {
                    return previousIndex() >= 0;
                }

                public Object previous() {
                    if (hasPrevious())
                        return i.previous();
                    else
                        throw new NoSuchElementException();
                }

                public int nextIndex() {
                    return i.nextIndex() - offset;
                }

                public int previousIndex() {
                    return i.previousIndex() - offset;
                }

                public void remove() {
                    i.remove();
                    size--;
                }

                public void set(Object o) {
                    i.set(o);
                }

                public void add(Object o) {
                    i.add(o);
                    size++;
                }
            };
        }

        public List subList(int fromIndex, int toIndex) {
            return new SubList(this, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index<0 || index>=size)
                throw new IndexOutOfBoundsException("Index: "+index+
                                                    ",Size: "+size);
        }
    }

    /* (non-Javadoc)
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator() {
        return new AppendListIterator(l1.listIterator(), l2.listIterator());
    }

    /* (non-Javadoc)
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index) {
        int n = l1.size();
        if (index < n)
            return new AppendListIterator(l1.listIterator(index), l2.listIterator());
        else
            return l2.listIterator(index-n);
    }

    /* (non-Javadoc)
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object element) {
        int n = l1.size();
        if (index < n)
            return l1.set(index, element);
        else
            return l2.set(index-n, element);
    }

    /* (non-Javadoc)
     * @see java.util.List#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] result) {
        int n = size();
        if (result.length < n) {
            result = (Object[]) Array.newInstance(result.getClass().getComponentType(), n);
        }
        l1.toArray(result); n = l1.size();
        if (USE_ARRAYCOPY) {
            System.arraycopy(l2.toArray(), 0, result, n, l2.size());
        } else {
            for (Iterator i=l2.iterator(); i.hasNext(); ) {
                result[n++] = i.next();
            }
        }
        return result;
    }

}
