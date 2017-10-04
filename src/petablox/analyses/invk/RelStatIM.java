package petablox.analyses.invk;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;
import petablox.analyses.invk.DomI;
import petablox.analyses.invk.StubRewrite;
import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (i,m) such that m is the resolved method of
 * method invocation quad i of kind {@code INVK_STATIC}.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "statIM",
    sign = "I0,M0:I0xM0"
)
public class RelStatIM extends ProgramRel {
    private static final String NOT_FOUND =
        "WARN: RelStatIM: Target %s of call site %s not in domain M; skipping.";
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomM domM = (DomM) doms[1];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit i = (Unit) domI.get(iIdx);
            if(SootUtilities.isInvoke(i)){
                InvokeExpr ie = SootUtilities.getInvokeExpr(i);
                if(ie instanceof StaticInvokeExpr){
                    SootMethod m = ie.getMethod();
                    if(m.isStatic()){
                        m = StubRewrite.maybeReplaceCallDest(SootUtilities.getMethod(i), m);
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
