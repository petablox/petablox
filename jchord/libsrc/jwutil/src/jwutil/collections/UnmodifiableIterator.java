// UnmodifiableIterator.java, created Tue Jun 15 22:00:21 1999 by cananian
// Copyright (C) 1999 C. Scott Ananian <cananian@alumni.princeton.edu>
// Licensed under the terms of the GNU GPL; see COPYING for details.
package jwutil.collections;

import java.util.Iterator;
/**
 * <code>UnmodifiableIterator</code> is an abstract superclass to save
 * you the trouble of implementing the <code>remove()</code> method
 * over and over again for those iterators which don't implement it.
 * The name's a bit clunky, but fits with the JDK naming in
 * <code>java.util.Collections</code> and etc.
 * 
 * @author  C. Scott Ananian <cananian@alumni.princeton.edu>
 * @version $Id: UnmodifiableIterator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public abstract class UnmodifiableIterator implements Iterator {
    /** Returns <code>true</code> if the iteration has more elements.
     * @return <code>true</code> if the iterator has more elements.
     */
    public abstract boolean hasNext();
    /** Returns the next element in the iteration.
     * @exception java.util.NoSuchElementException iteration has no more elements.
     */
    public abstract Object next();
    /** Always throws an <code>UnsupportedOperationException</code>.
     * @exception UnsupportedOperationException always.
     */
    public final void remove() {
        throw new UnsupportedOperationException("Unmodifiable Iterator");
    }
}
