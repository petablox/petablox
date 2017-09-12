package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name="InvokeExpr", sign="EXPR0,Invoke0:EXPR0_Invoke0")
public class RelInvokeExpr extends ProgramRel implements IExprVisitor {
    @Override
    public void visit(SootClass c) {	}

    @Override
    public void visit(SootMethod m) {	}

    @Override
    public void visit(Unit q) {	}

    @Override
    public void visit(Value e) {
        if (e instanceof InvokeExpr) {
            add(e, (InvokeExpr) e);
        }
    }
}
