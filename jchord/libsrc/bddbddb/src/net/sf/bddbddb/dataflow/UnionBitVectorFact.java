/*
 * Created on Jul 6, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.dataflow;

import jwutil.math.BitString;
import jwutil.util.Assert;
import net.sf.bddbddb.IterationList;
import net.sf.bddbddb.dataflow.Problem.Fact;

/**
 * @author Collective
 * @version $Id: UnionBitVectorFact.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public class UnionBitVectorFact extends BitVectorFact {
    public UnionBitVectorFact(int setSize) {
        super(setSize);
    }

    public UnionBitVectorFact(BitString s) {
        super(s);
    }

    public UnionBitVectorFact create(BitString s) {
        return new UnionBitVectorFact(s);
    }

    public Fact join(Fact that) {
        Assert._assert(location == ((BitVectorFact) that).location);
        BitString thatS = ((BitVectorFact) that).fact;
        BitString newS = new BitString(this.fact.size());
        newS.or(this.fact);
        boolean b = newS.or(thatS);
        if (!b) return this;
        UnionBitVectorFact f = create(newS);
        f.location = this.location;
        return f;
    }

    public Fact copy(IterationList list) {
        UnionBitVectorFact f = create(fact);
        f.location = list;
        return f;
    }
}