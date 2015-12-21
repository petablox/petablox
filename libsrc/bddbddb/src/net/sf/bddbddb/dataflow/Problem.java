// Problem.java, created Jul 3, 2004 1:17:45 PM by joewhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package net.sf.bddbddb.dataflow;

import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.ir.Operation;

/**
 * Problem
 * 
 * @author John Whaley
 * @version $Id: Problem.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class Problem {
    public abstract TransferFunction getTransferFunction(Operation o);

    public abstract Fact getBoundary();

    public abstract boolean direction();

    public boolean compare(Fact f1, Fact f2) {
        return f1.equals(f2);
    }
    public abstract static interface Fact {
        public abstract Fact join(Fact that);

        public abstract Fact copy(IterationList loc);

        public abstract void setLocation(IterationList loc);

        public abstract IterationList getLocation();
    }
    public abstract static class TransferFunction {
        public abstract Fact apply(Fact f);
    }
}