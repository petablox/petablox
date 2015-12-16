package chord.analyses.heapacc;

import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.internal.JAssignStmt;

/**
 * Relation containing each tuple (e,v) such that quad e accesses
 * (reads or writes) an instance field or array element of an
 * object denoted by local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "EV",
    sign = "E0,V0:E0_V0"
)
public class RelEV extends ProgramRel {
    public void fill() {
        DomE domE = (DomE) doms[0];
        DomV domV = (DomV) doms[1];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Unit q = (Unit) domE.get(eIdx);
            //Operator op = q.getOperator();
            Local bo = null;
            if(q instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)q;
            	if (SootUtilities.isLoadInst(j)) {
            		ArrayRef ar = j.getArrayRef();
                    bo = (Local)ar.getBase();
                } else if (j.containsFieldRef()) {
                	Value v = null;
                	if(SootUtilities.isFieldLoad(j))
                		v = j.rightBox.getValue();
                	else
                		v = j.leftBox.getValue();
                	if(v instanceof InstanceFieldRef){
                		InstanceFieldRef ifr = (InstanceFieldRef)v;
                		bo = (Local)ifr.getBase();
                	}
                } else if (SootUtilities.isStoreInst(j)) {
                	ArrayRef ar = j.getArrayRef();
                	bo = (Local)ar.getBase();
                } else
                    bo = null;
                if (bo != null) {
                    //Register b = bo.getRegister();
                    int vIdx = domV.indexOf(bo);
                    assert (vIdx >= 0);
                    add(eIdx, vIdx);
                }
            }
            
        }
    }
}
