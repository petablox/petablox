package petablox.program.visitors;

import soot.SootClass;

/**
 * Visitor over all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IClassVisitor {
    /**
     * Visits all classes in the program.
     *
     * @param c A class.
     */
    public void visit(SootClass c);
}
