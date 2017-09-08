package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JExitMonitorStmt;

import petablox.program.visitors.IExitMonitorInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "ExitMonitorInst", sign = "P0,EXPR0:P0_EXPR0")
public class RelExitMonitorInst extends ProgramRel implements IExitMonitorInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JExitMonitorStmt s) {
        add(s, s.getOp());
    }
}
