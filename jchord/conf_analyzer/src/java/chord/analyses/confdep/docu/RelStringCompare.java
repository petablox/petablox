package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Holds tuples (i, v, v2) where i compares string v to string v2
 *
 */
@Chord(
    name = "stringCompare",
    sign = "I0,V0,V1:I0xV0xV1"
  )
public class RelStringCompare extends ProgramRel implements IInvokeInstVisitor {
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
//    mIdx = domM.indexOf(m);
  }
  

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
 //   int iIdx = domI.indexOf(q);
    if(classname.equals("java.lang.String") && methname.startsWith("equals")) {
      Register lreg = Invoke.getParam(q,0).getRegister();
      Register rreg = Invoke.getParam(q,1).getRegister();
      
     
      super.add(q, lreg, rreg);
      
    }
  }

}
