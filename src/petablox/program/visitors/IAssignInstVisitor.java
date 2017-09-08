package petablox.program.visitors;

import soot.jimple.internal.JAssignStmt;

/**
 * Visitor over all assignment statements in all methods in the program.
 */

public interface IAssignInstVisitor extends IInstVisitor {
    public void visit(JAssignStmt s);
}
