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

@Petablox(name = "BootstrapMethodArg", sign = "Invoke0,Z0,EXPR0:Invoke0_Z0xEXPR0")
public class RelBootstrapMethodArg extends ProgramRel implements IInvokeExprVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    @Override
    public void visit(Value v) { }

    @Override
    public void visit(InvokeExpr e) {
        if(e instanceof JDynamicInvokeExpr) {
            JDynamicInvokeExpr ex = (JDynamicInvokeExpr) e;
            int pos = 0;
            for (Value arg : ex.getBootstrapArgs()) {
                add(e, new Integer(pos), arg);
                pos++;
            }
        }
    }
}
