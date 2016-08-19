package petablox.analyses.provenance.typestate;

import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;

/**
 * Relation containing each tuple (m,k) such that method m
 * has k reference type arguments.
 *
 * @author Ravi Mangal
 */
@Petablox(
    name = "MK",
    sign = "M0,K0:M0_K0"
)
public class RelMK extends ProgramRel {
    @Override
    public void fill() {
    	DomM domM = (DomM) doms[0];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m == null || m.isAbstract())
                continue;
            Local[] args = SootUtilities.getMethArgLocals(m);
            int numRefArgs = 0;
            for( Local arg : args){
            	if(arg.getType() instanceof RefLikeType)
            		numRefArgs++;
            }
            add(mIdx, numRefArgs);            
        }
    }
}
