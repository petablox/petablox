package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import chord.program.visitors.IMoveInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "PobjVarMoveInst", sign = "P0,V0,V1:P0_V0xV1")
public class RelPobjVarMoveInst extends ProgramRel implements IMoveInstVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) { }
    public void visitMoveInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		if(SootUtilities.isMoveInst(j)){
    			Local l = (Local)j.leftBox.getValue();
    			Local r = (Local)j.rightBox.getValue();
    			if(r.getType() instanceof RefLikeType){
    				add(q, l, r);
    			}
    		}
    	}
    }
}

