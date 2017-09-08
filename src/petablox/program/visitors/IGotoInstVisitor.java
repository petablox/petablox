package petablox.program.visitors;

import soot.jimple.internal.JGotoStmt;

/**
 * Visitor over all goto statements in all methods in the program.
 */

public interface IGotoInstVisitor extends IInstVisitor {
    public void visit(JGotoStmt s);
}
