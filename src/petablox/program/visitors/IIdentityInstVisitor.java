package petablox.program.visitors;

import soot.jimple.internal.JIdentityStmt;

/**
 * Visitor over all identity statements in all methods in the program.
 */
public interface IIdentityInstVisitor extends IInstVisitor {
    public void visit(JIdentityStmt s);
}
