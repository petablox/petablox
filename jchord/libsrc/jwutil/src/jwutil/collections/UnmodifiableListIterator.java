// UnmodifiableListIterator.java, created Wed Mar  5  0:26:28 2003 by joewhaley
// Copyright (C) 2001-3 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.ListIterator;
/**
 * <code>UnmodifiableListIterator</code> is an abstract superclass to save
 * you the trouble of implementing the <code>remove()</code> method
 * over and over again for those iterators which don't implement it.
 * The name's a bit clunky, but fits with the JDK naming in
 * <code>java.util.Collections</code> and etc.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: UnmodifiableListIterator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public abstract class UnmodifiableListIterator implements ListIterator {
    /** Returns <code>true</code> if the iteration has more elements.
     * @return <code>true</code> if the iterator has more elements.
     */
    public abstract boolean hasNext();
    /** Returns the next element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     */
    public abstract Object next();
    /** Returns the index of the next element.
     * @return the index of the next element.
     */
    public abstract int nextIndex();
    /** Returns <code>true</code> if the iteration has more elements.
     * @return <code>true</code> if the iterator has more elements.
     */
    public abstract boolean hasPrevious();
    /** Returns the previous element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     */
    public abstract Object previous();
    /** Returns the index of the previous element.
     * @return the index of the previous element.
     */
    public abstract int previousIndex();
    /** Always throws an <code>UnsupportedOperationException</code>.
     * @exception UnsupportedOperationException always.
     */
    public final void remove() {
        throw new UnsupportedOperationException("Unmodifiable Iterator");
    }
    /** Always throws an <code>UnsupportedOperationException</code>.
     * @exception UnsupportedOperationException always.
     */
    public final void set(Object o) {
        throw new UnsupportedOperationException("Unmodifiable Iterator");
    }
    /** Always throws an <code>UnsupportedOperationException</code>.
     * @exception UnsupportedOperationException always.
     */
    public final void add(Object o) {
        throw new UnsupportedOperationException("Unmodifiable Iterator");
    }
}
