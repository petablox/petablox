package chord.slicer;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing tuple (i, u) such that u is used as a parameter
 * of a method invocation i.
 * @author sangmin
 *
 */
@Chord(
		name="invkRet",
		sign="I0,U0:I0_U0"
)

public class RelInvkRet extends ProgramRel {

	public void fill() {
		DomI domI = (DomI) doms[0];
		DomU domU = (DomU) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			RegisterOperand uo = Invoke.getDest(q);
			if (uo != null) {
				Register u = uo.getRegister();
				int uIdx = domU.indexOf(u);
				assert (uIdx >= 0);
				add(iIdx, uIdx);
			}
		}
	}
}
