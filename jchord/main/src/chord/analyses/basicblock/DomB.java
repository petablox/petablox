package chord.analyses.basicblock;

import java.util.Map;
import java.util.HashMap;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of basic blocks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "B")
public class DomB extends ProgramDom<BasicBlock> implements IMethodVisitor {
    @Override
    public void visit(jq_Class c) { }

    @Override
    public void visit(jq_Method m) {
        if (m.isAbstract())
            return;
        ControlFlowGraph cfg = m.getCFG();
        for (BasicBlock b : cfg.reversePostOrder())
            getOrAdd(b);
    }

    @Override
    public String toUniqueString(BasicBlock b) {
        return b.getID() + "!" + b.getMethod();
    }
}
