package petablox.analyses.field;

import soot.SootClass;
import soot.SootField;
import petablox.program.visitors.IFieldVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing all static (as opposed to instance) fields.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "statF",
    sign = "F0"
)
public class RelStatF extends ProgramRel implements IFieldVisitor {
    public void visit(SootClass c) { }
    public void visit(SootField f) {
        if (f.isStatic())
            add(f);
    }
}
