package petablox.analyses.inst;

import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import petablox.analyses.alloc.DomH;
import petablox.analyses.point.DomP;
import petablox.analyses.var.DomV;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,v,h) such that the statement
 * at program point p is an object allocation statement h which
 * assigns to local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "PobjValAsgnInst",
    sign = "P0,V0,H0:P0_V0_H0"
)
public class RelPobjValAsgnInst extends ProgramRel {
    public void fill() {
        DomP domP = (DomP) doms[0];
        DomV domV = (DomV) doms[1];
        DomH domH = (DomH) doms[2];
        int numH = domH.size();
        for (int hIdx = 1; hIdx < numH; hIdx++) {
            Unit h = (Unit) domH.get(hIdx);
            int pIdx = domP.indexOf(h);
            assert (pIdx >= 0);
            if(h instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)h;
            	if(SootUtilities.isNew(j) || SootUtilities.isNewArray(j)){
            		Local v = (Local)j.leftBox.getValue();
            		int vIdx = domV.indexOf(v);
                    assert (vIdx >= 0);
                    add(pIdx, vIdx, hIdx);
            	}
            }
        }
    }
}
