package chord.analyses.type;

import joeq.Class.jq_Class;
import chord.program.visitors.IClassVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * Relation containing each type t the prefix of whose name is contained in the value of system property chord.scope.exclude.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
        name = "scopeExcludedT",
        sign = "T0"
    )
public class RelScopeExcludedT extends ProgramRel implements IClassVisitor {
    public void visit(jq_Class c) {
        if (Utils.prefixMatch(c.getName(), Config.scopeExcludeAry))
            add(c);
    }
}
