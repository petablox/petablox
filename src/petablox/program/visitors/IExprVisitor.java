package petablox.program.visitors;

import soot.Value;

/**
 * Visitor over all expressions of all methods in the program.
 */
public interface IExprVisitor extends IInstVisitor {
    public void visit(Value e);
}
