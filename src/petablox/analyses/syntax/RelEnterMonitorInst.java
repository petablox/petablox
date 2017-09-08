package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JEnterMonitorStmt;

import petablox.program.visitors.IEnterMonitorInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "EnterMonitorInst", sign = "P0,EXPR0:P0_EXPR0")
public class RelEnterMonitorInst extends ProgramRel implements IEnterMonitorInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JEnterMonitorStmt s) {
        add(s, s.getOp());
    }
}
