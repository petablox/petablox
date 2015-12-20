package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all class initializer methods, namely, methods
 * having signature <tt>&lt;clinit&gt;()</tt>.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "clinitM",
    sign = "M0"
)
public class RelClinitM extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.getName().contains("<clinit>"))
            add(m);
    }
}
