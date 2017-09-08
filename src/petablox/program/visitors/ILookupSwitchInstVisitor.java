package petablox.program.visitors;

import soot.jimple.internal.JLookupSwitchStmt;

/**
 * Visitor over all lookup-switch statements in all methods in the program.
 */

public interface ILookupSwitchInstVisitor extends IInstVisitor {
    public void visit(JLookupSwitchStmt s);
}
