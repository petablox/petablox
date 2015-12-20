package petablox.program.visitors;

import soot.Unit;

/**
 * Visitor over all monitorenter statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IAcqLockInstVisitor extends IMethodVisitor {
    /**
     * Visits all monitorenter statements in all methods in the program.
     * 
     * @param q A monitorenter statement.
     */
    public void visitAcqLockInst(Unit q);
}
