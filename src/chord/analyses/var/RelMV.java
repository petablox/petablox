package chord.analyses.var;

import soot.SootMethod;
import soot.Local;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.analyses.var.DomV;

/**
 * Relation containing each tuple (m,v) such that method m
 * declares local variable v, that is, v is either an
 * argument or temporary variable of m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MV",
    sign = "M0,V0:M0_V0"
)
public class RelMV extends ProgramRel {
    public void fill() {
        DomV domV = (DomV) doms[1];
        for (Local v : domV) {
            SootMethod m = domV.getMethod(v);
            add(m, v);
        }
    }
}
