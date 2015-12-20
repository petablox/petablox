package petablox.analyses.heapacc;

import petablox.analyses.heapacc.DomE;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;

/**
 * Relation containing all quads that write to an instance field,
 * a static field, or an array element.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
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
