package chord.analyses.confdep.rels;

import java.util.*;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.BasicBlockVisitor;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import chord.analyses.invk.DomI;
import chord.analyses.point.DomP;
import chord.analyses.primtrack.UniqueBQ;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;

/**
 * Set of points in each failure-path method that happen before the 
 * failure.
 * @author asrabkin
 *
 */
@Chord(
    name = "BeforeFail",
    sign = "P0:P0",
    consumes={"FailurePath","I"}
  )
public class RelFailPBefore extends ProgramRel implements IMethodVisitor {
	private DomP domP;
	private DomI domI;
	private Map<jq_Method, Quad> failPathI;
// 	private Set<jq_Method> failPathM;

	public void init() {
		domP = (DomP) doms[0];
		ClassicProject project = ClassicProject.g();
		domI = (DomI) project.getTrgt("I");
		ProgramRel failPathIRel = (ProgramRel) project.getTrgt("FailurePath");
		failPathIRel.load();
		failPathI = new HashMap<jq_Method, Quad>();
		for(Quad q: failPathIRel.<Quad>getAry1ValTuples()) {
			failPathI.put(q.getMethod(), q);
		}
		failPathIRel.close();
	}
	public void visit(jq_Class c) { }
	
	static class FindContainingBB implements BasicBlockVisitor {
		public FindContainingBB(Quad failPt) {this.failPt = failPt;}

		Quad failPt;
		BasicBlock bbWithTarg;
		int failIdx = -1;
		@Override
		public void visitBasicBlock(BasicBlock bb) {
			int i = bb.getQuadIndex(failPt);
			if(i >= 0) {
				failIdx = i;
				bbWithTarg = bb;
			}
		}
	}
	
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		Quad failPt = failPathI.get(m);
		if(failPt == null)
			return;
		System.out.println("Failpath P-Before processing " + m);

		ControlFlowGraph cfg = m.getCFG();
		/* 
		 * Algorithm:
		 *   find BB with failPt, call it b
		 *   Add quads in b before failPt
		 *   Add preds of b to worklist
		 *   Foreach BB in worklist
		 *   	add quads in BB
		 *    add preds to worklist
		 */
		BasicBlock bbWithFail = null;
		
		FindContainingBB finder = new FindContainingBB(failPt);
		cfg.visitBasicBlocks( finder);
		bbWithFail = finder.bbWithTarg;
		for(int i=0; i< finder.failIdx; ++i) //used to determine which aliases took effect. 
			super.add(bbWithFail.getQuad(i)); //No need to include index of failing call
		
		Set<BasicBlock> marked = new HashSet<BasicBlock>();
		UniqueBQ worklist = new UniqueBQ();
		if(bbWithFail == null) {
			System.err.println("In FailPBefore, couldn't find a BB with target quad");
			return;
		} else {
			//start with not the failblock, but its predecessors.
			//this way, if failBlock is its own indirect predecessor it'll get re-analyzed
			for (Object bo : bbWithFail.getPredecessors()) {
				BasicBlock bp = (BasicBlock) bo;
				worklist.add(bp);
			}
			
			while(!worklist.isEmpty()) {
				BasicBlock b = worklist.remove();
				if(marked.contains(b))
					continue;
				else
					marked.add(b);
				
				int bSize = b.size();
				for(int i=0; i< bSize; ++i)
					super.add(b.getQuad(i));
				for (Object bo : b.getPredecessors()) {
					BasicBlock bp = (BasicBlock) bo;
					worklist.add(bp);
				}
			}
		}
	}//end method
	
}
