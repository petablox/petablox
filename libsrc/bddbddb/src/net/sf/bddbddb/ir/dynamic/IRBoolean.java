// IRBoolean.java, created Jul 7, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

/**
 * IRBoolean
 * 
 * @author John Whaley
 * @version $Id: IRBoolean.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class IRBoolean {
    boolean bool;
    String name;

    public IRBoolean(String name, boolean bool) {
        this.name = name;
        this.bool = bool;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return name + "(" + Boolean.toString(bool) + ")";
    }

    /**
     * @return the value of this boolean
     */
    public boolean value() {
        return bool;
    }

    /**
     * @param bool
     */
    public void set(boolean bool) {
        this.bool = bool;
    }

    /**
     * @return the name of this boolean
     */
    public String getName() {
        return name;
    }
}
