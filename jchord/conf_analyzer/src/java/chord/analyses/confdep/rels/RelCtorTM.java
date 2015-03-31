package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.analyses.method.DomM;
import chord.analyses.type.DomT;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "ctorTM",
    sign = "T0,M0:T0xM0"
  )
public class RelCtorTM extends ProgramRel implements IMethodVisitor {
  DomT domT;
  DomM domM;

  jq_Class curC; 
  
  public void init() {
    domT = (DomT) doms[0];
    domM = (DomM) doms[1];
  }

  @Override
  public void visit(jq_Method m) {
    if(m.getName().equals("<init>")) {
      super.add(curC, m);
    }    
  }

  @Override
  public void visit(jq_Class c) {
    curC = c;    
  }

}
