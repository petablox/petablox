// AppendIterator.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Constructs a new iterator that appends two given iterators.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: AppendIterator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class AppendIterator implements Iterator {

    private final Iterator iterator1;
    private final Iterator iterator2;
    private boolean which;
    
    /** 
     * Given two iterators, creates a new AppendIterator.
     * 
     * @param iter1  first iterator
     * @param iter2  second iterator
     */
    public AppendIterator(Iterator iter1, Iterator iter2) {
        if (iter1 == null) {
            iterator1 = iter2; iterator2 = null;
        } else {
            iterator1 = iter1; iterator2 = iter2;
        }
        which = false;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if (which) {
            return iterator2.next();
        } else if (iterator1.hasNext()) {
            return iterator1.next();
        } else if (iterator2 != null) {
            which = true; return iterator2.next();
        } else throw new NoSuchElementException();
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        http://code.google.com/p/jchord/issues/detail?id=1
        if ( which ) return iterator2.hasNext();
        if ( iterator1.hasNext() ) return true;
        if ( iterator2 != null ) return iterator2.hasNext();
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        if (!which) iterator1.remove();
        else iterator2.remove();
    }
    
}
