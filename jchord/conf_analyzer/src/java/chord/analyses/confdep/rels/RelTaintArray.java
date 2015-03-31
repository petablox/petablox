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
 * Includes tuples (I,a,b) where a is a String or String[], b is a string and there's a quad:
 * I:  a = b.method(...)
 * @author asrabkin
 *
 */
@Chord(
    name = "TaintArray",
    sign = "I0,V0,V1:I0_V0_V1"
  )
public class RelTaintArray extends ProgramRel implements IInvokeInstVisitor {
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
    RegisterOperand vo = Invoke.getDest(q);
    if (vo != null) {
      Register v = vo.getRegister();
      if (v.getType().isReferenceType()) {

        jq_Method meth = Invoke.getMethod(q).getMethod();
        String classname = meth.getDeclaringClass().getName();
//        String methname = meth.getName().toString();
        if(classname.equals("java.lang.String")) { //any string op returning a reference
//          System.out.println("Saw split:  quad was:" + q);
          int outIdx = domV.indexOf(v);
          int baseIdx = domV.indexOf( ((RegisterOperand)Invoke.getParam(q, 0)).getRegister());
          int iIdx = domI.indexOf(q);
          if(outIdx == -1) {
            int lineno = q.getLineNumber();
            System.out.println("WARN: string op with no var for output on line "+ lineno + " of " + classname);
          } else if(baseIdx == -1) {
            int lineno = q.getLineNumber();
            System.out.println("WARN: string op with no var for base on line " + lineno + " of " + classname);
          } else
            super.add(iIdx, outIdx, baseIdx);
        }
        
      }
    }
    
  }
  
  

}
