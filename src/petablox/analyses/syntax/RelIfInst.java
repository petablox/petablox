package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JIfStmt;

import petablox.program.visitors.IIfInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "IfInst", sign = "P0,EXPR0,P1:P0_EXPR0xP1")
public class RelIfInst extends ProgramRel implements IIfInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JIfStmt s) {
        add(s, s.getCondition(), s.getTarget());
    }
}
