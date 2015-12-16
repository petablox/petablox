package chord.analyses.field;

import soot.SootClass;
import soot.SootField;
import soot.Type;
import chord.program.visitors.IFieldVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (f,t) such that field f has type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "FT",
    sign = "F0,T0:T0_F0"
)
public class RelFT extends ProgramRel implements IFieldVisitor {
    public void visit(SootClass c) { }
    public void visit(SootField f) {
        Type t = f.getType();
        add(f, t);
    }
}
