package chord.analyses.type;

import soot.SootClass;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each interface type.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "interfaceT",
    sign = "T0"
)
public class RelInterfaceT extends ProgramRel
        implements IClassVisitor {
    public void visit(SootClass c) {
    	if (c.isInterface())
    		add(c.getType());
    }
}
