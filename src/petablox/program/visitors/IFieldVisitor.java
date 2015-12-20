package petablox.program.visitors;

import soot.SootField;

/**
 * Visitor over all fields of all classes in the program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IFieldVisitor extends IClassVisitor {
    /**
     * Visits all fields of all classes in the program.
     * 
     * @param f A field.
     */
    public void visit(SootField f);
}
