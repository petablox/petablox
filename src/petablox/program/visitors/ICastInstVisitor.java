package petablox.program.visitors;

import soot.Unit;

/**
 * Visitor over all cast assignment statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface ICastInstVisitor extends IMethodVisitor {
    /**
     * Visits all cast assignment statements in all methods in the program.
     * 
     * @param q A cast assignment statement.
     */
    public void visitCastInst(Unit q);
}
