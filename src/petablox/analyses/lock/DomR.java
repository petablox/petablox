package petablox.analyses.lock;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import soot.tagkit.LineNumberTag;
import soot.jimple.internal.JExitMonitorStmt;
import petablox.analyses.method.DomM;
import petablox.program.visitors.IRelLockInstVisitor;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.util.Utils;
import petablox.util.soot.JEntryExitNopStmt;

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
        if (m.isAbstract())
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
    public String toFIString(Unit u) {		    
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if (printId) sb.append("(" + indexOf(u) + ")");
    	if(u instanceof JEntryExitNopStmt)
    		sb.append("SYNC METH");
    	else if(u instanceof JExitMonitorStmt)
    		sb.append("MONITOR EXIT");
    	sb.append(": "+SootUtilities.getMethod(u).getName() + "@" + SootUtilities.getMethod(u).getDeclaringClass().getName());
    	return sb.toString();
    }

    @Override
    public String toXMLAttrsString(Unit o) {
        SootMethod m = SootUtilities.getMethod(o);
        String file = SootUtilities.getSourceFile(m.getDeclaringClass());
        int line = SootUtilities.getLineNumber(o);
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " + "Mid=\"M" + mIdx + "\"";
    }
}
