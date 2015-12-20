package petablox.analyses.method;

import soot.SootClass;
import soot.SootMethod;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,m) such that method m is
 * the class initializer method of class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "clinitTM",
    sign = "T0,M0:M0_T0"
)
public class RelClinitTM extends ProgramRel implements IMethodVisitor {
    private SootClass ctnrClass;
    public void visit(SootClass c) {
        ctnrClass = c;
    }
    public void visit(SootMethod m) {
        if (m.getName().contains("<clinit>")) {
            add(ctnrClass.getType(), m);
        }
    }
}
