package petablox.program.visitors;

import soot.Unit;

/**
 * Visitor over all statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IInstVisitor extends IMethodVisitor {
    /**
     * Visits all statements in all methods in the program.
     * 
     * @param q A statement.
     */
    public void visit(Unit q);
}
