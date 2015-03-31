/*
 * Created on Mar 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.order;

import java.util.NoSuchElementException;
import java.util.Stack;


public class StackQueue extends Stack implements Queue{

    /**
     * Version ID for serialization.
     */
    private static final long serialVersionUID = 3257001047263425073L;

    /* (non-Javadoc)
     * @see java.util.Queue#offer(java.lang.Object)
     */
    public boolean offer(Object arg0) {
        // TODO Auto-generated method stub
        push(arg0);
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Queue#poll()
     */
    public Object poll() {
        if(isEmpty()) return null;
        return pop();
    }

    /* (non-Javadoc)
     * @see java.util.Queue#remove()
     */
    public Object remove() {
        Object val = poll();
        if(val == null) throw new NoSuchElementException();
        return val;
    }

    /* (non-Javadoc)
     * @see java.util.Queue#element()
     */
    public Object element() {
        if(isEmpty()) throw new NoSuchElementException();
        
        return peek();
    }

}