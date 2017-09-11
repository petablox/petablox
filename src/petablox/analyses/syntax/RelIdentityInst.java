package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JIdentityStmt;

import petablox.program.visitors.IIdentityInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "IdentityInst", sign = "P0,EXPR0,EXPR1:P0_EXPR0xEXPR1")
public class RelIdentityInst extends ProgramRel implements IIdentityInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JIdentityStmt s) {
        add(s, s.getLeftOp(), s.getRightOp());
    }
}
