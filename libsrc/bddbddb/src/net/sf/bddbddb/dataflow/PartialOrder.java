//PartialOrder.java, created Jul 3, 2004 1:44:46 PM by mcarbin
//Copyright (C) 2004 Michael Carbin <mcarbin@stanford.edu>
//Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jwutil.collections.GenericMultiMap;
import jwutil.collections.HashWorklist;
import jwutil.collections.MultiMap;
import jwutil.collections.Pair;
import jwutil.collections.UnionFind;
import jwutil.graphs.Navigator;
import jwutil.util.Assert;
import net.sf.bddbddb.Attribute;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.dataflow.PartialOrder.ConstraintGraph.ConstraintNavigator;
import net.sf.bddbddb.ir.IR;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;
import net.sf.bddbddb.ir.dynamic.If;
import net.sf.bddbddb.ir.dynamic.Nop;
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

/**
 * Partial order.
 * 
 * @author Michael Carbin
 * @version $Id: PartialOrder.java 531 2005-04-29 06:39:10Z joewhaley $
 */
public class PartialOrder extends OperationProblem {
    IR ir;
    boolean TRACE = false;
    public PartialOrderFact currFact;

    public PartialOrder(IR ir) {
        this.ir = ir;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.dataflow.Problem#direction()
     */
    public boolean direction() {
        return true;
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
     */
    public TransferFunction getTransferFunction(Operation o) {
        return new PartialOrderTF(o);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator it = operationFacts.entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            sb.append(e.getKey() + ": " + e.getValue() + "\n");
        }
        return sb.toString();
    }

    public Constraints getConstraints(Relation r) {
        return currFact.getConstraints(r);
    }
    public class PartialOrderFact implements OperationFact {
        Constraints[] constraintsMap;
        Operation op;
        IterationList loc;

        public PartialOrderFact() {
            constraintsMap = new Constraints[ir.solver.getNumberOfRelations()];
        }

        public Constraints getConstraints(Relation r) {
            return constraintsMap[r.id];
        }

        public Constraints[] getConstraintsMap() {
            return constraintsMap;
        }

        public void setConstraints(Relation r, Constraints constraints) {
            constraintsMap[r.id] = constraints;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.OperationProblem.OperationFact#getOperation()
         */
        public Operation getOperation() {
            return op;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.Problem.Fact#join(net.sf.bddbddb.dataflow.Problem.Fact)
         */
        public Fact join(Fact that) {
            PartialOrderFact result = (PartialOrderFact) copy(getLocation());
            PartialOrderFact thatFact = (PartialOrderFact) that;
            for (int i = 0; i < constraintsMap.length; i++) {
                result.constraintsMap[i].join(thatFact.constraintsMap[i]);
            }
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof PartialOrderFact) {
                PartialOrderFact that = (PartialOrderFact) o;
                return Arrays.equals(this.constraintsMap, that.constraintsMap);
            }
            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.constraintsMap.hashCode();
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.Problem.Fact#copy(net.sf.bddbddb.IterationList)
         */
        public Fact copy(IterationList loc) {
            PartialOrderFact result = new PartialOrderFact();
            System.arraycopy(constraintsMap, 0, result.constraintsMap, 0, constraintsMap.length);
            result.loc = loc;
            return result;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.Problem.Fact#setLocation(net.sf.bddbddb.IterationList)
         */
        public void setLocation(IterationList loc) {
            this.loc = loc;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.Problem.Fact#getLocation()
         */
        public IterationList getLocation() {
            return loc;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[ ");
            for (int i = 0; i < constraintsMap.length; i++) {
                Constraints c = constraintsMap[i];
                if (!c.isEmpty()) {
                    sb.append(ir.getRelation(i).toString() + ": ");
                    sb.append(c + " ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
    public class PartialOrderTF extends TransferFunction implements OperationVisitor {
        Operation op;

        public PartialOrderTF(Operation op) {
            this.op = op;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
         */
        public Fact apply(Fact f) {
            PartialOrderFact lastFact = (PartialOrderFact) f;
            currFact = (PartialOrderFact) lastFact.copy(lastFact.loc);
            op.visit(this);
            currFact.op = op;
            setFact(op, currFact);
            return currFact;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Join)
         */
        public Object visit(Join op) {
            if (TRACE) System.out.println(op);
            Relation src1 = op.getSrc1();
            Relation src2 = op.getSrc2();
            Relation dest = op.getRelationDest();
            Constraints union = visitUnionBinary(src1, src2);
            if (TRACE) System.out.println("union of src constraints: " + union);
            Constraints newCons = union.join(dest.getConstraints());
            if (TRACE) System.out.println("final constraints: " + newCons);
            currFact.setConstraints(op.getRelationDest(), newCons);
            return currFact;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Project)
         */
        public Object visit(Project op) {
            if (TRACE) System.out.println(op);
            Constraints c = currFact.getConstraints(op.getSrc());
            Constraints newCons = c.copy();
            if (TRACE) System.out.println("source constraints:" + c);
            List attrs = op.getAttributes();
            project(newCons, attrs);
            currFact.setConstraints(op.getRelationDest(), newCons);
            return currFact;
        }
        
        public Object visit(BDDProject op){
            Assert.UNREACHABLE(); /* TODO won't hande this for now */
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.ApplyEx)
         */
        public Object visit(ApplyEx op) {
            //decision
            if (TRACE) System.out.println(op);
            Constraints newCons = visitUnionBinary(op.getSrc1(), op.getSrc2());
            if (TRACE) System.out.println("union of source constraints: " + newCons);
            List attrs = op.getAttributes();
            project(newCons, attrs);
            Relation dest = op.getRelationDest();
            newCons.join(dest.getConstraints());
            if (TRACE) System.out.println("new constraints: " + newCons);
            currFact.setConstraints(dest, newCons);
            return currFact;
        }

        public Set project(Constraints cons, List attributes) {
            SortedSet relCons = cons.getRelevantConstraints(attributes);
            Constraints relCs = new Constraints(relCons);
            if (TRACE) System.out.println("relevant constraints: " + relCs);
            relCs.doTransitiveClosure();
            if (TRACE) System.out.println("transitive relevant constraints: " + relCs);
            cons.getBeforeConstraints().addAll(relCs.getBeforeConstraints());
            cons.getInterleavedConstraints().addAll(relCs.getInterleavedConstraints());
            Set removed = new HashSet();
            for (Iterator it = attributes.iterator(); it.hasNext();) {
                removed.addAll(cons.removeInvolving((Attribute) it.next()));
            }
            if (TRACE) System.out.println("removing: " + removed);
            return removed;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Rename)
         */
        public Object visit(Rename op) {
            if (TRACE) System.out.println(op);
            Constraints c = currFact.getConstraints(op.getSrc());
            BDDRelation dest = (BDDRelation) op.getRelationDest();
            Map renames = op.getRenameMap();
            List srcAttrs = op.getSrc().getAttributes();
            if (TRACE) System.out.println("src constraints: " + c);
            /*         if(TRACE) System.out.println("src attrs: " + srcAttrs);
             
             //Pair cons = c.getRelevantConstraints(srcAttrs);
             if(TRACE) System.out.println("relevant constraints: " + cons);
             */
          
            Constraints newCons = new Constraints();
            for (Iterator it = c.getBeforeConstraints().iterator(); it.hasNext();) {
                Constraint oldC = (Constraint) it.next();
                Attribute a1 = (Attribute) renames.get(oldC.getLeftAttribute());
                //a1 = a1 != null ? a1 : (Attribute) oldC.getLeftAttribute();
                Attribute a2 = (Attribute) renames.get(oldC.getRightAttribute());
                //a2 = a2 != null ? a2 : (Attribute) oldC.getRightAttribute();
                Constraint newC = a1 != null && a2 != null  ? new BeforeConstraint(dest, a1, dest, a2, oldC.confidence) : oldC;
             
            }
            for (Iterator it = c.getInterleavedConstraints().iterator(); it.hasNext();) {
                Constraint oldC = (Constraint) it.next();
                Attribute a1 = (Attribute) renames.get(oldC.getLeftAttribute());
               // a1 = a1 != null ? a1 : (Attribute) oldC.getLeftAttribute();
                Attribute a2 = (Attribute) renames.get(oldC.getRightAttribute());
              //  a2 = a2 != null ? a2 : (Attribute) oldC.getRightAttribute();
                Constraint newC = a1 != null && a2 != null ? new InterleavedConstraint(dest, a1, dest, a2, oldC.confidence) : oldC;
              
               newCons.addInterleavedConstraint(newC);
                
                
            }
            newCons.join(dest.getConstraints());
            if (TRACE) System.out.println("new constraints: " + newCons);
            currFact.setConstraints(dest, newCons);
            return currFact;
        }

        public Constraints visitUnionBinary(Relation src1, Relation src2) {
            Constraints c1 = currFact.getConstraints(src1);
            Constraints c2 = currFact.getConstraints(src2);
            return c1.join(c2);
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Union)
         */
        public Object visit(Union op) {
            if (TRACE) System.out.println(op);
            Relation src1 = op.getSrc1();
            Relation src2 = op.getSrc2();
            Relation dest = op.getRelationDest();
            Constraints newCons = visitUnionBinary(src1, src2);
            if (TRACE) System.out.println("union of source constraints: " + newCons);
            newCons.join(dest.getConstraints());
            if (TRACE) System.out.println("new constraints: " + newCons);
            currFact.setConstraints(dest, newCons);
            return currFact;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Difference)
         */
        public Object visit(Difference op) {
            if (TRACE) System.out.println(op);
            Relation src1 = op.getSrc1();
            Relation src2 = op.getSrc2();
            Relation dest = op.getRelationDest();
            Constraints newCons = visitUnionBinary(src1, src2);
            if (TRACE) System.out.println("union of source constraints: " + newCons);
            newCons.join(dest.getConstraints());
            if (TRACE) System.out.println("new constraints: " + newCons);
            currFact.setConstraints(dest, newCons);
            return currFact;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Load)
         */
        public Object visit(Load op) {
            if (TRACE) System.out.println(op);
            Relation dest = op.getRelationDest();
            if (TRACE) System.out.println("relation constraints: " + dest.getConstraints());
            currFact.setConstraints(dest, dest.getConstraints());
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.JoinConstant)
         */
        public Object visit(JoinConstant op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.GenConstant)
         */
        public Object visit(GenConstant op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Free)
         */
        public Object visit(Free op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Universe)
         */
        public Object visit(Universe op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Zero)
         */
        public Object visit(Zero op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Invert)
         */
        public Object visit(Invert op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Copy)
         */
        public Object visit(Copy op) {
            currFact.setConstraints(op.getRelationDest(), currFact.getConstraints(op.getSrc()));
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.highlevel.HighLevelOperationVisitor#visit(net.sf.bddbddb.ir.highlevel.Save)
         */
        public Object visit(Save op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.lowlevel.LowLevelOperationVisitor#visit(net.sf.bddbddb.ir.lowlevel.Replace)
         */
        public Object visit(Replace op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.If)
         */
        public Object visit(If op) {
            return null;
        }

        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.dynamic.DynamicOperationVisitor#visit(net.sf.bddbddb.ir.dynamic.Nop)
         */
        public Object visit(Nop op) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
     */
    public Fact getBoundary() {
        PartialOrderFact fact = new PartialOrderFact();
        for (int i = 0; i < fact.constraintsMap.length; i++)
            fact.constraintsMap[i] = new Constraints();
        return fact;
    }
    public abstract static class Constraint extends Pair implements Comparable{
        public static final int BEFORE = 0;
        public static final int INTERLEAVED = 1;
        double confidence;
        public Constraint(Relation leftRel, Attribute leftAttr,
            Relation rightRel, Attribute rightAttr, double confidence) {
            super(new Pair(leftRel,leftAttr), new Pair(rightRel,rightAttr));
            Assert._assert(leftRel.getAttributes().contains(leftAttr), "Left Attribute: " + leftAttr + " not part of Left Relation: " +leftRel);
            Assert._assert(rightRel.getAttributes().contains(rightAttr), "Right attribute: " + rightAttr + " not part of Right Relation: " + rightRel);
            this.confidence = confidence;
        }

        public Pair getLeftRelationAttrPair(){ return (Pair) left; }
        public Pair getRightRelationAttrPair(){ return (Pair) right; }
        public Relation getLeftRelation(){ return (Relation) ((Pair) left).left; }
        public Attribute getLeftAttribute(){ return (Attribute) ((Pair)left).right; }
        public Relation getRightRelation(){ return (Relation) ((Pair) right).left; }
        public Attribute getRightAttribute(){ return (Attribute) ((Pair)right).right; }
        
        public Constraint(Relation leftRel, Attribute leftAttr,
            Relation rightRel, Attribute rightAttr) {
            this(leftRel, leftAttr,rightRel, rightAttr, 0);
        }
        
       public String toString(){
           return  "(" + left + (getType() == BEFORE ? " before " : " interleaved with ") + right + " conf: " + confidence + ")";
       }
       
       public abstract int getType();
       
       public boolean isBeforeConstraint(){ return getType() == BEFORE; }
       public boolean isInterleavedConstraint(){ return getType() == INTERLEAVED; }
    
      public int compareTo(Object o){
          Constraint that = (Constraint) o;
          return Double.compare(that.confidence,this.confidence);
      }
    }
    
    public static class InterleavedConstraint extends Constraint{
        
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3257003254876616756L;
        
        /**
         * @param leftRel
         * @param leftAttr
         * @param rightRel
         * @param rightAttr
         */
        public InterleavedConstraint(Relation leftRel, Attribute leftAttr, Relation rightRel, Attribute rightAttr) {
            super(leftRel, leftAttr, rightRel, rightAttr);
      
        }
        /**
         * @param leftRel
         * @param leftAttr
         * @param rightRel
         * @param rightAttr
         * @param confidence
         */
        public InterleavedConstraint(Relation leftRel, Attribute leftAttr, Relation rightRel, Attribute rightAttr, double confidence) {
            super(leftRel, leftAttr, rightRel, rightAttr, confidence);

        }
        public int getType(){ return INTERLEAVED; }
     
        
        
    }
    
    public static class BeforeConstraint extends Constraint{
        
        /**
         * Version ID for serialization.
         */
        private static final long serialVersionUID = 3835158341730514996L;
        
        /**
         * @param leftRel
         * @param leftAttr
         * @param rightRel
         * @param rightAttr
         */
        public BeforeConstraint(Relation leftRel, Attribute leftAttr, Relation rightRel, Attribute rightAttr) {
            super(leftRel, leftAttr, rightRel, rightAttr);
      
        }
        /**
         * @param leftRel
         * @param leftAttr
         * @param rightRel
         * @param rightAttr
         * @param confidence
         */
        public BeforeConstraint(Relation leftRel, Attribute leftAttr, Relation rightRel, Attribute rightAttr, double confidence) {
            super(leftRel, leftAttr, rightRel, rightAttr, confidence);
  
        }
        public int getType(){ return BEFORE; }
    }
    public static class Constraints {
        static boolean TRACE = true;
        MultiMap graph;
        UnionFind uf;
  /*      Collection beforeConstraints;
        Collection interleavedConstraints;
   */
           SortedSet constraints;

        public Constraints() {
            constraints = new TreeSet();
     /*       beforeConstraints = new LinkedHashSet();
            interleavedConstraints = new LinkedHashSet();
       */
         }
/*
        public Constraints(Collection beforeConstraints, Collection interConstraints) {
            this.beforeConstraints = beforeConstraints;
            this.interleavedConstraints = interConstraints;
        }
*/
        public Constraints(SortedSet constraints){
            this.constraints = constraints;
        }
        public SortedSet getRelevantConstraints(Collection attributes) {
            SortedSet cons = new TreeSet();
            cons.addAll(relevantBeforeConstraints(attributes));
            cons.addAll(relevantInterConstraints(attributes));
            return cons;
        }

        public Collection getBeforeConstraints() {
            SortedSet cons = new TreeSet();
            for(Iterator it = constraints.iterator(); it.hasNext(); ){
                Constraint con = (Constraint) it.next();
                if(con.isBeforeConstraint())
                    cons.add(con);
            }
            
            return cons;
        }

        public SortedSet getInterleavedConstraints() {
            SortedSet cons = new TreeSet();
            for(Iterator it = constraints.iterator(); it.hasNext(); ){
                Constraint con = (Constraint) it.next();
                if(con.isInterleavedConstraint())
                    cons.add(con);
            }
            
            return cons;
        }
        
        public SortedSet getAllConstraints(){ return constraints; }

        private List relevantConstraints(Collection attributes, Collection srcCons) {
            List relevantConstraints = new LinkedList();
            for (Iterator it = srcCons.iterator(); it.hasNext();) {
                Constraint c = (Constraint) it.next();
                if (attributes.contains(c.left) || attributes.contains(c.right)) relevantConstraints.add(c);
            }
            return relevantConstraints;
        }

        public List relevantBeforeConstraints(Collection attributes) {
            return relevantConstraints(attributes, getBeforeConstraints());
        }

        public List relevantInterConstraints(Collection attributes) {
            return relevantConstraints(attributes, getInterleavedConstraints());
        }

        public void addBeforeConstraint(Constraint c) {
            constraints.add(c);
        }

        public void addInterleavedConstraint(Constraint c) {
            constraints.add(c);
        }

        public Constraints copy() {
            Constraints c = new Constraints();
            c.constraints = new TreeSet(this.constraints);
            return c;
        }

        public List removeInvolving(Attribute a) {
            List removed = new LinkedList();
            removed.addAll(removeInvolving(a, constraints));
           // removed.addAll(removeInvolving(a, interleavedConstraints));
            return removed;
        }

        private List removeInvolving(Attribute a, Collection cons) {
            List removed = new LinkedList();
            for (Iterator it = cons.iterator(); it.hasNext();) {
                Constraint c = (Constraint) it.next();
                if (c.contains(a)) {
                    removed.add(c);
                    it.remove();
                }
            }
            return removed;
        }

        public List buildGraphAndReps(){
            ConstraintGraph graph = new ConstraintGraph();
            MultiMap repToAttributes = new GenericMultiMap();
            uf = new UnionFind(4096);
           Set seen = new HashSet();
            for (Iterator it = getInterleavedConstraints().iterator(); it.hasNext();) {
                Pair p = (Pair) it.next();
                seen.add(p.left);
                seen.add(p.right);
                Object repl = uf.find(p.left);
                Object repr = uf.find(p.right);
                if (repl == null && repr == null) {
                    uf.union(p.left, p.right);
                } else if (repl != null && repr == null) {
                    uf.union(repl, p.right);
                } else if (repl == null && repr != null) {
                    uf.union(p.left, repr);
                } else {
                    uf.union(repl, repr);
                }
            }
            Set nodes = new HashSet();
          
            for (Iterator it = getBeforeConstraints().iterator(); it.hasNext();) {
                Pair p = (Pair) it.next();
                Object repl = uf.find(p.left);
                Object repr = uf.find(p.right);
                seen.add(repl);
                seen.add(repr);
                if (repl == null && repr == null) {
                    graph.addEdge(p.left, p.right);
                } else if (repl != null && repr == null) {
                    graph.addEdge(repl, p.right);
                } else if (repl == null && repr != null) {
                    graph.addEdge(p.left, repr);
                } else {
                    graph.addEdge(repl, repr);
                }
            }
            
            for (Iterator it = seen.iterator(); it.hasNext();) {
                Object o = it.next();
                Object rep = uf.find(o);
                graph.addNode(rep);
                repToAttributes.add(rep, o);
            }
            
            List result = new LinkedList();
            result.add(graph);
            result.add(uf);
            result.add(repToAttributes);
            return result;
        }
      

        List findCycles(Object start, Set visited, List trace, ConstraintNavigator nav) {
            List cycles = new LinkedList();
            trace.add(start);
            visited.add(start);
            Collection nexts = nav.next(start);
            for (Iterator it = nexts.iterator(); it.hasNext();) {
                Object next = it.next();
                if (visited.contains(next)) {
                    if (trace.contains(next)) {
                        int index = trace.indexOf(next);
                        List cycle = new LinkedList(trace.subList(index, trace.size()));
                        cycles.add(cycle);
                    }
                } else {
                    cycles.addAll(findCycles(next, visited, trace, nav));
                }
            }
            trace.remove(trace.size() - 1);
            return cycles;
        }

        List cycleToConstraints(List cycle, MultiMap repToAttributes) {
           // System.out.println("reps: " + repToAttributes);
            List constraints = new LinkedList();
            for (ListIterator it = cycle.listIterator(); it.hasNext();) {
                Object o1 = it.next();
                if (it.hasNext()) {
                    constraints.addAll(constraints(o1, it.next(), repToAttributes));
                    it.previous();
                }
            }
            constraints.addAll(constraints(cycle.get(cycle.size() - 1), cycle.get(0),repToAttributes));
            return constraints;
        }

        List constraints(Object o1, Object o2, MultiMap repToActual) {
            List constraints = new LinkedList();
            Collection o1jects = repToActual.getValues(o1);
            Collection o2jects = repToActual.getValues(o2);
            Collection beforeConstraints = getBeforeConstraints();
            for (Iterator it = o1jects.iterator(); it.hasNext();) {
                Pair left = (Pair) it.next();
                for (Iterator jt = o2jects.iterator(); jt.hasNext();) {
                    Pair right = (Pair) jt.next();
                    Constraint p = new BeforeConstraint((Relation) left.left, (Attribute) left.right,
                                                (Relation) right.left, (Attribute) right.right);
                    if (beforeConstraints.contains(p)) {
                        constraints.add(p);
                    }
                }
            }
            return constraints;
        }

   /*     void breakCycles(List cycles) {
            List ccycles = new LinkedList();
            for (Iterator it = cycles.iterator(); it.hasNext();) {
                List cycle = cycleToConstraints((List) it.next());
                ccycles.add(cycle);
            }
            Iterator it = ccycles.iterator();
            List lowest = (List) it.next();
            int lowestRate = rateCycle(lowest);
            for (; it.hasNext();) {
                List next = (List) it.next();
                int nextRate = rateCycle(next);
                if (nextRate < lowestRate) {
                    lowest = next;
                    lowestRate = nextRate;
                }
            }
            breakConstraintCycle(lowest);
        }

        int rateCycle(List cycle) {
            int i = 0;
            int sum = 0;
            for (; i < cycle.size(); i++) {
                sum += ((Constraint) cycle.get(i)).confidence;
            }
            return sum / i;
        }

        void breakConstraintCycle(List cycle) {
            Iterator jt = cycle.iterator();
            Constraint lowest = (Constraint) jt.next();
            for (; jt.hasNext();) {
                Constraint next = (Constraint) jt.next();
                if (next.confidence < lowest.confidence) lowest = next;
                
            }
            beforeConstraints.remove(lowest);
        }
*/
        public void satisfy() {
            System.out.println("satisfying constraints" + hashCode());
            List cycle = new LinkedList();
            List info = buildGraphAndReps();
            ConstraintGraph graph = (ConstraintGraph) info.get(0);
            UnionFind uf = (UnionFind) info.get(1);
            MultiMap repToAttributes = (MultiMap) info.get(2);
            while(true){
                cycle.clear();
                if(graph.isCycle(cycle)){
                   System.out.println("cycle: " + cycle);
                    List constraints = cycleToConstraints(cycle,repToAttributes);
                    System.out.println("possibilities: " + constraints);
                    Iterator jt = constraints.iterator();
                    Constraint lowest = (Constraint) jt.next();
                    for (; jt.hasNext(); ) {
                        Constraint next = (Constraint) jt.next();
                        if (next.confidence < lowest.confidence) lowest = next;
                    }
                    System.out.println("removing: " + lowest);
                    if(constraints.remove(lowest))
                        System.out.println("removed");
                    info = buildGraphAndReps();
                    graph = (ConstraintGraph) info.get(0);
                    uf = (UnionFind) info.get(1);
                    repToAttributes = (MultiMap) info.get(2);
                }else
                    break;
            }
        }

        public Constraints join(Constraints that) {
           
            Constraints result = new Constraints();
            /*result.beforeConstraints = new HashSet(this.beforeConstraints);
            result.beforeConstraints.addAll(that.beforeConstraints);
            result.interleavedConstraints = new HashSet(this.interleavedConstraints);
            result.interleavedConstraints.addAll(that.interleavedConstraints);
            result.satisfy();
            */
            result.constraints = new TreeSet(this.constraints);
            result.satisfy();
            return result;
        }

        public boolean isEmpty() {
            return constraints.isEmpty();
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof Constraints) {
                Constraints that = (Constraints) o;
                return this.constraints.equals(that.constraints);
            }
            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return constraints.hashCode();
        }
        
        public String toString() {
            return "[before constraints: " + getBeforeConstraints() + " interleaved constraints: " + getInterleavedConstraints() + "]";
        }

        public void doTransitiveClosure() {
            SortedSet newConstraints = new TreeSet();
            newConstraints.addAll(doTransitiveClosure(getBeforeConstraints()));
            newConstraints.addAll(doTransitiveClosure(getInterleavedConstraints()));
        }

        public Collection doTransitiveClosure(Collection/*Constraint*/ constraints) {
            ConstraintGraph graph = new ConstraintGraph(constraints);
            Collection newConstraints = new LinkedHashSet(constraints);
            ConstraintGraph.ConstraintNavigator nav = graph.getNavigator();
            HashWorklist w = new HashWorklist(true);
            w.addAll(constraints);
            //transitive closure
            while (!w.isEmpty()) {
                Constraint con = (Constraint) w.pull();
                Pair left = con.getLeftRelationAttrPair();
                Pair right = con.getRightRelationAttrPair();
                Collection nexts = nav.next(right);
                for (Iterator it = nexts.iterator(); it.hasNext();) {
                    Pair trans = (Pair) it.next();
                    if(con.getType() == Constraint.BEFORE)
                    newConstraints.add(new BeforeConstraint((Relation) left.left, (Attribute) left.right, (Relation) trans.left, (Attribute)trans.right)); //consider confidence
                    else
                        newConstraints.add(new InterleavedConstraint((Relation) left.left, (Attribute) left.right, (Relation) trans.left, (Attribute)trans.right)); //consider confidence
                    
                    w.add(new Pair(left, trans));
                }
            }
            return newConstraints;
        }
    }
    public static class ConstraintGraph {
        MultiMap graph;
        Collection nodes;
        ConstraintNavigator nav;

        public ConstraintGraph() {
            nav = new ConstraintNavigator();
            graph = new GenericMultiMap();
            nodes = new HashSet();
        }
        public ConstraintGraph(ConstraintGraph that){
            this.graph = ((GenericMultiMap)that.graph).copy();
            this.nodes = new HashSet(that.nodes);
            nav = new ConstraintNavigator();
        }
        
        public void update(UnionFind uf){
            MultiMap newGraph = new GenericMultiMap();
            Set newNodes = new HashSet();
            for(Iterator it = nodes.iterator(); it.hasNext(); ){
                Object node = it.next();
                System.out.println("node: " + node + " rep: " + uf.find(node));
                newNodes.add(uf.find(node));
            }
            for(Iterator it = graph.entrySet().iterator(); it.hasNext(); ){
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                Object keyRep = uf.find(key);
                Object valueRep = uf.find(value);
                newGraph.add(keyRep, valueRep);
            }
            graph = newGraph;
            nodes = newNodes;
        }

        public ConstraintGraph(Collection constraints) {
            this();
            for (Iterator it = constraints.iterator(); it.hasNext();) {
                Pair p = (Pair) it.next();
                Object o1 = p.left;
                Object o2 = p.right;
                graph.add(o1, o2);
                nodes.add(o1);
                nodes.add(o2);
            }
        }

        public ConstraintGraph(Collection nodes, Collection constraints) {
            this();
            this.nodes.addAll(nodes);
            for (Iterator it = constraints.iterator(); it.hasNext();) {
                Pair p = (Pair) it.next();
                Object o1 = p.left;
                Object o2 = p.right;
                graph.add(o1, o2);
            }
        }

        public void addEdge(Object o1, Object o2) {
            graph.add(o1, o2);
        }
        
        public void removeEdge(Object o1, Object o2){
            graph.remove(o1, o2);
        }

        public void removeEdgesFrom(Object o) {
            graph.remove(o);
        }

        public void addNode(Object o) {
            nodes.add(o);
        }
        public void addNodes(Collection nodes){
            this.nodes.addAll(nodes);
        }
        public void removeNode(Object o) {
            nodes.remove(o);
        }
        
        /**
         * @return collection of nodes
         */
        public Collection getNodes() {
            return nodes;
        }

        public boolean isCycle(List cycle) {
            for (Iterator it = nodes.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (isPath(obj, obj,cycle)) return true;
            }
            return false;
        }

        public boolean isPath(Object start, Object end, List path) {
            return isPath(start, end, new HashSet(), path);
        }

        private boolean isPath(Object start, Object target, Set visited, List path) {
            path.add(start);
            visited.add(start);
           // System.out.println("start: " + start);
            Collection nexts = graph.getValues(start);
           // System.out.println("nexts: " + nexts);
            for (Iterator it = nexts.iterator(); it.hasNext();) {
                Object next = it.next();
              //  System.out.println("next: " + next);
                if (next == target) {
                    return true;
                }
                if (!visited.contains(next)) 
                    if (isPath(next, target, visited, path)) 
                        return true;
            }
            path.remove(start);
            return false;
        }

        public Collection getRoots() {
            Set roots = new HashSet(nodes);
            roots.removeAll(graph.values());
            return roots;
        }

        public String toString() {
            return graph.toString();
        }

        public ConstraintNavigator getNavigator() {
            return nav;
        }
        public class ConstraintNavigator implements Navigator {
            /* (non-Javadoc)
             * @see net.sf.bddbddb.util.Navigator#next(java.lang.Object)
             */
            public Collection next(Object node) {
                return graph.getValues(node);
            }

            /* (non-Javadoc)
             * @see net.sf.bddbddb.util.Navigator#prev(java.lang.Object)
             */
            public Collection prev(Object node) {
                throw new UnsupportedOperationException();
            }
        }
    
    }
}
