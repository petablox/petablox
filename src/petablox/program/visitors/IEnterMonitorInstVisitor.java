package petablox.program.visitors;

import soot.jimple.internal.JEnterMonitorStmt;

/**
 * Visitor over all monitorenter statements in all methods in the program.
 */
public interface IEnterMonitorInstVisitor extends IInstVisitor {
    public void visit(JEnterMonitorStmt q);
}
