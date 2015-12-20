package petablox.analyses.invk;

import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.SootMethod;
import soot.Unit;

/**
 * Relation containing each tuple (m,i) such that method m contains
 * method invocation quad i.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "MI",
    sign = "M0,I0:I0xM0"
)
public class RelMI extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomI domI = (DomI) doms[1];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit q = (Unit) domI.get(iIdx);
            SootMethod m = SootUtilities.getMethod(q);
            int mIdx = domM.indexOf(m);
            assert (mIdx >= 0);
            add(mIdx, iIdx);
        }
    }
}
