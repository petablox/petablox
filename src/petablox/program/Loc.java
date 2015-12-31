package petablox.program;

import petablox.util.soot.JEntryExitNopStmt;
import petablox.util.soot.SootUtilities;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;


/**
 * Representation of the location of a statement.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Loc {
    public final Unit i;
    public final int qIdx;

    public Loc(Unit i, int qIdx) {
        // qIdx is -1 when referring to entry and exit of methods
        if(i instanceof JEntryExitNopStmt){
            assert(qIdx == -1);
        }
        this.i = i;
        this.qIdx = qIdx;
    }

    public int hashCode() { return i.hashCode(); }

    public boolean equals(Object o) {
        if (!(o instanceof Loc)) return false;
        Loc that = (Loc) o;
        return this.i == that.i;
    }

    public String toString() {
    	SootMethod m = null;
        m = SootUtilities.getMethod(i);
        return "<" + m + ", " + i + ">";
    }
}

