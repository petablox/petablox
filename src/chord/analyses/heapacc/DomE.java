package chord.analyses.heapacc;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.tagkit.SourceFileTag;
import soot.jimple.InstanceFieldRef;
import soot.jimple.internal.JAssignStmt;
import soot.tagkit.LineNumberTag;
import chord.program.visitors.IHeapInstVisitor;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.ProgramDom;
import chord.util.soot.SootUtilities;

/**
 * Domain of quads that access (read or write) an instance field, a static field, or an array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "E", consumes = { "M", "PM" })
public class DomE extends ProgramDom<Unit> implements IHeapInstVisitor {
    protected DomM domM;

    @Override
    public void init() {
        domM = (DomM) (Config.classic ?
            ClassicProject.g().getTrgt("M") : consumes[0]);
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visitHeapInst(Unit u) {
    	if (u instanceof JAssignStmt){
    		JAssignStmt as = (JAssignStmt) u;
    		
            if (SootUtilities.isFieldLoad(as)) {
                if (!(((InstanceFieldRef)as.rightBox.getValue()).getBase() instanceof Local))
                    return;
            }
            if (SootUtilities.isFieldStore(as)) {
            	if (!(((InstanceFieldRef)as.leftBox.getValue()).getBase() instanceof Local))
                    return;
            }
    	}
        
        add(u);
    }

    @Override
    public String toUniqueString(Unit u) {
        return SootUtilities.toByteLocStr(u);
    }

    @Override
    public String toXMLAttrsString(Unit u) {
        SootMethod m = SootUtilities.getMethod(u);
        JAssignStmt as = (JAssignStmt)u;
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
            "Mid=\"M" + mIdx + "\"" +
            " rdwr=\"" + ((SootUtilities.isFieldStore(as) || SootUtilities.isStaticPut(as) || SootUtilities.isStoreInst(as))? "Wr" : "Rd") + "\"";
    }
}
