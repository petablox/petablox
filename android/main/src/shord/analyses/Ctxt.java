package shord.analyses;

import soot.Unit;
import soot.SootMethod;
import java.io.Serializable;
import shord.program.Program;

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
 * @authod Saswat Anand
 */
public class Ctxt implements Serializable 
{
    /**
     * The sequence of statements comprising the abstract context, in decreasing order of significance.
     */
    private final Object[] elems;
    /**
     * Constructor.
     * 
     * @param elems The sequence of statements comprising this abstract context.
     */
    public Ctxt(Object[] elems) {
        this.elems = elems;
    }
    /**
     * Provides the sequence of statements comprising this abstract context.
     * 
     * @return The sequence of statements comprising this abstract context.
     */
    public Object[] getElems() {
        return elems;
    }

    public int hashCode() {
        int i = 5381;
        for (Object inst : elems) {
            int q = inst == null ? 9999 : inst.hashCode();
            i = ((i << 5) + i) + q; // i*33 + q
        }
        return i;
    }
	
    public boolean equals(Object o) {
        if (!(o instanceof Ctxt))
            return false;
        Ctxt that = (Ctxt) o;
        Object[] thisElems = this.elems;
        Object[] thatElems = that.elems;
        int n = thisElems.length;
        if (thatElems.length != n)
            return false;
        for (int i = 0; i < n; i++) {
            Object inst = thisElems[i];
            if (inst != thatElems[i])
                return false;
        }
        return true;
    }
	
	public String toString() {
		return toString(false);
	}

	public String toString(boolean asCtxtObj) {
		StringBuilder builder = new StringBuilder("[");
		boolean first = true;
		// If this is a contextified abstract object, skip the first element,
		// i.e. the allocation statement that identifies the object.
        for (int i = asCtxtObj ? 1 : 0; i < elems.length; i++) {
			Object inst = elems[i];
			if(!first) {
				builder.append(',');
			} else {
				first = false;
			}
			builder.append(inst == null ? "null"
						   : (inst instanceof Unit ? Program.unitToString((Unit) inst) : inst.toString()));
		}
		builder.append(']');
		if (asCtxtObj) {
			builder.append(':');
			builder.append(elems[0].toString());
			//builder.append(Program.unitToString(elems[0]));
		}
		return builder.toString();
	}

	/*
	public int length()
	{
		return elems.length;
	}

	public SootMethod containerMethod(int index)
	{
		Object c = elems[index];
		if(c instanceof Stmt)
			return Program.containerMethod(c);
		if(c instanceof AllocNode)
			return ((AllocNode) c).getMethod(); //can be null
		return null;
	}
	*/
}
