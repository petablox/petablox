package petablox.analyses.inst;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import petablox.analyses.alloc.DomH;
import petablox.analyses.point.DomP;
import petablox.analyses.var.DomV;
import petablox.program.visitors.ICastInstVisitor;
import petablox.program.visitors.IMoveInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NullConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;

/**
 * Relation containing each tuple (p,v) such that the statement at program
 * point p is an assignment statement where null is assigned to local
 *  reference variable v.
 * 
 * @author Xin Zhang
 */
@Petablox(name = "PobjNullAsgnInst", sign = "P0,V0:P0_V0")
public class RelPobjNullAsgnInst extends ProgramRel implements IMoveInstVisitor, ICastInstVisitor{
	public void visit(SootClass c) {
	}

	public void visit(SootMethod m) {
	}

	public void visitMoveInst(Unit q) {
		if(q instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)q;
    		if(SootUtilities.isMoveInst(j)){
    			Local l = (Local)j.leftBox.getValue();
    			Local r = (Local)j.rightBox.getValue();
    			if(r instanceof NullConstant && l.getType() instanceof RefLikeType){
    				add(q, l);
    			}
    		}
		}
	}
	
	public void visitCastInst(Unit q) {
		if(q instanceof JAssignStmt){
			JAssignStmt j = (JAssignStmt)q;
			if(j.rightBox.getValue() instanceof JCastExpr){
				JCastExpr jce = (JCastExpr)j.rightBox.getValue();
				Local l = (Local)j.leftBox.getValue();
				Value r = jce.getOp();
				if(r instanceof NullConstant && l.getType() instanceof RefLikeType){
					add(q, l);
				}
			}
		}
    }
}
