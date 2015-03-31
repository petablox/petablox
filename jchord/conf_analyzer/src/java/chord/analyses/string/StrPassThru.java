package chord.analyses.string;

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
   * Tuples (i,v,v2) where quad i passes v2 through to v
   * @author asrabkin
   *
   */
  @Chord(
      name = "StrPass",
      sign = "I0,V0,V1:I0xV0xV1"
    )
  public class StrPassThru extends ProgramRel implements IInvokeInstVisitor {
    DomI domI;
    DomV domV;
    jq_Method method;
    int mIdx = 0;
    public void init() {
      domI = (DomI) doms[0];
      domV = (DomV) doms[1];
    }

    public void visit(jq_Class c) { }
    public void visit(jq_Method m) {
      method = m;
//      mIdx = domM.indexOf(m);
    }
    

    @Override
    public void visitInvokeInst(Quad q) {
      jq_Method meth = Invoke.getMethod(q).getMethod();
      String classname = meth.getDeclaringClass().getName();
      String methname = meth.getName().toString();
   //   int iIdx = domI.indexOf(q);
      if(classname.startsWith("java.lang.String") && Invoke.getParamList(q).length() > 0) {
       // && !methname.startsWith("append")
        RegisterOperand dest = Invoke.getDest(q);
        if(dest == null)
          return;
        Register lreg = dest.getRegister();
        Register rreg = Invoke.getParam(q,0).getRegister();
       
        if(rreg.getType().isReferenceType() &&  meth.getReturnType().getName().startsWith("java.lang.String") ) //what should this really be?
          super.add(q, lreg, rreg);

      }
    }

}
