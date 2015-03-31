package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.*;
import chord.analyses.primtrack.*;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "getTime",
    sign = "U0,I0:U0xI0"
  )
public class RelMarkTime extends ProgramRel implements IInvokeInstVisitor {
  DomI domI;
  DomU domU;
  jq_Method method;
  int mIdx = 0;
  public void init() {
    domU = (DomU) doms[0];
    domI = (DomI) doms[1];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
//    mIdx = domM.indexOf(m);
  }
  

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
 //   String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
    if("currentTimeMillis".equals(methname) || "nanoTime".equals(methname)) { // && "java.lang.System".equals(classname)) {
      
      Register lreg = Invoke.getDest(q).getRegister();
      int lIdx = domU.indexOf(lreg);
//      int rIdx = domV.indexOf(rreg);
      int iIdx = domI.indexOf(q);
      super.add(lIdx, iIdx);
    }
  }

}
