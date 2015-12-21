// DynamicOperationVisitor.java, created Jul 3, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

/**
 * DynamicOperationVisitor
 * 
 * @author John Whaley
 * @version $Id: DynamicOperationVisitor.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public interface DynamicOperationVisitor {
    /**
     * @param op
     * @return  the result
     */
    public abstract Object visit(If op);

    /**
     * @param op
     * @return  the result
     */
    public abstract Object visit(Nop op);
}
