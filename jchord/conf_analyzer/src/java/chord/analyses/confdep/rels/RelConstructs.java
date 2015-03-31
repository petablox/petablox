package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Tuples (I,V) if I is a constructor invocation that constructs variable v
 * @author asrabkin
 *
 */

@Chord(
    name = "Constructs",
    sign = "I0,V0:I0_V0"
  )
public class RelConstructs extends ProgramRel implements IInvokeInstVisitor {
  DomI domI;
  DomV domV;
  jq_Method method;
  public void init() {
    domI = (DomI) doms[0];
    domV = (DomV) doms[1];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String methname = meth.getName().toString();
//    if(meth.isNative() && )
    if(methname.equals("<init>")) { //any string op returning a reference
      

      int iIdx = domI.indexOf(q);
      Register reg = ((RegisterOperand)Invoke.getParam(q, 0)).getRegister();

      int vIdx = domV.indexOf(reg);
      if(meth.getDeclaringClass().getName().startsWith("java."))
        super.add(iIdx,vIdx);
/*      int paramCount = Invoke.getParamList(q).length();
      for(int i=0; i < paramCount; ++i) {
        Register reg = ((RegisterOperand)Invoke.getParam(q, i)).getRegister();
        if(reg.getType().isReferenceType()) {
          int parmIdx = domV.indexOf(reg);
          super.add(iIdx, parmIdx);
        }
      }*/
    }
    
  }

}
