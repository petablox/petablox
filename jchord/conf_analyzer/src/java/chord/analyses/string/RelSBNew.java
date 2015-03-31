package chord.analyses.string;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.invk.DomI;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "SBNew",
    sign = "I0:I0"
  )
public class RelSBNew extends ProgramRel implements IInvokeInstVisitor {
  
  DomI domI;
  jq_Method method;
  int mIdx = 0;
  public void init() {
    domI = (DomI) doms[0];
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
    if( (classname.equals("java.lang.StringBuffer") || classname.equals("java.lang.StringBuilder"))
        && methname.equals("<init>")) {
      super.add(q);
    }
  }

}
