package petablox.analyses.heapacc;

import petablox.analyses.heapacc.DomE;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.SootMethod;
import soot.Unit;

/**
 * Relation containing each tuple (m,e) such that method m contains quad e that accesses
 * (reads or writes) an instance field, a static field, or an array element.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "ME",
    sign = "M0,E0:E0_M0"
)
public class RelME extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomE domE = (DomE) doms[1];
        int numE = domE.size();
        for (int hIdx = 0; hIdx < numE; hIdx++) {
            Unit q = (Unit) domE.get(hIdx);
            SootMethod m = SootUtilities.getMethod(q);
            int mIdx = domM.indexOf(m);
            assert (mIdx >= 0);
            add(mIdx, hIdx);
        }
    }
}
