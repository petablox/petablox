package chord.slicer;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


/**
 * Relation containing each tuple (i,u,z) such that local variable u
 * is the zth argument variable of method invocation statement i.
 *
 * @author sangmin
 */
@Chord(
	name = "invkArg",
	sign = "I0,U0,Z0:I0_U0_Z0"
)
public class RelInvkArg extends ProgramRel {
	public void fill() {
		DomI domI = (DomI) doms[0];
		DomU domU = (DomU) doms[1];
		int numI = domI.size();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			Quad q = (Quad) domI.get(iIdx);
			ParamListOperand l = Invoke.getParamList(q);
			int numArgs = l.length();
			for (int zIdx = 0; zIdx < numArgs; zIdx++) {
				RegisterOperand vo = l.get(zIdx);
				Register u = vo.getRegister();
				int uIdx = domU.indexOf(u);
				assert uIdx >= 0;
				add(iIdx, uIdx, zIdx);
			}
		}
	}
}
