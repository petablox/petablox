package chord.analyses.confdep.rels;


import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.method.DomM;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Should really be called PConstReturn
 */

@Chord(
		name = "PReturn",
		sign = "M0,P0:M0_P0"
)
public class PReturn extends ProgramRel implements IReturnInstVisitor {
	
	DomP domP;
	DomM domM;
//	DomU domU; //can't show var; what if we return a const?

	public void init() {
		domM = (DomM) doms[0];
		domP = (DomP) doms[1];
//		domU = (DomU) doms[2];

	}
	
	@Override
	public void visit(jq_Class c) { }
	

	@Override
	public void visit(jq_Method m) {		
	}
	
	public void visitReturnInst(Quad q) {
		Operand operand = Return.getSrc(q);
		jq_Method meth = q.getMethod();
		
		if(operand != null) {
//			System.out.println(meth + " had non-null return");
			super.add(meth,q);
		}

/*		if (operand instanceof Operand.) {
			Object wrapped = ((Operand.AConstOperand) operand).getWrapped();
			if (wrapped == null)
				super.add(meth,q);
		}*/
	}
}
