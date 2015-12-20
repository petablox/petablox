package petablox.analyses.reflect;

import soot.RefLikeType;
import petablox.analyses.alloc.DomH;
import petablox.analyses.type.DomT;
import petablox.program.PhantomClsVal;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,h) such that h is the hypothetical
 * site at which class t is reflectively created.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "clsTH",
    sign = "T0,H0:H0_T0"
)
public class RelClsTH extends ProgramRel {
    public void fill() {
        DomT domT = (DomT) doms[0];
        DomH domH = (DomH) doms[1];
        for (RefLikeType r : Program.g().getClasses()) {
            int tIdx = domT.indexOf(r);
            assert (tIdx >= 0);
            int hIdx = domH.indexOf(new PhantomClsVal(r));
            assert (hIdx >= 0);
            add(tIdx, hIdx);
        }
    }
}
