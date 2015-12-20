package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import petablox.program.visitors.IMoveInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "PobjVarMoveInst", sign = "P0,V0,V1:P0_V0xV1")
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

