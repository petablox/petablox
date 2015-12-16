package chord.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,m) such that method m is defined in type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "TM",
    sign = "T0,M0:M0_T0"
)
public class RelTM extends ProgramRel implements IMethodVisitor {
    private SootClass ctnrClass;
    public void visit(SootClass c) {
        ctnrClass = c;
    }
    public void visit(SootMethod m) {
        add(ctnrClass.getType(), m);
    }
}
