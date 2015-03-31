// IR.java, created Jun 29, 2004 12:24:59 PM 2004 by mcarbin
// Copyright (C) 2004 Michael Carbin
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import jwutil.collections.MultiMap;
import jwutil.io.SystemProperties;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.IterationFlowGraph;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;
import net.sf.bddbddb.Stratify;
import net.sf.bddbddb.dataflow.ConstantProp;
import net.sf.bddbddb.dataflow.CopyProp;
import net.sf.bddbddb.dataflow.DataflowSolver;
import net.sf.bddbddb.dataflow.DeadCode;
import net.sf.bddbddb.dataflow.DefUse;
import net.sf.bddbddb.dataflow.IRPass;
import net.sf.bddbddb.dataflow.Liveness;
import net.sf.bddbddb.dataflow.PartialRedundancy;
import net.sf.bddbddb.dataflow.Problem;
import net.sf.bddbddb.dataflow.ConstantProp.ConstantPropFacts;
import net.sf.bddbddb.dataflow.DataflowSolver.DataflowIterator;
import net.sf.bddbddb.dataflow.DefUse.DefUseFact;
import net.sf.bddbddb.ir.highlevel.BooleanOperation;
import net.sf.bddbddb.ir.highlevel.Copy;
import net.sf.bddbddb.ir.highlevel.Invert;
import net.sf.bddbddb.ir.highlevel.Load;
import net.sf.bddbddb.ir.highlevel.Project;
import net.sf.bddbddb.ir.highlevel.Rename;
import net.sf.bddbddb.ir.highlevel.Save;
import net.sf.bddbddb.ir.lowlevel.ApplyEx;
import net.sf.bddbddb.ir.lowlevel.BDDProject;
import net.sf.bddbddb.ir.lowlevel.Replace;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDPairing;
import net.sf.javabdd.BDDFactory.BDDOp;

