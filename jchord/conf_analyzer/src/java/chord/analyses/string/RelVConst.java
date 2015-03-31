package chord.analyses.string;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 *  A relation containing elements (v,c)
 *  where v is a reference variable which has string constant c assigned to it. 
 * @author asrabkin
 *
 */
@Chord(
    name = "VConst",
    sign = "V0,StrConst0:StrConst0_V0"
  )
public class RelVConst extends ProgramRel implements IMoveInstVisitor {
  
  private static final long serialVersionUID = 1L;

  public void init() {
    super.init();
  }

  public void visit(jq_Class c) {
  }

  public void visit(jq_Method m) {
  }

  @Override
  public void visitMoveInst(Quad q) {

    RegisterOperand rx = Move.getDest(q);
    if(rx != null) {
      RegisterFactory.Register reg = rx.getRegister();
      for (Object op : q.getAllOperands()) {
        Operand operand = (Operand) op;
        if (operand instanceof Operand.AConstOperand) {
          Object wrapped = ((Operand.AConstOperand) operand).getWrapped();
          if (wrapped != null && wrapped instanceof String) {
            add(reg, wrapped.toString());
          }
        }
      }
    }
  }

}
