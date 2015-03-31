// JoinConstant.java, created Jun 29, 2004 2:57:29 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.List;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * JoinConstant
 * 
 * @author jwhaley
 * @version $Id: JoinConstant.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class JoinConstant extends HighLevelOperation {
    Relation r0, r1;
    Attribute a;
    long value;

    /**
     * @param r0
     * @param r1
     * @param a
     * @param value
     */
    public JoinConstant(Relation r0, Relation r1, Attribute a, long value) {
        super();
        this.r0 = r0;
        this.r1 = r1;
        this.a = a;
        this.value = value;
    }

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
        return "restrict(" + r1.toString() + "," + a.toString() + "=" + value + ")";
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

    /**
     * @return  the value being joined
     */
    public long getValue() {
        return value;
    }

    /**
     * @return  the attribute of the value being joined
     */
    public Attribute getAttribute() {
        return a;
    }

    public Operation copy() {
        return new JoinConstant(r0, r1, a, value);
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
