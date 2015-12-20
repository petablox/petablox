package petablox.analyses.alloc;

import soot.SootMethod;
import soot.Unit;
import petablox.analyses.alloc.DomH;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,h) such that method m contains
 * object allocation unit h.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "MH",
    sign = "M0,H0:M0_H0"
)
public class RelMH extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomH domH = (DomH) doms[1];
        int numH = domH.getLastI() + 1;
        for (int hIdx = 1; hIdx < numH; hIdx++) {
            Unit u = (Unit) domH.get(hIdx);
            SootMethod m = SootUtilities.getMethod(u);
            int mIdx = domM.indexOf(m);
            assert (mIdx >= 0);
            add(mIdx, hIdx);
        }
    }
}
