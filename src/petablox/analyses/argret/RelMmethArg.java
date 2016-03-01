package petablox.analyses.argret;

import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;
import petablox.analyses.method.DomM;
import petablox.analyses.var.DomV;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,z,v) such that local variable
 * v is the zth argument variable of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "MmethArg",
    sign = "M0,Z0,V0:M0_V0_Z0"
)
public class RelMmethArg extends ProgramRel {
    @Override
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomV domV = (DomV) doms[2];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (!m.isConcrete())
                continue;
            Local[] rf = SootUtilities.getMethArgLocals(m);
            int numArgs = rf.length;
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Local v=rf[zIdx];
                if (v.getType() instanceof RefLikeType) {
                    int vIdx = domV.indexOf(v);
                    assert (vIdx >= 0);
                    add(mIdx, zIdx, vIdx);
                }
            }
        }
    }
}
