// FlattenedCollection.java, created Sep 12, 2003 12:16:03 AM by joewhaley
// Copyright (C) 2003 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Returns a collection view of a flattened collection.  (Elements
 * that are collections are replaced by their contained elements.)
 * 
 * @author jwhaley
 * @version $Id: FlattenedCollection.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public class FlattenedCollection extends AbstractCollection {

    private final Collection c;

    public FlattenedCollection(Collection c2) {
        this.c = c2;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    public int size() {
        int s = 0;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) s += ((Collection) o).size();
            else ++s;
        }
        return s;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#clear()
     */
    public void clear() {
        c.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                if (!((Collection) o).isEmpty())
                    return false;
            } else {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o) {
        return c.add(o);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c2) {
        return c.addAll(c2);
    }

    /* (non-Javadoc)
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c2) {
        boolean change = false;
        for (Iterator i = c.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Collection) {
                if (((Collection) o).removeAll(c2))
                    change = true;
            } else if (c2.contains(o)) {
                i.remove();
                change = true;
            }
        }
        return change;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        return new Iterator() {
            Object current;
            Iterator i = c.iterator();
            Iterator j;
            Iterator last;
            boolean more;
            
            { forward(); }
            
            void forward() {
                for (;;) {
                    if (j == null || !j.hasNext()) {
                        if (!i.hasNext()) {
                            more = false;
                            break;
                        }
                        current = i.next();
                        if (current instanceof Collection) {
                            j = ((Collection) current).iterator();
                            continue;
                        } else {
                            last = i;
                            more = true;
                            break;
                        }
                    } else {
                        current = j.next();
                        last = j;
                        more = true;
                        break;
                    }
                }
            }
            
            public void remove() {
                if (last == null)
                    throw new IllegalStateException();
                last.remove();
            }

            public boolean hasNext() {
                return more;
            }

            public Object next() {
                if (!more)
                    throw new NoSuchElementException();
                Object o = current;
                forward();
                return o;
            }
            
        };
    }

}
