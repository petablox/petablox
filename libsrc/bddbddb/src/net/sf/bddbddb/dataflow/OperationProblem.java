// RelationProblem.java, created Jul 3, 2004 1:44:46 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import java.util.HashMap;
import java.util.Map;
import jwutil.util.Assert;
import net.sf.bddbddb.ir.Operation;

/**
 * RelationProblem
 * 
 * @author John Whaley
 * @version $Id: OperationProblem.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class OperationProblem extends Problem {
    Map/* <Operation,OperationFact> */operationFacts;

    public OperationProblem() {
        this.initialize();
    }

    public void initialize() {
        operationFacts = new HashMap();
    }

    public OperationFact getFact(Operation o) {
        return (OperationFact) operationFacts.get(o);
    }

    public void setFact(Operation o, OperationFact f) {
        Assert._assert(f.getOperation() == o);
        operationFacts.put(o, f);
        //System.out.println("Setting operation "+o+" to "+f);
    }

    public abstract boolean direction();
    public static interface OperationFact extends Fact {
        public Operation getOperation();
    }
    public abstract class OperationTransferFunction extends TransferFunction {
    }
}