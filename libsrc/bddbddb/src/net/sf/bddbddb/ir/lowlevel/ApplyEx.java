// ApplyEx.java, created Jun 29, 2004 12:25:51 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.lowlevel;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jwutil.collections.Pair;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * ApplyEx
 * 
 * @author jwhaley
 * @version $Id: ApplyEx.java 445 2005-02-21 02:32:50Z cs343 $
 */
public class ApplyEx extends LowLevelOperation {
    BDDRelation r0, r1, r2;
    BDDOp op;
    List domainProjectSet;
    List/* <Attribute> */attributes;

    /**
     * @param r0
     * @param r1
     * @param r2
     */
    public ApplyEx(BDDRelation r0, BDDRelation r1, BDDOp op, BDDRelation r2) {
        this.r0 = r0;
        this.r1 = r1;
        this.r2 = r2;
        this.op = op;
        this.attributes = new LinkedList(r1.getAttributes());
        this.attributes.removeAll(r2.getAttributes());
        this.attributes.addAll(r2.getAttributes());
        this.attributes.removeAll(r0.getAttributes());
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperation#visit(net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor)
     */
    public Object visit(LowLevelOperationVisitor i) {
        return i.visit(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#toString()
     */
    public String toString() {
        return (TRACE_VERBOSE ? r0.verboseToString() : r0.toString()) + " = " + getExpressionString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        String opName;
        if (op == BDDFactory.and) opName = "relprod";
        else opName = op.toString() + "Ex";
        return opName + "(" + (TRACE_VERBOSE ? r1.verboseToString() : r1.toString()) + "," + (TRACE_VERBOSE ? r2.verboseToString() : r2.toString()) + "," + attributes + ")";
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
     * @return  the set to project
     */
    public BDD getProjectSet() {
        Assert._assert(domainProjectSet != null);
        BDD b = r1.getBDD().getFactory().one();
        for (Iterator i = domainProjectSet.iterator(); i.hasNext();) {
            BDDDomain d = (BDDDomain) i.next();
            if (d != null) b.andWith(d.set());
        }
        return b;
    }

    public void setProjectSet(){
        domainProjectSet = new LinkedList();
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d = r1.getBDDDomain(a);
            if (d == null) d = r2.getBDDDomain(a);
            if (d == null) {
                System.out.println("Warning: Trying to project attribute "+a+" which is neither in "+r1+" "+r1.getAttributes()+" "+r1.getBDDDomains()+" nor "+r2+" "+r2.getAttributes()+" "+r2.getBDDDomains());
                continue;
            }
            domainProjectSet.add(d);
        }
    }
    /**
     * @return Returns the attributes.
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * @return  the BDD operation
     */
    public BDDOp getOp() {
        return op;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
        if (r1 == r_old) r1 = (BDDRelation) r_new;
        if (r2 == r_old) r2 = (BDDRelation) r_new;
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation r0) {
        this.r0 = (BDDRelation) r0;
    }

    public Operation copy() {
        return new ApplyEx(r0, r1, op, r2);
    }
}