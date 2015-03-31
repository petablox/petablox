// Project.java, created Jun 29, 2004 12:25:38 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * Project
 * 
 * @author jwhaley
 * @version $Id: Project.java 445 2005-02-21 02:32:50Z cs343 $
 */
public class Project extends HighLevelOperation {
    Relation r0, r1;
    List/* <Attribute> */attributes;

    /**
     * @param r0
     * @param r1
     */
    public Project(Relation r0, Relation r1) {
        super();
        this.r0 = r0;
        this.r1 = r1;
        this.attributes = new LinkedList(r1.getAttributes());
        this.attributes.removeAll(r0.getAttributes());
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
        return "project(" + r1.toString() + "," + attributes.toString() + ")";
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
     * @return  list of attributes being projected
     */
    public List/*<Attribute>*/ getAttributes() {
        return attributes;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.ir.Operation#copy()
     */
    public Operation copy() {
        return new Project(r0, r1);
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
