package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JDynamicInvokeExpr;

import petablox.program.visitors.IInvokeExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "DynamicInvoke", sign = "Invoke0,MethodID0,MethodID1:Invoke0_MethodID0xMethodID1")
public class RelDynamicInvoke extends ProgramRel implements IInvokeExprVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    @Override
    public void visit(Value v) { }

    public void visit(InvokeExpr e) {
        if (e instanceof JDynamicInvokeExpr) {
            JDynamicInvokeExpr ex = (JDynamicInvokeExpr) e;
            add(e, ex.getBootstrapMethodRef(), ex.getMethodRef());
        }
    }
}
