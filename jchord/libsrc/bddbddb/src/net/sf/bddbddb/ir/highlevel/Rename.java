// Rename.java, created Jun 29, 2004 12:25:20 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir.highlevel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.Operation;

/**
 * Rename
 * 
 * @author jwhaley
 * @version $Id: Rename.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class Rename extends HighLevelOperation {
    Relation r0, r1;
    Map/*<Attribute,Attribute>*/ renames;

    /**
     * @param r0
     * @param r1
     */
    public Rename(Relation r0, Relation r1, Map/*<Attribute,Attribute>*/ renames) {
        super();
        this.r0 = r0;
        this.r1 = r1;
        this.renames = renames;
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
        StringBuffer sb = new StringBuffer();
        sb.append("rename(");
        sb.append(r1.toString());
        for (Iterator i = renames.entrySet().iterator(); i.hasNext();) {
            Map.Entry p = (Map.Entry) i.next();
            sb.append(',');
            Attribute a1 = (Attribute) p.getKey();
            sb.append(a1.getRelation());
            sb.append('.');
            sb.append(a1.toString());
            sb.append("->");
            Attribute a2 = (Attribute) p.getValue();
            sb.append(a2.getRelation());
            sb.append('.');
            sb.append(a2.toString());
        }
        sb.append(")");
        return sb.toString();
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
     * @return  the rename map
     */
    public Map getRenameMap() {
        return renames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.Operation#copy()
     */
    public Operation copy() {
        return new Rename(r0, r1, renames);
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
