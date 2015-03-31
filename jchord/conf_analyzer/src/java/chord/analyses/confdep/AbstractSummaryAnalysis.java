package chord.analyses.confdep;

import chord.bddbddb.Rel.RelView;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.analyses.*;
import joeq.Class.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.*;
import joeq.Compiler.Quad.RegisterFactory.Register;
import java.util.*;

/**
 * Termination:
  Summaries are moving monotonically UP lattice.  Proof by induction on iterations?

  Base case: started at bottom
  Inductive step: each move only can move a method up the lattice.  Callers are monotonic in summaries for callees.
 *


 *  USE:
 *    if a method has been summarized, then propagate labels from arg to return val,
 *  instead of using normal VV relation.
 *  Reason:  if a method of interest is a label sink, then we'll still find callers precise.
 *  
 */

public abstract class AbstractSummaryAnalysis extends JavaAnalysis {
  
  public class Summary  {

    public BitSet argDeps;
    
    public Summary() {
      argDeps = new BitSet(16);
    }

    public Summary(BitSet a) {
      argDeps = a;
    }

    @Override
    public boolean equals(Object o) {
      if(o == null || ! (o instanceof Summary)) 
        return false;
      else {
        Summary s2 = (Summary) o;
        return argDeps.equals(s2.argDeps);
      }
    }
    
    //true if s2 is as least as high in lattice
    public boolean contains(Summary s2) {
      BitSet newBits = (BitSet) s2.argDeps.clone();
      newBits.andNot(argDeps); //so now just has new bits, not in old bits
      return newBits.isEmpty();
    }

    public void mergeIn(Summary newS) {
      if(newS != null)
        argDeps.or(newS.argDeps);
    }

    public void setArg(int argID) {
      argDeps.set(argID);
    }
    
    public String toString() {
      return argDeps.toString();
    }
  }
  Summary BOTTOM = new Summary();
  
  protected DomM domM;
  protected HashMap<jq_Method, Summary> summaries;
  
  @Override
  public void run() {
    ClassicProject project = ClassicProject.g();

    
    domM = (DomM) project.getTrgt("M");
    
    summaries = new LinkedHashMap<jq_Method, Summary>(domM.size());
    
    ProgramRel MM = 
        (ProgramRel) project.getTrgt("MMx");
    MM.load();
   
    fillInit(summaries);
    
    Queue<jq_Method> queue = new LinkedList<jq_Method>();
    BitSet queued = new BitSet(domM.size());
    
    for(jq_Method initial: summaries.keySet()) {
      inspectCallers(MM, queue, queued, initial);
    }
    
//    HashSet<jq_Method> queued = new HashSet<jq_Method>();
    while(!queue.isEmpty()) {
      jq_Method meth = queue.remove();
//      System.out.println("summarizing " + meth);
      queued.clear(domM.indexOf(meth));

      Summary oldSum = summaries.get(meth);
      Summary sum = analyze(meth);
      
      if(sum != null && !sum.equals(oldSum)) {
        summaries.put(meth, sum);
        inspectCallers(MM, queue, queued, meth);
      }
    }
    dumpOutput();
    
    MM.close();
  }

  private void inspectCallers(ProgramRel MM, Queue<jq_Method> q, BitSet queued,
      jq_Method meth) {
    
    RelView callers = MM.getView();
    callers.selectAndDelete(1, meth);
//    System.out.println("found " + callers.size() + " callers of " + meth);
    for(jq_Method caller: callers.<jq_Method>getAry1ValTuples()) {
      int cIdx = domM.indexOf(caller);
//      System.out.println("\t" + caller);
      if(!queued.get(cIdx)) {
        q.add(caller);
        queued.set(cIdx);
      }
    }
    callers.free();
  }
  
  protected abstract String outputName();
  
  protected abstract void fillInit(Map<jq_Method, Summary> summaries);

