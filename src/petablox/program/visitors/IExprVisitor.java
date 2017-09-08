package petablox.program.visitors;

import soot.Value;

/**
 * Visitor over all expressions of all classes in the program.
 *
 * @author Kihong Heo (kheo@cis.upenn.edu)
 */

public interface IExprVisitor extends IInstVisitor {
    public void visit(Value e);
}
