package petablox.analyses.type;

import soot.SootClass;
import petablox.program.visitors.IClassVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing each (concrete or abstract) class type
 * (as opposed to interface types, primitive types, etc.).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "classT",
    sign = "T0"
)
public class RelClassT extends ProgramRel implements IClassVisitor {
    public void visit(SootClass c) {
    	if (!c.isInterface())
    		add(c.getType());
    }
}
