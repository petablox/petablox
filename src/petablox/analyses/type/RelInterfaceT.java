package petablox.analyses.type;

import soot.SootClass;
import petablox.program.visitors.IClassVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing each interface type.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
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
