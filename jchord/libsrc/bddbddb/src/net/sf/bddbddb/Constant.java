// Constant.java, created Mar 17, 2004 8:30:37 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb;

/**
 * A Constant is a special kind of variable that represents a constant value.
 * 
 * @author John Whaley
 * @version $Id: Constant.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Constant extends Variable {
    
    /**
     * Value of constant.
     */
    protected long value;

    /**
     * Create a constant with the given value.
     * 
     * @param value  value of constant
     */
    public Constant(long value) {
        super(Long.toString(value));
        this.value = value;
    }

    /**
     * Returns the value of this constant.
     * 
     * @return value
     */
    public long getValue() {
        return value;
    }
}