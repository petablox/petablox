package net.sf.bddbddb.dataflow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import jwutil.math.BitString;
import jwutil.math.BitString.BitStringIterator;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.IR;
import net.sf.bddbddb.ir.Operation;
import net.sf.bddbddb.ir.highlevel.Free;

/**
 * @author Administrator
 */
public class Liveness extends OperationProblem implements IRPass {
    public IR ir;
    int numRelations;
    boolean TRACE = false;
    Map opOuts;

    public Liveness(IR ir) {
        this.ir = ir;
        this.numRelations = ir.getNumberOfRelations();
        opOuts = new HashMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.dataflow.IRPass#run()
     */
    public boolean run() {
        System.out.print("Running Liveness...");
        long time = System.currentTimeMillis();
        IterationList list = ir.graph.getIterationList();
        DataflowSolver solver = new DataflowSolver();
        solver.solve(this, list);
        boolean result = transform(list);
        System.out.println(((System.currentTimeMillis()-time)/1000.)+"s");
        return result;
    }

    boolean transform(IterationList list) {
        boolean changed = false;
        for (ListIterator it = list.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Operation) {
                Operation op = (Operation) o;
                LivenessFact fact = (LivenessFact) getOut(op);
                if (TRACE) System.out.println("Live: " + fact);
                for (Iterator it2 = op.getSrcs().iterator(); it2.hasNext();) {
                    Relation r = (Relation) it2.next();
                    if (!fact.isAlive(r)) {
                        Free free = new Free(r);
                        if (TRACE) System.out.println("Adding a free for " + r);
                        it.add(free);
                        changed = true;
                    }
                }
            } else {
                IterationList p = (IterationList) o;
                boolean b = transform(p);
                if (!changed) changed = b;
            }
        }
        return changed;
    }

    public boolean direction() {
        return false;
    }

    public LivenessFact getOut(Operation op) {
        return (LivenessFact) opOuts.get(op);
    }

    public void setOut(Operation op, Fact fact) {
        opOuts.put(op, fact);
    }

    public Fact getBoundary() {
        return new LivenessFact(numRelations);
    }
    public class LivenessFact extends UnionBitVectorFact implements OperationFact {
        Operation op;

        public LivenessFact(BitString fact) {
            super(fact);
        }

        public LivenessFact(int size) {
            super(size);
        }

        public UnionBitVectorFact create(BitString bs) {
            return new LivenessFact(bs);
        }

        public Fact join(Fact that) {
            if (TRACE) System.out.println("Joining " + this + " and " + that);
            Fact result = super.join(that);
            if (TRACE) System.out.println("Result = " + result);
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(op);
            sb.append(" : ");
            for (BitStringIterator i = fact.iterator(); i.hasNext();) {
                sb.append(ir.getRelation(i.nextIndex()));
                sb.append(" ");
            }
            return sb.toString();
        }

        public boolean isAlive(Relation r) {
            return fact.get(r.id);
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

    public TransferFunction getTransferFunction(Operation op) {
        return new LivenessTF(op);
    }
    public class LivenessTF extends OperationTransferFunction {
        Operation op;

        public LivenessTF(Operation op) {
            this.op = op;
        }

        public Fact apply(Fact f) {
            //super.apply(f);
            LivenessFact oldFact = (LivenessFact) f;
            setOut(op, f);
            BitString bs = (BitString) oldFact.fact.clone();
            //kill
            Relation r = op.getRelationDest();
            if (r != null) bs.clear(r.id);
            //gen
            List srcs = op.getSrcs();
            for (Iterator it = srcs.iterator(); it.hasNext();) {
                Object o = it.next();
                if (o instanceof Relation) bs.set(((Relation) o).id);
            }
            LivenessFact newFact = (LivenessFact) oldFact.create(bs);
            newFact.op = op;
            setFact(op, newFact);
            return newFact;
        }
    }
}