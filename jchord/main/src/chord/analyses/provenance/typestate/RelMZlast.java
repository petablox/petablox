package chord.analyses.provenance.typestate;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,z) such that the index
 * of the last reference type argument of method m is z. If
 * m has no reference arguments no entry will be added.
 *
 * @author Ravi Mangal
 */
@Chord(
    name = "MZlast",
    sign = "M0,Z0:M0_Z0"
)
public class RelMZlast extends ProgramRel {
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
            for (int zIdx = numArgs - 1; zIdx >= 0; zIdx--) {
                Register v = rf.get(zIdx);
                if (v.getType().isReferenceType()) {
                    add(mIdx, zIdx);
                    break;
                }
            }
        }
    }
}
