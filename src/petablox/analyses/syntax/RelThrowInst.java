package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JThrowStmt;

import petablox.program.visitors.IThrowInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "ThrowInst", sign = "P0,EXPR0:P0_EXPR0")
public class RelThrowInst extends ProgramRel implements IThrowInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JThrowStmt s) {
        add(s, s.getOp());
    }
}
