package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import chord.analyses.heapacc.DomE;
import chord.analyses.var.*;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


/**
 * Relation mapping local variables to an array which they were pulled out of.
 * If a program includes a quad M like:
 * N:  a = b[c], then (N,a,b) will be included in this relation.
 * @author asrabkin
 *
 */
@Chord(
    name = "FromArray",
    sign = "E0,V0,V1:E0_V0_V1"
  )
public class RelFromArray extends ProgramRel implements IInstVisitor {

  
  DomE domE;
  DomV domV;
  jq_Method method;
  public void init() {
    domE = (DomE) doms[0];
    domV = (DomV) doms[1];
  }


  @Override
  public void visit(jq_Class c) { }

  @Override
  public void visit(jq_Method m) {
    method = m;
  }
  
  @Override
  public void visit(Quad q) {
    if(q.getOperator() instanceof Operator.ALoad) {
      joeq.Compiler.Quad.Operand.RegisterOperand out = Operator.ALoad.getDest(q);
      if(out.getRegister().getType().isReferenceType()) {
        int e_ID = domE.indexOf(q);
        int voutID = domV.indexOf(out.getRegister());
        RegisterOperand base = (RegisterOperand) Operator.ALoad.getBase(q);
        int vbaseID = domV.indexOf(base.getRegister());
        if(voutID == -1) {
          int lineno = q.getLineNumber();
          System.out.println("WARN: can't find var for subscript output on line " + lineno); 
        } else if(vbaseID == -1) {
          System.out.println("WARN: can't find var for array");
        } else
          super.add(e_ID, voutID, vbaseID);
      }
    } else if(q.getOperator() instanceof Operator.ALength) {
      //output should be tainted -- but we can't yet handle primitives!
    }
  }

}
