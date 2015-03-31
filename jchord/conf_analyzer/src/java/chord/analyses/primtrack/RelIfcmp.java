package chord.analyses.primtrack;

import chord.analyses.method.DomM;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.IntIfCmp;

@Chord(
    name = "MPrimCmp",
    sign = "M0,U0,U1:M0_U0xU1"
  )
public class RelIfcmp extends ProgramRel
  implements IInstVisitor {
  
  DomU domU;
  DomM domM;
  int mID;
  public void init() {
    domM= (DomM) doms[0];
     domU= (DomU) doms[1];
  }
    
  private jq_Method ctnrMethod;
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    ctnrMethod = m;
    mID = domM.indexOf(m);
  }
  public void visit(Quad q) {
    Operator op = q.getOperator();
    if(op instanceof Operator.IntIfCmp) {
      Operand op1 = IntIfCmp.getSrc1(q);
      Operand op2 = IntIfCmp.getSrc2(q);
      if(op1 != null && op2 != null && op1 instanceof RegisterOperand && op2 instanceof RegisterOperand) {
        int idx1 = domU.indexOf(((RegisterOperand) op1).getRegister());
        int idx2 = domU.indexOf(((RegisterOperand)op2).getRegister());
        if(idx1 > -1 && idx2 > -1 ) {
          add(mID, idx1, idx2);
        }
      }

    }
  }

}




