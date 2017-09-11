package petablox.program.visitors;

import soot.jimple.internal.JBreakpointStmt;

/**
 * Visitor over all break-point statements in all methods in the program.
 */
public interface IBreakInstVisitor extends IInstVisitor {
    public void visit(JBreakpointStmt s);
}
