package petablox.analyses.heapacc;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.tagkit.SourceFileTag;
import soot.jimple.InstanceFieldRef;
import soot.jimple.internal.JAssignStmt;
import soot.tagkit.LineNumberTag;
import petablox.analyses.method.DomM;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.SootUtilities;
import petablox.util.Utils;

/**
 * Domain of quads that access (read or write) an instance field, a static field, or an array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "E", consumes = { "M" })
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
                if(as.rightBox.getValue() instanceof InstanceFieldRef)
                    if (!(((InstanceFieldRef)as.rightBox.getValue()).getBase() instanceof Local))
                        return;
            }
            if (SootUtilities.isFieldStore(as)) {
                if(as.rightBox.getValue() instanceof InstanceFieldRef)
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
    public String toFIString(Unit u) {		   
    	JAssignStmt as = (JAssignStmt)u;
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if (printId) sb.append("(" + indexOf(u) + ")");
    	String rdWr = (SootUtilities.isFieldStore(as) || SootUtilities.isStaticPut(as) || SootUtilities.isStoreInst(as))? "Wr" : "Rd";
    	sb.append(rdWr+": " + SootUtilities.getMethod(u).getName() + "@" + SootUtilities.getMethod(u).getDeclaringClass().getName());
    	return sb.toString();
    }

    @Override
    public String toXMLAttrsString(Unit u) {
        SootMethod m = SootUtilities.getMethod(u);
        JAssignStmt as = (JAssignStmt)u;
        String file = ((SourceFileTag)m.getDeclaringClass().getTag("SourceFileTag")).getSourceFile();
        System.out.println(m);
        System.out.println(m.getDeclaringClass());
        System.out.println(m.getDeclaringClass().getTag("SourceFileTag"));
        System.out.println(((SourceFileTag)m.getDeclaringClass().getTag("SourceFileTag")).getSourceFile());
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
            "Mid=\"M" + mIdx + "\"" +
            " rdwr=\"" + ((SootUtilities.isFieldStore(as) || SootUtilities.isStaticPut(as) || SootUtilities.isStoreInst(as))? "Wr" : "Rd") + "\"";
    }
}
