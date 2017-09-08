package petablox.program.visitors;

import soot.jimple.internal.JExitMonitorStmt;

/**
 * Visitor over all monitorexit statements in all methods in the program.
 */
public interface IExitMonitorInstVisitor extends IInstVisitor {
    public void visit(JExitMonitorStmt q);
}
