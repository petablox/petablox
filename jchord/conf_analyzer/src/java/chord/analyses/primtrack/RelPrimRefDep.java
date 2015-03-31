package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALength;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.var.DomV;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import joeq.Compiler.Quad.RegisterFactory.Register;



/**
 * Contains tuples (u,v) where value u depends on reference value v
 * @author asrabkin
 *
 */
@Chord(
    name = "primRefDep",
    sign = "UV0,V0:UV0_V0"
  )

public class RelPrimRefDep extends ProgramRel implements IInstVisitor{
  
  DomUV domUV;
  DomV domV;
  jq_Method method;
  public void init() {
    domUV = (DomUV) doms[0];
    domV = (DomV) doms[1];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }

  @Override
  public void visit(Quad q) {
    if(q.getOperator() instanceof Invoke) {
      RegisterOperand vo = Invoke.getDest(q);
      if(vo != null) {
        Register r = vo.getRegister();
        int resultIdx = domUV.indexOf(r);
        if(r.getType().isReferenceType()) {
          super.add(resultIdx, domV.indexOf(r));
        }
        int paramCount = Invoke.getParamList(q).length();
        for(int i=0; i < paramCount; ++i) {
          Register arg = ((RegisterOperand)Invoke.getParam(q, i)).getRegister();
          if(arg.getType().isReferenceType()) {
            int argIdx = domV.indexOf(arg);
            super.add(resultIdx, argIdx);
          }
        }
      }
    } else if(q.getOperator() instanceof Operator.ALength) {
      RegisterOperand vo = ALength.getDest(q);
      Register r = vo.getRegister();
      int resultIdx = domUV.indexOf(r);
      Operand arg = ALength.getSrc(q);
      if(arg instanceof RegisterOperand) {
        int argIdx = domV.indexOf(((RegisterOperand)arg).getRegister());
        super.add(resultIdx, argIdx);
      }
      
    } else if(q.getOperator() instanceof Operator.CheckCast) {
    	RegisterOperand res = CheckCast.getDest(q);
      int destIdx = domUV.indexOf(res.getRegister());
      if(CheckCast.getSrc(q) instanceof RegisterOperand) {
	      Register src =  ((RegisterOperand) CheckCast.getSrc(q)).getRegister();
	      int srcIdx = domV.indexOf(src);
	      if(destIdx > -1 && srcIdx > -1)
	      	super.add(destIdx, srcIdx);
      }
    }
  }

}
