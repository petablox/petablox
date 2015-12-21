// Difference.java, created Jun 29, 2004 1:35:00 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * Difference
 * 
 * @author jwhaley
 * @version $Id: Difference.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Difference extends BooleanOperation {
    /**
     * @param r0
     * @param r1
     * @param r2
     */
    public Difference(Relation r0, Relation r1, Relation r2) {
        super(r0, r1, r2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.BooleanOperation#getName()
     */
    public String getName() {
        return "diff";
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
     * @see net.sf.bddbddb.ir.highlevel.BooleanOperation#getBDDOp()
     */
    public BDDOp getBDDOp() {
        return BDDFactory.diff;
    }

    public Operation copy() {
        return new Difference(r0, r1, r2);
    }
}