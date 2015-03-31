package chord.analyses.string;


import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * The domain of string constants in a program.
 * Element 0 is the distinguished hypothetical element UNKNOWN.
 * 
 * @author asrabkin
 * 
 */
@Chord(name = "StrConst")
public class DomStrConst extends ProgramDom<String> implements IInstVisitor {

  private static final long serialVersionUID = 1L;

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
    for (Object op :q.getAllOperands()) {
      Operand operand = (Operand) op;
      if (operand instanceof Operand.AConstOperand) {
        Object wrapped = ((Operand.AConstOperand) operand).getWrapped();
        if (wrapped != null && wrapped instanceof String)
          getOrAdd(wrapped.toString());
      }
    }
  }

}
