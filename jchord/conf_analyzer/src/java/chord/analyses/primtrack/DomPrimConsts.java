package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

@Chord(name = "PConst"
//		namesOfTypes = { "PConst" },
//		types = { DomPrimConsts.class }
)
public class DomPrimConsts extends ProgramDom<String> implements IInstVisitor {
	
  public void init() {
    super.init();
    getOrAdd("UNKNOWN");
  }

  public void visit(jq_Class c) {
  }

  public void visit(jq_Method m) {
  }

  @Override
  public void visit(Quad q) {
    for (Object op : q.getAllOperands()) {
      Operand operand = (Operand) op;
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
    		getOrAdd(wrapped.toString());
    }
  }

}
