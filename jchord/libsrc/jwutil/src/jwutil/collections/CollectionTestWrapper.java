// CollectionTestWrapper.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import jwutil.util.Assert;

/**
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: CollectionTestWrapper.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class CollectionTestWrapper implements Set, SortedSet, List {

    private final Collection c1;
    private final Collection c2;

    public CollectionTestWrapper(Collection c1, Collection c2) {
        this.c1 = c1; this.c2 = c2;
    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        int r1 = c1.size(); int r2 = c2.size();
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        boolean r1 = c1.isEmpty(); boolean r2 = c2.isEmpty();
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object arg0) {
        boolean r1 = c1.contains(arg0); boolean r2 = c2.contains(arg0);
        Assert._assert(r1 == r2);
        return r1;
    }

    public static class TestIterator implements Iterator {
        private final Collection c1, c2;
        private final Iterator i1, i2;
        private Object lastRet;
        private final boolean stable;
        private boolean removed;
        public TestIterator(Collection c1, Collection c2,
                            Iterator i1, Iterator i2, boolean stable) {
            this.c1 = c1; this.c2 = c2;
            this.i1 = i1; this.i2 = i2;
            this.stable = stable;
            this.removed = false;
        }
        public boolean hasNext() {
            boolean r1 = i1.hasNext();
            if (stable || !removed) {
                boolean r2 = i2.hasNext();
                Assert._assert(r1 == r2);
            }
            return r1;
        }
        public Object next() {
            Object r1 = i1.next();
            if (stable || !removed) {
                Object r2 = i2.next();
                if (stable) 
                    if (r1 != r2)
                        Assert.UNREACHABLE("c1="+c1+" c2="+c2+" "+r1+" != "+r2);
            }
            lastRet = r1;
            return r1;
        }
        public void remove() {
            i1.remove();
            if (stable) i2.remove();
            else {
                removed = true;
                boolean b = c2.remove(lastRet);
                Assert._assert(b);
            }
        }
    }

    private final boolean isStable() {
        if (c1 instanceof SortedSet && c2 instanceof SortedSet)
            return true;
        if (c1 instanceof List && c2 instanceof List)
            return true;
        return false;
    }

    /**
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        Iterator i1 = c1.iterator(), i2 = c2.iterator();
        return new TestIterator(c1, c2, i1, i2, isStable());
    }

    /**
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        Object[] r1 = c1.toArray(), r2 = c2.toArray();
        Assert._assert(r1.length == r2.length);
        if (c1 instanceof List || c1 instanceof SortedSet) {
            for (int i=0; i<r1.length; ++i) {
                Assert._assert(r1[i] == r2[i]);
            }
        }
        return r1;
    }

    /**
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] arg0) {
        Object[] arg1 = (Object[])java.lang.reflect.Array.newInstance(arg0.getClass(), arg0.length);
        Object[] r1 = c1.toArray(arg0), r2 = c2.toArray(arg1);
        Assert._assert(r1.length == r2.length);
        if (c1 instanceof List || c1 instanceof SortedSet) {
            for (int i=0; i<r1.length; ++i) {
                Assert._assert(r1[i] == r2[i]);
            }
        }
        return r1;
    }

    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object arg0) {
        boolean r1 = c1.add(arg0), r2 = c2.add(arg0);
        Assert._assert(r1 == r2);
        Assert._assert(c1.contains(arg0));
        Assert._assert(c2.contains(arg0));
        return r1;
    }

    /**
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object arg0) {
        boolean r1 = c1.remove(arg0), r2 = c2.remove(arg0);
        Assert._assert(r1 == r2);
        Assert._assert(!c1.contains(arg0));
        Assert._assert(!c2.contains(arg0));
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection arg0) {
        boolean r1 = c1.containsAll(arg0), r2 = c2.containsAll(arg0);
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection arg0) {
        boolean r1 = c1.addAll(arg0), r2 = c2.addAll(arg0);
        Assert._assert(r1 == r2);
        Assert._assert(c1.containsAll(arg0));
        Assert._assert(c2.containsAll(arg0));
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection arg0) {
        boolean r1 = c1.retainAll(arg0), r2 = c2.retainAll(arg0);
        Assert._assert(r1 == r2);
        Assert._assert(c1.containsAll(arg0));
        Assert._assert(c2.containsAll(arg0));
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection arg0) {
        boolean r1 = c1.removeAll(arg0), r2 = c2.removeAll(arg0);
        Assert._assert(r1 == r2);
        for (Iterator i=arg0.iterator(); i.hasNext(); ) {
            Object o = i.next();
            Assert._assert(!c1.contains(o));
            Assert._assert(!c2.contains(o));
        }
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        c1.clear(); c2.clear();
        Assert._assert(c1.size() == 0);
        Assert._assert(c2.size() == 0);
    }

    /**
     * @see java.util.SortedSet#comparator()
     */
    public Comparator comparator() {
        Comparator r1 = ((SortedSet)c1).comparator();
        Comparator r2 = ((SortedSet)c2).comparator();
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
     */
    public SortedSet subSet(Object arg0, Object arg1) {
        SortedSet r1 = ((SortedSet)c1).subSet(arg0, arg1);
        SortedSet r2 = ((SortedSet)c2).subSet(arg0, arg1);
        Assert._assert(r1.equals(r2));
        return r1;
    }

    /**
     * @see java.util.SortedSet#headSet(java.lang.Object)
     */
    public SortedSet headSet(Object arg0) {
        SortedSet r1 = ((SortedSet)c1).headSet(arg0);
        SortedSet r2 = ((SortedSet)c2).headSet(arg0);
        Assert._assert(r1.equals(r2));
        return r1;
    }

    /**
     * @see java.util.SortedSet#tailSet(java.lang.Object)
     */
    public SortedSet tailSet(Object arg0) {
        SortedSet r1 = ((SortedSet)c1).tailSet(arg0);
        SortedSet r2 = ((SortedSet)c2).tailSet(arg0);
        Assert._assert(r1.equals(r2));
        return r1;
    }

    /**
     * @see java.util.SortedSet#first()
     */
    public Object first() {
        Object r1 = ((SortedSet)c1).first();
        Object r2 = ((SortedSet)c2).first();
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.SortedSet#last()
     */
    public Object last() {
        Object r1 = ((SortedSet)c1).last();
        Object r2 = ((SortedSet)c2).last();
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int arg0, Collection arg1) {
        boolean r1 = ((List)c1).addAll(arg0, arg1);
        boolean r2 = ((List)c2).addAll(arg0, arg1);
        Assert._assert(r1 == r2);
        Assert._assert(c1.containsAll(arg1));
        Assert._assert(c2.containsAll(arg1));
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int arg0) {
        Object r1 = ((List)c1).get(arg0);
        Object r2 = ((List)c2).get(arg0);
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int arg0, Object arg1) {
        Object r1 = ((List)c1).set(arg0, arg1);
        Object r2 = ((List)c2).set(arg0, arg1);
        Assert._assert(r1 == r2);
        Assert._assert(((List)c1).get(arg0) == arg1);
        Assert._assert(((List)c2).get(arg0) == arg1);
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int arg0, Object arg1) {
        ((List)c1).add(arg0, arg1);
        ((List)c2).add(arg0, arg1);
        Assert._assert(((List)c1).get(arg0) == arg1);
        Assert._assert(((List)c2).get(arg0) == arg1);
        Assert._assert(c1.size() == c2.size());
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int arg0) {
        Object r1 = ((List)c1).remove(arg0);
        Object r2 = ((List)c2).remove(arg0);
        Assert._assert(r1 == r2);
        Assert._assert(c1.size() == c2.size());
        return r1;
    }

    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object arg0) {
        int r1 = ((List)c1).indexOf(arg0);
        int r2 = ((List)c2).indexOf(arg0);
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object arg0) {
        int r1 = ((List)c1).lastIndexOf(arg0);
        int r2 = ((List)c2).lastIndexOf(arg0);
        Assert._assert(r1 == r2);
        return r1;
    }

    /**
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator() {
        Iterator i1 = ((List)c1).listIterator(), i2 = ((List)c2).listIterator();
        for (;;) {
            if (!i1.hasNext()) {
                Assert._assert(!i2.hasNext());
                break;
            }
            Assert._assert(i2.hasNext());
            Object o1 = i1.next(), o2 = i2.next();
            Assert._assert(o1 == o2);
        }
        return ((List)c1).listIterator();
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int arg0) {
        Iterator i1 = ((List)c1).listIterator(arg0), i2 = ((List)c2).listIterator(arg0);
        for (;;) {
            if (!i1.hasNext()) {
                Assert._assert(!i2.hasNext());
                break;
            }
            Assert._assert(i2.hasNext());
            Object o1 = i1.next(), o2 = i2.next();
            Assert._assert(o1 == o2);
        }
        return ((List)c1).listIterator(arg0);
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List subList(int arg0, int arg1) {
        List r1 = ((List)c1).subList(arg0, arg1);
        List r2 = ((List)c2).subList(arg0, arg1);
        Assert._assert(r1.equals(r2));
        return r1;
    }

}
