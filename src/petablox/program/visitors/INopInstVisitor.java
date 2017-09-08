package petablox.program.visitors;

import soot.jimple.internal.JNopStmt;

/**
 * Visitor over all assignment statements in all methods in the program.
 */

public interface INopInstVisitor extends IInstVisitor {
    public void visit(JNopStmt s);
}
