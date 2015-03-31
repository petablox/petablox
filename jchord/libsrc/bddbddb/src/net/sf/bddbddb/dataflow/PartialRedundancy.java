//PartialOrder.java, created Jul 6, 2004 1:44:46 PM by mcarbin
//Copyright (C) 2004 Michael Carbin <mcarbin@stanford.edu>
//Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import jwutil.collections.IndexMap;
import jwutil.collections.IndexedMap;
import jwutil.collections.Pair;
import jwutil.math.BitString;
import jwutil.math.BitString.BitStringIterator;
import jwutil.util.Assert;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.Solver;
import net.sf.bddbddb.dataflow.DefUse.DefUseFact;
import net.sf.bddbddb.dataflow.OperationProblem.OperationFact;
import net.sf.bddbddb.dataflow.PartialRedundancy.Anticipated.AnticipatedFact;
import net.sf.bddbddb.dataflow.PartialRedundancy.Available.AvailableFact;
import net.sf.bddbddb.dataflow.PartialRedundancy.Earliest.EarliestFact;
import net.sf.bddbddb.dataflow.PartialRedundancy.Latest.LatestFact;
import net.sf.bddbddb.dataflow.Problem.Fact;
import net.sf.bddbddb.ir.IR;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.OperationVisitor;
import net.sf.bddbddb.ir.dynamic.Nop;
import net.sf.bddbddb.ir.highlevel.Copy;
import net.sf.bddbddb.ir.highlevel.GenConstant;
import net.sf.bddbddb.ir.highlevel.Join;
import net.sf.bddbddb.ir.highlevel.JoinConstant;
import net.sf.bddbddb.ir.highlevel.Load;
import net.sf.bddbddb.ir.highlevel.Project;
import net.sf.bddbddb.ir.highlevel.Rename;
import net.sf.bddbddb.ir.highlevel.Save;
import net.sf.bddbddb.ir.highlevel.Universe;
import net.sf.bddbddb.ir.highlevel.Zero;
import net.sf.bddbddb.ir.lowlevel.Replace;

