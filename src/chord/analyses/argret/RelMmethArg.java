package chord.analyses.argret;

import soot.Local;
import soot.RefLikeType;
import soot.SootMethod;
import soot.util.Chain;

import java.util.Iterator;

import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,z,v) such that local variable
 * v is the zth argument variable of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MmethArg",
    sign = "M0,Z0,V0:M0_V0_Z0"
)
public class RelMmethArg extends ProgramRel {
    @Override
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomV domV = (DomV) doms[2];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m.isAbstract())
                continue;
            Local[] rf = SootUtilities.getMethArgLocals(m);
            int numArgs = rf.length;
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Local v=rf[zIdx];
                if (v.getType() instanceof RefLikeType) {
                    int vIdx = domV.indexOf(v);
                    assert (vIdx >= 0);
                    add(mIdx, zIdx, vIdx);
                }
            }
        }
    }
}
