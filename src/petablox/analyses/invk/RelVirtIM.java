package petablox.analyses.invk;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (i,m) such that m is the resolved
 * method of method invocation quad i of kind {@code INVK_VIRTUAL} or
 * {@code INVK_INTERFACE}.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "virtIM",
    sign = "I0,M0:I0xM0"
)
public class RelVirtIM extends ProgramRel {
    private final static String NOT_FOUND =
        "WARN: RelVirtIM: Target method %s of call site %s not found in domain M.";
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomM domM = (DomM) doms[1];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit i = (Unit)domI.get(iIdx);
            if(i instanceof Stmt){
            	Stmt s = (Stmt)i;
            	if(s.containsInvokeExpr()){
            		InvokeExpr ie = s.getInvokeExpr();
            		if(ie instanceof JVirtualInvokeExpr || ie instanceof JInterfaceInvokeExpr){
	            		SootMethod m = s.getInvokeExpr().getMethod();
	            		int mIdx = domM.indexOf(m);
	                    if (mIdx >= 0) 
	                        add(iIdx, mIdx);
	                    else if (Config.verbose >= 2)
	                        Messages.log(NOT_FOUND, m, SootUtilities.toLocStr(i));
            		}
            	}
            }
        }
    }
}
