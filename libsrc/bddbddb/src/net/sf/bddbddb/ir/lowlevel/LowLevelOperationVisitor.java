// LowLevelOperationVisitor.java, created Jul 3, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.lowlevel;

/**
 * LowLevelOperationVisitor
 * 
 * @author John Whaley
 * @version $Id: LowLevelOperationVisitor.java,v 1.3 2004/07/12 09:06:30
 *          joewhaley Exp $
 */
public interface LowLevelOperationVisitor {
    /**
     * @param op
     * @return  the result
     */
    public abstract Object visit(ApplyEx op);

    /**
     * @param op
     * @return  the result
     */
    public abstract Object visit(Replace op);
    
    public abstract Object visit(BDDProject op);
}
