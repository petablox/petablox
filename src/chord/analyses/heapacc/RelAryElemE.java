package chord.analyses.heapacc;

import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

/**
 * Relation containing each quad that accesses (reads or writes) an array element
 * (as opposed to an instance or static field).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "aryElemE",
    sign = "E0"
)
public class RelAryElemE extends ProgramRel {
    public void fill() {
        DomE domE = (DomE) doms[0];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Unit e = (Unit) domE.get(eIdx);
            //Operator op = e.getOperator();
            if(e instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)e;
            	if(SootUtilities.isLoadInst(j) || SootUtilities.isStoreInst(j)){
            		add(eIdx);
            	}
            }
        }
    }
}
