package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all constructor methods, that is, methods
 * having name <tt>&lt;init&gt;</tt>.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "initM",
    sign = "M0"
)
public class RelInitM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.getName().contains("<init>"))
            add(m);
    }
}
