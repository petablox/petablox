package chord.analyses.confdep.rels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Move;
import chord.analyses.var.DomV;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "localMustAlias-J",
    sign = "V0,V1:V0_V1"
  )
public class RelLocalMustAlias extends ProgramRel implements IMethodVisitor {
  
  private Set<BasicBlock> visited = new HashSet<BasicBlock>();

  DomV domV;
  public void init() {
    domV = (DomV) doms[0];
  }
  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    if (m.isAbstract())
      return;
    if(m.getDeclaringClass().getName().equals("org.apache.hadoop.conf.Configuration"))
      return;
    ControlFlowGraph cfg = m.getCFG();
    BasicBlock entry = cfg.entry();
    processBB(entry);
    visited.clear();
  }

  private void processBB(BasicBlock bb) {
    HashMap<Integer, String> constVals = new HashMap<Integer, String>();
    
    int n = bb.size();
    for (int j = 0; j < n; j++) {
      Quad q = bb.getQuad(j);
      Operator op = q.getOperator();
      if(op instanceof Move) {
        Operand srcOperand = Move.getSrc(q);
//        System.out.println("moving " + srcOperand + " to " + Move.getDest(q));
        if(srcOperand instanceof Operand.AConstOperand) {
//          System.out.println("storing constant into " + Move.getDest(q));
          Object wrapped = ((Operand.AConstOperand)srcOperand).getWrapped();
          if(wrapped != null) {
            constVals.put(Move.getDest(q).getRegister().getNumber(), wrapped.toString());
          }
        }
        
      }
    }
    for (Object o : bb.getSuccessors()) {
      BasicBlock bb2 = (BasicBlock) o;
      if (!visited.contains(bb2)) {
        visited.add(bb2);
        processBB(bb2);
      }
    }

  }


}