/**
 * Partial redundancy elimination.
 * 
 * @author mcarbin
 * @version $Id: PartialRedundancy.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class PartialRedundancy implements IRPass {
    public DefUse defUse;
    public Anticipated anticipated;
    public Available available;
    public Earliest earliest;
    public Used used;
    public Postponed postponed;
    public Latest latest;
    Solver solver;
    IR ir;
    ExpressionSet allExpressions;
    boolean TRACE = false;
    int[] opToExpression;
    public PartialRedundancy(IR ir) {
        this.ir = ir;
        this.solver = ir.solver;
        
        anticipated = new Anticipated();
        available = new Available();
        earliest = new Earliest();
        used = new Used();
        postponed = new Postponed();
        latest = new Latest();
        initialize(ir.graph.getIterationList());
        //after creation of nops
        opToExpression = new int[Operation.getNumberOfOperations()];//new HashMap();
        defUse = new DefUse(ir);
    }
    
    public void initialize(IterationList list) {
        for (ListIterator it = list.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof IterationList) {
                IterationList l = (IterationList) o;
                if (l.isLoop()) {
                    l.getLoopEdge().addElement(new Nop());
                    IterationList newList = new IterationList(false);
                    newList.addElement(new Nop());
                    it.previous();                
                    it.add(newList);
                    it.next();
                }
                initialize(l);
            }
        }
    }
    
    public boolean run() {
        IterationList list = ir.graph.getIterationList();
            DataflowSolver solver = new DataflowSolver();
            solver.solve(defUse,ir.graph.getIterationList());
   
            getExpressions();
            
            solver = new DataflowSolver();
            solver.solve(anticipated, list);
            
            solver = new DataflowSolver();
            solver.solve(available, list);
            
            solver = new DataflowSolver();
            solver.solve(earliest, list);
            
            solver = new DataflowSolver();
            solver.solve(postponed, list);
            
            solver = new DataflowSolver();
            solver.solve(latest, list);
            
            solver = new DataflowSolver();
            solver.solve(used, list);

            if(TRACE) System.out.println("transform");
        return transform(list);
    }
    
    public void printOperationMap(Map operationMap){
        StringBuffer sb = new StringBuffer();
        SortedMap sortedMap = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Operation) o1).id - ((Operation) o2).id;
            }
        });
        sortedMap.putAll(operationMap);
        for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
        }
        System.out.println(sb.toString());  
    }
    Map phis = new HashMap();
    Map myOpToExpression = new HashMap();
    IndexedMap expressions = new IndexMap("expressions");
    public void getExpressions(){ 
        getExpressions(ir.graph.getIterationList(), 0);
        
        //fill in phi expressions
        Set visited = new HashSet();
        for(Iterator it = myOpToExpression.values().iterator(); it.hasNext(); ){
            Expression e = (Expression) it.next();
            for(Iterator jt = e.subExpressions().iterator(); jt.hasNext(); ){
                Expression e2 = (Expression) jt.next();
                if(e2.op instanceof Phi && !visited.contains(e2.op)){
                    Phi p = (Phi) e2.op;
                    for(Iterator kt = p.operations.iterator(); kt.hasNext(); ){
                        e2.subExpressions.add(myOpToExpression.get(kt.next()));
                    }
                    visited.add(p);
                }
            } 
        }
        
        mapOpsToExpressions(ir.graph.getIterationList());
        
        BitString s = new BitString(expressions.size());
        s.setAll();
        allExpressions = new ExpressionSet(s);
    }
    
    public void mapOpsToExpressions(IterationList list){
        for(Iterator it = list.iterator(); it.hasNext(); ){
            Object o = it.next();
            if(o instanceof Operation){
                Operation op = (Operation) o;
                Expression e = (Expression) myOpToExpression.get(op);
                int index = -1;
                if(TRACE) System.out.print("Op: " + op + " value: "); 
                if(e != null){
                 e = e.number();
                 opToExpression[op.id] = index = e.number; //expressions.get(e);
                }
         
                if(TRACE) System.out.println(Integer.toString(index));
            }else{
                mapOpsToExpressions((IterationList) o );
            }
        }
    }
    
    public boolean considerable(Operation op){
        if(op instanceof Copy) return false;
        if(op instanceof Load) return false;
        if(op instanceof Save) return false;
        if(op instanceof Universe) return false;
        if(op instanceof Zero) return false;
        return true;
    }
    
    public void getExpressions(IterationList list, int depth){
        for(Iterator it = list.iterator(); it.hasNext(); ){
            Object o = it.next();;
            if(o instanceof Operation){
               // if(TRACE) System.out.println("Next: " + o);
                Operation op = (Operation) o;
                DefUseFact fact = defUse.getIn(op);
                Relation dest = op.getRelationDest();
                if(dest != null){
                    List srcs = op.getSrcs();
                    Expression newExpression = new Expression(op, new LinkedList(), depth);
                    myOpToExpression.put(op,newExpression);
                    for(Iterator jt = srcs.iterator(); jt.hasNext(); ){
                        Relation r = (Relation) jt.next();   
                        if(r != null){   
                            Collection defs = null;
                            Expression subExpression = null;
                            if(( defs = fact.getReachingDefs(r)).size() > 1){
                                if(TRACE) System.out.println(r + " has multiple definitions");
                                Pair p = new Pair(r,fact.getLocation());
                                //Pair p = new Pair(r,defs);
                                Phi phi = (Phi) phis.get(p);
                                if(phi == null){
                                    phi = new Phi(r,defs);
                                    phis.put(p, phi);
                                }
                                //if(TRACE) System.out.println(phi);
                                subExpression = new Expression(phi, new LinkedList(),depth);
                            }else{
                                Iterator kt = defs.iterator();
                                if(kt.hasNext()){
                                    
                                    subExpression = (Expression) myOpToExpression.get(kt.next());
                                }
                            }
                            if(subExpression != null)
                                newExpression.subExpressions.add(subExpression);
       
                        }
                    }
                }
                
            }else{
                getExpressions((IterationList)o, depth + 1);
            }
         }
        
    }

    
    public static class Phi extends Operation{
        Relation dest;
        Collection operations;
        public Phi(Relation dest, Collection operations){
            this.dest = dest;
            this.operations = operations;
        }
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#visit(net.sf.bddbddb.ir.OperationVisitor)
         */
        public Object visit(OperationVisitor i) {
            return null;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#toString()
         */
        public String toString() {
            return dest + " = " + getExpressionString();
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#getRelationDest()
         */
        public Relation getRelationDest() {
            return dest;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#setRelationDest(net.sf.bddbddb.Relation)
         */
        public void setRelationDest(Relation r0) {             
            dest = r0;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#getSrcs()
         */
        public List getSrcs() {
            return Collections.EMPTY_LIST;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#replaceSrc(net.sf.bddbddb.Relation, net.sf.bddbddb.Relation)
         */
        public void replaceSrc(Relation r_old, Relation r_new) {
            /*     if (left == r_old) left = r_new;
             if (right == r_old) right = r_new;
             */ 
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#getExpressionString()
         */
        public String getExpressionString() {
            return "phi" + operations;
        }
        
        /* (non-Javadoc)
         * @see net.sf.bddbddb.ir.Operation#copy()
         */
        public Operation copy() {
            return new Phi(dest,operations);
        }
        
    }
    public class Anticipated extends OperationProblem {
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#direction()
         */
        public boolean direction() {
            return false;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
         */
        public Fact getBoundary() {
            return new AnticipatedFact();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
         */
        public TransferFunction getTransferFunction(Operation o) {
            return new AnticipatedTF(o);
        }
        public class AnticipatedFact extends PreFact {
            public PreFact create() {
                return new AnticipatedFact();
            }
            
            /*
             * (non-Javadoc) perform set intersection
             * 
             * @see net.sf.bddbddb.dataflow.Problem.Fact#join(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact join(Fact that) {
                AnticipatedFact thatFact = (AnticipatedFact) that;
                AnticipatedFact result = new AnticipatedFact();
                result.loc = this.loc;
                result.expressions.addAll(this.expressions);
                result.expressions.retainAll(thatFact.expressions);
                return result;
            }
            
         
        }
        class AnticipatedTF extends TransferFunction {
            Operation op;
            
            public AnticipatedTF(Operation op) {
                this.op = op;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact apply(Fact f) {
                AnticipatedFact lastFact = (AnticipatedFact) f;
                //System.out.println(" lastFact: " + lastFact);
                AnticipatedFact currFact = (lastFact != null) ? (AnticipatedFact) lastFact.copy() : new AnticipatedFact();
                Expression e = (Expression) expressions.get(opToExpression[op.id]);
               // if(TRACE) System.out.println("operation: " + op);
               // if(TRACE) System.out.println("input expressions: " + currFact.expressions);
                //Assert._assert(e != null, "expression for: " + op + " is null");
                if(op.getRelationDest() != null)
                    currFact.killExpressions(op);
                if(e.op == op) //add only if i am the representative
                    currFact.addExpression(e);
               // if(TRACE) System.out.println("ouput expressions: " + currFact.expressions);
                
                //System.out.println(" result : " + currFact);
                currFact.op = op;
                setFact(op, currFact);
                return currFact;
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    public class Available extends OperationProblem {
        public Map availOpIns;
        
        /**
         *  
         */
        public Available() {
            availOpIns = new HashMap();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.OperationProblem#direction()
         */
        public boolean direction() {
            return true;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
         */
        public TransferFunction getTransferFunction(Operation o) {
            // TODO Auto-generated method stub
            return new AvailableTF(o);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
         */
        public Fact getBoundary() {
            return new AvailableFact();
        }
        public class AvailableFact extends PreFact {
            public PreFact create() {
                return new AvailableFact();
            }
            
            /*
             * perform intersection
             * 
             * @see net.sf.bddbddb.dataflow.Problem.Fact#join(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact join(Fact that) {
                AvailableFact result = new AvailableFact();
                AvailableFact thatFact = (AvailableFact) that;
                result.expressions.addAll(this.expressions);
                result.expressions.retainAll(thatFact.expressions);
                result.loc = this.loc;
                return result;
            }
       
        }
        class AvailableTF extends TransferFunction {
            Operation op;
            
            public AvailableTF(Operation op) {
                this.op = op;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact apply(Fact f) {
                AvailableFact lastFact = (AvailableFact) f;
                setIn(op, lastFact);
                AvailableFact currFact = (lastFact) != null ? (AvailableFact) lastFact.copy() : new AvailableFact();
             //   if(TRACE) System.out.println("operation: " + op);
             //  if(TRACE) System.out.println("input expressions: " + currFact.expressions);
               
                AnticipatedFact antiFact = (AnticipatedFact) anticipated.getFact(op);
               
                currFact.addExpressions(antiFact.getExpressions());
                currFact.killExpressions(op);
              //  if(TRACE) System.out.println("ouput expressions: " + currFact.expressions);
                
                currFact.op = op;
                setFact(op, currFact);
                return currFact;
            }
        }
        
        private void setIn(Operation op, AvailableFact lastFact) {
            availOpIns.put(op, lastFact);
        }
        
        public AvailableFact getIn(Operation op) {
            return (AvailableFact) availOpIns.get(op);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    public class Earliest extends OperationProblem {
        public boolean direction() {
            return true;
        }
        
        public TransferFunction getTransferFunction(Operation o) {
            return new EarliestTF(o);
        }
        
        public Fact getBoundary() {
            return new EarliestFact();
        }
        public class EarliestTF extends TransferFunction {
            Operation op;
            
            /**
             * @param op
             */
            public EarliestTF(Operation op) {
                super();
                this.op = op;
            }
            
            public Fact apply(Fact f) {
                EarliestFact lastFact = (EarliestFact) f;
                AnticipatedFact antiFact = (AnticipatedFact) anticipated.getFact(op);
                AvailableFact availFact = available.getIn(op);
                EarliestFact currFact = new EarliestFact();
                currFact.addExpressions(antiFact.getExpressions());
                if (availFact != null) currFact.removeExpressions(availFact.getExpressions());
                //currFact.removeExpression((Expression)
                // opToExpression.get(op));
                currFact.op = op;
                setFact(op, currFact);
                return currFact;
            }
        }
        public class EarliestFact extends PreFact {
            public PreFact create() {
                return new EarliestFact();
            }
            
            public Fact join(Fact that) {
                return this;
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    public class Postponed extends OperationProblem {
        Map opIns;
        
        /**
         *  
         */
        public Postponed() {
            super();
            opIns = new HashMap();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.OperationProblem#direction()
         */
        public boolean direction() {
            return true;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
         */
        public TransferFunction getTransferFunction(Operation o) {
            return new PostponedTF(o);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
         */
        public Fact getBoundary() {
            return new PostponedFact();
        }
        class PostponedTF extends TransferFunction {
            Operation op;
            
            /**
             * @param op
             */
            public PostponedTF(Operation op) {
                super();
                this.op = op;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact apply(Fact f) {
                PostponedFact lastFact = (PostponedFact) f;
                setIn(op, lastFact);
                EarliestFact earlFact = (EarliestFact) earliest.getFact(op);
                PostponedFact newFact = (PostponedFact) lastFact.copy();
                newFact.addExpressions(earlFact.expressions);
                newFact.removeExpression((Expression) expressions.get(opToExpression[op.id]));
                newFact.op = op;
                setFact(op, newFact);
                return newFact;
            }
        }
        public class PostponedFact extends PreFact {
            public Fact join(Fact that) {
                PostponedFact thatFact = (PostponedFact) that;
                PostponedFact result = new PostponedFact();
                result.expressions.addAll(this.expressions);
                result.expressions.retainAll(thatFact.expressions);
                result.loc = this.loc;
                return result;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.PartialRedundancy.PreFact#create()
             */
            public PreFact create() {
                return new PostponedFact();
            }
        }
        
        public void setIn(Operation op, PostponedFact fact) {
            opIns.put(op, fact);
        }
        
        public PostponedFact getIn(Operation op) {
            return (PostponedFact) opIns.get(op);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    public class Latest extends OperationProblem {
        /**
         * @author Administrator
         * 
         * TODO To change the template for this generated type comment go to
         * Window - Preferences - Java - Code Style - Code Templates
         */
        public class LatestTF extends TransferFunction {
            Operation op;
            
            /**
             * @param op
             */
            public LatestTF(Operation op) {
                super();
                this.op = op;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact apply(Fact f) {
                LatestFact lastFact = (LatestFact) f;
                Set right = new ExpressionSet(lastFact.expressions);
                Set trueRight = new ExpressionSet(allExpressions);
                trueRight.removeAll(right);
                trueRight.add(expressions.get(opToExpression[op.id]));
                Set left = new ExpressionSet(((EarliestFact) earliest.getFact(op)).expressions);
                left.addAll(postponed.getIn(op).expressions);
                LatestFact returnLeft = new LatestFact();
                returnLeft.addExpressions(left);
                left.retainAll(trueRight);
                LatestFact thisLatest = new LatestFact();
                thisLatest.addExpressions(left);
                thisLatest.op = op;
                setFact(op, thisLatest);
                return returnLeft;
            }
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.OperationProblem#direction()
         */
        public boolean direction() {
            return false;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
         */
        public TransferFunction getTransferFunction(Operation o) {
            return new LatestTF(o);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
         */
        public Fact getBoundary() {
            // TODO Auto-generated method stub
            return new LatestFact();
        }
        public class LatestFact extends PreFact {
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.PartialRedundancy.PreFact#create()
             */
            public PreFact create() {
                return new LatestFact();
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.Fact#join(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact join(Fact that) {
                LatestFact thatFact = (LatestFact) that;
                LatestFact result = new LatestFact();
                result.expressions.addAll(this.expressions);
                result.expressions.retainAll(thatFact.expressions);
                result.loc = this.loc;
                return result;
            }
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    public class Used extends OperationProblem {
        public HashMap opOuts;
        
        /**
         *  
         */
        public Used() {
            opOuts = new HashMap();
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.OperationProblem#direction()
         */
        public boolean direction() {
            return false;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
         */
        public TransferFunction getTransferFunction(Operation o) {
            return new UsedTF(o);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
         */
        public Fact getBoundary() {
            return new UsedFact();
        }
        public class UsedFact extends PreFact {
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.PartialRedundancy.PreFact#create()
             */
            public PreFact create() {
                return new UsedFact();
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.Fact#join(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact join(Fact that) {
                UsedFact thatFact = (UsedFact) that;
                UsedFact result = (UsedFact) create();
                result.addExpressions(this.expressions);
                result.addExpressions(thatFact.expressions);
                result.loc = this.loc;
                return result;
            }
            
            
        }
        public class UsedTF extends TransferFunction {
            Operation op;
            
            /**
             * @param op
             */
            public UsedTF(Operation op) {
                super();
                this.op = op;
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see net.sf.bddbddb.dataflow.Problem.TransferFunction#apply(net.sf.bddbddb.dataflow.Problem.Fact)
             */
            public Fact apply(Fact f) {
                UsedFact lastFact = (UsedFact) f;
                setOut(op, lastFact);
                UsedFact newFact = new UsedFact();
                newFact.addExpressions(lastFact.expressions);
               // newFact.killExpressions(op);
                newFact.addExpression((Expression) expressions.get(opToExpression[op.id]));
                newFact.removeExpressions(((EarliestFact) earliest.getFact(op)).expressions);
                newFact.op = op;
                setFact(op, newFact);
                return newFact;
            }
        }
        
        public void setOut(Operation op, UsedFact fact) {
            opOuts.put(op, fact);
        }
        
        public UsedFact getOut(Operation op) {
            return (UsedFact) opOuts.get(op);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            SortedMap sortedMap = new TreeMap(new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((Operation) o1).id - ((Operation) o2).id;
                }
            });
            sortedMap.putAll(operationFacts);
            for (Iterator it = sortedMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                sb.append("@" + e.getKey() + " : " + e.getValue() + '\n');
            }
            return sb.toString();
        }
    }
    Map exprToOp = new HashMap();
    
    public boolean transform(IterationList list) {
        boolean changed = false;
        if (list.isLoop()) {
            boolean b = transform(list.getLoopEdge());
            if (!changed) changed = b; //check not needed. here in case of move
        }
        for (ListIterator it = list.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Operation) {
 /*               
           if(TRACE) System.out.println("Analyzing Operation: " + o);
           if(TRACE) System.out.println(" anticipated: " + anticipated.getFact((Operation) o));
           if(TRACE) System.out.println(" available: " + available.getIn((Operation) o));
           if(TRACE) System.out.println(" postponed: " + postponed.getIn((Operation) o));
           if(TRACE) System.out.println(" earliest: " + earliest.getFact((Operation)o));
           if(TRACE) System.out.println(" latest: " + latest.getFact((Operation) o)); 
           if(TRACE) System.out.println(" used after: " + used.getOut((Operation) o ));
   */              
                Set latest = ((LatestFact) PartialRedundancy.this.latest.getFact((Operation) o)).expressions;
                latest.retainAll(used.getOut(((Operation) o)).expressions);
                //System.out.println(" adding ops for: " + latest);
                it.previous();
                for (Iterator it2 = latest.iterator(); it2.hasNext();) {
                    Expression e = (Expression) it2.next();
                    Operation relOp = (Operation) exprToOp.get(e);
                    if (relOp == null) {
                        BDDRelation oldR = (BDDRelation) e.op.getRelationDest();
                        if (oldR != null) {
                            BDDRelation r = (BDDRelation) solver.createRelation("pre_" + e.toString(), oldR.getAttributes());
                            r.initialize();
                            r.setDomainAssignment(oldR.getBDDDomains());
                            relOp = e.op.copy();
                            relOp.setRelationDest(r);
                            exprToOp.put(e, relOp);
                        }
                    }
                    if (TRACE) System.out.println("Adding " + relOp + " before " + o);
                    it.add(relOp);
                    changed = true;
                }
                it.next();
                Set notLatest = new ExpressionSet(allExpressions);
                notLatest.removeAll(((LatestFact) PartialRedundancy.this.latest.getFact((Operation) o)).expressions);
                notLatest.addAll(used.getOut((Operation) o).expressions);
                if (notLatest.contains(expressions.get(opToExpression[((Operation) o).id]))) {
                    Expression e = (Expression) expressions.get(opToExpression[((Operation) o).id]);
                    Operation op = (Operation) exprToOp.get(e);
                    if (e != null && op != null) {
                        Copy newOp = new Copy(((Operation) o).getRelationDest(), op.getRelationDest());
                        if (TRACE) System.out.println("Replacing " + o + " with " + newOp);
                        it.set(newOp);
                        changed = true;
                    }
                }
                if (o instanceof Nop) it.remove(); //remove nops
            } else {
                IterationList l = (IterationList) o;
                boolean b = transform(l);
                if (!changed) changed = b;
                //clean up empty lists
                if (l.isEmpty()) it.remove();
            }
        }
        return changed;
    }
    abstract class PreFact implements OperationFact {
        Operation op;
        IterationList loc;
        public ExpressionSet expressions;
        public PreFact() {
            expressions = new ExpressionSet();
        }
        
        public boolean containsExpression(Expression e) {
            return expressions.contains(e);
        }
        
        public boolean addExpression(Expression e) {
            if (e != null &&  considerable(e.op) ) //&& !e.toString().equals("diff(IE,IE')")) 
                return expressions.add(e);
            return false;
        }
        
        public boolean addExpressions(Set e) {
            return expressions.addAll(e);
        }
        
        public boolean removeExpression(Expression e) {
            return expressions.remove(e);
        }
        
        public boolean removeExpressions(Set e) {
            return expressions.removeAll(e);
        }
        
        public Set getExpressions() {
            return expressions;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem.Fact#copy(net.sf.bddbddb.IterationList)
         */
        public Fact copy(IterationList list) {
            PreFact result = this.copy();
            result.loc = list;
            return result;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem.Fact#setLocation(net.sf.bddbddb.IterationList)
         */
        public void setLocation(IterationList list) {
            loc = list;
        }
        
        public void killExpressions(Operation op) {
            for (Iterator it = expressions.iterator(); it.hasNext();) {
                Expression e = (Expression) it.next();
                if (e.killedBy(op)) it.remove();
            }
        }
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.Problem.Fact#getLocation()
         */
        public IterationList getLocation() {
            return loc;
        }
        
        public String toString() {
            return expressions.toString();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            return expressions.equals(((PreFact) o).expressions);
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.expressions.hashCode();
        }
        
        public abstract PreFact create();
        
        public PreFact copy() {
            PreFact result = create();
            result.expressions.addAll(this.expressions);
            result.loc = loc;
            return result;
        }       
        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.OperationProblem.OperationFact#getOperation()
         */
        public Operation getOperation() {
            return op;
        }
    }
    
    class ExpressionSet extends AbstractSet{
        BitString s;
        
        public ExpressionSet(){
            this(PartialRedundancy.this.expressions.size());
        }
        
        public ExpressionSet(int numExpressions){
            this(new BitString(numExpressions));
        }
        
        public ExpressionSet(BitString s){
            this.s = s;
        }
        
        public ExpressionSet(ExpressionSet other){
            this((BitString) other.s.clone());
        }
        
        /* (non-Javadoc)
         * @see java.util.AbstractCollection#size()
         */
        public int size() {
            return s.numberOfOnes();
        }

        public boolean contains(Object o){
            if(o instanceof Expression){
                return s.get(expressions.get(o));
            }
            return false;
        }
        
        public boolean containsAll(Collection c){
            if(c instanceof ExpressionSet){
                ExpressionSet those = (ExpressionSet) c;
                return s.contains(those.s);
            }
            return false;
        }
        public boolean add(Object o){
            boolean changed = false;
            if(o instanceof Expression){
                int index = expressions.get(o);
                changed = !s.get(index);
                if(changed) s.set(index);
            }
            return changed;
        }
        
        public boolean addAll(Collection c){
            if(c instanceof ExpressionSet){
                ExpressionSet those = (ExpressionSet) c;
                return s.or(those.s);
            }
            return false;
        }
        
        public boolean remove(Object o){
            boolean changed = false;
            if(o instanceof Expression){
                int index = expressions.get(o);
                changed = s.get(index);
                if(changed) s.clear(index);
            }
            return changed;
        }
        
        public boolean removeAll(Collection c){
            if(c instanceof ExpressionSet){
                ExpressionSet those = (ExpressionSet) c;
                return s.minus(those.s);
            }
            
            return false;
        }
        
        /* (non-Javadoc)
         * @see java.util.AbstractCollection#iterator()
         */
        public Iterator iterator() {
            return new ExpressionIterator();
        }
  
        class ExpressionIterator implements Iterator{
               
            BitStringIterator it;
            int lastIndex; 
            public ExpressionIterator(){
                lastIndex = -1;
                it = s.iterator();
            }
            /* (non-Javadoc)
             * @see java.util.Iterator#remove()
             */
            public void remove() {
               if(lastIndex != -1)
                   s.clear(lastIndex);
                
            }

            /* (non-Javadoc)
             * @see java.util.Iterator#hasNext()
             */
            public boolean hasNext() {
                return it.hasNext();
            }

            /* (non-Javadoc)
             * @see java.util.Iterator#next()
             */
            public Object next() {
                return expressions.get(lastIndex = it.nextIndex());
            }
            
        }
    }

    static class ExpressionWrapper {
        Expression e;
                   
        /**
         * @param e
         */
        public ExpressionWrapper(Expression e) {
            super();
            this.e = e;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o){
            ExpressionWrapper that = (ExpressionWrapper) o;
            return this.e == that.e;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return System.identityHashCode(e);
        }
        
        public String toString(){ return e.toString(); }
    }
    
    class Expression {
        int number = -1;
        Operation op;
        int depth = -1;
        public List subExpressions;
        public Collection reachingDefs;
        public Expression(Operation op, List subExpressions, int depth) {
            this.op = op;
            this.subExpressions = subExpressions;
            this.depth = depth;
        }
        
        public List getSrcs() {
            return op.getSrcs();
        }
        
        public List subExpressions(){
            return subExpressions;
        }
        public Class getType() {
            return op.getClass();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            Expression that = (Expression) obj;
            if(this.number != -1 && that.number != -1)
                return this.number == that.number;
        
            return equals(that, new HashSet()); 
        }
        
        private boolean equals(Expression that, Collection visited){

           
            ExpressionWrapper thisWrap = new ExpressionWrapper(this);
            ExpressionWrapper thatWrap = new ExpressionWrapper(that);
            if(visited.contains(thisWrap) && visited.contains(thatWrap)) return true;
            if(this.depth != that.depth) return false;
           
            visited.add(thisWrap);
            visited.add(thatWrap);
            if (this == that) return true;
//          if (!this.op.getExpressionString().equals(that.op.getExpressionString())) return false;
           
            if(!this.op.getClass().equals(that.op.getClass())) return false;
           
            if(!passesOpSpecificChecks(that)) return false;
            
            if(this.subExpressions.size() != that.subExpressions.size()) return false;
            for(int i = 0; i < this.subExpressions.size(); i++){
                Expression e1 = (Expression) this.subExpressions.get(i); 
                Expression e2 = (Expression) that.subExpressions.get(i); 
                if(!e1.equals(e2,visited)) return false;
            }
            visited.remove(thisWrap);
            visited.remove(thatWrap);
            return true;
        }
        
        boolean passesOpSpecificChecks(Expression that){
            if(this.op instanceof Load){
                Load thisL = (Load) this.op;
                Load thatL = (Load) that.op;
                if(!thisL.getRelationDest().equals(thatL.getRelationDest())) return false;
           
            }else if(this.op instanceof Project){
                Project thisP = (Project) this.op;
                Project thatP = (Project) that.op;
                if(!thisP.getAttributes().equals(thatP.getAttributes())) return false;
            }else if(this.op instanceof Rename){
                Rename thisR = (Rename) this.op;
                Rename thatR = (Rename) that.op;
                if(!thisR.getRenameMap().equals(thatR.getRenameMap())) return false;
            }else if(this.op instanceof Join){
                Join thisJ = (Join) this.op;
                Join thatJ = (Join) that.op;
                if(!thisJ.getAttributes().equals(thatJ.getAttributes())) return false;
            }else if(this.op instanceof GenConstant){
                GenConstant thisG = (GenConstant) this.op;
                GenConstant thatG = (GenConstant) that.op;
                if(!thisG.getAttribute().equals(thatG.getAttribute()) 
                    || thisG.getValue() != thatG.getValue())
                    return false;
            }else if(this.op instanceof JoinConstant){
                JoinConstant thisG = (JoinConstant) this.op;
                JoinConstant thatG = (JoinConstant) that.op;
                if(!thisG.getAttribute().equals(thatG.getAttribute()) 
                    || thisG.getValue() != thatG.getValue())
                    return false;
            }else if(this.op instanceof Replace){
                Replace thisR = (Replace) this.op;
                Replace thatR = (Replace) that.op;
                if(!thisR.getPairing().equals(thatR.getPairing())) return false;
            }
            return true;
        }
        
        public Expression number(){
            if(this.number != -1) return this; //just in case
            int initSize = expressions.size();
            int number = expressions.get(this);
            if(expressions.size() - initSize > 0){
//              if this expression was never added before
                this.number = number;
                List newSubs = new LinkedList();
                for(Iterator it = subExpressions.iterator(); it.hasNext(); ){
                    Expression e = (Expression) it.next();
                    newSubs.add(e.number());
                }
                subExpressions = newSubs;
            }
          
           return (Expression) expressions.get(number);
        }
        public boolean killedBy(Operation op) {
            //if(e == null) return false;
            //return uses(e,new HashSet());
           // if(subExpressions.op);
            Expression killE = (Expression) expressions.get(opToExpression[op.id]);
            for(Iterator it = subExpressions.iterator(); it.hasNext(); ){
                Expression subE = (Expression) it.next();
                if(subE.op instanceof Phi){
                    Phi phi = (Phi) subE.op;
                   if(phi.operations.contains(op)) return true;
                   //if(subE.subExpressions.contains(killE)) return true;
                }else{
                    if(subE.equals(killE)) return true;
                }
            }
            return false;
        }
        
        public boolean uses(Expression e, Collection visited){
            Assert._assert(e != null, "expression is null");
            ExpressionWrapper thisWrap = new ExpressionWrapper(this);
            if(visited.contains(this)) return false;
            visited.add(this);
            if(subExpressions.contains(e)) return true;
            
            for(Iterator it = subExpressions.iterator(); it.hasNext(); ){
                
                if(((Expression) it.next()).uses(e,visited)) return true;
            }
            return false;
            
        }
        public String toString() { 
           /* if(op instanceof Load){
                return "Load("  + op.getRelationDest() + ")";
            }else if(op instanceof Phi){
                StringBuffer sb = new StringBuffer();
                sb.append("Phi " + Integer.toString(op.hashCode()));
       
                return sb.toString();
                
            }else if(op instanceof Copy && subExpressions.size() == 0){
                return "Copy("+ ((Copy)op).getSrc() + ")";
            }
            
            StringTokenizer st = new StringTokenizer(op.getClass().toString(),".");
            String name  = null;
            while(st.hasMoreTokens()) name = st.nextToken(); 
            return name + subExpressions;
            */
            return op.getExpressionString();
        }
        public int hashCode() {
            return 1;
        }
        
    }
}