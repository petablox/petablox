// Nop.java, created Jul 7, 2004 11:50:51 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.dynamic;

import java.util.Collections;
import java.util.List;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * No operation.
 * 
 * @author John Whaley
 * @version $Id: Nop.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Nop extends DynamicOperation {
    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.dynamic.DynamicOperation#visit(net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor)
     */
    public Object visit(DynamicOperationVisitor i) {
        return i.visit(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getExpressionString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getRelationDest()
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

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        return "nop" + Integer.toString(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation dest) {
    }

    public Operation copy() {
        return new Nop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
    }
}
