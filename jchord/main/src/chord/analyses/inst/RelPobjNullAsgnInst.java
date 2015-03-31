package chord.analyses.inst;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.visitors.IMoveInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p,v) such that the statement at program
 * point p is an assignment statement where null is assigned to local
 *  reference variable v.
 * 
 * @author Xin Zhang
 */
@Chord(name = "PobjNullAsgnInst", sign = "P0,V0:P0_V0")
public class RelPobjNullAsgnInst extends ProgramRel implements IMoveInstVisitor {
	public void visit(jq_Class c) {
	}

	public void visit(jq_Method m) {
	}

	public void visitMoveInst(Quad q) {
		Operand rx = Move.getSrc(q);
		if (!(rx instanceof RegisterOperand)) {
			RegisterOperand lo = Move.getDest(q);
			if (lo.getType().isReferenceType()) {
				Register l = lo.getRegister();
				add(q, l);
			}
		}
	}
}
