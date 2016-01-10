package petablox.analyses.type;

import soot.SootClass;
import petablox.program.visitors.IClassVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;
import petablox.util.Utils;


/**
 * Relation containing each type t the prefix of whose name is contained in the value of system property petablox.check.exclude.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "checkExcludedT",
    sign = "T0:T0"
)
public class RelCheckExcludedT extends ProgramRel implements IClassVisitor {
    public void visit(SootClass c) {
        if (Utils.prefixMatch(c.getName(), Config.checkExcludeAry))
			add(c.getType());
    }
}
