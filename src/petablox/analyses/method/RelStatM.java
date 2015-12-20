package petablox.analyses.method;


import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all static (as opposed to instance) methods.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "statM",
    sign = "M0"
)
public class RelStatM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isStatic())
            add(m);
    }
}
