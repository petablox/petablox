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
    name = "VarType",
    sign = "Var0,T0:Var0_T0"
)
public class RelVarType extends ProgramRel implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        for(Local l : SootUtilities.getLocals(m)) {
            add(l,l.getType());
        }
    }
}
