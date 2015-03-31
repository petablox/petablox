package chord.analyses.confdep.rels;

import java.util.*;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.*;
import chord.analyses.invk.DomI;
import chord.analyses.primtrack.UniqueBQ;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Outputs relation exceptionControlDep
 * This relation contains tuples i0,i1 if reaching i1 control-depends on
 * an exception having been thrown at i0
 * @author asrabkin
 *
 */
@Chord(
    name = "exceptionControlDep",
    sign = "I0,UV0:I0_UV0"
  )
public class RelExceptionDeps extends ProgramRel implements IMethodVisitor {

  DomI domI;
  jq_Method method;
  public void init() {
    domI = (DomI) doms[0];
  }
  
  public void visit(jq_Class c) { }
  
  public void visit(jq_Method m) {
    if (m.isAbstract())
      return;
    
    ControlFlowGraph cfg = m.getCFG();
    
    //need to find set of blocks on the regular path
    Set<BasicBlock> nonExceptionPath = new HashSet<BasicBlock>();
		for (BasicBlock bb : cfg.reversePostOrder()) {
			nonExceptionPath.add(bb);
		}
    
    for(Object _eh: cfg.getExceptionHandlers()) {
    	ExceptionHandler eh = (ExceptionHandler) _eh;
  		Set<BasicBlock> exBlocks = getExPath(eh.getEntry(), nonExceptionPath);

    	for(Object _bb: eh.getHandledBasicBlocks()) {
    		BasicBlock bb = (BasicBlock) _bb;
    		for(BasicBlock exBB: exBlocks)
    			addAllPairs(bb, exBB);
    		
    	}
    }
/*		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
    	for(Iterator ehIt = cfg.getExceptionHandlersMatchingEntry(bb); ehIt.hasNext(); ) {
    		ExceptionHandler eh = (ExceptionHandler) ehIt.next();
    		eh.g
    		
    		
    	}
    }*/
  }

	private void addAllPairs(BasicBlock bb, BasicBlock exBB) {
		int maxI = bb.size();
		int maxJ = exBB.size();
		for(int i =0; i < maxI; ++i) {
			Quad iQ = bb.getQuad(i);
			for(int j = 0; j < maxJ; ++j) {
				Quad jQ = exBB.getQuad(j);
				super.add(iQ,jQ);
			}
		}
	}

	private Set<BasicBlock> getExPath(BasicBlock entry,
			Set<BasicBlock> nonExceptionPath) {
		HashSet<BasicBlock> exPath = new HashSet<BasicBlock>();
		
		UniqueBQ toScan = new UniqueBQ(); //hold
		toScan.add(entry);
		while(!toScan.isEmpty()) {
			BasicBlock b = toScan.remove();
			exPath.add(b);
			for(Object suc_: b.getSuccessors()) {
				BasicBlock suc = (BasicBlock) suc_;
				if(!nonExceptionPath.contains(b) && !exPath.contains(b))
					toScan.add(suc);
			}
		}
		return exPath;
	}

}
