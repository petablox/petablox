package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


/**
 * Contains points (p0,u0) where at point p, value u is set to a constant.
 * @author asrabkin
 *
 */
@Chord(
		name = "PPrimConstAssign",
		sign = "P0,U0:P0_U0"
)
public class PPrimAssign extends ProgramRel implements IMoveInstVisitor {


	public void visit(jq_Class c) { }


	public void visitMoveInst(Quad q) {
		Operand operand = Move.getSrc(q);
		Register dest = Move.getDest(q).getRegister();

		if (operand instanceof Operand.IConstOperand ||
				operand instanceof Operand.FConstOperand ||
				operand instanceof Operand.LConstOperand ||
				operand instanceof Operand.DConstOperand )  {
				super.add(q, dest);
		}
	}

	@Override
	public void visit(jq_Method m) {
	}


}
