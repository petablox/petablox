// DefUse.java, created Jul 3, 2004 9:53:25 PM by jwhaley
//Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
//Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jwutil.math.BitString;
import jwutil.math.BitString.BitStringIterator;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.bddbddb.ir.IR;
import net.sf.bddbddb.ir.Operation;

/**
 * DefUse
 * 
 * @author jwhaley
 * @version $Id: DefUse.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class DefUse extends OperationProblem {
    boolean TRACE = false;
    // Global information.
    BitString[] defs; /* <Relation,Operation> */
    BitString[] uses; /* <Relation,Operation> */
    Operation[] opMap;
    IR ir;
    Map/*Operation,DefUseFact*/ opIns;
    
    public DefUse(IR ir) {
        this.ir = ir;
        int numRelations = ir.getNumberOfRelations();
        int numOperations = Operation.getNumberOfOperations();
        if (TRACE) System.out.println(numRelations + " relations, " + numOperations + " operations");
        this.defs = new BitString[numRelations];
        for (int i = 0; i < defs.length; ++i) {
            defs[i] = new BitString(numOperations);
        }
        this.uses = new BitString[numRelations];
        for (int i = 0; i < uses.length; ++i) {
            uses[i] = new BitString(numOperations);
        }
        opMap = new Operation[numOperations];
        initialize(ir.graph.getIterationList());
        opIns = new HashMap();
    }

    public void setIn(Operation op, Fact fact){
        opIns.put(op,fact);
    }
    
    public DefUseFact getIn(Operation op){
        return (DefUseFact) opIns.get(op);
    }
    void initialize(IterationList block) {
        for (Iterator i = block.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof IterationList) {
                initialize((IterationList) o);
            } else {
                Operation op = (Operation) o;
                opMap[op.id] = op;
                Relation def = op.getRelationDest();
                if (def != null) defs[def.id].set(op.id);
                Collection use = op.getSrcs();
                for (Iterator j = use.iterator(); j.hasNext();) {
                    Relation r = (Relation) j.next();
                    uses[r.id].set(op.id);
                }
            }
        }
    }

    public OperationSet getDefs(Relation r) {
        return new OperationSet(defs[r.id]);
    }

    public OperationSet getUses(Relation r) {
        return new OperationSet(uses[r.id]);
    }
    public class OperationSet extends AbstractSet {
        BitString s;

        /**
         * @param s
         */
        public OperationSet(BitString s) {
            this.s = s;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.AbstractCollection#iterator()
         */
        public Iterator iterator() {
            return new OperationIterator(s);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Collection#contains(java.lang.Object)
         */
        public boolean contains(Object o) {
            if (o instanceof Operation) {
                int id = ((Operation) o).id;
                return s.get(id);
            }
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.AbstractCollection#size()
         */
        public int size() {
            return s.numberOfOnes();
        }
    }
    public class OperationIterator implements Iterator {
        BitStringIterator i;

        /**
         * @param s
         */
        public OperationIterator(BitString s) {
            i = s.iterator();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return i.hasNext();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            int index = i.nextIndex();
            return opMap[index];
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    public class DefUseFact extends UnionBitVectorFact implements OperationFact {
        Operation op;

        /**
         * @param setSize
         */
        public DefUseFact(int setSize) {
            super(setSize);
        }

        /**
         * @param s
         */
        public DefUseFact(BitString s) {
            super(s);
        }

        /*
         * (non-Javadoc)
         * 
         * @see net.sf.bddbddb.dataflow.UnionBitVectorFact#create(net.sf.bddbddb.util.BitString)
         */
        public UnionBitVectorFact create(BitString s) {
            return new DefUseFact(s);
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (BitStringIterator i = fact.iterator(); i.hasNext();) {
                sb.append(opMap[i.nextIndex()]);
                sb.append(" ");
            }
            return sb.toString();
        }

        /**
         * Get the reaching defs for this relation.
         * 
         * @param r  relation
         * @return  reaching defs for this relation
         */
        public OperationSet getReachingDefs(Relation r) {
            BitString bs = (BitString) fact.clone();
            bs.and(defs[r.id]);
            return new OperationSet(bs);
        }

        /**
         * Get the reaching defs for this collection of relations.
         * 
         * @param rs  collection of relations
         * @return  reaching defs for these relations
         */
        public OperationSet getReachingDefs(Collection rs) {
            BitString bs = new BitString(fact.size());
            for (Iterator i = rs.iterator(); i.hasNext();) {
                Relation r = (Relation) i.next();
                bs.or(defs[r.id]);
            }
            bs.and(fact);
            return new OperationSet(bs);
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

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.dataflow.Problem#direction()
     */
    public boolean direction() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.dataflow.Problem#getTransferFunction(net.sf.bddbddb.ir.Operation)
     */
    public TransferFunction getTransferFunction(Operation op) {
        return new DefUseTransferFunction(op);
    }
    public class DefUseTransferFunction extends OperationTransferFunction {
        Operation op;

        DefUseTransferFunction(Operation op) {
            this.op = op;
        }

        public Fact apply(Fact f) {
            //super.apply(f);
            DefUseFact oldFact = (DefUseFact) f;
            setIn(op, oldFact);
            BitString bs = (BitString) oldFact.fact.clone();
            //kill
            Relation r = op.getRelationDest();
            if (r != null) bs.minus(defs[r.id]);
            //gen
            bs.set(op.id);
            DefUseFact newFact = (DefUseFact) oldFact.create(bs);
            newFact.op = op;
            setFact(op, newFact);
            return newFact;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.bddbddb.dataflow.Problem#getBoundary()
     */
    public Fact getBoundary() {
        return new DefUseFact(Operation.getNumberOfOperations());
    }
}