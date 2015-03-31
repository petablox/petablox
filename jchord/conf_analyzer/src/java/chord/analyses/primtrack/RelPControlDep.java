package chord.analyses.primtrack;

import java.util.*;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.ExceptionHandler;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.LookupSwitch;
import chord.analyses.invk.DomI;
import chord.analyses.point.DomP;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Tuples (i, uv) where reaching i depends on uv
 * @author asrabkin
 *
 */
@Chord(
    name = "PControlDep",
    sign = "P0,UV0:P0xUV0"
  )
public class RelPControlDep extends ProgramRel implements IMethodVisitor {

  DomUV domUV;
  DomP domP;
  jq_Method method;
  public void init() {
    domP = (DomP) doms[0];
    domUV = (DomUV) doms[1];
  }
  

  enum Constraint {
    ALWAYS,
    TRUE,
    FALSE
  }
  
  //list of control dependencies. Immutable after construction
  private class CDepList {
    
    CDepList addT(BasicBlock bb) {
      CDepList newC = new CDepList();
      newC.constraints.putAll(constraints);
      int bid = bb.getID();
      Constraint old = constraints.get(bid);
      if(old == null)
        newC.constraints.put(bid, Constraint.TRUE);
      else if(old == Constraint.FALSE)
        newC.constraints.put(bid, Constraint.ALWAYS);
      return newC;
    }
    
    CDepList addF(BasicBlock bb) {
      CDepList newC = new CDepList();
      newC.constraints.putAll(constraints);
      int bid = bb.getID();
      Constraint old = constraints.get(bid);
      if(old == null)
        newC.constraints.put(bid, Constraint.FALSE);
      else if(old == Constraint.TRUE)
        newC.constraints.put(bid, Constraint.ALWAYS);
      return newC;
    }
    
    
    //maps from BASIC BLOCK # to constraint on which way we went at the end of the block
    HashMap<Integer, Constraint> constraints = new HashMap<Integer, Constraint>();
    
    @Override
    public boolean equals(Object o) {
      if(o == null || !(o instanceof CDepList))
        return false;
      else {
        CDepList rhs = (CDepList) o;
        if(rhs.constraints.size() != this.constraints.size())
          return false;
        else {
          for(Map.Entry<Integer, Constraint> c: constraints.entrySet()) {
            if(rhs.constraints.get(c.getKey()) != c.getValue())
              return false;
          }
          return true;
        }
      }
    }
    
    Set<Integer> regIDs(HashMap<Integer,Quad> lastQofB) {

      Set<Integer> ids = new HashSet<Integer>(constraints.size()); 
      for(Integer blockID: constraints.keySet()) {
        Constraint constr = constraints.get(blockID);
        if(constr == Constraint.ALWAYS) {
    //      System.out.println("WRITEBACK: tail of block " + blockID + " is reached always.");
          continue;
       }
        Quad tail = lastQofB.get(blockID);
        
//        System.out.println("WRITEBACK: tail of block " + blockID + " uses " + tail.getUsedRegisters().size() + " registers");
        for(Object r: tail.getUsedRegisters()) {
          RegisterOperand reg = (RegisterOperand) r;
          int idx = domUV.indexOf(reg.getRegister());
          if(idx != -1)
            ids.add(idx);
        }
      }
      return ids;
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      for(Map.Entry<Integer, Constraint> c: constraints.entrySet()) {
        sb.append(c.getKey());
        Constraint constr = c.getValue();
        if(constr == Constraint.TRUE)
          sb.append('T');
        else if(constr == Constraint.FALSE)
          sb.append('F');
        else sb.append('A');
        sb.append(" ");
      }
      return sb.toString();
    }

    public CDepList merge(CDepList prevCD) {
      if(prevCD == null)
        return this;
      
      CDepList newC = new CDepList();
      newC.constraints.putAll(constraints);
      for(Map.Entry<Integer, Constraint> c: prevCD.constraints.entrySet()) {
        Constraint prev = newC.constraints.get(c.getKey());
        Constraint newer = c.getValue();
        if(prev == null)
          newC.constraints.put(c.getKey(), c.getValue());
        if(prev != null && prev != newer) {
          newC.constraints.put(c.getKey(),Constraint.ALWAYS);
        }
      }
      return newC;
    }
    
  }

  public void visit(jq_Class c) { }
  
