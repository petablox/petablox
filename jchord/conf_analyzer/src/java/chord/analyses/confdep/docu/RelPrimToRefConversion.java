package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.primtrack.DomU;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "primToRefConversion",
    sign = "V0,I0,U0:V0xI0xU0"
  )

public class RelPrimToRefConversion extends ProgramRel implements IInvokeInstVisitor {
  DomU domU;
  DomI domI;
  DomV domV;
  jq_Method method;
  int mIdx = 0;
  
  public void init() {
    domV = (DomV) doms[0];
    domI = (DomI) doms[1];
    domU = (DomU) doms[2];
    ConfDefines.wInit();
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

    
    if((ConfDefines.wrapperClasses.contains(classname) || "java.lang.String".equals(classname)) &&
        methname.equals("valueOf")) {
      Register lreg = Invoke.getDest(q).getRegister();
      Register rreg = Invoke.getParam(q,0).getRegister();

      int lIdx = domV.indexOf(lreg);
      int rIdx = domU.indexOf(rreg);
      int iIdx = domI.indexOf(q);
      if(lIdx < 0 || rIdx < 0) {
        System.err.println("args to " + methname +" not in dom [lIdx = " + lIdx + ", rIdx = " + rIdx +
        		" for i in " + classname + " " + methname);
      } else
        super.add(lIdx, iIdx,rIdx);
    }
  }

}
