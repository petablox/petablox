package petablox.analyses.reflect;

import java.util.List;

import soot.RefLikeType;
import soot.Unit;
import petablox.analyses.invk.DomI;
import petablox.analyses.type.DomT;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;

/**
 * Relation containing each tuple (i,t) such that call site i
 * calling method "static Class forName(String className)" defined in
 * class "java.lang.Class" was determined by reflection analysis as
 * potentially loading class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "clsForNameIT",
    sign = "I0,T0:I0_T0"
)
public class RelClsForNameIT extends ProgramRel {
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomT domT = (DomT) doms[1];
        List<Pair<Unit, List<RefLikeType>>> l =
            Program.g().getReflect().getResolvedClsForNameSites();
        for (Pair<Unit, List<RefLikeType>> p : l) {
            Unit q = p.val0;
            int iIdx = domI.indexOf(q);
            assert (iIdx >= 0);
            for (RefLikeType t : p.val1) {
                int tIdx = domT.indexOf(t);
                assert (tIdx >= 0);
                add(iIdx, tIdx);
            }
        }
    }
}
