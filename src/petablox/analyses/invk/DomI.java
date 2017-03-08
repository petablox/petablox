package petablox.analyses.invk;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.tagkit.SourceFileTag;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.tagkit.LineNumberTag;
import petablox.analyses.method.DomM;
import petablox.program.visitors.IInvokeInstVisitor;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;


/**
 * Domain of method invocation quads.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "I", consumes = { "M", "PM" })
public class DomI extends ProgramDom<Unit> implements IInvokeInstVisitor {
    protected DomM domM;

    @Override
    public void init() {
        domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visitInvokeInst(Unit u) {
        add(u);
    }

    @Override
    public String toUniqueString(Unit u) {
        return SootUtilities.toByteLocStr(u);                      
    }

    @Override
    public String toFIString(Unit u) {
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(u) + ")");
    	InvokeExpr ie = SootUtilities.getInvokeExpr(u);
    	if(ie instanceof DynamicInvokeExpr){
    		return "";
    	}
    	if (SootUtilities.isInterfaceInvoke(u))
    		sb.append("INTERFACEINVK:");
    	else if (SootUtilities.isVirtualInvoke(u))
    		sb.append("VIRTUALINVK:");
    	else if (SootUtilities.isStaticInvoke(u))
    		sb.append("STATICINVK:");
    	else if (SootUtilities.isInstanceInvoke(u))
    		sb.append("SPECINVK:");
    	SootMethod m = SootUtilities.getInvokeExpr(u).getMethod();
    	sb.append(m.getName() + "@" + m.getDeclaringClass().getName());
    	return sb.toString();
    }
    
    @Override
    public String toXMLAttrsString(Unit u) {
    	// this piece of code seems to be copied from DomE.java
    	// which does not make much sense here
        SootMethod m = SootUtilities.getMethod(u);
        //JAssignStmt as = (JAssignStmt)u;
        String file = ((SourceFileTag)m.getDeclaringClass().getTags().get(0)).getSourceFile();              
        int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
        int mIdx = domM.indexOf(m);
        return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
            "Mid=\"M" + mIdx + "\"" ;
            //" rdwr=\"" + ((SootUtilities.isFieldStore(as) || SootUtilities.isStaticPut(as) || SootUtilities.isStoreInst(as)) ? "Wr" : "Rd") + "\"";
    }
}
