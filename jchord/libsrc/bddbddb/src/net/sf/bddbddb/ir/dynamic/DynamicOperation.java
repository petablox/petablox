// DynamicOperation.java, created Jul 3, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;

/**
 * DynamicOperation
 * 
 * @author jwhaley
 * @version $Id: DynamicOperation.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class DynamicOperation extends Operation {
    /**
     * @param i  the visitor
     * @return  the result from the visitor
     */
    public Object visit(OperationVisitor i) {
        return visit((DynamicOperationVisitor) i);
    }

    /**
     * @param i  the visitor
     * @return  the result from the visitor
     */
    public abstract Object visit(DynamicOperationVisitor i);
}
