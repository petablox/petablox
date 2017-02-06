package petablox.analyses.basicblock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.util.Utils;
import petablox.util.soot.JEntryNopStmt;
import petablox.util.soot.JExitNopStmt;

/**
 * Domain of basic blocks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "B")
public class DomB extends ProgramDom<Block> implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        ICFG cfg = SootUtilities.getCFG(m);
        for (Block b : cfg.reversePostOrder())
            getOrAdd(b);
    }

    @Override
    public String toUniqueString(Block b) {
        return b.getIndexInMethod() + "!" + b.getBody().getMethod();
    }
    
    @Override
    public String toFIString(Block b) {		
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(b) +")");
    	if(b.getHead() instanceof JEntryNopStmt)
    		sb.append("ENTRY");
    	else if(b.getHead() instanceof JExitNopStmt)
    		sb.append("EXIT");
    	else
    		sb.append("BODY");
    	sb.append(": "+b.getBody().getMethod());
    	return sb.toString();
    }
}
