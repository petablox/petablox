package petablox.util.soot;

/*****
 * FGStmtSwitch extends the AbstractStmtSwitch in a more fine-grained way.
 *****/

import soot.ArrayType;
import soot.Local;
import soot.RefType;
import soot.Value;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.Constant;
import soot.jimple.ThrowStmt;
import soot.shimple.PhiExpr;

public abstract class FGStmtSwitch extends AbstractStmtSwitch {
    protected Stmt statement;

    /** A statement of the form l = v; */
    protected void caseCopyStmt( Local dest, Value src ) {}

    /** A statement of the form l = (cl) v; */
    protected void caseCastStmt( Local dest, Value src, CastExpr c ) {
		// default is to just ignore the cast
		caseCopyStmt( dest, src );
    }
    
    /** An identity statement assigning a parameter to a local. */
    protected void caseIdentityStmt( Local dest, IdentityRef src ) {}

    /** A statement of the form l1 = l2.f; */
    protected void caseLoadStmt( Local dest, InstanceFieldRef src ) {}

    /** A statement of the form l1.f = l2; */
    protected void caseStoreStmt( InstanceFieldRef dest, Value src ) {}

    /** A statement of the form l1 = l2[i]; */
    protected void caseArrayLoadStmt( Local dest, ArrayRef src ) {}
    
    /** A statement of the form l1[i] = l2; */
    protected void caseArrayStoreStmt( ArrayRef dest, Value src ) {}
    
    /** A statement of the form l = cl.f; */
    protected void caseGlobalLoadStmt( Local dest, StaticFieldRef src ) {}
    
    /** A statement of the form cl.f = l; */
    protected void caseGlobalStoreStmt( StaticFieldRef dest, Value src ) {}
    
    /** A return statement. e is null if a non-reference type is returned. */
    protected void caseReturnStmt( Local val ) {}
    
    /** A statement of the form l = phi (...); */
    protected void casePhiStmt( Local dest, PhiExpr src ) {}
    
    /** A return statement returning a constant. */
    protected void caseReturnConstStmt( Constant val ) {
		// default is uninteresting
		caseUninterestingStmt( statement );
    }
    
    /** A new statement */
    protected void caseNewStmt( Local dest, NewExpr e ) {}
   
    /** A new array statement */
    protected void caseNewArrayStmt( Local dest, NewArrayExpr e ) {}
    
    /** A new multi array statement */
    protected void caseNewMultiArrayStmt( Local dest, NewMultiArrayExpr e ) {}
    
    /** A method invocation. dest is null if there is no reference type return value. */
    protected void caseInvokeStmt( Local dest, InvokeExpr e ) {}
    
    /** A throw statement */
    protected void caseThrowStmt( Local thrownException ) {}
    
    /** A catch statement */
    protected void caseCatchStmt( Local dest, CaughtExceptionRef cer ) {}
    
    /** Any other statement */
    protected void caseUninterestingStmt( Stmt s ) {};

    
    
    public final void caseAssignStmt( AssignStmt s ) {
		statement = s;
		Value lhs = s.getLeftOp();
		Value rhs = s.getRightOp();
		if( ! (lhs.getType() instanceof RefType)
			&&  ! (lhs.getType() instanceof ArrayType) ) {
		    if( rhs instanceof InvokeExpr ) {
		    	caseInvokeStmt( null, (InvokeExpr) rhs );
		    	return;
		    }
		    caseUninterestingStmt( s );
		    return;
		}
		if( rhs instanceof InvokeExpr ) {
		    caseInvokeStmt( (Local) lhs, (InvokeExpr) rhs );
		    return;
		}
		if( lhs instanceof Local ) {
		    if( rhs instanceof Local ) {
		    	caseCopyStmt( (Local) lhs, rhs );
		    } else if( rhs instanceof InstanceFieldRef ) {
		    	caseLoadStmt( (Local) lhs, (InstanceFieldRef) rhs );
		    } else if( rhs instanceof ArrayRef ) {
		    	caseArrayLoadStmt( (Local) lhs, (ArrayRef) rhs );
		    } else if( rhs instanceof StaticFieldRef ) {
		    	caseGlobalLoadStmt( (Local) lhs, (StaticFieldRef) rhs );
		    } else if( rhs instanceof NewExpr ) {
		    	caseNewStmt( (Local) lhs, (NewExpr) rhs );
		    } else if( rhs instanceof NewArrayExpr ) {
		    	caseNewArrayStmt( (Local) lhs, (NewArrayExpr) rhs );
		    } else if( rhs instanceof NewMultiArrayExpr ) {
		    	caseNewMultiArrayStmt( (Local) lhs, (NewMultiArrayExpr) rhs );
		    } else if( rhs instanceof CastExpr ) {
				CastExpr r = (CastExpr) rhs;
				Value rv = r.getOp();
			    caseCastStmt( (Local) lhs, rv, r );
		    } else if( rhs instanceof Constant ) {
		    	caseCopyStmt( (Local) lhs, rhs );
		    } else if( rhs instanceof PhiExpr ) {
		    	casePhiStmt( (Local) lhs, (PhiExpr) rhs );
		    } else throw new RuntimeException( "unhandled stmt "+s );
		} else if( lhs instanceof InstanceFieldRef ) {
		    if( rhs instanceof Local || rhs instanceof Constant) {
		    	caseStoreStmt( (InstanceFieldRef) lhs, rhs );
		    } else throw new RuntimeException( "unhandled stmt "+s );
		} else if( lhs instanceof ArrayRef ) {
		    if( rhs instanceof Local || rhs instanceof Constant ) {
		    	caseArrayStoreStmt( (ArrayRef) lhs, rhs );
		    } else throw new RuntimeException( "unhandled stmt "+s );
		} else if( lhs instanceof StaticFieldRef ) {
		    if( rhs instanceof Local || rhs instanceof Constant ) {
			caseGlobalStoreStmt( (StaticFieldRef) lhs, rhs );
		    } else throw new RuntimeException( "unhandled stmt "+s );
		} else throw new RuntimeException( "unhandled stmt "+s );
    }
    
    
    public final void caseReturnStmt(ReturnStmt s) {
		statement = s; 
		Value op = s.getOp();
		if( op.getType() instanceof RefType 
		|| op.getType() instanceof ArrayType ) { 
		    if( op instanceof Constant ) {
			caseReturnConstStmt( (Constant) op );
		    } else {
			caseReturnStmt( (Local) op );
		    }
		} else {
		    caseReturnStmt( (Local) null );
		}
    }
    
    
    public final void caseReturnVoidStmt(ReturnVoidStmt s) {
		statement = s;
		caseReturnStmt( (Local) null );
    }
    
    
    public final void caseInvokeStmt(InvokeStmt s) {
		statement = s;
		caseInvokeStmt( null, s.getInvokeExpr() );
    }
    
    
    public final void caseIdentityStmt(IdentityStmt s) {
		statement = s;
		Value lhs = s.getLeftOp();
		Value rhs = s.getRightOp();
		if( !( lhs.getType() instanceof RefType ) 
		&& !(lhs.getType() instanceof ArrayType ) ) {
		     caseUninterestingStmt( s );
		     return;
		}
		Local llhs = (Local) lhs;
		if( rhs instanceof CaughtExceptionRef ) {
		    caseCatchStmt( llhs, (CaughtExceptionRef) rhs );
		} else {
		    IdentityRef rrhs = (IdentityRef) rhs;
		    caseIdentityStmt( llhs, rrhs );
		}
    }
    
    
    public final void caseThrowStmt( ThrowStmt s) {
		statement = s;
		caseThrowStmt( (Local) s.getOp() );
    }
}

