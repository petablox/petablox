package chord.analyses.inficfa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.ExceptionHandler;
import joeq.Compiler.Quad.Quad;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each method m such that method m doesn't
 * have a path from the entry block to the exit block
 */
@Chord(
    name = "NonTerminatingM",
    sign = "M0:M0"
)
public class RelNonTerminatingMethods extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            jq_Method m = domM.get(mIdx);
            if(m.isAbstract()) continue;
            if(isNonTerminatingM(m))
            	add(mIdx);
        }
    }
    
    private boolean isNonTerminatingM(jq_Method m){
    	ControlFlowGraph cfg = m.getCFG();
        boolean[] visited = new boolean[cfg.getNumberOfBasicBlocks()];
        return reversePostOrder_helper(cfg.entry(), visited);
    }
    
    private boolean reversePostOrder_helper(BasicBlock b, boolean[] visited) {
        if (visited[b.getID()]) return true;
        if(b.isExit()) return false;
        
        visited[b.getID()] = true;
        List<BasicBlock> bbs = b.getSuccessors();
        for (BasicBlock b2 : bbs){
            if(!reversePostOrder_helper(b2, visited))
            	return false;
        }
        
        return true;
    }
}
