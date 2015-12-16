package chord.analyses.heapacc;

import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

/**
 * Relation containing all quads that write to an instance field,
 * a static field, or an array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "writeE",
    sign = "E0"
)
public class RelWriteE extends ProgramRel {
    public void fill() {
        DomE domE = (DomE) doms[0];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Unit e = (Unit) domE.get(eIdx);
            //Operator op = e.getOperator();
            if(e instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)e;
            	if (SootUtilities.isFieldStore(j) || SootUtilities.isStaticPut(j) || SootUtilities.isStoreInst(j))
                    add(eIdx);
            }
            
        }
    }
}
