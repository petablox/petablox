package chord.analyses.type;

import joeq.Class.jq_Class;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * Relation containing each type t the prefix of whose name is not contained in the value of system property chord.scope.exclude.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
        name = "scopeIncludedT",
        sign = "T0"
    )
public class RelScopeIncludedT extends ProgramRel implements IClassVisitor {
    public void visit(jq_Class c) {
        if (!Utils.prefixMatch(c.getName(), Config.scopeExcludeAry))
            add(c);
    }
}
