package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

import petablox.program.visitors.IAssignInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "AssignInst", sign = "P0,EXPR0,EXPR1:P0_EXPR0xEXPR1")
public class RelAssignInst extends ProgramRel implements IAssignInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JAssignStmt s) {
        add(s, s.getLeftOp(), s.getRightOp());
    }
}
