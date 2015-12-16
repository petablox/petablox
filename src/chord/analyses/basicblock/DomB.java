package chord.analyses.basicblock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of basic blocks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "B")
public class DomB extends ProgramDom<Block> implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        CFG cfg = SootUtilities.getCFG(m);
        for (Block b : cfg.reversePostOrder())
            getOrAdd(b);
    }

    @Override
    public String toUniqueString(Block b) {
        return b.getIndexInMethod() + "!" + b.getBody().getMethod();
    }
}
