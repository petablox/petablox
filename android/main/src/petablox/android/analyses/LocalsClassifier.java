package petablox.android.analyses;

import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.Unit;
import soot.Body;
import soot.Type;
import soot.RefType;
import soot.ArrayType;
import soot.RefLikeType;
import soot.PrimType;
import soot.NullType;
import soot.jimple.Constant;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.MonitorStmt;
import soot.jimple.AnyNewExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.LengthExpr;
import soot.jimple.NegExpr;
import soot.jimple.EqExpr;
import soot.jimple.NeExpr;

import java.util.*;

public class LocalsClassifier
{
	private Set<Local> nonPrimLocals = new HashSet();
	private Set<Local> primLocals = new HashSet();
	private SootMethod method;

	private Map<Local,List<Local>> localToNeighbors = new HashMap();

	public LocalsClassifier(Body body)
	{
		method = body.getMethod();

		for(Local l : body.getLocals()){
			Type type = l.getType();
			if(type instanceof PrimType)
				addPrim(l);
			else if(type instanceof RefType || type instanceof ArrayType)
				addNonPrim(l);
			else if(type instanceof NullType)
				;
			else 
				System.out.println("untyped " + l + ": " + l.getType()+" in " +method);
		}

		for(Unit unit : body.getUnits()){
			Stmt s = (Stmt) unit;
			handleStmt(s);
		}
		fixpoint(nonPrimLocals);
		fixpoint(primLocals);
		localToNeighbors = null;
		

		int total, primCount, nonPrimCount, both, unclassified;
		total = primCount = nonPrimCount = both = unclassified = 0;
		for(Local l : body.getLocals()){
			total++;
			boolean prim = primLocals.contains(l);
			boolean nonPrim = nonPrimLocals.contains(l);
			if(prim || nonPrim){
				if(prim)
					primCount++;
				if(nonPrim)
					nonPrimCount++;
				if(prim && nonPrim)
					both++;
			} else
				unclassified++;
		}
		if(unclassified != 0){
			System.out.println("LC: "+method + " t = "+total+" p = "+primCount+" n = "+nonPrimCount+" b = "+both+" u = "+unclassified);
			System.out.println("Unclassified: ");
			for(Local l : body.getLocals()){
				boolean prim = primLocals.contains(l);
				boolean nonPrim = nonPrimLocals.contains(l);
				if(!prim && !nonPrim)
					System.out.print(l + " ");
			}
			System.out.println("");
			//System.out.println(body.toString());
			}
	}

	public Set<Local> primLocals()
	{
		return primLocals;
	}

	public Set<Local> nonPrimLocals()
	{
		return nonPrimLocals;
	}

	private void add(Value v, Type type)
	{
		if(!(v instanceof Local))
			return;
		if(type instanceof RefLikeType)
			nonPrimLocals.add((Local) v);
		else if(type instanceof PrimType)
			primLocals.add((Local) v);
	}

	private void addNonPrim(Value v)
	{
		if(!(v instanceof Local))
			return;
		nonPrimLocals.add((Local) v);
	}

	private void addPrim(Value v)
	{
		if(!(v instanceof Local))
			return;
		primLocals.add((Local) v);
	}	

