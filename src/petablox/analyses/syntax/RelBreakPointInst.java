package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JBreakpointStmt;

import petablox.program.visitors.IBreakPointInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "BreakInst", sign = "P0")
public class RelBreakPointInst extends ProgramRel implements IBreakPointInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JBreakpointStmt s) {
        add(s);
    }
}
