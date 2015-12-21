// GenConstant.java, created Jun 29, 2004 2:57:29 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.List;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;

/**
 * GenConstant
 * 
 * @author jwhaley
 * @version $Id: GenConstant.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class GenConstant extends Operation {
    Relation r0;
    Attribute a;
    long value;

    /**
     * @param r0
     * @param a
     * @param value
     */
    public GenConstant(Relation r0, Attribute a, long value) {
        super();
        this.r0 = r0;
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
        return "const(" + a.toString() + "=" + value + ")";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#visit(net.sf.bddbddb.ir.HighLevelOperationVisitor)
     */
    public Object visit(OperationVisitor i) {
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
        return Collections.EMPTY_LIST;
    }

    /**
     * @return  the value of this constant
     */
    public long getValue() {
        return value;
    }

    /**
     * @return  the attribute of this constant
     */
    public Attribute getAttribute() {
        return a;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.ir.Operation#copy()
     */
    public Operation copy() {
        return new GenConstant(r0, a, value);
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
        this.r0 = r0;
    }
}