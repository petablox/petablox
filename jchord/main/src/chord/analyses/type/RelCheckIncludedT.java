package chord.analyses.type;

import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;
import joeq.Class.jq_Class;

/**
 * Relation containing each type t the prefix of whose name is not contained in the value of system property chord.check.exclude.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "checkIncludedT",
    sign = "T0:T0"
)
public class RelCheckIncludedT extends ProgramRel implements IClassVisitor {
    public void visit(jq_Class c) {
        if (!Utils.prefixMatch(c.getName(), Config.checkExcludeAry))
			add(c);
    }
}
