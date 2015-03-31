package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.analyses.method.DomM;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Holds tuples (m, v, i, u) where i is a call to clone, in method m, that
 * causes v to be a clone of u.
 */
@Chord(
    name = "Mclones",
    sign = "M0,V0,I0,V1:M0_V0xI0xV1"
  )
public class RelClone extends ProgramRel implements IInvokeInstVisitor {
  DomI domI;
  DomV domV;
  DomM domM;
  jq_Method method;
  int mIdx = 0;
  public void init() {
    domM = (DomM) doms[0];
    domV = (DomV) doms[1];
    domI = (DomI) doms[2];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
    mIdx = domM.indexOf(m);
  }
  

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
//    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
    if("clone".equals(methname)) {
      
      Register lreg = Invoke.getDest(q).getRegister();
      Register rreg = Invoke.getParam(q,0).getRegister();
      int lIdx = domV.indexOf(lreg);
      int rIdx = domV.indexOf(rreg);
      int iIdx = domI.indexOf(q);
      super.add(mIdx, lIdx, iIdx, rIdx);
    }
  }
}