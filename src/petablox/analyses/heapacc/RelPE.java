package petablox.analyses.heapacc;

import petablox.analyses.heapacc.DomE;
import petablox.analyses.point.DomP;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.Unit;

/**
 * Relation containing each tuple (p,e) such that the quad at program point p is
 * a heap-accessing quad e that accesses (reads or writes) an instance field, a
 * static field, or an array element.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "PE",
    sign = "P0,E0:E0_P0"
)
public class RelPE extends ProgramRel {
    public void fill() {
        DomP domP = (DomP) doms[0];
        DomE domE = (DomE) doms[1];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Unit e = (Unit) domE.get(eIdx);
            int pIdx = domP.indexOf(e);
            assert (pIdx >= 0);
            add(pIdx, eIdx);
        }
    }
}
