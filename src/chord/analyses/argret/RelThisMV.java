package chord.analyses.argret;

import soot.SootMethod;
import soot.Local;

import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v) such that local variable
 * v is the implicit this argument variable of instance method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "thisMV",
    sign = "M0,V0:M0_V0"
)
public class RelThisMV extends ProgramRel {
    @Override
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomV domV = (DomV) doms[1];
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            SootMethod m = domM.get(mIdx);
            if (m.isAbstract() || m.isStatic())
                continue;
            Local v = m.getActiveBody().getThisLocal();
            int vIdx = domV.indexOf(v);
            assert (vIdx >= 0);
            add(mIdx, vIdx);
        }
    }
}
