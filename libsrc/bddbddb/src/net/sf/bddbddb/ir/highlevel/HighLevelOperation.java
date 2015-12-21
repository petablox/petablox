// HighLevelOperation.java, created Jun 29, 2004 12:50:19 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;

/**
 * HighLevelOperation
 * 
 * @author John Whaley
 * @version $Id: HighLevelOperation.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class HighLevelOperation extends Operation {
    /**
     * @param i  the visitor
     * @return  the result from the visitor
     */
    public Object visit(OperationVisitor i) {
        return visit((HighLevelOperationVisitor) i);
    }

    /**
     * @param i  the visitor
     * @return  the result from the visitor
     */
    public abstract Object visit(HighLevelOperationVisitor i);
}
