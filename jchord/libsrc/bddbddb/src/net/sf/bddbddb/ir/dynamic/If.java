// If.java, created Jul 7, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;

/**
 * If
 * 
 * @author John Whaley
 * @version $Id: If.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class If extends Operation {
    IRBoolean bool;
    IterationList block;

    public If(IRBoolean bool, IterationList block) {
        this.bool = bool;
        this.block = block;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#visit(net.sf.bddbddb.ir.OperationVisitor)
     */
    public Object visit(DynamicOperationVisitor i) {
        return i.visit(this);
    }

    public Object visit(OperationVisitor i) {
        return i.visit(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("If(" + bool.getName() + ") " + block + ": [ ");
        for (Iterator it = block.iterator(); it.hasNext();) {
            Object elem = (Object) it.next();
            sb.append(elem.toString() + "; ");
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getDest()
     */
    public Relation getRelationDest() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getSrcs()
     */
    public List getSrcs() {
        return Collections.EMPTY_LIST;
    }

    public IRBoolean getBoolSrc() {
        return bool;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        return toString();
    }

    /**
     * @return the target block
     */
    public IterationList getBlock() {
        return block;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public Operation copy() {
        return new If(bool, block);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation r0) {
    }
}
