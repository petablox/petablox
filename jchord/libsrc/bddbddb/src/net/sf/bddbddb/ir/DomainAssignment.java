// DomainAssignment.java, created Jul 11, 2004 2:33:35 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import jwutil.collections.GenericMultiMap;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.BDDSolver;
import net.sf.bddbddb.Domain;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;
import net.sf.bddbddb.dataflow.PartialOrder.Constraint;
import net.sf.bddbddb.dataflow.PartialOrder.Constraints;
import net.sf.bddbddb.ir.dynamic.If;
import net.sf.bddbddb.ir.dynamic.Nop;
import net.sf.bddbddb.ir.highlevel.BooleanOperation;
import net.sf.bddbddb.ir.highlevel.Copy;
import net.sf.bddbddb.ir.highlevel.Difference;
import net.sf.bddbddb.ir.highlevel.Free;
import net.sf.bddbddb.ir.highlevel.GenConstant;
import net.sf.bddbddb.ir.highlevel.Invert;
import net.sf.bddbddb.ir.highlevel.Join;
import net.sf.bddbddb.ir.highlevel.JoinConstant;
import net.sf.bddbddb.ir.highlevel.Load;
import net.sf.bddbddb.ir.highlevel.Project;
import net.sf.bddbddb.ir.highlevel.Rename;
import net.sf.bddbddb.ir.highlevel.Save;
import net.sf.bddbddb.ir.highlevel.Union;
import net.sf.bddbddb.ir.highlevel.Universe;
import net.sf.bddbddb.ir.highlevel.Zero;
import net.sf.bddbddb.ir.lowlevel.ApplyEx;
import net.sf.bddbddb.ir.lowlevel.BDDProject;
import net.sf.bddbddb.ir.lowlevel.Replace;
import net.sf.javabdd.BDDDomain;

