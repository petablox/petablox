package petablox.program.visitors;

import soot.jimple.InvokeExpr;
/**
 * Visitor over invoke expressions of all methods in the program.
 */
public interface IInvokeExprVisitor extends IExprVisitor {
    public void visit(InvokeExpr e);
}
