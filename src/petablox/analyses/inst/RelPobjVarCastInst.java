package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import petablox.program.visitors.ICastInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "PobjVarCastInst", sign = "P0,V0,V1:P0_V0xV1")
public class RelPobjVarCastInst extends ProgramRel implements ICastInstVisitor {
    public void visit(SootClass c) { }
    public void visit(SootMethod m) { }
    public void visitCastInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		if(j.rightBox.getValue() instanceof JCastExpr){
    			JCastExpr jce = (JCastExpr)j.rightBox.getValue();
    			if(jce.getOp() instanceof Local){
	    			Local r = (Local)jce.getOp();
	    			if(r.getType() instanceof RefLikeType){
	    				Local l = (Local)j.leftBox.getValue();
	    				add(q, l, r);
	    			}
    			}
    		}
    	}
    }
}

