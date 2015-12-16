package chord.analyses.point;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,p) such that p is the unique exit basic block of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "MPtail", sign = "M0,P0:M0xP0")
public class RelMPtail extends ProgramRel implements IMethodVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        CFG cfg = SootUtilities.getCFG(m);
        Unit bx = cfg.getTails().get(0).getHead();
        add(m, bx);
    }
}
