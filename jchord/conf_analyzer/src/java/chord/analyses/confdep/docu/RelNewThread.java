package chord.analyses.confdep.docu;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
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
 * Holds (i,m,v) if i is a call to a constructor for some runnable variable v,
 * m is the associated run method
 *
 */
@Chord(
    name = "newThread",
    sign = "I0,M0,V0:I0xM0xV0"
  )
public class RelNewThread extends ProgramRel implements IInvokeInstVisitor{
  
DomI domI;
DomM domM;
DomV domV;
jq_Method method;
int mIdx = 0;
public jq_Type RUNNABLE;

public void init() {
  domI = (DomI) doms[0];
  domM = (DomM) doms[1];
  domV = (DomV) doms[2];

  RUNNABLE = jq_Type.parseType("java.lang.Runnable");
  RUNNABLE.prepare();  
}

public void visit(jq_Class c) { }
public void visit(jq_Method m) {
  method = m;
//  mIdx = domM.indexOf(m);
}


@Override
public void visitInvokeInst(Quad q) {
  jq_Method meth = Invoke.getMethod(q).getMethod();
  jq_Class cl = meth.getDeclaringClass();
  if(meth.getName().toString().equals("<init>") && !cl.getName().equals("java.lang.Thread") &&
      cl.isSubtypeOf(RUNNABLE)) {
    System.out.println("found runnable (" + cl.getName() +  ") in construction");
    jq_Method runMeth = cl.getDeclaredMethod("run");
    Register thisObj = Invoke.getParam(q, 0).getRegister();
    if(runMeth != null && thisObj != null) {
      if(domM.contains(runMeth))
        add(q, runMeth, thisObj);
      else
        System.out.println("WARN: run method of " + cl.getName() +  "  not found in DomM");
    } else
      System.out.println("NOTE: no run method for type " + cl.getName());
  }
}

}
