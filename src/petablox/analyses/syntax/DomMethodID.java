package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InvokeExpr;

import petablox.program.visitors.IInvokeExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;

@Petablox(name = "MethodID")
public class DomMethodID extends ProgramDom<String> implements IInvokeExprVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    @Override
    public void visit(Value e) { }

    public void visit(InvokeExpr e) {
        add(e.getMethodRef().getSignature());
        if (e instanceof DynamicInvokeExpr) {
            DynamicInvokeExpr ex = (DynamicInvokeExpr) e;
            add(ex.getBootstrapMethodRef().getSignature());
        }
    }
}
