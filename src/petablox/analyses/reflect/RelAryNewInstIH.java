package petablox.analyses.reflect;

import java.util.List;

import soot.RefType;
import soot.Unit;
import petablox.analyses.alloc.DomH;
import petablox.analyses.invk.DomI;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;

/**
 * Relation containing each tuple (i,h) such that call site i
 * calling method "static Object newInstance(Class componentType, int length)"
 * defined in class "java.lang.reflect.Array" is treated as
 * object allocation site h.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "aryNewInstIH",
    sign = "I0,H0:I0_H0"
)
public class RelAryNewInstIH extends ProgramRel {
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomH domH = (DomH) doms[1];
        List<Pair<Unit, List<RefType>>> l =
            Program.g().getReflect().getResolvedAryNewInstSites();
        for (Pair<Unit, List<RefType>> p : l) {
            Unit q = p.val0;
            int iIdx = domI.indexOf(q);
            assert (iIdx >= 0);
            int hIdx = domH.indexOf(q);
            assert (hIdx >= 0);
            add(iIdx, hIdx);
        }
    }
}

