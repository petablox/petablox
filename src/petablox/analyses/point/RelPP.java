package petablox.analyses.point;

import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.analyses.point.DomP;

/**
 * Relation containing each tuple (p1,p2) such that program point p2 is an immediate successor of program point p1.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "PP", sign = "P0,P1:P0xP1")
public class RelPP extends ProgramRel implements IMethodVisitor {
    private DomP domP;
	@Override
    public void init() {
        domP = (DomP) doms[0];
    }
	@Override
    public void visit(SootClass c) { }
	@Override
    public void visit(SootMethod m) {
        if (!m.isConcrete())
            return;
        ICFG cfg = SootUtilities.getCFG(m);
        for (Block bq : cfg.reversePostOrder()) {
            Unit y = bq.getHead();
            int yIdx = domP.indexOf(y);
            assert (yIdx >= 0);
            if (true) {
                int pIdx = yIdx;
                Iterator<Unit> uit = bq.iterator();
                while (uit.hasNext()) {
                    Unit q = uit.next();
                    int qIdx = domP.indexOf(q);
                    assert (qIdx >= 0);
                    if (pIdx != qIdx) {
                    	add(pIdx, qIdx);
                    	pIdx = qIdx;
                    }
                }
            }
            for (Block bp : bq.getPreds()) {
                Unit x = bp.getTail();
                int xIdx = domP.indexOf(x);
                assert (xIdx >= 0);
                add(xIdx, yIdx);
            }
        }
    }
}
