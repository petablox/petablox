package chord.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.shimple.internal.SPhiExpr;
import soot.toolkits.scalar.ValueUnitPair;
import chord.program.visitors.IMoveInstVisitor;
import chord.program.visitors.IPhiInstVisitor;

import java.util.List;

import chord.program.visitors.ICastInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,v1,v2) such that method m contains a
 * statement of the form v1 = v2 where v1 and v2 are of reference type.
 * 
 * Includes three kinds of quads: MOVE, CHECKCAST, PHI.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "MobjVarAsgnInst", sign = "M0,V0,V1:M0_V0xV1")
public class RelMobjVarAsgnInst extends ProgramRel implements IMoveInstVisitor, IPhiInstVisitor, ICastInstVisitor {
    private SootMethod ctnrMethod;
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        ctnrMethod = m;
    }
    public void visitMoveInst(Unit q) {
        if(q instanceof JAssignStmt){
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isMoveInst(j)){
        		if(j.rightBox.getValue().getType() instanceof RefLikeType){
        			Local r = (Local)j.rightBox.getValue();
        			Local l = (Local)j.leftBox.getValue();
        			add(ctnrMethod, l, r);
        		}
        	}
        }
    }
    public void visitPhiInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		Value left = j.leftBox.getValue();
    		Value right = j.rightBox.getValue();
    		if(right instanceof SPhiExpr && left.getType() instanceof RefLikeType){
    			SPhiExpr phi = (SPhiExpr)right;
    			Local l =(Local)left;
    			List<ValueUnitPair> args = phi.getArgs();
    			for(ValueUnitPair vu : args){
    				Value v = vu.getValue();
    				if(v instanceof Local){
    					add(ctnrMethod, l, (Local)v);
    				}
    			}
    		}
    	}
    }
    public void visitCastInst(Unit q) {
    	if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		Value left = j.leftBox.getValue();
    		Value right = j.rightBox.getValue();
    		if(right instanceof JCastExpr){
    			Local l = (Local)left;
    			Value rv = ((JCastExpr)right).getOp();
    			if(rv.getType() instanceof RefLikeType && rv instanceof Local){
    				Local r = (Local)rv;
    				add(ctnrMethod, l, r);
    			}
    		}
    	}
    }
}
