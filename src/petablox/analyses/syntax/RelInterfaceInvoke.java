package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;

import petablox.program.visitors.IInvokeExprVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (i,v,x) such that interface method
 * invocation i is of the form of v.x(...).
 */
@Petablox(name = "InterfaceInvoke", sign = "Invoke0,EXPR0,MethodID0:Invoke0_EXPR0xMethodID0")
public class RelInterfaceInvoke extends ProgramRel implements IInvokeExprVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    @Override
    public void visit(Value v) { }

    public void visit(InvokeExpr e) {
        if (e instanceof JInterfaceInvokeExpr) {
            JInterfaceInvokeExpr ex = (JInterfaceInvokeExpr) e;
            add(e, ex.getBase(), ex.getMethodRef().getSignature());
        }
    }
}
