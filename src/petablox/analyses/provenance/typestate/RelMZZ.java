package petablox.analyses.provenance.typestate;

import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;

/**
 * Relation containing each tuple (m,z1,z2) such that z1 & z2
 * are indices of consecutive reference type arguments of method m.
 *
 * @author Ravi Mangal
 */
@Petablox(
    name = "MZZ",
    sign = "M0,Z0,Z1:M0_Z0xZ1"
)
public class RelMZZ extends ProgramRel {
    @Override
    public void fill() {
    	DomM domM = (DomM) doms[0];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (!m.isConcrete())
                continue;
            Local[] args = SootUtilities.getMethArgLocals(m);
            int numArgs = args.length;
            int prevZIdx = -1;
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Local v = args[zIdx];
                if (v.getType() instanceof RefLikeType) {
                	if(prevZIdx != -1)
                		add(mIdx, prevZIdx,zIdx);
                	prevZIdx = zIdx;
                }
            }
        }
    }
}
