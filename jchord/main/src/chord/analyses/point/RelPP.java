package chord.analyses.point;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p1,p2) such that program point p2 is an immediate successor of program point p1.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "PP", sign = "P0,P1:P0xP1")
public class RelPP extends ProgramRel implements IMethodVisitor {
    private DomP domP;
	@Override
    public void init() {
        domP = (DomP) doms[0];
    }
	@Override
    public void visit(jq_Class c) { }
	@Override
    public void visit(jq_Method m) {
        if (m.isAbstract())
            return;
        ControlFlowGraph cfg = m.getCFG();
        for (BasicBlock bq : cfg.reversePostOrder()) {
            int n = bq.size();
            Inst y = (n == 0) ? bq : bq.getQuad(0);
            int yIdx = domP.indexOf(y);
            assert (yIdx >= 0);
            if (n != 0) {
                int pIdx = yIdx;
                for (int i = 1; i < n; i++) {
                    Quad q = bq.getQuad(i);
                    int qIdx = domP.indexOf(q);
                    assert (qIdx >= 0);
                    add(pIdx, qIdx);
                    pIdx = qIdx;
                }
            }
            for (BasicBlock bp : bq.getPredecessors()) {
                int l = bp.size();
                Inst x = (l == 0) ? bp : bp.getQuad(l - 1);
                int xIdx = domP.indexOf(x);
                assert (xIdx >= 0);
                add(xIdx, yIdx);
            }
        }
    }
}
