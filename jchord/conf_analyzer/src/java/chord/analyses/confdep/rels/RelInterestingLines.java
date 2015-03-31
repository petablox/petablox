package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "InterestingLocals",
    sign = "I0,V0:I0_V0"
  )
public class RelInterestingLines extends ProgramRel implements IInstVisitor{

  private boolean isLineInteresting(int i) {
//    return i == 48;
    return true;
  }

  DomI domI;
  DomV domV;
  boolean methodMatches;
  jq_Method m;
  public void init() {
    domI = (DomI) doms[0];
    domV = (DomV) doms[1];
  }
  
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    this.m = m;
    String fqName = m.getDeclaringClass().getName()+"."+m.getName();
    methodMatches = !fqName.startsWith("org.apache.hadoop.conf") &&
        !fqName.startsWith("org.apache.commons.logging");

//    methodMatches = true;//fqName.startsWith("edu.berkeley.ToyConf.") ||
                   // fqName.startsWith("org.apache.hadoop.");
  }
  @Override
  public void visit(Quad q) {
    if(!methodMatches)
      return;
    
    int lineno = q.getLineNumber();
    if(q.getOperator() instanceof Invoke) {

      int iIdx = domI.indexOf(q);
      ParamListOperand parms = Invoke.getParamList(q);
      for(int i=0; i < parms.length(); ++i) {
        RegisterOperand operand = parms.get(i);
        if(operand != null ) { 
          Register v = operand.getRegister();
          if (v.getType()!= null && v.getType().isReferenceType()) {
            int vIdx = domV.indexOf(v);
//            System.out.println("adding " + v + " at line " + lineno);
            add(iIdx,vIdx);
            
          }
         }
       }
      RegisterOperand r_out = Invoke.getDest(q);
      if(r_out != null) {
        Register v = r_out.getRegister();
        if (v.getType()!= null && v.getType().isReferenceType()) {
          int vIdx = domV.indexOf(v);
          add(iIdx,vIdx);
        }
      }
    }
  }
  
}
