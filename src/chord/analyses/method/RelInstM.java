package chord.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing all instance (as opposed to static) methods.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "instM",
    sign = "M0"
)
public class RelInstM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (!m.isStatic())
            add(m);
    }
}