  public void visit(jq_Method m) {
    if (m.isAbstract())
      return;
    
    if(m.getDeclaringClass().getName().equals("org.apache.hadoop.conf.Configuration"))
      return;
    boolean DEBUGLOG = false; 
    //can replace this with any useful predicate, e.g.:
    //m.getName().toString().contains("replayRecoveredEditsIfAny");

    ControlFlowGraph cfg = m.getCFG();
    BasicBlock entry = cfg.entry();
    HashMap<BasicBlock, CDepList> blockDep = new HashMap<BasicBlock, CDepList>(cfg.getNumberOfBasicBlocks());
    HashMap<Integer, Quad> lastQOfB = new HashMap<Integer,Quad>(cfg.getNumberOfBasicBlocks());
    
    if(DEBUGLOG)
    	System.err.println("WORKLIST: visiting " + m.getName()); 
    UniqueBQ worklist = new UniqueBQ();
    worklist.add(entry);
    blockDep.put(entry, new CDepList());
    
    //every exception handler is reachable.
    //But exceptions don't depend on prims, so not adding those deps in this class
    for(Object _eh: cfg.getExceptionHandlers()) {
    	ExceptionHandler eh = (ExceptionHandler) _eh;
    	blockDep.put(eh.getEntry(), new CDepList());
      worklist.add(eh.getEntry());
      if(DEBUGLOG)
      	System.err.println("WORKLIST: adding EH block " + eh.getEntry().getID());
    }
    
    while(!worklist.isEmpty()) {
      BasicBlock b = worklist.remove();
      CDepList curConds = blockDep.get(b);
      if(DEBUGLOG)
      	System.err.println("WORKLIST: block "+b.getID() + " has condition " + curConds);
      CDepList[] conds = getSuccConds(b, curConds);
      int i = 0;
      for(Object nextB_: b.getSuccessors()) {
        BasicBlock nextB = (BasicBlock) nextB_;
        CDepList prevCD = blockDep.get(nextB);
        if(conds[i] == null) {
          System.err.println("ERR: conds["+i+"] unexpectedly null for block" + b.getID() + " of " + m.getName());
          System.exit(1);
        } else {
          if(DEBUGLOG)
          	System.err.println("WORKLIST:  "+ nextB + " has entry condition " + conds[i]);

          if(!conds[i].equals(prevCD)) {
            CDepList merged = conds[i].merge(prevCD);
            blockDep.put(nextB, merged);
            worklist.add(nextB);
          }
        }
        i++;
      }      
    }
    
    for(BasicBlock b: blockDep.keySet()) {
      lastQOfB.put(b.getID(), b.getLastQuad());
    }
    
    for(Map.Entry<BasicBlock, CDepList> deps: blockDep.entrySet()) {
      BasicBlock bb = deps.getKey();
      CDepList l = deps.getValue();
      if(DEBUGLOG)
      	System.out.println("WRITEBACK: block" + bb.getID() + " of " + m.getName() + " has constraint set " + l);
      for(int uIdx: l.regIDs(lastQOfB)) {
        int n = bb.size();
        for (int j = 0; j < n; j++) {
          Quad q = bb.getQuad(j);
          Operator op = q.getOperator();
          int pIdx = domP.indexOf(q);
          super.add(pIdx, uIdx);
        }
      }
    }
  }

  private CDepList[] getSuccConds(BasicBlock b, CDepList curConds) {
    assert curConds != null;
    CDepList[] out = new CDepList[b.getSuccessors().size()];
    Quad endQuad = b.getLastQuad();

    if(out.length == 0) 
      return out; //no output from this block
    
    //empty blocks can happen, e.g. at method entry
    if(endQuad == null) {
      if(b.getSuccessors().size() > 1) {
        System.err.println("ERR: multiple outputs from empty block!");
      }
      for(int i= 0; i < out.length; ++i)
        out[i] = curConds;
      return out;
    }

    Operator last = endQuad.getOperator();
    if(last instanceof IntIfCmp) {
      int i= 0;
      for(Object nextB_: b.getSuccessors()) {
        BasicBlock nextB = (BasicBlock) nextB_;
        if(nextB == b.getFallthroughSuccessor())
          out[i++] = curConds.addF(b);
        else
          out[i++] = curConds.addT(b);
      }   
      if(i > 2) {
        System.err.println("ERR: more than two outputs from IntIfCmp!");
      }
    } else if(last instanceof LookupSwitch) {
      for(int i= 0; i < out.length; ++i)
        out[i] = curConds;
    } else {
    
//      assert out.length == 1: "expected only one exit from operator of type " + last.getClass().getCanonicalName();
      for(int i= 0; i < out.length; ++i)
        out[i] = curConds;
    }
    return out;
  }

}
