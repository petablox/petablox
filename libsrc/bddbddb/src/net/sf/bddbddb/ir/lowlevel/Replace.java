// Replace.java, created Jul 12, 2004 1:34:38 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.lowlevel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDDPairing;

/**
 * Replace
 * 
 * @author John Whaley
 * @version $Id: Replace.java 445 2005-02-21 02:32:50Z cs343 $
 */
public class Replace extends LowLevelOperation {
    BDDRelation r0, r1;
    BDDPairing pairing;
    String pairingString;

    /**
     * @param r0
     * @param r1
     */
    public Replace(BDDRelation r0, BDDRelation r1) {
        this.r0 = r0;
        this.r1 = r1;
    }

    /**
     * 
     */
    public BDDPairing setPairing() {
        this.pairing = makePairing(r0.getBDD().getFactory());
        this.pairingString = Operation.getRenames(r1, r0);
        return this.pairing;
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
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return (TRACE_VERBOSE ? r0.verboseToString() : r0.toString()) + " = " + getExpressionString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getRelationDest()
     */
    public Relation getRelationDest() {
        return r0;
    }

    /*
     * 
     * (non-Javadoc)
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation r0) {
        this.r0 = (BDDRelation) r0;
    }

    /**
     * @return  the source relation
     */
    public BDDRelation getSrc() {
        return r1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getSrcs()
     */
    public List getSrcs() {
        return Collections.singletonList(r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
        if (r1 == r_old) {
            BDDRelation r1_b = (BDDRelation) r1;
            BDDRelation r_new_b = (BDDRelation) r_new;
            /* Assert only throws when each relation has
               the same domains but are in a different order
            List oldDomains = r1_b.getBDDDomains();
            List newDomains = r_new_b.getBDDDomains();
            Assert._assert(oldDomains.equals(newDomains), "old domains: " + oldDomains + " new domains: " + newDomains);
            */
            r1 = (BDDRelation) r_new;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        return "replace(" + (TRACE_VERBOSE ? r1.verboseToString() : r1.toString()) + pairingString + ")";
    }

    /**
     * @return  the pairing
     */
    public BDDPairing getPairing() {
        Assert._assert(pairingString != null,this.toString());
        return pairing;
    }

    BDDPairing makePairing(BDDFactory factory) {
        boolean any = false;
        BDDPairing pair = factory.makePair();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            BDDDomain d1 = r1.getBDDDomain(a);
            BDDDomain d2 = r0.getBDDDomain(a);
            if (d2 == null || d1 == d2) continue;
            any = true;
            pair.set(d1, d2);
        }
        if (any) return pairing = pair;
        else return pairing = null;
    }

    public Operation copy() {
       Replace r = new Replace(r0, r1);
       r.pairing = this.pairing;
       r.pairingString = this.pairingString;
       return r;
    }
}