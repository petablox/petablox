package chord.analyses.lock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;

import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;
import chord.program.visitors.IAcqLockInstVisitor;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.ProgramDom;

/**
 * Domain of all lock acquire points, including monitorenter quads and entry basic blocks of synchronized methods.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "L", consumes = { "M", "PM" })
public class DomL extends ProgramDom<Unit> implements IAcqLockInstVisitor {
    protected DomM domM;
    protected SootMethod ctnrMethod;

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
            Unit head = cfg.getHeads().get(0).getHead();
            add(head);
        }
    }

    @Override
    public void visitAcqLockInst(Unit u) {
        add(u);
    }

    @Override
    public String toUniqueString(Unit u) {
        return SootUtilities.toByteLocStr(u);                             
    }

    @Override
    public String toXMLAttrsString(Unit u) {
        SootMethod m = SootUtilities.getMethod(u);    
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + "Mid=\"M" + mIdx + "\"";
    }
}
