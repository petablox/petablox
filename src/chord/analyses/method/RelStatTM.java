package chord.analyses.method;


import soot.SootClass;
import soot.SootMethod;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,m) such that m is a
 * static method defined in type t.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "staticTM",
    sign = "T0,M0:M0_T0"
)
public class RelStatTM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isStatic()) {
            SootClass t = m.getDeclaringClass();
            add(t.getType(), m);
        }
    }
}
