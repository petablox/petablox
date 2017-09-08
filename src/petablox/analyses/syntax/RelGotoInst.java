package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JGotoStmt;

import petablox.program.visitors.IGotoInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "GotoInst", sign = "P0,P1:P0_P1")
public class RelGotoInst extends ProgramRel implements IGotoInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JGotoStmt s) {
        add(s, s.getTarget());
    }
}
