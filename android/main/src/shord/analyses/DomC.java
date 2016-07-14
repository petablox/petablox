package shord.analyses;

import soot.Unit;

import shord.project.analyses.ProgramDom;

/**
 * Domain of abstract contexts.
 * <p>
 * The 0th element in this domain denotes the distinguished abstract context <tt>epsilon</tt>
 * (see {@link chord.analyses.alias.Ctxt}).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Saswat Anand
 */
public class DomC extends ProgramDom<Ctxt> 
{
    public Ctxt setCtxt(Object[] elems) {
        Ctxt cVal = new Ctxt(elems);
        int cIdx = indexOf(cVal);
        if (cIdx != -1)
            return (Ctxt) get(cIdx);
        getOrAdd(cVal);
        return cVal;
    }
}
