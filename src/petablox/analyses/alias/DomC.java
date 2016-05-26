package petablox.analyses.alias;

import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import petablox.analyses.alloc.DomH;
import petablox.analyses.invk.DomI;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.SootUtilities;
import petablox.util.Utils;


/**
 * Domain of abstract contexts.
 * <p>
 * The 0th element in this domain denotes the distinguished abstract context <tt>epsilon</tt>
 * (see {@link petablox.analyses.alias.Ctxt}).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomC extends ProgramDom<Ctxt> {
    private DomH domH;
    private DomI domI;

    public Ctxt setCtxt(Unit[] elems) {
        Ctxt cVal = new Ctxt(elems);
        int cIdx = indexOf(cVal);
        if (cIdx != -1)
            return (Ctxt) get(cIdx);
        getOrAdd(cVal);
        return cVal;
    }
    
    @Override
    public String toFIString(Ctxt cVal) {		  
    	if (domH == null)
            domH = (DomH) ClassicProject.g().getTrgt("H");
        if (domI == null)
            domI = (DomI) ClassicProject.g().getTrgt("I");
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if (printId) sb.append("(" + indexOf(cVal) + ")");	
    	sb.append("[");
        Unit[] elems = cVal.getElems();
        int n = elems.length;
        for (int i = 0; i < n; i++) {
            Unit eVal = elems[i];
            if (SootUtilities.isInvoke(eVal))
            	sb.append(domI.toFIString(eVal));
            else if(eVal instanceof JAssignStmt){
            	JAssignStmt as =(JAssignStmt) eVal;
            	if(SootUtilities.isNewStmt(as)||SootUtilities.isNewArrayStmt(as)||SootUtilities.isNewMultiArrayStmt(as))
            		sb.append(domH.toFIString(eVal));
            } else
                assert false;
            if (i < n - 1)
                sb.append(",");
        }
        sb.append("]");
    	return sb.toString();
    }

    @Override
    public String toXMLAttrsString(Ctxt cVal) {
        if (domH == null)
            domH = (DomH) ClassicProject.g().getTrgt("H");
        if (domI == null)
            domI = (DomI) ClassicProject.g().getTrgt("I");
        Unit[] elems = cVal.getElems();
        int n = elems.length;
        if (n == 0)
            return "";
        String s = "ids=\"";
        for (int i = 0; i < n; i++) {
            Unit eVal = elems[i];
            if (SootUtilities.isInvoke(eVal)) {
	            int iIdx = domI.indexOf(eVal);
	            s += "I" + iIdx;
            } else if(eVal instanceof JAssignStmt){
            	JAssignStmt as =(JAssignStmt) eVal;
            	if(SootUtilities.isNewStmt(as)||SootUtilities.isNewArrayStmt(as)||SootUtilities.isNewMultiArrayStmt(as)){
            		int hIdx = domH.indexOf(eVal);
                    s += "H" + hIdx;
            	}
            } else
                assert false;
            if (i < n - 1)
                s += " ";
        }
        return s + "\" ";
    }
}
