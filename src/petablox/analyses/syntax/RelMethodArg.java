package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JSpecialInvokeExpr;

import petablox.program.visitors.IInvokeExprVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

@Petablox(name = "MethodArg", sign = "Invoke0,Z0,EXPR0:Invoke0_Z0xEXPR0")
public class RelMethodArg extends ProgramRel implements IInvokeExprVisitor {
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
        int pos = 0;
        for (Value arg : e.getArgs()) {
            add(e, new Integer(pos), arg);
            pos++;
        }
    }
}
