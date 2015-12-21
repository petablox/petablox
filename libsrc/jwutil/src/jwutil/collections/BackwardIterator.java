// BackwardIterator.java, created Wed Mar  5  0:26:27 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: BackwardIterator.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class BackwardIterator implements ListIterator {

    private final ListIterator i;
    public BackwardIterator(ListIterator x) {
        while (x.hasNext()) x.next();
        this.i = x;
    }
    public BackwardIterator(List x) {
        this.i = x.listIterator(x.size());
    }
    
    public int previousIndex() { return i.nextIndex(); }
    public boolean hasNext() { return i.hasPrevious(); }
    public void set(Object obj) { i.set(obj); }
    public Object next() { return i.previous(); }
    public int nextIndex() { return i.previousIndex(); }
    public void remove() { i.remove(); }
    public boolean hasPrevious() { return i.hasNext(); }
    public void add(Object obj) { i.add(obj); i.previous(); }
    public Object previous() { return i.next(); }
    
}
