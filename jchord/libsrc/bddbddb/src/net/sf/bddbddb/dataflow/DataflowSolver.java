// DataflowSolver.java, created Jul 4, 2004 2:30:15 AM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import jwutil.util.Assert;
import net.sf.bddbddb.IterationFlowGraph;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.dataflow.Problem.Fact;
import net.sf.bddbddb.dataflow.Problem.TransferFunction;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.dynamic.If;

/**
 * DataflowSolver
 * 
 * @author John Whaley
 * @version $Id: DataflowSolver.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class DataflowSolver {
    boolean TRACE = false;
    boolean WORKLIST = false;
    
    /** Matches blocks to their dataflow information. */
    Map/* <IterationList,Fact> */blockToFact;

    public DataflowSolver() {
        blockToFact = new HashMap();
    }

    /** Resets dataflow information. */
    public void reset() {
        blockToFact.clear();
    }

    /** Returns the dataflow information for a given block. */
    public Fact getFact(IterationList block) {
        return (Fact) blockToFact.get(block);
    }

    public DataflowIterator getIterator(Problem p, IterationFlowGraph g) {
        IterationList block = g.getIterationList();
        Fact f = getFact(block);
        if (f == null) {
            f = p.getBoundary();
            f.setLocation(block);
        }
        return new DataflowIterator(p, f, block);
    }
    public class DataflowIterator implements ListIterator {
        Problem p;
        Fact fact;
        IterationList block;
        ListIterator ops;
        DataflowIterator nested;

        //public DataflowIterator(Problem p, IterationList block) {
        //    this(p, DataflowSolver.this.getFact(block), block);
       // }

        public DataflowIterator(Problem p, Fact startingFact, IterationList block) {
            this.p = p;
            this.fact = startingFact;
            this.block = block;
            this.ops = block.iterator();
        }

        public Fact getFact() {
            if (nested != null) return nested.getFact();
            return fact;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            if (nested != null && nested.hasNext()) return true;
            return ops.hasNext();
        }

        public boolean hasPrevious() {
            throw new UnsupportedOperationException();
        }

        public Object next() {
            if (nested != null) {
                if (!nested.hasNext()) {
                    if (TRACE) System.out.println("Exiting " + nested.block);
                    nested = null;
                } else {
                    return nested.next();
                }
            }
            Object o = ops.next();
            if (o instanceof Operation) {
                Operation op = (Operation) o;
                TransferFunction tf = p.getTransferFunction(op);
                fact = tf.apply(fact);
            } else {
                IterationList list = (IterationList) o;
                Fact f = (Fact) blockToFact.get(list);
                if (f != null) fact = f;
            }
            return o;
        }

        public Object previous() {
            throw new UnsupportedOperationException();
        }

        public int nextIndex() {
            throw new UnsupportedOperationException();
        }

        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        public void enter(IterationList list) {
            if (nested != null) nested.enter(list);
            else {
                if (TRACE) System.out.println("Entering " + list);
                Fact f = (Fact) blockToFact.get(list);
                if (f == null) {
                    f = fact.copy(list);
                }
                nested = new DataflowIterator(p, f, list);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            if (nested != null) nested.remove();
            else ops.remove();
        }

        public void set(Object o) {
            if (nested != null) nested.set(o);
            else ops.set(o);
        }

        public void add(Object o) {
            if (nested != null) nested.add(o);
            else ops.add(o);
        }
    }

    boolean again;
    
    public void solve(Problem p, IterationList g) {
        do {
            Fact startFact;
            startFact = (Fact) blockToFact.get(g);
            if (startFact == null) {
                startFact = p.getBoundary();
                startFact.setLocation(g);
                //blockToFact.put(g, startFact);
            }
            again = false;
            if (TRACE) System.out.println("Main iteration!  Start fact: "+System.identityHashCode(startFact));
            solve2(startFact, p, g);
        } while (again);
    }

    Fact solve2(Fact currentFact, Problem p, IterationList g) {
        Assert._assert(currentFact.getLocation() == g);
        if (g.isLoop()) {
            Fact startFact = (Fact) blockToFact.get(g);
            if (startFact == null) {
                if (TRACE) System.out.println("Caching dataflow value at entry " + g);
                blockToFact.put(g, startFact = currentFact.copy(g));
            } else {
                if (TRACE) System.out.println("Joining dataflow value at entry " + g);
                Assert._assert(startFact.getLocation() == g);
                Fact joinResult = startFact.join(currentFact);
                Assert._assert(joinResult.getLocation() == g);
                blockToFact.put(g, joinResult);
                currentFact = joinResult.copy(g);
            }
            if (TRACE) System.out.println("At start of "+g+", we cached fact " + System.identityHashCode(blockToFact.get(g)));
        } else {
            if (TRACE) System.out.println(g+" is not a loop, incoming fact " + System.identityHashCode(currentFact));
            currentFact = currentFact.copy(g);
        }
        if (TRACE) System.out.println("Using fact "+System.identityHashCode(currentFact)+" to iterate through "+g);
        for (;;) {
            for (Iterator i = p.direction() ? g.iterator() : g.reverseIterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof IterationList || o instanceof If) {
                    IterationList list = null;
                    Fact preIfFact = null;
                    if (o instanceof If) {
                        if (p.direction()) {
                            TransferFunction tf = p.getTransferFunction((Operation) o);
                            currentFact = tf.apply(currentFact);
                        }
                        preIfFact = currentFact.copy(g);
                        list = ((If) o).getBlock();
                    } else {
                        list = (IterationList) o;
                    }
                    if (TRACE) System.out.println("Entering " + list + " with fact "+System.identityHashCode(currentFact));
                    currentFact.setLocation(list);
                    currentFact = solve2(currentFact, p, list);
                    currentFact.setLocation(g);
                    if (TRACE) System.out.println("Leaving " + list + ", current fact "+System.identityHashCode(currentFact));
                    if (o instanceof If) {
                        currentFact = preIfFact.join(currentFact);
                        if (!p.direction()) {
                            TransferFunction tf = p.getTransferFunction((Operation) o);
                            currentFact = tf.apply(currentFact);
                        }
                    }
                } else {
                    Operation op = (Operation) o;
                    if (TRACE) System.out.println("   Operation: " + op);
                    TransferFunction tf = p.getTransferFunction(op);
                    currentFact = tf.apply(currentFact);
                }
            }
            if (TRACE) System.out.println("Finished walking through "+g+", current fact is now "+System.identityHashCode(currentFact));
            if (!g.isLoop()) break;
            Fact blockFact = currentFact;
            currentFact.setLocation(g.getLoopEdge());
            Fact loopEdgeFact = solve2(currentFact, p, g.getLoopEdge());
            loopEdgeFact.setLocation(g);
            currentFact.setLocation(g);
            if (TRACE) System.out.println("Loop edge fact: "+System.identityHashCode(loopEdgeFact));
            Fact startFact = (Fact) blockToFact.get(g);
            if (TRACE) System.out.println("Fact that was cached at start of "+g+": " + System.identityHashCode(startFact));
            Assert._assert(startFact.getLocation() == g, startFact.getLocation()+" != "+g);
            Fact joinResult = startFact.join(loopEdgeFact);
            if (TRACE) System.out.println("Result: " + joinResult);
            Assert._assert(joinResult.getLocation() == g);
            if (joinResult.equals(startFact)) {
                if (TRACE) System.out.println("No change after join, exiting.");
                currentFact = blockFact;
                break;
            }
            if (TRACE) System.out.println(g + " changed, iterating again...");
            if (TRACE) System.out.println("Caching join result for "+g+": " + System.identityHashCode(joinResult));
            blockToFact.put(g, joinResult);
            currentFact = joinResult.copy(g);
            if (TRACE) System.out.println("Current fact at end of "+g+": " + System.identityHashCode(currentFact));
            if (!WORKLIST) {
                again = true;
                break;
            }
        }
        if (TRACE) System.out.println("Returning current fact from "+g+": " + System.identityHashCode(currentFact));
        return currentFact;
    }
}