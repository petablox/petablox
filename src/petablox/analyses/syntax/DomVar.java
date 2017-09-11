package petablox.analyses.syntax;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;

import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.SootUtilities;

/**
 * Domain of local vairables of all types.
 */
@Petablox(name = "Var")
public class DomVar extends ProgramDom<Local> implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        for (Local v : SootUtilities.getLocals(m))
            add(v);
    }
}
