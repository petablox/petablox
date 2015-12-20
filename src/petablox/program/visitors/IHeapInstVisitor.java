package petablox.program.visitors;

import soot.Unit;

/**
 * Visitor over all heap accessing statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IHeapInstVisitor extends IMethodVisitor {
    /**
     * Visits all heap accessing statements in all methods in the program.
     * 
     * @param q A heap accessing statement.
     */
    public void visitHeapInst(Unit q);
}
