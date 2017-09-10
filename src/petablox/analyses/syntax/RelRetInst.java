package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JRetStmt;

import petablox.program.visitors.IRetInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "RetInst", sign = "P0,EXPR0:P0_EXPR0")
public class RelRetInst extends ProgramRel implements IRetInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    public void visit(JRetStmt s) {
        add(s, s.getStmtAddress());
    }
}
