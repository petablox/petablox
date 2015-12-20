package petablox.analyses.method;


import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all private (as opposed to protected or
 * public) methods.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "privateM",
    sign = "M0"
)
public class RelPrivateM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isPrivate())
            add(m);
    }
}
