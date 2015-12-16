package chord.program.visitors;

import soot.Unit;

/**
 * Visitor over all phi statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IPhiInstVisitor extends IMethodVisitor {
    /**
     * Visits all phi statements in all methods in the program.
     * 
     * @param q A copy assignment statement.
     */
    public void visitPhiInst(Unit q);
}
