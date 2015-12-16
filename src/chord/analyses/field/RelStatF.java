package chord.analyses.field;

import soot.SootClass;
import soot.SootField;
import chord.program.visitors.IFieldVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing all static (as opposed to instance) fields.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
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
