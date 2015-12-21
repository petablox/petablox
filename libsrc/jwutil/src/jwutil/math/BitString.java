// BitString.java, created Wed May 16 17:26:33 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.math;

import jwutil.collections.UnmodifiableIterator;
import jwutil.util.Assert;

/**
 * <code>BitString</code> implements a vector of bits much like <code>java.util.BitSet</code>,
 * except that this implementation actually works.  Also, <code>BitString</code>
 * has some groovy features which <code>BitSet</code> doesn't; mostly related to
 * efficient iteration over <code>true</code> and <code>false</code> components.
 * <p>
 * Each component of the <code>BitString</code> has a boolean value.
 * The bits of a <code>BitString</code> are indexed by non-negative
 * integers (that means they are zero-based, of course).  Individual
 * indexed bits can be examined, set, or cleared.  One
 * <code>BitString</code> may be used to modify the contents of another
 * <code>BitString</code> through logical AND, logical inclusive OR,
 * and logical exclusive OR operations.
 * <p>
 * By default, all bits in the set initially have the value 
 * <code>false</code>.
 * <p>
 * Every bit set has a current size, which is the number of bits of
 * space currently in use by the bit set.  Note that the size is related
 * to the implementation of a bit set, so it may change with implementation.
 * The length of a bit set related to the logical length of a bit set
 * and is defined independently of implementation.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: BitString.java,v 1.4 2005/05/28 10:23:16 joewhaley Exp $
 */
public final class BitString implements Cloneable, java.io.Serializable {
    
    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3257570590025265971L;
    
    /* There are 2^BITS_PER_UNIT bits in each unit (int) */
    private static final int BITS_PER_UNIT = 5;
    private static final int MASK = (1 << BITS_PER_UNIT) - 1;
    private int[] bits;

    /**
     * Convert bitIndex to a subscript into the bits[] array.
     */
    private static int subscript(int bitIndex) {
        return bitIndex >> BITS_PER_UNIT;
    }

    /**
     * Creates an empty string with the specified size.
     * @param nbits the size of the string
     */
    public BitString(int nbits) {
        /* subscript(nbits + MASK) is the length of the array needed to hold
         * nbits.  Can also be written 1+subscript(nbits-1). */
        bits = new int[subscript(nbits + MASK)];
    }

    /**
     * Returns the first index in the bit string which is set, or
     * -1 if there is no such index.
     */
    public int firstSet() {
        return firstSet(-1);
    }

    /**
     * Returns the first index greater than <code>where</code> in the
     * bit string which is set, or -1 if there is no such index.
     * @param where the starting point for the search.  May be negative.
     */
    public int firstSet(int where) {
        // convert exclusive starting point to inclusive starting point
        where = (where < -1) ? 0 : (where + 1);
        // search in first unit is masked.
        int mask = (~0) << (where & MASK);
        // search through units
        for (int i = subscript(where); i < bits.length; i++, mask = ~0) {
            int unit = bits[i] & mask;
            if (unit != 0) return (i << BITS_PER_UNIT) + (bsf(unit) - 1);
        }
        return -1;
    }

    /**
     * Utility function to return the number of 1 bits in the given integer
     * value.
     * 
     * @param b value to check
     * @return byte number of one bits
     */
    public static final byte popcount(int b) {
        int t, x;
        x = b;
        x = x - ((x >> 1) & 0x55555555);
        t = ((x >> 2) & 0x33333333);
        x = (x & 0x33333333) + t;
        x = (x + (x >> 4)) & 0x0F0F0F0F;
        x = x + (x >> 8);
        x = x + (x >> 16);
        return (byte) x;
    }

    /**
     * Utility function to return the number of 1 bits in the given long value.
     * 
     * @param b value to check
     * @return byte number of one bits
     */
    public static final byte popcount(long b) {
        long t, x;
        x = b;
        x = x - ((x >> 1) & 0x5555555555555555L);
        t = ((x >> 2) & 0x3333333333333333L);
        x = (x & 0x3333333333333333L) + t;
        x = (x + (x >> 4)) & 0x0F0F0F0F0F0F0F0FL;
        x = x + (x >> 8);
        x = x + (x >> 16);
        x = x + (x >> 32);
        return (byte) x;
    }

