package petablox.program.visitors;

import soot.jimple.internal.JIfStmt;

/**
 * Visitor over all if statements in all methods in the program.
 */

public interface IIfInstVisitor extends IInstVisitor {
    public void visit(JIfStmt s);
}
