// BDDInterpreter.java, created Mar 16, 2004 12:49:19 PM 2004 by mcarbin
// Copyright (C) 2004 Michael Carbin
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.ir;

import java.util.Iterator;
import net.sf.bddbddb.BDDRelation;
import net.sf.bddbddb.BDDSolver;
import net.sf.bddbddb.InferenceRule;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.Relation;
import net.sf.javabdd.BDD;

/**
 * BDDInterpreter
 * 
 * @author mcarbin
 * @version $Id: BDDInterpreter.java 522 2005-04-29 02:34:44Z joewhaley $
 */
public class BDDInterpreter extends Interpreter {
    /**
     * @param ir
     */
    public BDDInterpreter(IR ir) {
        this.ir = ir;
        if (ir != null) {
            opInterpreter = new BDDOperationInterpreter((BDDSolver) ir.solver, ((BDDSolver) ir.solver).getBDDFactory());
        }
    }
    public void interpret() {
        if (ir.DOMAIN_ASSIGNMENT)
            ((BDDOperationInterpreter)opInterpreter).needsDomainMatch = false;
        interpret(ir.graph.getIterationList());
    }

    int MAX_ITERATIONS = 128;
    
    public boolean interpret(IterationList list) {
        boolean everChanged = false;
        boolean change;
        int iterations = 0;
        for (;;) {
            ++iterations;
            change = false;
            for (Iterator i = list.iterator(); i.hasNext();) {
                Object o = i.next();
                if (TRACE) System.out.println(o);
                if (o instanceof IterationList) {
                    IterationList sublist = (IterationList) o;
                    if (interpret(sublist)) {
                        change = true;
                    }
                } else if (o instanceof Operation) {
                    Operation op = (Operation) o;
                    BDDRelation dest = (BDDRelation) op.getRelationDest();
                    BDD oldValue = null;
                    Relation changed = null;
                    if (!change && dest != null && dest.getBDD() != null) {
                        oldValue = dest.getBDD().id();
                        changed = dest;
                    }
                    op.visit(opInterpreter);
                    if (oldValue != null) {
                        change = !oldValue.equals(dest.getBDD());
                        if (TRACE && change) System.out.println(changed + " Changed!");
                        oldValue.free();
                    }
                } else if (o instanceof InferenceRule) {
                    InferenceRule ir = (InferenceRule) o;
                    if (ir.update()) {
                        change = true;
                    }
                }
            }
            if (!change) break;
            everChanged = true;
            if (!list.isLoop()) break;
            // MAYUR if (iterations == MAX_ITERATIONS) break;
            interpret(list.getLoopEdge());
        }
        return everChanged;
    }
}
