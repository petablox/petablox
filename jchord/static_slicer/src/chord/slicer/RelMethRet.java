package chord.slicer;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IReturnInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing tuples (m, u) such that a method m returns u.
 * @author sangmin
 *
 */
@Chord(
	name = "methRet",
	sign = "M0,U0:M0_U0"
)
public class RelMethRet extends ProgramRel implements IReturnInstVisitor {
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitReturnInst(Quad q) {
		Operand rx = Return.getSrc(q);
		// note: rx is null if this method returns void
		if (rx != null && rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			Register u = ro.getRegister();
			add(ctnrMethod, u);
		}
	}
}