    /**
     * Utility function to return the index of the first (lowest-order) one bit
     * in the given integer.  Returns zero if the given number is zero.
     * 
     * @param b value to check
     * @return byte index of first one bit, or zero if the number is zero
     */
    public static final int bsf(int b) {
        int t = ~(b | -b);
        return popcount(t);
    }

    /**
     * Utility function to return the index of the last one bit in the given
     * integer.  Returns zero if the given number is zero.
     * 
     * @param v value to check
     * @return byte index of first one bit, or zero if the number is zero
     */
    public static final int bsr(int v) {
        if ((v & 0xFFFF0000) != 0) {
            if ((v & 0xFF000000) != 0)
                return 24 + bytemsb[(v >> 24) & 0xFF];
            else
                return 16 + bytemsb[v >> 16];
        }
        if ((v & 0x0000FF00) != 0)
            return 8 + bytemsb[v >> 8];
        else
            return bytemsb[v];
    }

    /** Highest bit set in a byte. */
    private static final byte bytemsb[] = {0, 1, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 /* 256 */};

    /**
     * Returns the last index less than <code>where</code> in the
     *  bit string which is set, or -1 if there is no such index.
     * @param where the starting point for the search.
     */
    public int lastSet(int where) {
        // convert exclusive starting point to inclusive starting point
        if (--where < 0) return -1;
        int start = (bits.length - 1), mask = ~0;
        if (subscript(where) < bits.length) {
            // search in first unit is masked.
            start = subscript(where);
            mask = (~0) >>> (MASK - (where & mask));
        }
        // search through units
        for (int i = start; i >= 0; i--, mask = ~0) {
            int unit = bits[i] & mask;
            if (unit != 0) return (i << BITS_PER_UNIT) + (bsr(unit) - 1);
        }
        return -1;
    }

    /**
     * Returns the last index in the bit string which is set, or
     * -1 if there is no such index.
     */
    public int lastSet() {
        return lastSet(size());
    }

    /**
     * Sets all bits.
     */
    public void setAll() {
        int i = bits.length;
        while (i-- > 0) {
            bits[i] = ~0;
        }
    }

    /**
     * Sets all bits up to and including the given bit.
     * @param bit the bit to be set up to (zero-based)
     */
    public void setUpTo(int bit) {
        int where = subscript(bit);
        /* preaddition of 1 to bit is a clever hack to avoid long arithmetic */
        bits[where] |= ((1 << ((bit + 1) & MASK)) - 1);
        while (where-- > 0) {
            bits[where] = ~0;
        }
    }

    /**
     * Sets a bit.
     * @param bit the bit to be set (zero-based)
     */
    public void set(int bit) {
        bits[subscript(bit)] |= (1 << (bit & MASK));
    }

    /**
     * Clears all bits.
     */
    public void clearAll() {
        int i = bits.length;
        while (i-- > 0) {
            bits[i] = 0;
        }
    }

    /**
     * Clears all bits up to and including the given bit.
     * @param bit the bit to be set up to (zero-based)
     */
    public void clearUpTo(int bit) {
        int where = subscript(bit);
        /* preaddition of 1 to bit is a clever hack to avoid long arithmetic */
        bits[where] &= ~((1 << ((bit + 1) & MASK)) - 1);
        while (where-- > 0) {
            bits[where] = 0;
        }
    }

    /**
     * Clears a bit.
     * @param bit the bit to be cleared (zero-based)
     */
    public void clear(int bit) {
        bits[subscript(bit)] &= ~(1 << (bit & MASK));
    }

    /**
     * Gets a bit.
     * @param bit the bit to be gotten (zero-based)
     */
    public boolean get(int bit) {
        int n = subscript(bit);
        return ((bits[n] & (1 << (bit & MASK))) != 0);
    }

