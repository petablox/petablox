package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JReturnVoidStmt;

import petablox.program.visitors.IReturnVoidInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "ReturnVoidInst", sign = "P0")
public class RelReturnVoidInst extends ProgramRel implements IReturnVoidInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    public void visit(JReturnVoidStmt s) {
        add(s);
    }
}
