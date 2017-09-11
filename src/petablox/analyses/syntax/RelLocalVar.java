package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Local;

import petablox.program.visitors.IMethodVisitor;
import petablox.analyses.syntax.DomVar;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,v) such that method m
 * declares local variable v, that is, v is either an
 * argument or temporary variable of m.
 */
@Petablox(
    name = "LocalVar",
    sign = "M0,Var0:M0_Var0"
)
public class RelLocalVar extends ProgramRel implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        for (Local v : SootUtilities.getLocals(m))
            add(m, v);
    }
}