	private void handleStmt(Stmt s)
	{
		if(s.containsInvokeExpr()){
			InvokeExpr ie = s.getInvokeExpr();
			SootMethod callee = ie.getMethod();
			
			//handle receiver
			if(ie instanceof InstanceInvokeExpr)
				addNonPrim(((InstanceInvokeExpr) ie).getBase());
				
			//handle args
			int numArgs = ie.getArgCount();
			for(int i = 0; i < numArgs; i++)
				add(ie.getArg(i), callee.getParameterType(i));
				
			//return value
			if(s instanceof AssignStmt)
				add(((AssignStmt) s).getLeftOp(), callee.getReturnType());
		} else if(s.containsFieldRef()){
			AssignStmt as = (AssignStmt) s;
			FieldRef fr = s.getFieldRef();
			SootField field = fr.getField();
			Type fieldType = field.getType();
			if(!field.isStatic())
				addNonPrim(((InstanceFieldRef) fr).getBase());
			add(as.getLeftOp(), fieldType);
			add(as.getRightOp(), fieldType);
		} else if(s.containsArrayRef()){
			ArrayRef ar = s.getArrayRef();
			addNonPrim(ar.getBase());
			addPrim(ar.getIndex());
		} else if(s instanceof AssignStmt){
			AssignStmt as = (AssignStmt) s;
			Value leftOp = as.getLeftOp();
			Value rightOp = as.getRightOp();
			if(rightOp instanceof AnyNewExpr){
				addNonPrim(leftOp);
			} else if(rightOp instanceof Constant){
				add(leftOp, rightOp.getType());
			} else if(rightOp instanceof CastExpr){
				Type type = ((CastExpr) rightOp).getCastType();
				add(leftOp, type);
				add(((CastExpr) rightOp).getOp(), type);
			} else if(leftOp instanceof Local && rightOp instanceof Local){
				addNeighbors((Local) leftOp, (Local) rightOp);
			} if(rightOp instanceof NegExpr){
				addPrim(leftOp);
				addPrim(rightOp);
			} if(rightOp instanceof LengthExpr){
				addNonPrim(((LengthExpr) rightOp).getOp());
				addPrim(leftOp);
			} else if(rightOp instanceof BinopExpr){
				BinopExpr binExpr = (BinopExpr) rightOp;
				Value op1 = binExpr.getOp1();
				Value op2 = binExpr.getOp2();
				if(!(rightOp instanceof EqExpr) && !(rightOp instanceof NeExpr)){
					addPrim(op1);
					addPrim(op2);
				} else {
					if(op1 instanceof Constant)
						add(op2, op1.getType());
					else if(op2 instanceof Constant)
						add(op1, op2.getType());
					else
						addNeighbors((Local) op1, (Local) op2);
				}
				addPrim(leftOp);
			}
		} else if(s instanceof ReturnStmt){
			Type retType = method.getReturnType();
			add(((ReturnStmt) s).getOp(), retType);
		} else if(s instanceof IdentityStmt){
			IdentityStmt is = (IdentityStmt) s;
			Local leftOp = (Local) is.getLeftOp();
			Value rightOp = is.getRightOp();
			if(rightOp instanceof ThisRef || rightOp instanceof CaughtExceptionRef)
				addNonPrim(leftOp);
			else {
				int index = ((ParameterRef) rightOp).getIndex();
				add(leftOp, method.getParameterType(index));
			}
		} else if(s instanceof ThrowStmt){
			addNonPrim(((ThrowStmt) s).getOp());
		} else if(s instanceof MonitorStmt){
			addNonPrim(((MonitorStmt) s).getOp());
		}
	}
	
	private void addNeighbors(Local l1, Local l2)
	{
		List<Local> l1Nbrs = localToNeighbors.get(l1);
		if(l1Nbrs == null)
			localToNeighbors.put(l1, (l1Nbrs = new LinkedList()));
		if(!l1Nbrs.contains(l2))
			l1Nbrs.add(l2);

		List<Local> l2Nbrs = localToNeighbors.get(l2);
		if(l2Nbrs == null)
			localToNeighbors.put(l2, (l2Nbrs = new LinkedList()));
		if(!l2Nbrs.contains(l1))
			l2Nbrs.add(l1);
	}
	
	private void fixpoint(Set<Local> vars)
	{
		List<Local> workList = new LinkedList();
		for(Local l : vars){
			workList.add(l);
		}
		while(!workList.isEmpty()){
			Local l = workList.remove(0);
			List<Local> nbrs = localToNeighbors.get(l);
			if(nbrs == null)
				continue;
			for(Local nbr : nbrs){
				if(vars.add(nbr))
					workList.add(nbr);
			}
		}
	}
}


