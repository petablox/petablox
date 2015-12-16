package chord.analyses.inst;

import soot.Local;
import soot.PrimType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
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
 * Relation containing each tuple (m,v,b,f) such that method m
 * contains a quad of the form <tt>v = b.f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "MgetInstFldInst",
    sign = "M0,V0,V1,F0:F0_M0_V0xV1"
)
public class RelMgetInstFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomM domM;
    private DomV domV;
    private DomF domF;
    public void init() {
        domM = (DomM) doms[0];
        domV = (DomV) doms[1];
        domF = (DomF) doms[3];
    }
    private SootMethod ctnrMethod;
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitHeapInst(Unit q) {
        if (q instanceof JAssignStmt) {
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isLoadInst(j)){
        		Type right = j.rightBox.getValue().getType();
        		if(!(right instanceof PrimType)){
        			Local l = (Local)j.leftBox.getValue();
        			Local b = (Local)((JArrayRef)j.rightBox.getValue()).getBase();
        			int mIdx = domM.indexOf(ctnrMethod);
                    assert (mIdx >= 0);
                    int lIdx = domV.indexOf(l);
                    assert (lIdx >= 0);
                    int bIdx = domV.indexOf(b);
                    assert (bIdx >= 0);
                    int fIdx = 0;
                    add(mIdx, lIdx, bIdx, fIdx);
        		}
        	}else if(SootUtilities.isFieldLoad(j)){
        		Type right = j.rightBox.getValue().getType();
        		if(!(right instanceof PrimType)&&(j.rightBox.getValue() instanceof JInstanceFieldRef)){
        			JInstanceFieldRef jifr = (JInstanceFieldRef)j.rightBox.getValue();
        			Local l = (Local)j.leftBox.getValue();
        			Local b = (Local)jifr.getBase();
        			SootField f = j.getFieldRef().getField();
        			int mIdx = domM.indexOf(ctnrMethod);
                    assert (mIdx >= 0);
                    int bIdx = domV.indexOf(b);
                    assert (bIdx >= 0);
                    int lIdx = domV.indexOf(l);
                    assert (lIdx >= 0);
                    int fIdx = domF.indexOf(f);
                    assert (fIdx >= 0);
                    add(mIdx, lIdx, bIdx, fIdx);
        		}
        	}
        }
    }
}
