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
 * Relation containing each tuple (m,z,v) such that
 * local variable v is the z-th formal parameter of
 * method m.
 */
@Petablox(
    name = "ParamVar",
    sign = "M0,Z0,Var0:M0_Z0xVar0"
)
public class RelParamVar extends ProgramRel implements IMethodVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (m.isConcrete()) {
            int k = 0;
            for(Local v : m.getActiveBody().getParameterLocals()) {
                add(m, new Integer(k), v);
                k++;
            }
        }
    }
}
