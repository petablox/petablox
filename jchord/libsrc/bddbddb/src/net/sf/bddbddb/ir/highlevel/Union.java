// Union.java, created Jun 29, 2004 1:32:45 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * Union
 * 
 * @author jwhaley
 * @version $Id: Union.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Union extends BooleanOperation {
    /**
     * @param r0
     * @param r1
     * @param r2
     */
    public Union(Relation r0, Relation r1, Relation r2) {
        super(r0, r1, r2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.BooleanOperation#getName()
     */
    public String getName() {
        return "union";
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
        return BDDFactory.or;
    }

    public Operation copy() {
        return new Union(r0, r1, r2);
    }
}