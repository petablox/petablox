package chord.analyses.heapacc;

import chord.analyses.field.DomF;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import soot.SootField;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

/**
 * Relation containing each tuple (e,f) such that quad e accesses
 * (reads or writes) instance field, static field, or array element f.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "EF",
    sign = "E0,F0:F0_E0"
)
public class RelEF extends ProgramRel {
    public void fill() {
        DomE domE = (DomE) doms[0];
        DomF domF = (DomF) doms[1];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Unit e = (Unit)domE.get(eIdx);
            //jq_Field f = e.getField();
            if(e instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)e;
            	if(j.containsFieldRef()){
            		SootField f = j.getFieldRef().getField();
                	int fIdx = domF.indexOf(f);
                    assert (fIdx >= 0);
                    add(eIdx, fIdx);
            	}
            }
        }
    }
}
