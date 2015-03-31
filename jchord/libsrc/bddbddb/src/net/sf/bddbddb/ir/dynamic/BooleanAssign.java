// BooleanAssign.java, created Jul 7, 2004 12:50:19 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

import net.sf.bddbddb.ir.Operation;

/**
 * BooleanAssign
 * 
 * @author jwhaley
 * @version $Id: BooleanAssign.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class BooleanAssign extends Operation {
    IRBoolean dest;

    public BooleanAssign(IRBoolean dest) {
        this.dest = dest;
    }

    public IRBoolean getBoolDest() {
        return dest;
    }
}
