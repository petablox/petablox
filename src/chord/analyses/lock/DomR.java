package chord.analyses.lock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;

import chord.program.visitors.IRelLockInstVisitor;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.ProgramDom;
import chord.analyses.method.DomM;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;

/**
 * Domain of all lock release points, including monitorexit quads and exit basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "R", consumes = { "M", "PM" })
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
        if (m.isAbstract())
            return;
        if (m.isSynchronized()) {
            CFG cfg = SootUtilities.getCFG(m);
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
