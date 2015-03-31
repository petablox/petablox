package chord.slicer;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing tuples (m, u, z) such that u (Register) is
 * the z-th argument of method m.
 * @author sangmin
 *
 */
@Chord(
	name = "methArg",
	sign = "M0,U0,Z0:M0_U0_Z0"
)
public class RelMethArg extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomU domU = (DomU) doms[1];
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract()) continue;
			int nArgs = m.getParamTypes().length;
			RegisterFactory rf = m.getCFG().getRegisterFactory();
			for (int zIdx=0; zIdx < nArgs; zIdx++) {
				int uIdx = domU.indexOf(rf.get(zIdx));
				add(mIdx, uIdx, zIdx);				
			}	
		}			
	}

}
