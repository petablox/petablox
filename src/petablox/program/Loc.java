package petablox.program;

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
    public final Object i;
    public final int qIdx;

    public Loc(Object i, int qIdx) {
    	if(i instanceof Block){
    		assert(qIdx == -1);
    	}else{
    		assert(qIdx >= 0);
    		assert(i instanceof Unit);
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
    	if(i instanceof Unit){
    		m = SootUtilities.getMethod(((Unit)i));
    	}else{
    		Block b = (Block)i;
    		m = SootUtilities.getMethod(b.getHead());
    	}
        return "<" + m + ", " + i + ">";
    }
}

