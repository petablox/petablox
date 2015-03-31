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
    name = "primConversion",
    sign = "U0,I0,V0:U0xI0xV0"
  )
public class RelPrimConversion extends ProgramRel implements IInvokeInstVisitor {
  DomU domU;
  DomI domI;
  DomV domV;
  jq_Method method;
  int mIdx = 0;
  
  
  public void init() {
    domU = (DomU) doms[0];
    domI = (DomI) doms[1];
    domV = (DomV) doms[2];
    
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
    if(ConfDefines.wrapperClasses.contains(classname) && (methname.startsWith("parse") ||
        methname.endsWith("Value"))) { // || methname.endsWith("Value")) {

      /*if( ("parseLong".equals(methname) && "java.lang.Long".equals(classname)) ||
        ("parseInt".equals(methname) && "java.lang.Integer".equals(classname)) ||
        ("parseDouble".equals(methname) && "java.lang.Double".equals(classname)) ||
        ("parseFloat".equals(methname) && "java.lang.Float".equals(classname)) ||
        ("parseShort".equals(methname) && "java.lang.Short".equals(classname)) ||
        ("parseByte".equals(methname) && "java.lang.Byte".equals(classname)) ||
        ("parseBoolean".equals(methname) && "java.lang.Boolean".equals(classname)) 
    ) */ 
      Register lreg = Invoke.getDest(q).getRegister();
      Register rreg = Invoke.getParam(q,0).getRegister();

      int lIdx = domU.indexOf(lreg);
      int rIdx = domV.indexOf(rreg);
      int iIdx = domI.indexOf(q);
//      if(lIdx < 0 || rIdx < 0) {
//        System.err.println("args to " + methname + " not in dom");
//      }
      super.add(lIdx, iIdx,rIdx);
    }
  }

}
