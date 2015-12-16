package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JAssignStmt;
import chord.analyses.field.DomF;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,f,v) such that the quad
 * at program point p is of the form <tt>f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "PputStatFldInst",
    sign = "P0,F0,V0:F0_P0_V0"
)
public class RelPputStatFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomP domP;
    private DomF domF;
    private DomV domV;
    public void init() {
        domP = (DomP) doms[0];
        domF = (DomF) doms[1];
        domV = (DomV) doms[2];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) { }
    public void visitHeapInst(Unit q) {
    	if(q instanceof JAssignStmt){
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isStaticPut(j)){
        		SootField f = j.getFieldRef().getField();
        		if(j.leftBox.getValue().getType() instanceof RefLikeType &&
        				j.rightBox.getValue() instanceof Local){
        			Local r = (Local)j.rightBox.getValue();
        			int pIdx = domP.indexOf(q);
                    assert (pIdx >= 0);
                    int rIdx = domV.indexOf(r);
                    assert (rIdx >= 0);
                    int fIdx = domF.indexOf(f);
                    assert (fIdx >= 0);
                    add(pIdx, fIdx, rIdx);
        		}
        	}
        }
    }
}
