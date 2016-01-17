package petablox.analyses.type;

import soot.SootClass;
import petablox.program.visitors.IClassVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;
import petablox.util.Utils;

/**
 * Relation containing each type t the prefix of whose name is not contained in the value of system property petablox.scope.exclude.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
        name = "scopeIncludedT",
        sign = "T0"
    )
public class RelScopeIncludedT extends ProgramRel implements IClassVisitor {
    public void visit(SootClass c) {
        if (!Utils.prefixMatch(c.getName(), Config.scopeExcludeAry))
            add(c.getType());
    }
}
