// SortedIntList.java, created Wed Mar  5  0:26:26 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.SortedSet;
import java.io.Serializable;

/**
 * Set that stores ints as a sorted list.  This allows linear-time merge operations,
 * among other things.
 * 
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: SortedIntList.java,v 1.2 2005/04/29 02:32:24 joewhaley Exp $
 */
public class SortedIntList implements Cloneable, Serializable, RandomAccess {

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3760846774608344633L;

    /**
     * The array buffer into which the elements of the SortedArraySet are stored.
     * The capacity of the SortedArraySet is the length of this array buffer.
     */
    private transient int elementData[];

    /**
     * The size of the SortedArraySet (the number of elements it contains).
     */
    private int size;

    /**
     * Constructs an empty set with the specified initial capacity.
     *
     * @param   initialCapacity   the initial capacity of the set.
     * @exception IllegalArgumentException if the specified initial capacity
     *            is negative
     */
    public SortedIntList(int initialCapacity) {
        super();
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        this.elementData = new int[initialCapacity];
        this.size = 0;
    }
    
    /**
     * Constructs an empty set with an initial capacity of ten.
     */
    public SortedIntList() {
        this(10);
    }

    public SortedIntList(SortedIntList c) {
        this((int) Math.min((c.size()*110L)/100, Integer.MAX_VALUE));
        this.addAll(c);
    }
    
    public int get(int arg0) {
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

    public int first() {
        int i = this.size;
        if (i == 0)
            throw new NoSuchElementException();
        return this.elementData[i];
    }

    public int last() {
        try {
            return this.elementData[this.size-1];
        } catch (ArrayIndexOutOfBoundsException x) {
            throw new NoSuchElementException();
        }
    }

    private int whereDoesItGo(int o) {
        int lo = 0;
        int hi = this.size-1;
        if (hi < 0)
            return 0;
        int mid = hi >> 1;
        for (;;) {
            int o2 = this.elementData[mid];
            if (o < o2) {
                hi = mid - 1;
                if (lo > hi) return mid;
            } else if (o > o2) {
                lo = mid + 1;
                if (lo > hi) return lo;
            } else {
                return mid;
            }
            mid = ((hi - lo) >> 1) + lo;
        }
    }
    
    /**
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(int arg0) {
        int i = whereDoesItGo(arg0);
        int s = this.size;
        if (i != s && elementData[i] == arg0)
            return false;
        ensureCapacity(s+1);
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
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            int oldData[] = elementData;
            int newCapacity = ((oldCapacity * 3) >> 1) + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            this.elementData = new int[newCapacity];
            System.arraycopy(oldData, 0, this.elementData, 0, this.size);
        }
    }
    
    public static final boolean DISALLOW_DIRECT_MODIFICATIONS = false;
    
    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int arg0, int arg1) {
        if (DISALLOW_DIRECT_MODIFICATIONS)
            throw new UnsupportedOperationException();
        int s = this.size;
        if (arg0 > s) {
            throw new IndexOutOfBoundsException("Index: "+arg0+", Size: "+s);
        }
        ensureCapacity(s+1);
        System.arraycopy(this.elementData, arg0, this.elementData, arg0 + 1, s - arg0);
        elementData[arg0] = arg1;
        this.size++;
    }

    /**
     * @see java.util.List#remove(int)
     */
    public int remove(int arg0) {
        checkAgainstSize(arg0);
        int oldValue = elementData[arg0];
        int numMoved = this.size - arg0 - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, arg0+1, elementData, arg0, numMoved);
        // BEGIN MAYUR's FIX
        this.size--;
        // END MAYUR's FIX
        return oldValue;
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public int set(int arg0, int arg1) {
        if (DISALLOW_DIRECT_MODIFICATIONS)
            throw new UnsupportedOperationException();
        checkAgainstSize(arg0);
        int oldValue = elementData[arg0];
        elementData[arg0] = arg1;
        return oldValue;
    }

    // Set this to true if allocations are more expensive than arraycopy.
    public static final boolean REDUCE_ALLOCATIONS = false;

    public boolean addAll(SortedIntList that) {
        if (this == that) return false;
        int s1 = this.size, s2 = that.size();
        int[] e1 = this.elementData;
        int[] e2 = that.elementData;
        int newSize = Math.max(e1.length, s1 + s2);
        int i1, new_i1=0;
        int[] new_e1;
        if (REDUCE_ALLOCATIONS && newSize <= e1.length) {
            System.arraycopy(e1, 0, e1, s2, s1);
            new_e1 = e1;
            i1 = s2; s1 += s2;
        } else {
            new_e1 = new int[newSize];
            this.elementData = new_e1;
            i1 = 0;
        }
        int i2 = 0;
        boolean change = false;
        for (;;) {
            if (i2 == s2) {
                System.arraycopy(e1, i1, new_e1, new_i1, s1-i1);
                this.size = new_i1 + s1 - i1;
                return change;
            }
            int o2 = e2[i2++];
            for (;;) {
                if (i1 == s1) {
                    new_e1[new_i1++] = o2;
                    System.arraycopy(e2, i2, new_e1, new_i1, s2-i2);
                    new_i1 += s2-i2;
                    this.size = new_i1;
                    return true;
                }
                int o1 = e1[i1];
                if (o1 <= o2) {
                    new_e1[new_i1++] = o1;
                    i1++;
                    if (o1 == o2) break;
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
    public int indexOf(int arg0) {
        int i = whereDoesItGo(arg0);
        if (i == size || arg0 != this.elementData[i]) return -1;
        return i;
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(int arg0) {
        return this.indexOf(arg0);
    }

    public boolean equals(Object arg0) {
        if (arg0 instanceof SortedSet)
            return equals((SortedSet)arg0);
        if (arg0 instanceof Collection)
            return equals((Collection)arg0);
        // BEGIN MAYUR's FIX
        if (arg0 instanceof SortedIntList)
            return equals((SortedIntList)arg0);
       // END MAYUR'S FIX
        return false;
    }

    public boolean equals(SortedIntList that) {
        if (this.size != that.size) return false;
        int[] e1 = this.elementData;
        int[] e2 = that.elementData;
        for (int i = 0; i < this.size; ++i) {
            if (e1[i] != e2[i]) return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (int i=0; i<this.size; ++i) {
            hash += this.elementData[i];
        }
        return hash;
    }

    protected void removeRange(int arg0, int arg1) {
        int[] e = this.elementData;
        int s = this.size;
        System.arraycopy(e, arg1, e, arg0, s - arg1);
        this.size = s - arg1 + arg0;
    }

    public boolean contains(int arg0) {
        return this.indexOf(arg0) != -1;
    }

    public boolean removeElement(int arg0) {
        int i = this.indexOf(arg0);
        if (i == -1) return false;
        this.remove(i);
        return true;
    }

    public Object clone() {
        try {
            SortedIntList s = (SortedIntList) super.clone();
            int initialCapacity = this.elementData.length;
            s.elementData = new int[initialCapacity];
            s.size = this.size;
            System.arraycopy(this.elementData, 0, s.elementData, 0, this.size);
            return s;
        } catch (CloneNotSupportedException _) { return null; }
    }

}
