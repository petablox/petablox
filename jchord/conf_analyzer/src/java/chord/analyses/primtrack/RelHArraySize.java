package chord.analyses.primtrack;

import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.analyses.alloc.DomH;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation contains pairs (h,u) where h allocates an array of length u
 * @author asrabkin
 *
 */
@Chord(
    name = "HSize",
    sign = "H0,U0:H0_U0"
  )
public class RelHArraySize extends ProgramRel {
  

  public void fill() {
    DomH domH = (DomH) doms[0];
    DomU domU = (DomU) doms[1];
    int numA = domH.getLastA() + 1;
    for (int hIdx = 1; hIdx < numA; hIdx++) {
      Quad h = (Quad) domH.get(hIdx);
      Operator op = h.getOperator();
      // do NOT merge handling of New and NewArray
      if (op instanceof NewArray){
        Operand size = NewArray.getSize(h);
        if(size instanceof RegisterOperand) {
          RegisterFactory.Register sz = ((RegisterOperand) size).getRegister();
          int szIdx = domU.indexOf(sz);
          if(szIdx != -1)
            add(hIdx, szIdx);
        }
           
      }
    }
  }

}