  protected Summary analyze(jq_Method meth) {
    
    HashMap<Register,Summary> regVals = new HashMap<Register,Summary>();
    Summary ret = null;
    ControlFlowGraph cfg = meth.getCFG();
    
    RegisterFactory rf = cfg.getRegisterFactory();
    int numArgs = meth.getParamTypes().length;
    for (int zIdx = 0; zIdx < numArgs; zIdx++) {
      Register v = rf.get(zIdx);
      Summary sum = new Summary();
      sum.setArg(zIdx);
      regVals.put(v, sum);
    }

    boolean changed = true;
    while (changed) {
      changed = false;
      for (BasicBlock bb: cfg.reversePostOrder()) {
        for (Iterator<Quad> it2 = bb.iterator(); it2.hasNext();) {
          Quad q = it2.next();
          Operator op = q.getOperator();
          if (op instanceof Move || op instanceof CheckCast) {
            Operand ro = Move.getSrc(q);
            if (ro instanceof RegisterOperand) {
              Register l = Move.getDest(q).getRegister();
              Register r = ((RegisterOperand) ro).getRegister();
              changed |= processCopy(regVals, l, r);
            }
          } else if (op instanceof Phi) {
            Register l = Phi.getDest(q).getRegister();
            ParamListOperand roList = Phi.getSrcs(q);
            int n = roList.length();
            for (int i = 0; i < n; i++) {
              RegisterOperand ro = roList.get(i);
              if (ro != null) {
                Register r = ro.getRegister();
                changed |= processCopy(regVals, l, r);
              }
            }
          } else if(op instanceof Return)  {
            Operand r_op = Return.getSrc(q);
            if(r_op instanceof RegisterOperand) {
              Register r = ((RegisterOperand) r_op).getRegister();
              if(r != null)
                ret = regVals.get(r);
            } 
          } else if (op instanceof Invoke) {
            jq_Method targM = Invoke.getMethod(q).getMethod();
            //FIXME: should lookup in IM here?
            changed |= processInvoke(regVals, q, targM);
          }
        }//end loop over insts in block
      } //end loop over blocks
    } //end fixpoint loop

    return ret;
  }

  private boolean processInvoke(HashMap<Register, Summary> regVals, Quad q, jq_Method targM) {
    
    RegisterOperand returnedOper = Invoke.getDest(q);
    if(returnedOper == null)
      return false;
    Register resReg = returnedOper.getRegister();
    Summary oldS = getOrElse(regVals, resReg);
    Summary callSummary = summaries.get(targM);
    if(callSummary == null)
      return false;
//    else
//      System.out.println("found summary for callee " + targM + " : " + callSummary);
    
    Summary retSum = new Summary();
    
    ParamListOperand l = Invoke.getParamList(q);
    int numArgs = l.length();
    for (int zIdx = 0; zIdx < numArgs; zIdx++) {
      if(callSummary.argDeps.get(zIdx)) {

        RegisterOperand vo = l.get(zIdx);
        Register arg = vo.getRegister();
        Summary argSummary = getOrElse(regVals, arg);
        retSum.mergeIn(argSummary);
      }
    }
    
    if(oldS.contains(retSum))
      return false;
    else {
      regVals.put(resReg, retSum);
      return true;
    }
  }

  private Summary getOrElse(HashMap<Register, Summary> regVals, Register arg) {
    Summary s = regVals.get(arg);
    if(s == null)
      return BOTTOM;
    else
      return s;
  }

  //process copy from r to l.
  //return true if this adds a potential value to l
  private boolean processCopy(HashMap<Register, Summary> regVals, Register l,
      Register r) {
    Summary oldS = regVals.get(l);
    Summary newS = regVals.get(r);
    if(newS ==null)//nothing to copy
      return false;
    if(oldS == null) {
      regVals.put(l, newS);
      return true;
    }
    
    if(!oldS.contains(newS)) {
      oldS.mergeIn(newS); //add in new bits
      return true;
    }
    return false;
  }
  
  protected void dumpOutput() {
    System.out.println(summaries.size() + " final summaries");
    ClassicProject project = ClassicProject.g();

    ProgramRel summaryRel =(ProgramRel) project.getTrgt(outputName());
    summaryRel.zero();
    
    for(Map.Entry<jq_Method,Summary> e: summaries.entrySet()) {
      jq_Method meth = e.getKey();
      Summary sum = e.getValue();
      BitSet argDeps = sum.argDeps;
      for (int i = argDeps.nextSetBit(0); i >= 0; i = argDeps.nextSetBit(i+1)) {
        int mIdx = domM.indexOf(meth);
        summaryRel.add(mIdx, i);
      }
      System.out.println(meth.getDeclaringClass() + " " + meth.getName() + " has summary " + sum);
    }
    summaryRel.save();
    
  }
  

}