/**
 * DomainAssignment
 * 
 * @author John Whaley
 * @version $Id: DomainAssignment.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public abstract class DomainAssignment implements OperationVisitor {
    int SIZE = 16;
    Solver solver;
    MultiMap/* <Domain,Attribute> */domainToAttributes;
    List inserted;
    static boolean TRACE = false;
    ListIterator currentBlock;
    Map/*Relation,Constraints*/constraintMap;

    public abstract void doAssignment();

    private void addConstraints(List loops, int loopDepth, IterationList list) {
        if (TRACE) solver.out.println("Entering: " + list);
        List s;
        if (loopDepth >= loops.size()) {
            loops.add(s = new LinkedList());
        } else {
            s = (List) loops.get(loopDepth);
        }
        s.add(list);
        for (Iterator i = list.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IterationList) {
                IterationList that = (IterationList) o;
                addConstraints(loops, loopDepth + (that.isLoop() ? 1 : 0), that);
            }
        }
    }

    public void addConstraints(IterationList list) {
        // TODO: a better order to add the constraints.
        List loops = new LinkedList();
        addConstraints(loops, 0, list);
        while (!loops.isEmpty()) {
            int index = loops.size() - 1;
            List s = (List) loops.remove(index);
            if (TRACE) solver.out.println("Doing loop depth " + index);
            for (Iterator j = s.iterator(); j.hasNext();) {
                list = (IterationList) j.next();
                if (TRACE) solver.out.println("Doing " + list);
                //add constraints for relprods first
                for (ListIterator i = list.iterator(); i.hasNext();) {
                    Object o = i.next();
                    if (o instanceof ApplyEx) {
                        Operation op = (Operation) o;
                        currentBlock = i;
                        op.visit(this);
                    }
                }
                //add all other operation constraints in iteration order
                for (ListIterator i = list.iterator(); i.hasNext();) {
                    Object o = i.next();
                    if (o instanceof Operation) {
                        if (o instanceof ApplyEx) continue;
                        Operation op = (Operation) o;
                        currentBlock = i;
                        op.visit(this);
                    }
                }
            }
        }
    }

    /**
     *  
     */
    public DomainAssignment(Solver s) {
        this.solver = s;
        domainToAttributes = new GenericMultiMap();
        this.inserted = new LinkedList();
        int totalNumber = 0;
        for (int i = 0; i < s.getNumberOfRelations(); ++i) {
            Relation r = s.getRelation(i);
            totalNumber += r.getAttributes().size();
        }
        for (int i = 0; i < s.getNumberOfRelations(); ++i) {
            Relation r = s.getRelation(i);
            int numAttribs = r.getAttributes().size();
            for (int j = 0; j < numAttribs; ++j) {
                Attribute a = r.getAttribute(j);
                domainToAttributes.add(a.getDomain(), a);
            }
        }
        constraintMap = new HashMap();
    }

    /**
     *  
     */
    public DomainAssignment(Solver s, Constraints[] constraints) {
        this(s);
        for (int i = 0; i < constraints.length; i++) {
            constraintMap.put(s.getRelation(i), constraints[i]);
        }
    }

    void initialize() {
        BDDSolver s = (BDDSolver) solver;
        // Attributes of the same relation must be assigned to different
        // domains.
        for (int i = 0; i < solver.getNumberOfRelations(); ++i) {
            Relation r = solver.getRelation(i);
            forceDifferent(r);
        }
        // Equality relations are treated special here, because we don't support
        // renaming them yet.
        for (Iterator i = s.getComparisonRelations().iterator(); i.hasNext();) {
            BDDRelation r = (BDDRelation) i.next();
            forceEqual(new Pair(r, r.getAttribute(0)), r.getBDDDomain(0));
            forceEqual(new Pair(r, r.getAttribute(1)), r.getBDDDomain(1));
            if (r.getNegated() != null) {
                forceEqual(new Pair(r.getNegated(), r.getAttribute(0)), r.getBDDDomain(0));
                forceEqual(new Pair(r.getNegated(), r.getAttribute(1)), r.getBDDDomain(1));
            }
        }
        // Add constraints from file.
        String domainFile = SystemProperties.getProperty("domainfile", "domainfile");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(domainFile));
            loadDomainAssignment(in);
        } catch (IOException x) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException _) {
            }
        }
    }

    abstract void forceDifferent(Relation r);

    abstract boolean forceEqual(Object o1, Object o2);

    abstract boolean forceNotEqual(Object o1, Object o2);

    abstract boolean forceBefore(Object o1, Object o2);

    abstract boolean forceBefore(Relation r1, Attribute a1, Relation r2, Attribute a2);

    abstract boolean forceInterleaved(Object o1, Object o2);

    abstract boolean forceInterleaved(Relation r1, Attribute a1, Relation r2, Attribute a2);

    public void forceConstraints(Relation r) {
        Constraints cons = (Constraints) constraintMap.get(r);
        //Assert._assert(cons != null, "Constraints for " + r + " are null");
        if (cons != null) {
            Collection bcons = cons.getBeforeConstraints();
            if (TRACE && bcons.size() == 0) solver.out.println("No before constraints for " + r);
            for (Iterator it = bcons.iterator(); it.hasNext();) {
                Constraint c = (Constraint) it.next();
                forceBefore(c.getLeftRelation(), c.getLeftAttribute(),
                    c.getRightRelation(), c.getRightAttribute());
            }
            Collection icons = cons.getInterleavedConstraints();
            for (Iterator it = icons.iterator(); it.hasNext();) {
                Constraint c = (Constraint) it.next();
                forceInterleaved(c.getLeftRelation(), c.getLeftAttribute(),
                    c.getRightRelation(), c.getRightAttribute());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.DomainAssignment#forceEqual(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Attribute, int)
     */
    boolean forceEqual(Relation r1, Attribute a1, int k) {
        Domain dom = a1.getDomain();
        BDDDomain d = ((BDDSolver) solver).getBDDDomain(dom, k);
        return forceEqual(new Pair(r1, a1), d);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.DomainAssignment#forceEqual(net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Attribute, net.sf.bddbddb.Relation,
     *      net.sf.bddbddb.Attribute, boolean)
     */
    boolean forceEqual(Relation r1, Attribute a1, Relation r2, Attribute a2, boolean equal) {
        Pair p1 = new Pair(r1, a1);
        Pair p2 = new Pair(r2, a2);
        if (equal) {
            return forceEqual(p1, p2);
        } else {
            return forceNotEqual(p1, p2);
        }
    }

    void insertBefore(Operation op) {
        if (TRACE) solver.out.println("Inserting before current operation: " + op);
        inserted.add(op);
        currentBlock.previous();
        currentBlock.add(op);
        currentBlock.next();
    }

    void insertAfter(Operation op) {
        if (TRACE) solver.out.println("Inserting after current operation: " + op);
        inserted.add(op);
        currentBlock.add(op);
    }

    abstract Relation allocNewRelation(Relation old_r);

    Relation insertReplaceBefore(Operation op, Relation r1) {
        Relation r1_new = allocNewRelation(r1);
        Operation move = new Replace((BDDRelation) r1_new, (BDDRelation) r1);
        insertBefore(move);
        op.replaceSrc(r1, r1_new);
        r1 = r1_new;
        return r1;
    }

    Relation insertReplaceAfter(Operation op, Relation r0) {
        Relation r0_new = allocNewRelation(r0);
        Operation move = new Replace((BDDRelation) r0, (BDDRelation) r0_new);
        insertAfter(move);
        op.setRelationDest(r0_new);
        r0 = r0_new;
        return r0;
    }

    Object visitUnaryOp(Operation op, Relation r0, Relation r1) {
        if (TRACE) solver.out.println("Unary op: " + op);
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a1 = (Attribute) i.next();
            if (r0.getAttributes().contains(a1)) {
                if (!forceEqual(r1, a1, r0, a1, true)) {
                    // Assignment failed, replace operation required.
                    // TODO: we have a choice whether to rename r0 or r1.
                    r1 = insertReplaceBefore(op, r1);
                    return visitUnaryOp(op, r0, r1);
                }
            }
        }
        forceConstraints(r0);
        return null;
    }

    Object visitBooleanOp(BooleanOperation op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc1();
        Relation r2 = op.getSrc2();
        return visitBooleanOp(op, r0, r1, r2);
    }

    Object visitBooleanOp(Operation op, Relation r0, Relation r1, Relation r2) {
        if (TRACE) solver.out.println("Boolean op: " + op);
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a1 = (Attribute) i.next();
            for (Iterator j = r2.getAttributes().iterator(); j.hasNext();) {
                Attribute a2 = (Attribute) j.next();
                if (a1 == a2) {
                    if (!forceEqual(r1, a1, r2, a2, true)) {
                        // Assignment failed, rename required.
                        // TODO: we have a choice whether to rename r1 or r2.
                        if (false) {
                            r1 = insertReplaceBefore(op, r1);
                        } else {
                            r2 = insertReplaceBefore(op, r2);
                        }
                        return visitBooleanOp(op, r0, r1, r2);
                    }
                } else if (a1.getDomain() == a2.getDomain()) {
                    if (!forceEqual(r1, a1, r2, a2, false)) {
                        // Assignment failed, rename required.
                        // TODO: we have a choice whether to rename r1 or r2.
                        if (false) {
                            r1 = insertReplaceBefore(op, r1);
                        } else {
                            r2 = insertReplaceBefore(op, r2);
                        }
                        return visitBooleanOp(op, r0, r1, r2);
                    }
                }
            }
        }
        for (Iterator i = r0.getAttributes().iterator(); i.hasNext();) {
            Attribute a0 = (Attribute) i.next();
            for (Iterator j = r1.getAttributes().iterator(); j.hasNext();) {
                Attribute a1 = (Attribute) j.next();
                if (a0 == a1) {
                    if (!forceEqual(r0, a0, r1, a1, true)) {
                        // Assignment failed, rename required.
                        r0 = insertReplaceAfter(op, r0);
                        return visitBooleanOp(op, r0, r1, r2);
                    }
                }
            }
            for (Iterator j = r2.getAttributes().iterator(); j.hasNext();) {
                Attribute a2 = (Attribute) j.next();
                if (a0 == a2) {
                    if (!forceEqual(r0, a0, r2, a2, true)) {
                        // Assignment failed, rename required.
                        r0 = insertReplaceAfter(op, r0);
                        return visitBooleanOp(op, r0, r1, r2);
                    }
                }
            }
        }
        forceConstraints(r0);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Join)
     */
    public Object visit(Join op) {
        return visitBooleanOp(op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Project)
     */
    public Object visit(Project op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        return visitUnaryOp(op, r0, r1);
    }
    public Object visit(BDDProject op) {
       Assert.UNREACHABLE(); /* shouldn't see these before domain assignment */
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Rename)
     */
    public Object visit(Rename op) {
        if (TRACE) solver.out.println("Rename op: " + op);
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a1 = (Attribute) i.next();
            Attribute a0 = (Attribute) op.getRenameMap().get(a1);
            if (a0 == null){
                a0 = a1;                
            }   
            if (!forceEqual(r1, a1, r0, a0, true)) {
                // Assignment failed, rename required.
                // TODO: we have a choice whether to rename r0 or r1.
                r1 = insertReplaceBefore(op, r1);
                return visit(op);
            }
        }
        forceConstraints(r0);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Union)
     */
    public Object visit(Union op) {
        return visitBooleanOp(op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Difference)
     */
    public Object visit(Difference op) {
        return visitBooleanOp(op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.JoinConstant)
     */
    public Object visit(JoinConstant op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        return visitUnaryOp(op, r0, r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.GenConstant)
     */
    public Object visit(GenConstant op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Free)
     */
    public Object visit(Free op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Universe)
     */
    public Object visit(Universe op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Zero)
     */
    public Object visit(Zero op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Invert)
     */
    public Object visit(Invert op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        return visitUnaryOp(op, r0, r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Copy)
     */
    public Object visit(Copy op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        return visitUnaryOp(op, r0, r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.Replace)
     */
    public Object visit(Replace op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc();
        return visitUnaryOp(op, r0, r1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Load)
     */
    public Object visit(Load op) {
        if (TRACE) solver.out.println("Load op: " + op);
        Relation r0 = op.getRelationDest();
        for (Iterator i = r0.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            String option = a.getOptions();
            if (option == null || option.length() == 0) continue;
            Domain d = a.getDomain();
            int number = Integer.parseInt(option.substring(d.toString().length()));
            if (!forceEqual(r0, a, number)) {
                // Assignment failed, rename required.
                r0 = insertReplaceAfter(op, r0);
                return visit(op);
            }
        }
        forceConstraints(r0);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Save)
     */
    public Object visit(Save op) {
        if (TRACE) solver.out.println("Save op: " + op);
        Relation r1 = op.getSrc();
        for (Iterator i = r1.getAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            String option = a.getOptions();
            if (option == null || option.length() == 0) continue;
            Domain d = a.getDomain();
            int number = Integer.parseInt(option.substring(d.toString().length()));
            if (!forceEqual(r1, a, number)) {
                // Assignment failed, rename required.
                r1 = insertReplaceBefore(op, r1);
                return visit(op);
            }
        }
        forceConstraints(r1);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.ApplyEx)
     */
    public Object visit(ApplyEx op) {
        Relation r0 = op.getRelationDest();
        Relation r1 = op.getSrc1();
        Relation r2 = op.getSrc2();
        return visitBooleanOp(op, r0, r1, r2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.If)
     */
    public Object visit(If op) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.Nop)
     */
    public Object visit(Nop op) {
        return null;
    }

    public abstract void saveDomainAssignment(BufferedWriter out) throws IOException;

    public void loadDomainAssignment(BufferedReader in) throws IOException {
        BDDSolver bs = (BDDSolver) solver;
        int count = 0;
        solver.out.println("Loading domain assignment from file...");
        for (;;) {
            String s = in.readLine();
            if (s == null) break;
            s = s.trim();
            if (s.length() == 0) continue;
            if (s.startsWith("#")) continue;
            StringTokenizer st = new StringTokenizer(s);
            {
                Object o1 = null, o2 = null;
                String constraint;
                String s1 = st.nextToken();
                String s2 = st.nextToken();
                String s3 = st.nextToken();
                if (s2.equals("=") || s2.equals("!=") || s2.equals("<") || s2.equals("~")) {
                    o1 = bs.getBDDDomain(s1);
                    constraint = s2;
                    if (!st.hasMoreTokens()) {
                        o2 = bs.getBDDDomain(s3);
                    } else {
                        String s4 = st.nextToken();
                        Relation r = bs.getRelation(s3);
                        Attribute a = r != null ? r.getAttribute(s4) : null;
                        if (r != null && a != null) o2 = new Pair(r, a);
                    }
                } else {
                    Relation r1 = bs.getRelation(s1);
                    Attribute a1 = r1 != null ? r1.getAttribute(s2) : null;
                    if (r1 != null && a1 != null) o1 = new Pair(r1, a1);
                    constraint = s3;
                    String s4 = st.nextToken();
                    if (!st.hasMoreTokens()) {
                        o2 = bs.getBDDDomain(s4);
                    } else {
                        String s5 = st.nextToken();
                        Relation r2 = bs.getRelation(s4);
                        Attribute a2 = r2 != null ? r1.getAttribute(s5) : null;
                        if (r2 != null && a2 != null) o2 = new Pair(r2, a2);
                    }
                }
                boolean success = false;
                if (o1 != null && o2 != null) {
                    if (constraint.equals("=")) {
                        success = forceEqual(o1, o2);
                    } else if (constraint.equals("!=")) {
                        success = forceNotEqual(o1, o2);
                    } else if (constraint.equals("<")) {
                        success = forceBefore(o1, o2);
                    } else if (constraint.equals("~")) {
                        success = forceInterleaved(o1, o2);
                    }
                }
                if (!success) {
                    if(TRACE) solver.out.println("Cannot add constraint: " + s);
                } else {
                    ++count;
                }
            }
        }
        solver.out.println("Incorporated " + count + " constraints from file.");
    }
    
    public abstract void setVariableOrdering();
}