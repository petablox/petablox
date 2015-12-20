package petablox.analyses.alias;


import java.io.Serializable;

import petablox.util.soot.SootUtilities;
import soot.Unit;

/**
 * Representation of an abstract context of a method.
 * <p>
 * Each abstract context is a possibly empty sequence of the form
 * <tt>[e1,...,en]</tt> where each <tt>ei</tt> is either an object
 * allocation statement or a method invocation statement in
 * decreasing order of significance.
 * <p>
 * The abstract context corresponding to the empty sequence, called
 * <tt>epsilon</tt>, is the lone context of methods that are
 * analyzed context insensitively.  These include the main method,
 * all class initializer methods, and any additional user-specified
 * methods (see {@link chord.analyses.alias.CtxtsAnalysis}).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Ctxt implements Serializable {
    /**
     * The sequence of statements comprising the abstract context, in decreasing order of significance.
     */
    private final Unit[] elems;
    /**
     * Constructor.
     * 
     * @param elems The sequence of statements comprising this abstract context.
     */
    public Ctxt(Unit[] elems) {
        this.elems = elems;
    }
    /**
     * Provides the sequence of statements comprising this abstract context.
     * 
     * @return The sequence of statements comprising this abstract context.
     */
    public Unit[] getElems() {
        return elems;
    }
    /**
     * Determines whether this abstract context contains a given statement.
     * 
     * @param inst A statement.
     * 
     * @return true iff this abstract context contains the given statement.
     */
    public boolean contains(Unit inst) {
        for (int i = 0; i < elems.length; i++) {
            if (elems[i] == inst)
                return true;
        }
        return false;
    }
  public int count(Unit inst) {
    int n = 0;
        for (int i = 0; i < elems.length; i++) {
            if (elems[i] == inst)
        n++;
        }
    return n;
  }
    public int hashCode() {
        int i = 5381;
        for (Unit inst : elems) {
            int q = inst == null ? 9999 : SootUtilities.getID(inst);
            i = ((i << 5) + i) + q; // i*33 + q
        }
        return i;
    }
    public boolean equals(Object o) {
        if (!(o instanceof Ctxt))
            return false;
        Ctxt that = (Ctxt) o;
        Unit[] thisElems = this.elems;
        Unit[] thatElems = that.elems;
        int n = thisElems.length;
        if (thatElems.length != n)
            return false;
        for (int i = 0; i < n; i++) {
            Unit inst = thisElems[i];
            if (inst != thatElems[i])
                return false;
        }
        return true;
    }
    public String toString() {
        String s = "[";
        int n = elems.length;
        for (int i = 0; i < n; i++) {
            Unit q = elems[i];
            s += q == null ? "null" : SootUtilities.toByteLocStr(q);
            if (i < n - 1)
                s += ",";
        }
        return s + "]";
    }

  public int length() { return elems.length; }
  public Unit get(int i) { return elems[i]; }
  public Unit head() { return elems[0]; }
  public Unit last() { return elems[elems.length-1]; }
  public Ctxt tail() { return suffix(elems.length-1); }
  public Ctxt prefix(int k) {
    if (k >= elems.length) return this;
    Unit[] newElems = new Unit[k];
    if (k > 0) System.arraycopy(elems, 0, newElems, 0, k);
    return new Ctxt(newElems);
  }
  public Ctxt suffix(int k) {
    if (k >= elems.length) return this;
    Unit[] newElems = new Unit[k];
    if (k > 0) System.arraycopy(elems, elems.length-k, newElems, 0, k);
    return new Ctxt(newElems);
  }

  // Maximize length of returned context is max
  public Ctxt prepend(Unit q) { return prepend(q, Integer.MAX_VALUE); }
  public Ctxt prepend(Unit q, int max) {
    int oldLen = elems.length;
    int newLen = Math.min(max, oldLen+1);
    Unit[] newElems = new Unit[newLen];
    if (newLen > 0) newElems[0] = q;
    if (newLen > 1) System.arraycopy(elems, 0, newElems, 1, newLen-1);
    return new Ctxt(newElems);
  }

  public Ctxt append(Unit q) {
    Unit[] newElems = new Unit[elems.length+1];
    System.arraycopy(elems, 0, newElems, 0, elems.length);
    newElems[newElems.length-1] = q;
    return new Ctxt(newElems);
  }
}
