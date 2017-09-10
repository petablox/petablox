package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JReturnStmt;

import petablox.program.visitors.IReturnInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "ReturnInst", sign = "P0,EXPR0:P0_EXPR0")
public class RelReturnInst extends ProgramRel implements IReturnInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    public void visit(JReturnStmt s) {
        add(s, s.getOp());
    }
}
