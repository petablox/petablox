// Save.java, created Jul 3, 2004 12:54:37 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.List;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * Save
 * 
 * @author John Whaley
 * @version $Id: Save.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Save extends HighLevelOperation {
    Relation r;
    String fileName;
    boolean tuples;

    /**
     * @param r
     */
    public Save(Relation r, String fileName, boolean tuples) {
        super();
        this.r = r;
        this.fileName = fileName;
        this.tuples = tuples;
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
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "save(" + r.toString() + ")";
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getExpressionString()
     */
    public String getExpressionString() {
        return toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#getDest()
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
        return Collections.singletonList(r);
    }

    /**
     * @return Returns the source relation.
     */
    public Relation getSrc() {
        return r;
    }

    public Operation copy() {
        return new Save(r, fileName, tuples);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Relation)
     */
    public void replaceSrc(Relation r_old, Relation r_new) {
        if (r == r_old) r = r_new;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
     */
    public void setRelationDest(Relation r0) {
    }

    /**
     * @return  the file name being saved
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return  whether we are saving tuples
     */
    public boolean isTuples() {
        return tuples;
    }
}
