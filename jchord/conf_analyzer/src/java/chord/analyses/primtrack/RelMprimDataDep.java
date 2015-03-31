package chord.analyses.primtrack;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Binary;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Unary;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.method.DomM;
import chord.program.visitors.*;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v1,v2) such that method m
 * contains a statement of the form <tt>v1 = op v2 </tt> or v1 = v2 op .....
 *
 *  INCLUDES copies
 *
 */
@Chord(
  name = "MprimDataDep",
  sign = "M0,U0,U1:M0_U0xU1"
)
public class RelMprimDataDep extends ProgramRel
  implements IInstVisitor {
  
  DomU domU;
  DomM domM;
  int mID;
  public void init() {
    domM= (DomM) doms[0];
     domU= (DomU) doms[1];
  }
    
  private jq_Method ctnrMethod;
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    ctnrMethod = m;
    mID = domM.indexOf(m);
  }
  public void visit(Quad q) {
    
    Operator op = q.getOperator();
    if(op instanceof Binary) {
      RegisterOperand dest = Binary.getDest(q);
      Operand src1 = Binary.getSrc1(q);
      Operand src2 = Binary.getSrc2(q);
      if(dest != null) {
        Register l = dest.getRegister();
        int i = domU.indexOf(l);
        if(!l.getType().isPrimitiveType()  || i == -1) {
          System.out.println("ERR: got non-primitive datadep.  Quad was " + q + " and type was " + l.getType());
          return;
        }
        if(src1 != null && src1 instanceof RegisterOperand) {
          Register r1 = ((RegisterOperand) src1).getRegister();
          int j = domU.indexOf(r1);
          if(j != -1)
            add(mID, i, j);
        }
        if(src2 != null && src2 instanceof RegisterOperand) {
          Register r2 = ((RegisterOperand) src2).getRegister();
          int j = domU.indexOf(r2);
          if(j != -1)
            add(mID, i, j);
        }
      }
      
    } else if(op instanceof Unary) {
      RegisterOperand dest = Unary.getDest(q);
      Operand src = Unary.getSrc(q);
      if(dest != null && src != null && src instanceof RegisterOperand) {
        Register l = dest.getRegister();
        Register r = ((RegisterOperand) src).getRegister();
        int i = domU.indexOf(l);
        int j = domU.indexOf(r);
        if(i != -1 && j != -1)
          add(mID, i, j);
      } 
    } else if(op instanceof Move) {
      Operand rx = Move.getSrc(q);
      if (rx instanceof RegisterOperand) {
        RegisterOperand ro = (RegisterOperand) rx;
        if (!ro.getType().isReferenceType()) {
          Register r = ro.getRegister();
          RegisterOperand lo = Move.getDest(q);
          Register l = lo.getRegister();
          add(ctnrMethod, l, r);
        }
      }
    } else if(op instanceof Phi) {
      RegisterOperand lo = Phi.getDest(q);
      jq_Type t = lo.getType();
      if (t != null && !t.isReferenceType()) {
        Register l = lo.getRegister();
        ParamListOperand ros = Phi.getSrcs(q);
        int n = ros.length();
        for (int i = 0; i < n; i++) {
          RegisterOperand ro = ros.get(i);
          if (ro != null) {
            Register r = ro.getRegister();
            add(ctnrMethod, l, r);
          }
        }
      }
    }
  }

}