    /**
     * Logically ANDs this bit set with the specified set of bits.
     * Returns <code>true</code> if <code>this</code> was modified in
     * response to the operation.
     * @param set the bit set to be ANDed with
     */
    public boolean and(BitString set) {
        if (this == set) { // should help alias analysis
            return false;
        }
        int n = bits.length;
        boolean changed = false;
        for (int i = n; i-- > 0;) {
            int old = bits[i];
            bits[i] &= set.bits[i];
            changed |= (old != bits[i]);
        }
        return changed;
    }

    /**
     * Logically ORs this bit set with the specified set of bits.
     * Returns <code>true</code> if <code>this</code> was modified in
     * response to the operation.
     * @param set the bit set to be ORed with
     */
    public boolean or(BitString set) {
        if (this == set) { // should help alias analysis
            return false;
        }
        int setLength = set.bits.length;
        boolean changed = false;
        for (int i = setLength; i-- > 0;) {
            int old = bits[i];
            bits[i] |= set.bits[i];
            changed |= (old != bits[i]);
        }
        return changed;
    }

    /**
     * Logically ORs this bit set with the specified set of bits.
     * Returns <code>true</code> if <code>this</code> was modified in
     * response to the operation.
     * @param set the bit set to be ORed with
     */
    public boolean or_upTo(BitString set, int bit) {
        if (this == set) { // should help alias analysis
            return false;
        }
        boolean result;
        int where = subscript(bit);
        int old = bits[where];
        bits[where] |= (set.bits[where] & ((1 << ((bit + 1) & MASK)) - 1));
        result = (bits[where] != old);
        while (where-- > 0) {
            old = bits[where];
            bits[where] |= set.bits[where];
            result |= (bits[where] != old);
        }
        return result;
    }

    /**
     * Logically XORs this bit set with the specified set of bits.
     * Returns <code>true</code> if <code>this</code> was modified in
     * response to the operation.
     * @param set the bit set to be XORed with
     */
    public boolean xor(BitString set) {
        int setLength = set.bits.length;
        boolean changed = false;
        for (int i = setLength; i-- > 0;) {
            int old = bits[i];
            bits[i] ^= set.bits[i];
            changed |= (old != bits[i]);
        }
        return changed;
    }

    /**
     * Logically subtracts this bit set with the specified set of bits.
     * Returns <code>true</code> if <code>this</code> was modified in
     * response to the operation.
     * @param set the bit set to subtract
     */
    public boolean minus(BitString set) {
        int n = bits.length;
        boolean changed = false;
        for (int i = n; i-- > 0;) {
            int old = bits[i];
            bits[i] &= ~set.bits[i];
            changed |= (old != bits[i]);
        }
        return changed;
    }
    
    /**
     * Check if the intersection of the two sets is empty
     * @param other the set to check intersection with
     */
    public boolean intersectionEmpty(BitString other) {
        int n = bits.length;
        for (int i = n; i-- > 0;) {
            if ((bits[i] & other.bits[i]) != 0) return false;
        }
        return true;
    }

    /**
     * Check if this set contains all bits of the given set.
     * @param other the set to check containment with
     */
    public boolean contains(BitString other) {
        int n = bits.length;
        for (int i = n; i-- > 0;) {
            if ((bits[i] & other.bits[i]) != other.bits[i]) return false;
        }
        return true;
    }

    private static void shld(int[] bits, int i1, int i2, int amt) {
        Assert._assert(amt >= 0 && amt < BITS_PER_UNIT);
        bits[i1] = (bits[i1] << amt) | ((bits[i2] << (BITS_PER_UNIT - amt)) >> (BITS_PER_UNIT - amt));
    }

