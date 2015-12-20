package petablox.analyses.argret;

import java.util.List;

import soot.Unit;
import soot.RefLikeType;
import soot.Local;
import soot.Value;
import petablox.analyses.invk.DomI;
import petablox.analyses.var.DomV;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (i,v) such that local variable v
 * is the 0th argument variable of method invocation quad i.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "IinvkArg0",
    sign = "I0,V1:I0_V1"
)
public class RelIinvkArg0 extends ProgramRel {
    @Override
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomV domV = (DomV) doms[1];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit u = (Unit) domI.get(iIdx);
            Value v = null;
            if (SootUtilities.isInstanceInvoke(u)) {
            	v = SootUtilities.getInstanceInvkBase(u);
            } else if (SootUtilities.isStaticInvoke(u)) {
	            List<Value> l = SootUtilities.getInvokeArgs(u);
	            if (l.size() > 0) {
	            	v = l.get(0);  
	            }
            }
            if (v != null && v.getType() instanceof RefLikeType && v instanceof Local) {
            	Local r=(Local)v;
                int vIdx = domV.indexOf(r);
                assert (vIdx >= 0);
                add(iIdx, vIdx);
            }
        }
    }
}
