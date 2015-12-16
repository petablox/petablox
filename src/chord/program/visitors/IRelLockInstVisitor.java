package chord.program.visitors;

import soot.Unit;

/**
 * Visitor over all monitorexit statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IRelLockInstVisitor extends IMethodVisitor {
    /**
     * Visits all monitorexit statements in all methods in the program.
     * 
     * @param q A monitorexit statement.
     */
    public void visitRelLockInst(Unit q);
}
