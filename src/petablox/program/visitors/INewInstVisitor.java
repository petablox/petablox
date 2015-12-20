package petablox.program.visitors;

import soot.Unit;

/**
 * Visitor over all new/newarray statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface INewInstVisitor extends IMethodVisitor {
    /**
     * Visits all new/newarray statements in all methods in the program.
     * 
     * @param q A new/newarray statement.
     */
    public void visitNewInst(Unit q);
}
