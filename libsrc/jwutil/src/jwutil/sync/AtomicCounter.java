// AtomicCounter.java, created Mon Apr  9  1:53:53 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.sync;

/**
 * An atomic counter class.  Provides atomic increment and reset
 * functionality.
 * 
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: AtomicCounter.java,v 1.2 2005/05/28 10:23:17 joewhaley Exp $
 */
public class AtomicCounter {

    private int current;
    
    /** Creates new AtomicCounter */
    public AtomicCounter(int initialValue) { current = initialValue-1;}
    /** Creates new AtomicCounter, initialized to one. */
    public AtomicCounter() { this(0); }

    /**
     * Increments this counter, returning the old value.
     */
    public synchronized int increment() { return ++current; }
    /**
     * Resets this counter to the given value.
     */
    public synchronized void reset(int v) { current = v-1; }
    
    /**
     * Returns the current value.
     */
    public int value() { return current+1; }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return Integer.toString(value()); }
}
