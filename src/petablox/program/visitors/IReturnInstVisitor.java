package petablox.program.visitors;

import soot.Unit;
import soot.jimple.internal.JReturnStmt;

/**
 * Visitor over all return statements in all methods in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IReturnInstVisitor extends IMethodVisitor {
    public void visit(JReturnStmt s);
}
