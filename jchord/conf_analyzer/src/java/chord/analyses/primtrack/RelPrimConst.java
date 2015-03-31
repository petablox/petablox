package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import chord.program.visitors.IMoveInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
		consumes = { "PConst"},
		name = "primConst",
		sign = "U0,PConst0:U0xPConst0"
)
public class RelPrimConst extends ProgramRel implements IMoveInstVisitor  {

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }

	public void visitMoveInst(Quad q) {
		Operand operand = Move.getSrc(q);
		RegisterOperand dest = Move.getDest(q);
		
    if (!dest.getType().isReferenceType()) {
			Object wrapped = null;
			if (operand instanceof Operand.IConstOperand) {
				wrapped = ((Operand.IConstOperand) operand).getWrapped();
			} else if(operand instanceof Operand.FConstOperand) {
				wrapped = ((Operand.FConstOperand) operand).getWrapped();
			} else if(operand instanceof Operand.LConstOperand) {
				wrapped = ((Operand.LConstOperand) operand).getWrapped();
			} else if(operand instanceof Operand.DConstOperand) {
				wrapped = ((Operand.DConstOperand) operand).getWrapped();
			}
			if(wrapped != null)
				add(dest.getRegister(), wrapped.toString());
    }
	}
}
