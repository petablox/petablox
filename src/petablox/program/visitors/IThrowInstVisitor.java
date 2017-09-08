package petablox.program.visitors;

import soot.jimple.internal.JThrowStmt;

/**
 * Visitor over all throw statements in all methods in the program.
 */

public interface IThrowInstVisitor extends IInstVisitor {
    public void visit(JThrowStmt s);
}
