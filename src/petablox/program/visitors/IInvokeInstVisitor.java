package petablox.program.visitors;

import soot.Unit;
import soot.jimple.internal.JInvokeStmt;

/**
 * Visitor over all method invocation statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IInvokeInstVisitor extends IMethodVisitor {
    /**
     * Visits all method invocation statements in all methods in the program.
     * 
     * @param q A method call statement.
     */
    public void visitInvokeInst(Unit q);
    public void visit(JInvokeStmt q);
}
