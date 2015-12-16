package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInstanceFieldRef;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,b,f,v) such that method m
 * contains a quad of the form <tt>b.f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MputInstFldInst",
    sign = "M0,V0,F0,V1:F0_M0_V0xV1"
)
public class RelMputInstFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomM domM;
    private DomV domV;
    private DomF domF;
    private SootMethod ctnrMethod;
    public void init() {
        domM = (DomM) doms[0];
        domV = (DomV) doms[1];
        domF = (DomF) doms[2];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitHeapInst(Unit q) {
        if(q instanceof JAssignStmt){
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isStoreInst(j)){
        		Value left = j.leftBox.getValue();
        		Value right = j.rightBox.getValue();
        		if(right.getType() instanceof RefLikeType && right instanceof Local){
        			JArrayRef jar = (JArrayRef)left;
            		Local b = (Local)jar.getBase();
            		Local r = (Local)right;
            		int mIdx = domM.indexOf(ctnrMethod);
                    assert (mIdx >= 0);
                    int bIdx = domV.indexOf(b);
                    assert (bIdx >= 0);
                    int rIdx = domV.indexOf(r);
                    assert (rIdx >= 0);
                    int fIdx = 0;
                    add(mIdx, bIdx, fIdx, rIdx);
        		}
        		return;
        	} else if(SootUtilities.isFieldStore(j)){
        		SootField f = j.getFieldRef().getField();
        		Value right = j.rightBox.getValue();
        		if(j.leftBox.getValue().getType() instanceof RefLikeType && j.leftBox.getValue() instanceof InstanceFieldRef 
        				&& right instanceof Local){
        			JInstanceFieldRef jifr = (JInstanceFieldRef)j.leftBox.getValue();
        			Local b = (Local)jifr.getBase();
        			Local r = (Local)j.rightBox.getValue();
        			int mIdx = domM.indexOf(ctnrMethod);
                    assert (mIdx >= 0);
                    int bIdx = domV.indexOf(b);
                    assert (bIdx >= 0);
                    int rIdx = domV.indexOf(r);
                    assert (rIdx >= 0);
                    int fIdx = domF.indexOf(f);
                    assert (fIdx >= 0);
                    add(mIdx, bIdx, fIdx, rIdx);
        		}
        	}
        }
    }
}