    /**
     * Performs a left-shift operation.
     * @param amt number of bits to shift, can be negative
     */
    public void shl(int amt) {
        final int div = amt >> BITS_PER_UNIT;
        final int mod = amt & MASK;
        final int size = bits.length;
        if (amt < 0) {
            shr(-amt); return;
        }
        // big moves
        if (div > 0) {
            System.arraycopy(bits, 0, bits, div, size - div);
            for (int i = 0; i < div; ++i)
                bits[i] = 0;
            /*
            int i;
            for (i = size - 1; i >= div; --i) {
                bits[i] = bits[i - div];
            }
            for (; i >= 0; --i) {
                bits[i] = 0;
            }
            */
        }
        // small moves
        if (mod > 0) {
            int i;
            for (i = size - 1; i > div; --i) {
                shld(bits, i, i - 1, mod);
            }
            bits[i] <<= mod;
        }
    }

    private static void shrd(int[] bits, int i1, int i2, int amt) {
        Assert._assert(amt >= 0 && amt < BITS_PER_UNIT);
        bits[i1] = (bits[i1] >>> amt) | ((bits[i2] >>> (BITS_PER_UNIT - amt)) << (BITS_PER_UNIT - amt));
    }

    /**
     * Performs a right-shift operation.
     * @param amt number of bits to shift
     */
    public void shr(int amt) {
        final int div = amt >> BITS_PER_UNIT;
        final int mod = amt & MASK;
        final int size = bits.length;
        // big moves
        if (div > 0) {
            System.arraycopy(bits, div, bits, 0, size - div);
            for (int i = size-div; i < size; ++i)
                bits[i] = 0;
            /*
            int i;
            for (i = 0; i < size - div; ++i) {
                bits[i] = bits[i + div];
            }
            for (; i < size; ++i) {
                bits[i] = 0;
            }
            */
        }
        // small moves
        if (mod > 0) {
            int i;
            for (i = 0; i < size - div - 1; ++i) {
                shrd(bits, i, i + 1, mod);
            }
            bits[i] >>>= mod;
        }
    }

    /**
     * Copies the values of the bits in the specified set into this set.
     * @param set the bit set to copy the bits from
     */
    public void copyBits(BitString set) {
        System.arraycopy(set.bits, 0, bits, 0, bits.length);
    }

    /**
     * Returns a hash code value for this bit string whose value depends
     * only on which bits have been set within this <code>BitString</code>.
     */
    public int hashCode() {
        int h = 1234;
        for (int i = bits.length; --i >= 0;) {
            h ^= bits[i] * (i + 1);
        }
        return h;
    }

    /**
     * Returns the "logical size" of this <code>BitString</code>: the
     * index of the highest set bit in the <code>BitString</code> plus
     * one.  Returns zero if the <code>BitString</code> contains no
     * set bits.
     */
    public int length() {
        return lastSet() + 1;
    }

    /**
     * Returns the number of bits of space actually in use by this
     * <code>BitString</code> to represent bit values.
     * The maximum element in the set is the size - 1st element.
     * The minimum element in the set is the zero'th element.
     */
    public int size() {
        return bits.length << BITS_PER_UNIT;
    }

