package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import petablox.analyses.field.DomF;
import petablox.analyses.point.DomP;
import petablox.analyses.var.DomV;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,u,b,f) such that the quad
 * at program point p is of the form <tt>u = b.f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "PgetInstFldInst",
    sign = "P0,V0,V1,F0:F0_P0_V0xV1"
)
public class RelPgetInstFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomP domP;
    private DomV domV;
    private DomF domF;
    public void init() {
        domP = (DomP) doms[0];
        domV = (DomV) doms[1];
        domF = (DomF) doms[3];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) { }
    public void visitHeapInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		if(SootUtilities.isLoadInst(j) && 
    				j.rightBox.getValue().getType() instanceof RefLikeType){
    			Local l = (Local)j.leftBox.getValue();
    			Local b = (Local)((JArrayRef)j.leftBox.getValue()).getBase();
    			int pIdx = domP.indexOf(q);
                assert (pIdx >= 0);
                int lIdx = domV.indexOf(l);
                assert (lIdx >= 0);
                int bIdx = domV.indexOf(b);
                assert (bIdx >= 0);
                int fIdx = 0;
                add(pIdx, lIdx, bIdx, fIdx);
                return;
    		}else if(SootUtilities.isFieldLoad(j)){
    			SootField f = j.getFieldRef().getField();
    			if(j.rightBox.getValue().getType() instanceof RefLikeType &&
    					(j.rightBox.getValue() instanceof JInstanceFieldRef)){
    				Local l = (Local)j.leftBox.getValue();
    				JInstanceFieldRef jifr = (JInstanceFieldRef)j.rightBox.getValue();
    				Local b = (Local)jifr.getBase();
    				int pIdx = domP.indexOf(q);
                    assert (pIdx >= 0);
                    int bIdx = domV.indexOf(b);
                    assert (bIdx >= 0);
                    int lIdx = domV.indexOf(l);
                    assert (lIdx >= 0);
                    int fIdx = domF.indexOf(f);
                    assert (fIdx >= 0);
                    add(pIdx, lIdx, bIdx, fIdx);
    			}
    		}	
    	}
    }
}
