package petablox.program.visitors;

import soot.jimple.internal.JTableSwitchStmt;

/**
 * Visitor over all table switch statements in all methods in the program.
 */

public interface ITableSwitchInstVisitor extends IInstVisitor {
    public void visit(JTableSwitchStmt s);
}
