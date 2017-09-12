package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;

import petablox.program.visitors.IInvokeExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;

@Petablox(name = "Invoke")
public class DomInvoke extends ProgramDom<InvokeExpr> implements IInvokeExprVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    @Override
    public void visit(Value e) { }

    public void visit(InvokeExpr e) {
        add(e);
    }
}
