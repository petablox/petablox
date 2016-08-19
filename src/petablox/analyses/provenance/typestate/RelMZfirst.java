package petablox.analyses.provenance.typestate;

import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;

/**
 * Relation containing each tuple (m,z) such that the index
 * of the first reference type argument of method m is z. If
 * m has no reference arguments no entry will be added.
 *
 * @author Ravi Mangal
 */
@Petablox(
    name = "MZfirst",
    sign = "M0,Z0:M0_Z0"
)
public class RelMZfirst extends ProgramRel {
    @Override
    public void fill() {
    	DomM domM = (DomM) doms[0];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m == null || m.isAbstract())
                continue;
            Local[] args = SootUtilities.getMethArgLocals(m);
            for (int zIdx = 0; zIdx < args.length; zIdx++) {
                if (args[zIdx].getType() instanceof RefLikeType) {
                    add(mIdx, zIdx);
                    break;
                }
            }
        }
    }
}
