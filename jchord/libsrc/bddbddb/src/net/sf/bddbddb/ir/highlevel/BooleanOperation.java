// BooleanOperation.java, created Jun 29, 2004 1:52:09 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.List;
import jwutil.collections.Pair;
import net.sf.bddbddb.Relation;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * BooleanOperation
 * 
 * @author jwhaley
 * @version $Id: BooleanOperation.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class BooleanOperation extends HighLevelOperation {
    Relation r0, r1, r2;

    /**
     * @param r0
     * @param r1
     * @param r2
     */
    public BooleanOperation(Relation r0, Relation r1, Relation r2) {
        super();
        this.r0 = r0;
        this.r1 = r1;
        this.r2 = r2;
    }

    /**
     * @return  the name of this operation
     */
    public abstract String getName();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
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
        return getName() + "(" + r1.toString() + "," + r2.toString() + ")";
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
        return new Pair(r1, r2);
    }

    /**
     * @return Returns the source relation.
     */
    public Relation getSrc1() {
        return r1;
    }

    /**
     * @return Returns the source relation.
     */
    public Relation getSrc2() {
        return r2;
    }

    /**
     * @return  the BDDOp for this operation
     */
    public abstract BDDOp getBDDOp();

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
        if (r1 == r_old) r1 = r_new;
        if (r2 == r_old) r2 = r_new;
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