    /**
     * Compares this object against the specified object.
     * @param obj the object to compare with
     * @return true if the contents of the bitsets are the same; false otherwise.
     */
    public boolean equals(Object obj) {
        BitString set;
        if (obj == null) return false;
        if (this == obj) return true; //should help alias analysis
        try {
            set = (BitString) obj;
        } catch (ClassCastException e) {
            return false;
        }
        if (length() != set.length()) return false;
        int n = bits.length - 1;
        while (n >= 0 && bits[n] == 0) n--;
        // now n has the first non-zero entry
        for (int i = n; i >= 0; i--) {
            if (bits[i] != set.bits[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether this <code>BitString</code> is all zeroes.
     * @return true if it is all zeroes.
     */
    public boolean isZero() {
        int setLength = bits.length;
        for (int i = setLength; i-- > 0;) {
            if (bits[i] != 0) return false;
        }
        return true;
    }

    /**
     * Returns the number of ones in this <code>BitString</code>.
     * @return number of bits set.
     */
    public int numberOfOnes() {
        int setLength = bits.length;
        int number = 0;
        for (int i = setLength; i-- > 0;) {
            number += popcount(bits[i]);
        }
        return number;
    }

    /**
     * Returns the number of ones in this <code>BitString</code> up to a given index.
     * @return number of bits set.
     */
    public int numberOfOnes(int where) {
        int setLength = subscript(where);
        int number = 0;
        for (int i = setLength; i-- > 0;) {
            number += popcount(bits[i]);
        }
        number += popcount(bits[setLength] & ((1 << ((where + 1) & MASK)) - 1));
        return number;
    }

    /**
     * Clones the BitString.
     */
    public Object clone() {
        BitString result = null;
        try {
            result = (BitString) super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
        result.bits = new int[bits.length];
        System.arraycopy(bits, 0, result.bits, 0, result.bits.length);
        return result;
    }

    /**
     * Converts the BitString to a String.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        boolean needSeparator = false;
        buffer.append('{');
        for (ForwardBitStringIterator i=iterator(); i.hasNext(); ) {
            int x = i.nextIndex();
            if (needSeparator) {
                buffer.append(", ");
            } else {
                needSeparator = true;
            }
            buffer.append(x);
        }
        buffer.append('}');
        return buffer.toString();
    }

    /**
     * Returns an iterator that iterates through the bits in forward order.
     */
    public ForwardBitStringIterator iterator() {
        return new ForwardBitStringIterator();
    }

    /**
     * Returns an iterator that iterates through the bits in backward order.
     */
    public BackwardBitStringIterator backwardsIterator() {
        return new BackwardBitStringIterator();
    }

    /**
     * Returns an iterator that iterates through the bits in backward order,
     * starting at the given index.
     */
    public BackwardBitStringIterator backwardsIterator(int i) {
        return new BackwardBitStringIterator(i);
    }

    /**
     * Abstract bit string iterator class.
     */
    public abstract static class BitStringIterator extends UnmodifiableIterator {

        /**
         * Returns the index of the next bit set.
         */
        public abstract int nextIndex();

        /**
         * @see java.util.Iterator#next()
         */
        public final Object next() {
            return new Integer(nextIndex());
        }

    }

    /**
     * Iterator for iterating through a bit string in forward order.
     */
    public class ForwardBitStringIterator extends BitStringIterator {
        private int j, k, t;

        ForwardBitStringIterator() {
            j = 0;
            k = 0;
            t = bits[0];
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            while (t == 0) {
                if (j == bits.length - 1) return false;
                t = bits[++j];
                k += 1 << BITS_PER_UNIT;
            }
            return true;
        }

        /**
         * @see jwutil.math.BitString.BitStringIterator#nextIndex()
         */
        public int nextIndex() {
            while (t == 0) {
                if (j == bits.length - 1) throw new java.util.NoSuchElementException();
                t = bits[++j];
                k += 1 << BITS_PER_UNIT;
            }
            int t2 = (t ^ (-t));
            int index = 31 - popcount(t2);
            t &= t2;
            return k + index;
            /*
            int t2 = ~(t | (-t));
            int index = popcount(t2);
            t &= ~(t2 + 1);
            return k + index;
             */
        }

    }

    /**
     * Iterator for iterating through a bit string in backward order.
     */
    public class BackwardBitStringIterator extends BitStringIterator {
        private int j, k, t;

        BackwardBitStringIterator(int i) {
            j = subscript(i);
            t = bits[j];
            t &= (1 << ((i + 1) & MASK)) - 1;
            k = j << BITS_PER_UNIT;
        }

        BackwardBitStringIterator() {
            j = bits.length - 1;
            t = bits[j];
            k = j << BITS_PER_UNIT;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            while (t == 0) {
                if (j == 0) {
                    return false;
                }
                t = bits[--j];
                k -= 1 << BITS_PER_UNIT;
            }
            return true;
        }

        /**
         * @see jwutil.math.BitString.BitStringIterator#nextIndex()
         */
        public int nextIndex() {
            while (t == 0) {
                if (j == 0) throw new java.util.NoSuchElementException();
                t = bits[--j];
                k -= 1 << BITS_PER_UNIT;
            }
            int index = bsr(t) - 1;
            t &= ~(1 << index);
            return k + index;
        }
        
    }
}
