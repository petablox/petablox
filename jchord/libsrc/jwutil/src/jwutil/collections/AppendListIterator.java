// AppendListIterator.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/*
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: AppendListIterator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class AppendListIterator implements ListIterator {

    private final ListIterator iterator1;
    private final ListIterator iterator2;
    private boolean which;
    
    /** Creates new AppendListIterator */
    public AppendListIterator(ListIterator iter1, ListIterator iter2) {
        if (iter1 == null) {
            iterator1 = iter2; iterator2 = null;
        } else {
            iterator1 = iter1; iterator2 = iter2;
        }
        which = false;
    }

    public boolean hasPrevious() {
        if (!which || !iterator2.hasPrevious())
            return iterator1.hasPrevious();
        else
            return iterator2.hasPrevious();
    }
    public boolean hasNext() {
        if (which || ((iterator2 != null) && !iterator1.hasNext()))
            return iterator2.hasNext();
        else
            return iterator1.hasNext();
    }
    public Object previous() {
        if (!which) return iterator1.previous();
        else if (iterator2.hasPrevious()) return iterator2.previous();
        else {
            which = false; return iterator1.previous();
        }
    }
    public Object next() {
        if (which) return iterator2.next();
        else if (iterator1.hasNext()) return iterator1.next();
        else if (iterator2 != null) {
            which = true; return iterator2.next();
        } else throw new NoSuchElementException();
    }
    public int previousIndex() {
        if (!which) return iterator1.previousIndex();
        else return iterator1.nextIndex() + iterator2.previousIndex();
    }
    public int nextIndex() {
        if (!which) return iterator1.nextIndex();
        else return iterator1.nextIndex() + iterator2.nextIndex();
    }
    public void remove() {
        if (!which) iterator1.remove();
        else iterator2.remove();
    }
    public void set(Object o) {
        if (!which) iterator1.set(o);
        else iterator2.set(o);
    }
    public void add(Object o) {
        if (!which) iterator1.add(o);
        else iterator2.add(o);
    }

}
