package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DynamicInvokeExpr;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;

@Petablox(name = "EXPR")
public class DomExpr extends ProgramDom<Value> implements IExprVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(Value e) {
        add(e);
        if (e instanceof DynamicInvokeExpr) {
            DynamicInvokeExpr ex = (DynamicInvokeExpr) e;
            for (Value arg : ex.getBootstrapArgs())
                add(arg);
        }
    }
}
