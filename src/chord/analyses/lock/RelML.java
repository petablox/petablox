package chord.analyses.lock;

import soot.SootMethod;
import soot.Unit;
import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,l) such that method m contains
 * lock acquisition point l.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "ML",
    sign = "M0,L0:M0_L0"
)
public class RelML extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomL domL = (DomL) doms[1];
        int numL = domL.size();
        for (int lIdx = 0; lIdx < numL; lIdx++) {
            Unit u = domL.get(lIdx);
            SootMethod m = SootUtilities.getMethod(u);
            int mIdx = domM.indexOf(m);
            assert (mIdx >= 0);
            add(mIdx, lIdx);
        }
    }
}
