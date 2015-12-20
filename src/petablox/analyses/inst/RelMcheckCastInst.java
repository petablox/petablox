package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import petablox.analyses.method.DomM;
import petablox.analyses.type.DomT;
import petablox.analyses.var.DomV;
import petablox.program.visitors.ICastInstVisitor;
import petablox.program.visitors.IMoveInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (m,v1,t,v2) such that method m
 * contains a statement of the form <tt>v1 = (t) v2</tt>.
 *
 * @author Omer Tripp (omertrip@post.tau.ac.il)
 */
@Petablox(
    name = "McheckCastInst",
    sign = "M0,V0,T0,V1:M0_T0_V0xV1"
)
public class RelMcheckCastInst extends ProgramRel
        implements ICastInstVisitor {
    private SootMethod ctnrMethod;
    private DomM domM;
    private DomV domV;
    private DomT domT;
    public void init() {
        domM = (DomM) doms[0];
        domV = (DomV) doms[1];
        domT = (DomT) doms[2];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitCastInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		Value left = j.leftBox.getValue();
    		Value right = j.rightBox.getValue();
    		if(right instanceof JCastExpr){
    			JCastExpr jce = (JCastExpr)right;
    			Type t = jce.getCastType();
    			if(jce.getOp() instanceof Local && t instanceof RefLikeType){
	    			Local src = (Local)jce.getOp();
	    			Local dst = (Local)left;
	    			int mIdx = domM.indexOf(ctnrMethod);
	                assert (mIdx >= 0);
	                int lIdx = domV.indexOf(dst);
	                assert (lIdx >= 0);
	                int tIdx = domT.indexOf(t);
	                assert (tIdx >= 0);
	                int rIdx = domV.indexOf(src);
	                assert (rIdx >= 0);
	                add(mIdx, lIdx, tIdx, rIdx);
	    		}
    		}
    	}
    }
}
