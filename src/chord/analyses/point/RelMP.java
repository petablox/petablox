package chord.analyses.point;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import chord.program.visitors.IInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,p) such that method m contains program point p.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MP",
    sign = "M0,P0:M0xP0"
)
public class RelMP extends ProgramRel implements IInstVisitor {
    private SootMethod ctnrMethod;
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visit(Unit q) {
        add(ctnrMethod, q);
    }
}
