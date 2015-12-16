package chord.analyses.lock;

import soot.Unit;
import soot.Value;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JimpleLocal;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (l,v) such that monitorenter quad l
 * is synchronized on variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "syncLV",
    sign = "L0,V0:L0_V0"
)
public class RelSyncLV extends ProgramRel {
    public void fill() {
        DomL domL = (DomL) doms[0];
        int numL = domL.size();
        for (int lIdx = 0; lIdx < numL; lIdx++) {
            Unit i = domL.get(lIdx);
            if (i instanceof JEnterMonitorStmt) {
                JEnterMonitorStmt em = (JEnterMonitorStmt) i;
                Value op = em.getOp();
                if (op instanceof JimpleLocal) {
                    add(em, op);
                }
            }
        }
    }
}
