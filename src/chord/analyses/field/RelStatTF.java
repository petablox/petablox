package chord.analyses.field;

import soot.SootClass;
import soot.SootField;
import chord.program.visitors.IFieldVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (t,f) such that f is a
 * static field defined in type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "staticTF",
    sign = "T0,F0:F0_T0"
)
public class RelStatTF extends ProgramRel implements IFieldVisitor {
    private SootClass ctnrClass;
    public void visit(SootClass c) {
        ctnrClass = c;
    }
    public void visit(SootField f) {
        if (f.isStatic()) {
            add(ctnrClass.getType(), f);
        }
    }
}
