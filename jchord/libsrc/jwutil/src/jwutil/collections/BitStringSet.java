// BitStringSet.java, created May 5, 2004 12:15:22 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import jwutil.math.BitString;

/**
 * A set backed by a bitstring.
 * 
 * @author jwhaley
 * @version $Id: BitStringSet.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class BitStringSet extends AbstractSet {

    final BitString b;
    final List elements;
    
    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object arg0) {
        int index = elements.indexOf(arg0);
        boolean a;
        if (index == -1) {
            elements.add(arg0);
            index = elements.size() - 1;
            a = false;
        } else {
            a = b.get(index);
        }
        if (!a)
            b.set(index);
        return !a;
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object arg0) {
        int index = elements.indexOf(arg0);
        if (index == -1) return false;
        return b.get(index);
    }
    
    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object arg0) {
        int index = elements.indexOf(arg0);
        if (index == -1) return false;
        boolean a = b.get(index);
        if (a) b.clear(index);
        return a;
    }
    
    public BitStringSet(BitString b, List elements) {
        this.b = b;
        this.elements = elements;
    }
    
    /* (non-Javadoc)
     * @see java.util.AbstractCollection#iterator()
     */
    public Iterator iterator() {
        return new BitStringIterator(b.iterator(), elements);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    public int size() {
        return b.numberOfOnes();
    }
    
    static class BitStringIterator implements Iterator {
        final BitString.BitStringIterator i;
        final List elements;
        
        BitStringIterator(BitString.BitStringIterator i, List elements) {
            this.i = i;
            this.elements = elements;
        }
        
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return i.hasNext();
        }
        
        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public Object next() {
            int index = i.nextIndex();
            return elements.get(index);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            i.remove();
        }
        
    }
    
}
