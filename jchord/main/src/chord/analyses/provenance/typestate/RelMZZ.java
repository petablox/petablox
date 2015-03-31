package chord.analyses.provenance.typestate;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,z1,z2) such that z1 & z2
 * are indices of consecutive reference type arguments of method m.
 *
 * @author Ravi Mangal
 */
@Chord(
    name = "MZZ",
    sign = "M0,Z0,Z1:M0_Z0xZ1"
)
public class RelMZZ extends ProgramRel {
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
            int prevZIdx = -1;
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Register v = rf.get(zIdx);
                if (v.getType().isReferenceType()) {
                	if(prevZIdx != -1)
                		add(mIdx, prevZIdx,zIdx);
                	prevZIdx = zIdx;
                }
            }
        }
    }
}
