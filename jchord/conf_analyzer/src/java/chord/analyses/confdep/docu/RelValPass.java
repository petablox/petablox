package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;

import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "passThru",
    sign = "V0,I0,V1:V0xI0xV1"
  )
public class RelValPass extends ProgramRel implements IInvokeInstVisitor{

  DomI domI;
  DomV domV;
  jq_Method method;
  int mIdx = 0;
  
  
  public void init() {
    domV = (DomV) doms[0];
    domI = (DomI) doms[1];
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
    int args = Invoke.getParamList(q).length();
    
//    System.out.println("passThru: classname = " + classname + " methname = " + methname);
    
    if( (ConfDefines.wrapperClasses.contains(classname) && methname.startsWith("valueOf")) ||
        ("java.lang.String".equals(classname) &&  (args == 1 || methname.endsWith("erCase") )) ) {
      RegisterOperand lrop = Invoke.getDest(q);
      RegisterOperand rop = Invoke.getParam(q,0);
      if(lrop != null && rop != null) {
        Register lreg = lrop.getRegister();
        Register rreg = rop.getRegister();
  
        int lIdx = domV.indexOf(lreg);
        int iIdx = domI.indexOf(q);
        int rIdx = domV.indexOf(rreg);
        if(lIdx < 0 || rIdx < 0) {
          // System.err.println("arg or return for " +classname + "."+ methname + " not in domV ("+ lIdx + " " + rIdx + ")");
        } else
          super.add(lIdx, iIdx,rIdx);
      }
    }  else if("java.lang.Enum".equals(classname) && "valueOf".equals(methname)) {
      Register lreg = Invoke.getDest(q).getRegister();
      Register rreg = Invoke.getParam(q,1).getRegister();//first arg is classname
      
      super.add(lreg,q,rreg);
    }
  }
  
}
