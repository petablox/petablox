// HighLevelOperationVisitor.java, created Jul 3, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

/**
 * HighLevelOperationVisitor
 * 
 * @author John Whaley
 * @version $Id: HighLevelOperationVisitor.java,v 1.1 2004/07/06 23:36:25 cs343
 *          Exp $
 */
public interface HighLevelOperationVisitor {
    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Join op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Project op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Rename op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Union op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Difference op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(JoinConstant op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(GenConstant op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Free op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Universe op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Zero op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Invert op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Copy op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Load op);

    /**
     * @param op  the operation
     * @return  the result
     */
    public abstract Object visit(Save op);
}
