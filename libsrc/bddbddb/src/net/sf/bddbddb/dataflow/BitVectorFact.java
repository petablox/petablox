/*
 * Created on Jul 6, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
package net.sf.bddbddb.dataflow;

import jwutil.math.BitString;
import net.sf.bddbddb.IterationList;

/**
 * BitVectorFact
 * 
 * @author Collective
 * @version $Id: BitVectorFact.java 328 2004-10-16 02:45:30Z joewhaley $
 */
public abstract class BitVectorFact implements Problem.Fact {
    protected final BitString fact;

    IterationList location;
    
    public void setLocation(IterationList list) {
        location = list;
    }

    public IterationList getLocation() {
        return location;
    }

    protected BitVectorFact(int setSize) {
        fact = new BitString(setSize);
    }

    protected BitVectorFact(BitString s) {
        this.fact = s;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof BitVectorFact) {
            return this.fact.equals(((BitVectorFact) o).fact);
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.fact.hashCode();
    }
    
    public String toString() {
        return fact.toString();
    }
}