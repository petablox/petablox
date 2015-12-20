package petablox.analyses.point;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,p) such that p is the unique entry basic block of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "MPhead", sign = "M0,P0:M0xP0")
public class RelMPhead extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        CFG cfg = SootUtilities.getCFG(m);
        Unit be = cfg.getHeads().get(0).getHead();
        add(m, be);
    }
}
