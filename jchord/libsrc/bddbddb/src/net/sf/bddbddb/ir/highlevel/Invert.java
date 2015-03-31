// Invert.java, created Jul 1, 2004 11:04:17 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.List;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * Invert
 * 
 * @author John Whaley
 * @version $Id: Invert.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Invert extends HighLevelOperation {
    Relation r0, r1;

    /**
     * @param r0
     * @param r1
     */
    public Invert(Relation r0, Relation r1) {
        super();
        this.r0 = r0;
        this.r1 = r1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#visit(net.sf.bddbddb.ir.HighLevelOperationVisitor)
     */
    public Object visit(HighLevelOperationVisitor i) {
        return i.visit(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#toString()
     */
    public String toString() {
        return r0.toString() + " = " + getExpressionString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        return "invert(" + r1.toString() + ")";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getDest()
     */
    public Relation getRelationDest() {
        return r0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getSrcs()
     */
    public List getSrcs() {
        return Collections.singletonList(r1);
    }

    /**
     * @return Returns the source relation.
     */
    public Relation getSrc() {
        return r1;
    }

    public Operation copy() {
        return new Invert(r0, r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
        if (r1 == r_old) r1 = r_new;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation r0) {
        this.r0 = r0;
    }
}