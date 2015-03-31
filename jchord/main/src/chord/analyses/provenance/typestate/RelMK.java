package chord.analyses.provenance.typestate;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,k) such that method m
 * has k reference type arguments.
 *
 * @author Ravi Mangal
 */
@Chord(
    name = "MK",
    sign = "M0,K0:M0_K0"
)
public class RelMK extends ProgramRel {
    @Override
    public void fill() {
    	DomM domM = (DomM) doms[0];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            jq_Method m = domM.get(mIdx);
            if (m.isAbstract())
                continue;
            ControlFlowGraph cfg = m.getCFG();
            RegisterFactory rf = cfg.getRegisterFactory();
            int numArgs = m.getParamTypes().length;
            int numRefArgs = 0;
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Register v = rf.get(zIdx);
                if (v.getType().isReferenceType()) {
                    numRefArgs++;
                }
            }
            add(mIdx, numRefArgs);            
        }
    }
}