/**
 * Intermediate representation.
 * 
 * @author mcarbin
 * @version $Id: IR.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class IR {
    public Solver solver;
    public IterationFlowGraph graph;
    boolean ALL_OPTS = !SystemProperties.getProperty("allopts", "no").equals("no");
    boolean FREE_DEAD = ALL_OPTS && !SystemProperties.getProperty("freedead", "yes").equals("no") || !SystemProperties.getProperty("freedead", "no").equals("no");
    boolean CONSTANTPROP = ALL_OPTS && !SystemProperties.getProperty("constantprop", "yes").equals("no")
        || !SystemProperties.getProperty("constantprop", "no").equals("no");
    boolean DEFUSE = ALL_OPTS && !SystemProperties.getProperty("defuse", "yes").equals("no") || !SystemProperties.getProperty("defuse", "no").equals("no");
    boolean PRE = ALL_OPTS && !SystemProperties.getProperty("pre", "yes").equals("no") || !SystemProperties.getProperty("pre", "no").equals("no");
    boolean COPYPROP = ALL_OPTS && !SystemProperties.getProperty("copyprop", "yes").equals("no") || !SystemProperties.getProperty("copyprop", "no").equals("no");
    boolean DEAD_CODE = ALL_OPTS && !SystemProperties.getProperty("deadcode", "yes").equals("no") || !SystemProperties.getProperty("deadcode", "no").equals("no");
    boolean DOMAIN_ASSIGNMENT = ALL_OPTS && !SystemProperties.getProperty("domainassign", "yes").equals("no")
        || !SystemProperties.getProperty("domainassign", "no").equals("no");
    boolean TRACE = false;

    public static IR create(Stratify s) {
        return create(s.solver, s.firstSCCs, s.innerSCCs);
    }

    public static IR create(Solver solver, List firstSCCs, MultiMap innerSCCs) {
        IterationFlowGraph ifg = new IterationFlowGraph(solver.getRules(), firstSCCs, innerSCCs);
        IterationList list = ifg.expand();
        // Add load operations.
        if (!solver.getRelationsToLoad().isEmpty()) {
            Assert._assert(!list.isLoop());
            IterationList loadList = new IterationList(false);
            for (Iterator j = solver.getRelationsToLoad().iterator(); j.hasNext();) {
                Relation r = (Relation) j.next();
                loadList.addElement(new Load(r, solver.getBaseDir() + r + ".bdd", false));
                if (r.getNegated() != null) {
                    loadList.addElement(new Invert(r.getNegated(), r));
                }
            }
            list.addElement(0, loadList);
        }
        // Add save operations.
        if (!solver.getRelationsToSave().isEmpty()) {
            Assert._assert(!list.isLoop());
            IterationList saveList = new IterationList(false);
            for (Iterator j = solver.getRelationsToSave().iterator(); j.hasNext();) {
                Relation r = (Relation) j.next();
                saveList.addElement(new Save(r, solver.getBaseDir() + r + ".bdd", false));
            }
            list.addElement(saveList);
        }
        return new IR(solver, ifg);
    }
   
    //public void optimize(){}
    public void optimize() {

        if (CONSTANTPROP) {
            solver.out.print("Running ConstantProp...");
            long time = System.currentTimeMillis();
            DataflowSolver df_solver = new DataflowSolver();
            Problem problem = new ConstantProp();
            IterationList list = graph.getIterationList();
            df_solver.solve(problem, list);
            DataflowIterator di = df_solver.getIterator(problem, graph);
            while (di.hasNext()) {
                Object o = di.next();
                if (TRACE) solver.out.println("Next: " + o);
                if (o instanceof Operation) {
                    Operation op = (Operation) o;
                    ConstantPropFacts f = (ConstantPropFacts) di.getFact();
                    Operation op2 = ((ConstantProp) problem).simplify(op, f);
                    if (op != op2) {
                        if (TRACE) solver.out.println("Replacing " + op + " with " + op2);
                        di.set(op2);
                    }
                } else {
                    IterationList b = (IterationList) o;
                    di.enter(b);
                }
            }
            solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
        }
        if (DEFUSE) {
            if (TRACE) solver.out.print("Running Def Use...");
            long time = System.currentTimeMillis();
            boolean changed = false;
            while (doDefUse())
                changed = true;
            solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
            if (TRACE && changed) solver.out.println("IR Changed after Defuse");
        }
        if (true) {
            solver.out.print("Running Peephole...");
            long time = System.currentTimeMillis();
            doPeephole(graph.getIterationList());
            solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
        }
        //printIR();
        if (DOMAIN_ASSIGNMENT) {
            solver.out.print("Running DomainAssignment...");
            long time = System.currentTimeMillis();
          /*  DataflowSolver solver = new DataflowSolver();
            PartialOrder p = new PartialOrder(this);
            solver.solve(p, graph.getIterationList());
            IndexMap relations =  this.solver.getRelations();
            
            Constraints[] constraintsMap = new Constraints[relations.size()];
            for(Iterator it = relations.iterator(); it.hasNext(); ){
                Relation r = (Relation) it.next();
                constraintsMap[r.id] = r.getConstraints();
            }*/
           // DomainAssignment ass = new PartialOrderDomainAssignment(this.solver, constraintsMap);
            DomainAssignment ass = new UFDomainAssignment(this.solver);
            IterationList list = graph.getIterationList();
            ass.addConstraints(list);
            ass.doAssignment();
            ass.setVariableOrdering();
            BufferedWriter dos = null;
            try {
                dos = new BufferedWriter(new FileWriter("domainassign.gen"));
                ass.saveDomainAssignment(dos);
            } catch (IOException x) {
                x.printStackTrace();
            } finally {
                if (dos != null) try {
                    dos.close();
                } catch (IOException x) {
                }
            }
            solver.out.println("cleaning up");
            cleanUpAfterAssignment(list);
            solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
        }
        //printIR();
      
        while (true) {
        
            boolean changed = false;
            if (false && PRE) {
                if (TRACE) solver.out.print("Running Partial Redundancy...");
                long time = System.currentTimeMillis();
                IRPass pre = new PartialRedundancy(this);
                boolean b = pre.run();
                if (TRACE) solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
                if (TRACE && b) solver.out.println("IR changed after partial redundancy");
                changed |= b;
            }
            if (COPYPROP) {
                if (TRACE) solver.out.print("Running Copy Propagation...");
                long time = System.currentTimeMillis();
                IRPass copy = new CopyProp(this);
                boolean b = copy.run();
                if (TRACE) solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
                if (TRACE && b) solver.out.println("IR changed after copy propagation");
                changed |= b;
            }
            if (DEAD_CODE) {
                if (TRACE) solver.out.print("Running Dead Code Elimination...");
                long time = System.currentTimeMillis();
                IRPass deadCode = new DeadCode(this);
                boolean b = deadCode.run();
                if (TRACE) solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
                if (TRACE && b) solver.out.println("IR Changed after dead code elimination");
                changed |= b;
            }
            if (!changed) break;
            // printIR();
        }
        
        if (FREE_DEAD) {
            if (TRACE) solver.out.print("Running Liveness Analysis...");
            long time = System.currentTimeMillis();
            IRPass liveness = new Liveness(this);
            liveness.run();
            if (TRACE) solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
        }
    }

    void verifyRename(Rename r){
        BDDRelation r0 = (BDDRelation) r.getRelationDest();
        BDDRelation r1 = (BDDRelation) r.getSrc();
        Map renameMap = r.getRenameMap();
        for(Iterator it = r1.getAttributes().iterator(); it.hasNext(); ){
            Attribute a1 = (Attribute) it.next();
            Attribute a0 = (Attribute) renameMap.get(a1);
            if(a0 == null){
                Assert._assert(r0.getAttributes().contains(a1));
                a0 = a1;
            }
            BDDDomain d1 = ((BDDRelation) r1).getBDDDomain(a1);
            BDDDomain d0 = ((BDDRelation) r0).getBDDDomain(a0);
            if( d1 != d0 ){
                solver.out.println(r);
                solver.out.println("src attributes: " + r1.getAttributes()+ " domains: " + ((BDDRelation) r1).getBDDDomains() +
                        " dest attributes: " + r0.getAttributes() + "domains: " + ((BDDRelation) r0).getBDDDomains() +
                        Operation.getRenames((BDDRelation) r0, (BDDRelation) r1));
                solver.out.println( "a0: " + a0 + "  d0: " + d0 + "  a1: " + a1 +" d1: " + d1);
                solver.out.println("rename map: " + renameMap);
                Assert.UNREACHABLE();
            }
        }
    }
    void cleanUpAfterAssignment(IterationList list) {
        for (ListIterator i = list.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof Rename) {
                i.remove();
                Rename r = (Rename) o;
                Relation r0 = r.getRelationDest();
                Relation r1 = r.getSrc();
                Copy op = new Copy(r0, r1);
                i.add(op);
            } else if (o instanceof Replace) {
                Replace r = (Replace) o;
                BDDPairing p = r.setPairing();
                if (p == null) {
                    i.remove();
                    Relation r0 = r.getRelationDest();
                    Relation r1 = r.getSrc();
                    Copy op = new Copy(r0, r1);
                    i.add(op);
                }
            }else if (o instanceof ApplyEx){
                ApplyEx a = (ApplyEx) o;
                a.setProjectSet();
            }else if(o instanceof Project){
                Project p = (Project) o;
                i.remove();
                BDDRelation r0 = (BDDRelation) p.getRelationDest();
                BDDRelation r1 = (BDDRelation) p.getSrc();
                BDDProject b = new BDDProject(r0,r1);
                i.add(b);
            } else if (o instanceof IterationList) {
                cleanUpAfterAssignment((IterationList) o);
            }
        }
    }

    void doPeephole(IterationList list) {
        for (ListIterator i = list.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof Copy) {
                Copy op = (Copy) o;
                if (op.getRelationDest() == op.getSrc()) i.remove();
            } else if (o instanceof IterationList) {
                doPeephole((IterationList) o);
            }
        }
    }

    boolean doDefUse() {
        solver.out.print("Running DefUse...");
        long time = System.currentTimeMillis();
        boolean change = false;
        DataflowSolver df_solver = new DataflowSolver();
        DefUse problem = new DefUse(this);
        IterationList list = graph.getIterationList();
        df_solver.solve(problem, list);
        DataflowIterator di = df_solver.getIterator(problem, graph);
        List to_remove = new LinkedList();
        outer : while (di.hasNext()) {
            Object o = di.next();
            if (TRACE) solver.out.println("Next: " + o);
            if (o instanceof Operation) {
                Operation op = (Operation) o;
                DefUseFact f = (DefUseFact) di.getFact();
                if (op.getRelationDest() != null) {
                    Collection uses = problem.getUses(op.getRelationDest());
                    if (TRACE) solver.out.println("Uses: " + uses);
                    if (uses.size() == 0) {
                        if (TRACE) solver.out.println("Removing: " + op);
                        di.remove();
                        change = true;
                        continue;
                    }
                }
                if (op instanceof Project) {
                    Project p = (Project) op;
                    Relation src = p.getSrc();
                    Set defs = f.getReachingDefs(src);
                    if (TRACE) solver.out.println("Defs: " + defs);
                    if (defs.size() == 1) {
                        Operation op2 = (Operation) defs.iterator().next();
                        if (op2 instanceof BooleanOperation) {
                            BooleanOperation boolop = (BooleanOperation) op2;
                            // check if this specific def reaches any other
                            // uses.
                            Set uses = problem.getUses(src);
                            if (TRACE) solver.out.println("Uses of " + src + ": " + uses);
                            for (Iterator i = uses.iterator(); i.hasNext();) {
                                Operation other = (Operation) i.next();
                                if (other == p) continue;
                                DefUseFact duf2 = (DefUseFact) problem.getFact(other);
                                if (duf2.getReachingDefs(src).contains(boolop)) {
                                    continue outer;
                                }
                            }
                            BDDOp bddop = boolop.getBDDOp();
                            ApplyEx new_op = new ApplyEx((BDDRelation) p.getRelationDest(), (BDDRelation) boolop.getSrc1(), bddop,
                                (BDDRelation) boolop.getSrc2());
                            if (TRACE) solver.out.println("Replacing " + op + " with " + new_op);
                            di.set(new_op);
                            if (TRACE) solver.out.println("Marking " + boolop + " for deletion.");
                            to_remove.add(boolop);
                        }
                    }
                }
            } else {
                IterationList b = (IterationList) o;
                di.enter(b);
            }
        }
        if (!to_remove.isEmpty()) {
            list.removeElements(to_remove);
            change = true;
        }
        solver.out.println(((System.currentTimeMillis() - time) / 1000.) + "s");
        return change;
    }

    /**
     *  
     */
    private IR(Solver solver, IterationFlowGraph g) {
        this.solver = solver;
        this.graph = g;
    }

    public Relation getRelation(String name) {
        return solver.getRelation(name);
    }

    public Relation getRelation(int index) {
        return solver.getRelation(index);
    }

    public int getNumberOfRelations() {
        return solver.getNumberOfRelations();
    }

    public void printIR() {
        printIR(graph.getIterationList(), "");
    }

    public void printIR(IterationList list, String space) {
        solver.out.println(space + list + ":");
        for (Iterator it = list.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Operation) {
                solver.out.println(space + "  " + o + "    " + getRenames((Operation) o));
            } else {
                printIR((IterationList) o, space + "  ");
            }
        }
    }

    public String getRenames(Operation op) {
        BDDRelation r0 = (BDDRelation) op.getRelationDest();
        if (r0 == null) return "";
        List srcs = op.getSrcs();
        StringBuffer sb = new StringBuffer();
        for (Iterator i = srcs.iterator(); i.hasNext();) {
            BDDRelation r2 = (BDDRelation) i.next();
            sb.append(Operation.getRenames(r2, r0));
        }
        if (sb.length() == 0) return "";
        return sb.substring(1);
    }
}
