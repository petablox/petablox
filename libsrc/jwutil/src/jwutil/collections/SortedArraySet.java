// SortedArraySet.java, created Wed Mar  5  0:26:26 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Set that is stored as a sorted list.  This allows linear-time merge operations,
 * among other things.
 * 
 * Does not handle "null" elements.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: SortedArraySet.java,v 1.3 2005/05/05 19:39:34 joewhaley Exp $
 */
public class SortedArraySet
    extends AbstractList
    implements SortedSet, List, Cloneable, Serializable, RandomAccess {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3258416123022947382L;

    /**
     * The array buffer into which the elements of the SortedArraySet are stored.
     * The capacity of the SortedArraySet is the length of this array buffer.
     */
    private transient Object elementData[];

    /**
     * The size of the SortedArraySet (the number of elements it contains).
     */
    private int size;

    /**
     * The comparator used for this SortedArraySet, or "null" if we are using
     * the default element ordering.
     */
    private final Comparator comparator;

    /**
     * Constructs an empty set with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the set.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    private SortedArraySet(int initialCapacity) {
        super();
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        this.elementData = new Object[initialCapacity];
        this.size = 0;
        this.comparator = null;
    }
    
    /**
     * Constructs an empty set with an initial capacity of ten.
     */
    private SortedArraySet() {
        this(10);
    }

    private SortedArraySet(Collection c) {
        this((int) Math.min((c.size()*110L)/100, Integer.MAX_VALUE));
        this.addAll(c);
    }
    
    private SortedArraySet(Comparator comparator) {
        this(10, comparator);
    }

    private SortedArraySet(int initialCapacity, Comparator comparator) {
        super();
        this.elementData = new Object[initialCapacity];
        this.size = 0;
        this.comparator = comparator;
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int arg0) {
        checkAgainstSize(arg0);
        return this.elementData[arg0];
    }

    private void checkAgainstSize(int arg0) {
        if (arg0 >= this.size)
            throw new IndexOutOfBoundsException(arg0+" >= "+this.size);
    }

    /**
     * @see java.util.Collection#size()
     */
    public int size() {
        return this.size;
    }

    /**
     * @see java.util.SortedSet#comparator()
     */
    public Comparator comparator() {
        return comparator;
    }

    /**
     * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
     */
    public SortedSet subSet(Object arg0, Object arg1) {
        return new SubSet(arg0, arg1);
    }

    /**
     * @see java.util.SortedSet#headSet(java.lang.Object)
     */
    public SortedSet headSet(Object arg0) {
        return new SubSet(arg0, true);
    }

    /**
     * @see java.util.SortedSet#tailSet(java.lang.Object)
     */
    public SortedSet tailSet(Object arg0) {
        return new SubSet(arg0, false);
    }

    /**
     * @see java.util.SortedSet#first()
     */
    public Object first() {
        int i = this.size;
        if (i == 0)
            throw new NoSuchElementException();
        return this.elementData[i];
    }

    /**
     * @see java.util.SortedSet#last()
     */
    public Object last() {
        try {
            return this.elementData[this.size-1];
        } catch (ArrayIndexOutOfBoundsException x) {
            throw new NoSuchElementException();
        }
    }

    private int compare(Object o1, Object o2) {
        return (comparator==null ? ((Comparable)o1).compareTo(o2)
                                  : comparator.compare(o1, o2));
    }
    
    private int whereDoesItGo(Object o) {
        int lo = 0;
        int hi = this.size-1;
        if (hi < 0)
            return 0;
        int mid = hi >> 1;
        for (;;) {
            Object o2 = this.elementData[mid];
            int r = compare(o, o2);
            if (r < 0) {
                hi = mid - 1;
                if (lo > hi) return mid;
            } else if (r > 0) {
                lo = mid + 1;
                if (lo > hi) return lo;
            } else {
                return mid;
            }
            mid = ((hi - lo) >> 1) + lo;
        }
    }
    
    private class SubSet
    extends AbstractList
    implements SortedSet, List, Serializable, RandomAccess {
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3690476935247770425L;
        
        private final Object from, to;
        private int startIndex, endIndex, parentModCount;
        private final boolean fromStart, toEnd;
        
        SubSet(Object from, Object to, int startIndex, int endIndex,
               boolean fromStart, boolean toEnd) {
            this.from = from; this.to = to;
            this.startIndex = startIndex; this.endIndex = endIndex;
            this.fromStart = fromStart; this.toEnd = toEnd;
            this.parentModCount = SortedArraySet.this.modCount;
        }
        
        SubSet(Object from, Object to) {
            if (compare(from, to) > 0)
                throw new IllegalArgumentException(from+" > "+to);
            this.from = from; this.to = to;
            this.fromStart = false; this.toEnd = false;
            this.parentModCount = SortedArraySet.this.modCount;
            updateIndices();
        }
        
        SubSet(Object key, boolean headSet) {
            if (headSet) {
                fromStart = true; toEnd = false;
                from = null; to = key;
            } else {
                fromStart = false; toEnd = true;
                from = key; to = null;
            }
            this.parentModCount = SortedArraySet.this.modCount;
            updateIndices();
        }
        
        private void checkModCount() {
            int mc = SortedArraySet.this.modCount;
            if (this.parentModCount != mc) {
                this.parentModCount = mc;
                updateIndices();
            }
        }
        
        private void updateIndices() {
            if (!this.fromStart) {
                this.startIndex = SortedArraySet.this.whereDoesItGo(from);
            }
            if (!this.toEnd) {
                this.endIndex = SortedArraySet.this.whereDoesItGo(to);
            } else {
                this.endIndex = SortedArraySet.this.size;
            }
        }
        
        private void checkBounds(Object o) {
            if (!this.fromStart && compare(this.from, o) > 0)
                throw new IllegalArgumentException(o+" < "+from);
            if (!this.toEnd && compare(this.to, o) <= 0)
                throw new IllegalArgumentException(o+" >= "+to);
        }
        
        private int checkWithinRange2(Object o) {
            int i = SortedArraySet.this.whereDoesItGo(o);
            if (!this.fromStart &&
                (i < this.startIndex ||
                 (i == this.startIndex && compare(this.from, o) > 0))) {
                throw new IllegalArgumentException(o+" < "+from);
            }
            if (!this.toEnd &&
                (i > this.endIndex ||
                 (i == this.endIndex && compare(o, this.to) > 0))) {
                throw new IllegalArgumentException(o+" > "+to);
            }
            return i;
        }
        
        public boolean add(Object o) {
            checkBounds(o);
            return SortedArraySet.this.add(o);
        }
        
        public boolean remove(Object o) {
            checkBounds(o);
            return SortedArraySet.this.remove(o);
        }
        
        /**
         * @see java.util.AbstractCollection#size()
         */
        public int size() {
            checkModCount();
            return this.endIndex - this.startIndex;
        }
        
        /**
         * @see java.util.SortedSet#comparator()
         */
        public Comparator comparator() {
            return SortedArraySet.this.comparator;
        }
        
        /**
         * @see java.util.SortedSet#subSet(java.lang.Object, java.lang.Object)
         */
        public SortedSet subSet(Object fromElement, Object toElement) {
            checkModCount();
            int start_index = SortedArraySet.this.whereDoesItGo(fromElement);
            if (!this.fromStart &&
                (start_index < this.startIndex ||
                 (start_index == this.startIndex && compare(this.from, fromElement) > 0))) {
                throw new IllegalArgumentException(fromElement+" < "+from);
            }
            int end_index = SortedArraySet.this.whereDoesItGo(toElement);
            if (!this.toEnd &&
                (end_index > this.endIndex ||
                 (end_index == this.endIndex && compare(toElement, this.to) > 0))) {
                throw new IllegalArgumentException(toElement+" > "+to);
            }
            if (start_index > end_index ||
                (start_index == end_index && compare(toElement, toElement) > 0)) {
                throw new IllegalArgumentException(fromElement+" > "+toElement);
            }
            return new SubSet(fromElement, toElement, start_index, end_index, false, false);
        }
        
        /**
         * @see java.util.SortedSet#headSet(java.lang.Object)
         */
        public SortedSet headSet(Object arg0) {
            checkModCount();
            int end_index = checkWithinRange2(arg0);
            return new SubSet(this.from, arg0, this.startIndex, end_index, this.fromStart, false);
        }
        
        /**
         * @see java.util.SortedSet#tailSet(java.lang.Object)
         */
        public SortedSet tailSet(Object arg0) {
            checkModCount();
            int start_index = checkWithinRange2(arg0);
            return new SubSet(arg0, this.to, start_index, this.endIndex, false, this.toEnd);
        }
        
        /**
         * @see java.util.SortedSet#first()
         */
        public Object first() {
            checkModCount();
            int start = this.startIndex;
            if (this.endIndex <= start)
                throw new NoSuchElementException();
            return SortedArraySet.this.elementData[start];
        }
        
        /**
         * @see java.util.SortedSet#last()
         */
        public Object last() {
            checkModCount();
            int end = this.endIndex;
            if (end <= this.startIndex)
                throw new NoSuchElementException();
            return SortedArraySet.this.elementData[end-1];
        }
        
        protected void removeRange(int arg0, int arg1) {
            checkModCount();
            int s = this.startIndex;
            SortedArraySet.this.removeRange(s + arg0, s + arg1);
        }
        
        private int checkIndex(int arg0) {
            if (arg0 < 0)
                throw new IndexOutOfBoundsException(arg0+" < 0");
            checkModCount();
            int start = this.startIndex;
            int i = start + arg0;
            int end = this.endIndex;
            if (i >= end)
                throw new IndexOutOfBoundsException(arg0+" >= "+(end-start));
            return i;
        }
        
        /**
         * @see java.util.AbstractList#get(int)
         */
        public Object get(int arg0) {
            int i = checkIndex(arg0);
            return SortedArraySet.this.elementData[i];
        }
        
        public void add(int arg0, Object arg1) {
            if (DISALLOW_DIRECT_MODIFICATIONS)
                throw new UnsupportedOperationException();
            if (arg0 < 0)
                throw new IndexOutOfBoundsException(arg0+" < 0");
            checkModCount();
            int start = this.startIndex;
            int i = start + arg0;
            int end = this.endIndex;
            if (i > end)
                throw new IndexOutOfBoundsException(arg0+" > "+(end-start));
            SortedArraySet.this.add(i, arg1);
        }
        
        /**
         * @see java.util.List#remove(int)
         */
        public Object remove(int arg0) {
            if (DISALLOW_DIRECT_MODIFICATIONS)
                throw new UnsupportedOperationException();
            int i = checkIndex(arg0);
            return SortedArraySet.this.remove(i);
        }

        /**
         * @see java.util.List#set(int, java.lang.Object)
         */
        public Object set(int arg0, Object arg1) {
            if (DISALLOW_DIRECT_MODIFICATIONS)
                throw new UnsupportedOperationException();
            int i = checkIndex(arg0);
            return SortedArraySet.this.set(i, arg1);
        }

        /**
         * @see java.util.List#indexOf(java.lang.Object)
         */
        public int indexOf(Object arg0) {
            int i = SortedArraySet.this.indexOf(arg0);
            checkModCount();
            int s = this.startIndex;
            if (i < s) return -1;
            if (i >= this.endIndex) return -1;
            return i - s;
        }

        /**
         * @see java.util.List#lastIndexOf(java.lang.Object)
         */
        public int lastIndexOf(Object arg0) {
            return this.lastIndexOf(arg0);
        }

        /**
         * @see java.util.List#subList(int, int)
         */
        public List subList(int arg0, int arg1) {
            Object lo = this.get(arg0);
            if (arg1 == this.size())
                return (List) this.tailSet(lo);
            Object hi = this.get(arg1);
            return (List) this.subSet(lo, hi);
        }
    
        /**
         * @see java.util.Collection#contains(java.lang.Object)
         */
        public boolean contains(Object arg0) {
            return this.indexOf(arg0) != -1;
        }
    
    }
    
    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object arg0) {
        int i = whereDoesItGo(arg0);
        int s = this.size;
        if (i != s && elementData[i].equals(arg0))
            return false;
        ensureCapacity(s+1);  // increments modCount
        System.arraycopy(this.elementData, i, this.elementData, i + 1, s - i);
        elementData[i] = arg0;
        this.size++;
        return true;
    }
    
    /**
     * Increases the capacity of this SortedArraySet instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument. 
     *
     * @param   minCapacity   the desired minimum capacity.
     */
    public void ensureCapacity(int minCapacity) {
        this.modCount++;
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            Object oldData[] = elementData;
            int newCapacity = ((oldCapacity * 3) >> 1) + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            this.elementData = new Object[newCapacity];
            System.arraycopy(oldData, 0, this.elementData, 0, this.size);
        }
    }
    
    public static final boolean DISALLOW_DIRECT_MODIFICATIONS = false;
    
    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int arg0, Object arg1) {
        if (DISALLOW_DIRECT_MODIFICATIONS)
            throw new UnsupportedOperationException();
        int s = this.size;
        if (arg0 > s) {
            throw new IndexOutOfBoundsException("Index: "+arg0+", Size: "+s);
        }
        ensureCapacity(s+1);  // increments modCount
        System.arraycopy(this.elementData, arg0, this.elementData, arg0 + 1, s - arg0);
        elementData[arg0] = arg1;
        this.size++;
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int arg0) {
        checkAgainstSize(arg0);
        Object oldValue = elementData[arg0];
        this.modCount++;
        int numMoved = this.size - arg0 - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, arg0+1, elementData, arg0, numMoved);
        elementData[--this.size] = null; // for gc
        return oldValue;
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int arg0, Object arg1) {
        if (DISALLOW_DIRECT_MODIFICATIONS)
            throw new UnsupportedOperationException();
        checkAgainstSize(arg0);
        Object oldValue = elementData[arg0];
        elementData[arg0] = arg1;
        return oldValue;
    }

    // Set this to true if allocations are more expensive than arraycopy.
    public static final boolean REDUCE_ALLOCATIONS = false;

    public boolean addAll(Collection that) {
        if (that instanceof SortedSet) return addAll((SortedSet)that);
        return super.addAll(that);
    }

    public boolean addAll(SortedSet that) {
        if (this == that) return false;
        Comparator c1 = this.comparator, c2 = that.comparator();
        if (c1 != c2 && (c1 == null || !c1.equals(c2)))
            return super.addAll(that);
        int s1 = this.size, s2 = that.size();
        Object[] e1 = this.elementData;
        int newSize = Math.max(e1.length, s1 + s2);
        int i1, new_i1=0;
        Object[] new_e1;
        if (REDUCE_ALLOCATIONS && newSize <= e1.length) {
            System.arraycopy(e1, 0, e1, s2, s1);
            new_e1 = e1;
            i1 = s2; s1 += s2;
        } else {
            new_e1 = new Object[newSize];
            this.elementData = new_e1;
            i1 = 0;
        }
        Iterator i2 = that.iterator();
        boolean change = false;
        for (;;) {
            if (!i2.hasNext()) {
                System.arraycopy(e1, i1, new_e1, new_i1, s1-i1);
                this.size = new_i1 + s1 - i1;
                return change;
            }
            Object o2 = i2.next();
            for (;;) {
                if (i1 == s1) {
                    new_e1[new_i1++] = o2;
                    while (i2.hasNext())
                        new_e1[new_i1++] = i2.next();
                    this.size = new_i1;
                    return true;
                }
                Object o1 = e1[i1];
                int r = compare(o1, o2);
                if (r <= 0) {
                    new_e1[new_i1++] = o1;
                    if (REDUCE_ALLOCATIONS && new_e1 == (Object)e1) e1[i1] = null;
                    i1++;
                    if (r == 0) break;
                } else {
                    new_e1[new_i1++] = o2;
                    change = true;
                    break;
                }
            }
        }
    }
    
    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object arg0) {
        int i = whereDoesItGo(arg0);
        if (i == size || !arg0.equals(this.elementData[i])) return -1;
        return i;
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object arg0) {
        return this.indexOf(arg0);
    }

    public boolean equals(Object arg0) {
        if (arg0 instanceof SortedSet)
            return equals((SortedSet)arg0);
        if (arg0 instanceof Collection)
            return equals((Collection)arg0);
        return false;
    }

    public boolean equals(SortedSet that) {
        if (this.size != that.size()) return false;
        Object[] e = this.elementData;
        int k = 0;
        for (Iterator i=that.iterator(); i.hasNext(); ) {
            if (!e[k++].equals(i.next())) return false;
        }
        return true;
    }

    public boolean equals(Collection that) {
        if (this.size != that.size()) return false;
        for (Iterator i=that.iterator(); i.hasNext(); ) {
            if (!this.contains(i.next())) return false;
        }
        return true;
    }
    
    public int hashCode() {
        int hash = 0;
        for (int i=0; i<this.size; ++i) {
            hash += this.elementData[i].hashCode();
        }
        return hash;
    }

    /**
     * @see java.util.AbstractList#removeRange(int, int)
     */
    protected void removeRange(int arg0, int arg1) {
        this.modCount++;
        Object[] e = this.elementData;
        int s = this.size;
        System.arraycopy(e, arg1, e, arg0, s - arg1);
        int i = this.size = s - arg1 + arg0;
        for ( ; i < s; ++i)
            e[i] = null;
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List subList(int arg0, int arg1) {
        if (arg0 >= size || arg1 > size)
            throw new IndexOutOfBoundsException();
        Object[] e = this.elementData;
        if (arg1 == this.size)
            return (List) this.tailSet(e[arg0]);
        return (List) this.subSet(e[arg0], e[arg1]);
    }

    /**
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object arg0) {
        return this.indexOf(arg0) != -1;
    }

    /**
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object arg0) {
        int i = this.indexOf(arg0);
        if (i == -1) return false;
        this.remove(i);
        return true;
    }

    public Object clone() {
        try {
            SortedArraySet s = (SortedArraySet) super.clone();
            int initialCapacity = this.elementData.length;
            s.elementData = new Object[initialCapacity];
            s.size = this.size;
            //s.comparator = comparator;
            System.arraycopy(this.elementData, 0, s.elementData, 0, this.size);
            return s;
        } catch (CloneNotSupportedException _) { return null; }
    }

    public static final SortedArraySetFactory FACTORY = new SortedArraySetFactory();
    public static class SortedArraySetFactory extends SetFactory {
        
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3258407318323475251L;

        private SortedArraySetFactory() {}
        
        public static final boolean TEST = false;
        public static final boolean PROFILE = false;
        
        public Set makeSet(Comparator c) {
            if (TEST)
                return new CollectionTestWrapper(new TreeSet(c), new SortedArraySet(c));
            if (PROFILE)
                return new InstrumentedSetWrapper(new SortedArraySet(c));
            return new SortedArraySet(c);
        }
        public Set makeSet(int capacity) {
            if (TEST)
                return new CollectionTestWrapper(new LinkedHashSet(capacity), new SortedArraySet(capacity));
            if (PROFILE)
                return new InstrumentedSetWrapper(new SortedArraySet(capacity));
            return new SortedArraySet(capacity);
        }
        public Set makeSet(Collection c) {
            if (TEST)
                return new CollectionTestWrapper(new LinkedHashSet(c), new SortedArraySet(c));
            if (PROFILE)
                return new InstrumentedSetWrapper(new SortedArraySet(c));
            return new SortedArraySet(c);
        }
    }

}
