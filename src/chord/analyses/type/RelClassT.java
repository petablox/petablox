package chord.analyses.type;

import soot.SootClass;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each (concrete or abstract) class type
 * (as opposed to interface types, primitive types, etc.).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "classT",
    sign = "T0"
)
public class RelClassT extends ProgramRel implements IClassVisitor {
    public void visit(SootClass c) {
    	if (!c.isInterface())
    		add(c.getType());
    }
}
