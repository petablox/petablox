package chord.analyses.confdep.optnames;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.primtrack.DomUV;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "rawConfUVV",
    sign = "I0,UV0,V0:I0_UV0_V0"
  )
public class RelConfUVV extends ProgramRel implements IInvokeInstVisitor{
  DomI domI;
  DomV domV;
  DomUV domUV;
  jq_Method method;
  public void init() {
    domI = (DomI) doms[0];
    domUV = (DomUV) doms[1];
    domV = (DomV) doms[2];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }

  @Override
  public void visitInvokeInst(Quad q) {
    
    int optPos = ConfDefines.confOptionPos(q);
    if(optPos >= 0) {
      RegisterOperand vo = Invoke.getDest(q);  
      int i_id = domI.indexOf(q);
      int o_id = domUV.indexOf(vo.getRegister());
      ParamListOperand plo = Invoke.getParamList(q);
      if(plo.length() > optPos) {
        RegisterOperand arg = Invoke.getParam(q, optPos);
        int parm_id = domV.indexOf(arg.getRegister());
        if(i_id > -1 && o_id > -1 && parm_id > -1)
          add(i_id, o_id, parm_id);
      } else {
        System.err.println("trouble on " + method.getName() +  q.getLineNumber()+ 
            " expected at least " + optPos + " params");
      }
    }
    
  }

}
