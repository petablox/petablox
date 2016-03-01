package petablox.analyses.lock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;
import petablox.analyses.method.DomM;
import petablox.program.visitors.IRelLockInstVisitor;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;

/**
 * Domain of all lock release points, including monitorexit quads and exit basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "R", consumes = { "M" })
public class DomR extends ProgramDom<Unit> implements IRelLockInstVisitor {
    protected DomM domM;

    @Override
    public void init() {
        domM = (DomM) (Config.classic ?  ClassicProject.g().getTrgt("M") : consumes[0]);
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        if (!m.isConcrete())
            return;
        if (m.isSynchronized()) {
            ICFG cfg = SootUtilities.getCFG(m);
            Unit tail = cfg.getTails().get(0).getHead();
            add(tail);
        }
    }

    @Override
    public void visitRelLockInst(Unit u) {
        add(u);
    }

    @Override
    public String toUniqueString(Unit o) {
        return SootUtilities.toByteLocStr(o);
    }

    @Override
    public String toXMLAttrsString(Unit o) {
        SootMethod m = SootUtilities.getMethod(o);
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        int line = ((LineNumberTag)o.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + "Mid=\"M" + mIdx + "\"";
    }
}
