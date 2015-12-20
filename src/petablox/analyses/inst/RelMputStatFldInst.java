package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JAssignStmt;
import petablox.analyses.field.DomF;
import petablox.analyses.method.DomM;
import petablox.analyses.var.DomV;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,f,v) such that method m contains
 * a quad of the form <tt>f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "MputStatFldInst",
    sign = "M0,F0,V0:F0_M0_V0"
)
public class RelMputStatFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomM domM;
    private DomF domF;
    private DomV domV;
    private SootMethod ctnrMethod;
    public void init() {
        domM = (DomM) doms[0];
        domF = (DomF) doms[1];
        domV = (DomV) doms[2];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitHeapInst(Unit q) {
        if(q instanceof JAssignStmt){
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isStaticPut(j)){
        		SootField f = j.getFieldRef().getField();
        		if(j.leftBox.getValue().getType() instanceof RefLikeType){
        			if (j.rightBox.getValue() instanceof Local) {
	        			Local r = (Local)j.rightBox.getValue();
	        			int mIdx = domM.indexOf(ctnrMethod);
	                    assert (mIdx >= 0);
	                    int rIdx = domV.indexOf(r);
	                    assert (rIdx >= 0);
	                    int fIdx = domF.indexOf(f);
	                    assert (fIdx >= 0);
	                    add(mIdx, fIdx, rIdx);
        			}
        		}
        	}
        }
    }
}
