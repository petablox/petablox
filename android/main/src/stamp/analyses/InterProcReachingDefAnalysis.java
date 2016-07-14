package stamp.analyses;

import soot.Scene;
import soot.SootMethod;
import soot.Local;
import soot.Immediate;
import soot.Value;
import soot.SootField;
import soot.Unit;
import soot.IntType;
import soot.RefType;
import soot.ArrayType;
import soot.Type;
import soot.PointsToSet;
import soot.PointsToAnalysis;
import soot.jimple.IntConstant;
import soot.jimple.Constant;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.ArrayRef;
import soot.jimple.ReturnStmt;
import soot.jimple.NewExpr;
import soot.jimple.CastExpr;
//import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.ArrayElement;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

import shord.program.Program;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Quad;

import java.util.*;

/*
 * @author Saswat Anand
 */
public class InterProcReachingDefAnalysis
{
	private List<Trio<Local,Stmt,SootMethod>> workList;
	private List<Pair<Local,Stmt>> mLocalList;
	private Set<Pair<Local,Stmt>> visited;
	private Set<Integer> reachingDefs;

	private SootMethod m;

	private static final Type intType = IntType.v();
	private static final Type integerType = RefType.v("java.lang.Integer");
	private static final Type objectType = RefType.v("java.lang.Object");

	private Map<SparkField,List<Quad<Local,Stmt,Immediate,SootMethod>>> fieldToInstanceStores = new HashMap();
	private Map<SootField,List<Trio<Stmt,Immediate,SootMethod>>> fieldToStaticStores = new HashMap();

	//private PAG pag;
	private PointsToAnalysis pta;
	private CallGraph callGraph;

	public InterProcReachingDefAnalysis()
	{
		init();
	}

	public Set<Integer> computeReachingDefsFor(Local l, Stmt s, SootMethod m)
	{
		this.workList = new LinkedList();
		this.visited = new HashSet();
		this.mLocalList = null;
		this.m = null;
		this.reachingDefs = new HashSet();

		workList.add(new Trio(l, s, m));

		while(!workList.isEmpty()){
			Trio<Local,Stmt,SootMethod> t = workList.remove(0);

			Local local = t.val0;
			Stmt stmt = t.val1;
			SootMethod method = t.val2;

			visit(method, stmt, local);			
		}				  
		System.out.println("RDQuery: "+l+" at "+s+" in "+m.getSignature());
		for(Integer i : reachingDefs)
			System.out.print(i+" ");
		System.out.println("");
		return reachingDefs;
	}

	private void visit(SootMethod method, Stmt s, Local l)
	{
		this.m = method;
		this.mLocalList = new LinkedList();
		mLocalList.add(new Pair(l,s));
		
		SimpleLocalDefs ld = new SimpleLocalDefs(new ExceptionalUnitGraph(method.retrieveActiveBody()));

		while(!mLocalList.isEmpty()){
			Pair<Local,Stmt> p = mLocalList.remove(0);
			if(visited.contains(p))
				continue;
			visited.add(p);

			Local local = p.val0;
			Stmt useStmt = p.val1;

			//System.out.println("Processing local:"+ local + " useStmt:"+useStmt);

			for(Unit stmt : ld.getDefsOfAt(local, useStmt)){
				if(stmt instanceof DefinitionStmt){
					DefinitionStmt ds = (DefinitionStmt) stmt;
					Value leftOp = ds.getLeftOp();
					Value rightOp = ds.getRightOp();
					
					assert local.equals(leftOp);
					handleDefinitionStmt(ds, (Local) leftOp, rightOp, useStmt);
				}				
			}
		}
	}

	private void handleDefinitionStmt(Stmt dfnStmt, Local local, Value rightOp, Stmt useStmt)
	{
		//System.out.println("handleDefinitionStmt: "+dfnStmt);
		if(rightOp instanceof ParameterRef){
			//find caller
			int index = ((ParameterRef) rightOp).getIndex();

			Iterator<Edge> edgeIt = callGraph.edgesInto(m);
			while(edgeIt.hasNext()){
				Edge edge = edgeIt.next();
				if(!edge.isExplicit() && !edge.isThreadRunCall())
					continue;
				Stmt callsite = edge.srcStmt();
				SootMethod containerMethod = (SootMethod) edge.src();
				InvokeExpr ie = callsite.getInvokeExpr();
				Immediate arg = (Immediate) ie.getArg(index);
				if(isInteresting(arg)){
					if(arg instanceof Local){
						Local loc = (Local) arg;
						workList.add(new Trio(loc, callsite, containerMethod));
					} else if(arg instanceof IntConstant)
						reachingDefs.add(((IntConstant) arg).value);
				}
			}
		} else if(rightOp instanceof InstanceFieldRef || rightOp instanceof ArrayRef){
			Local base; SparkField field;
			int index = -1;
			if(rightOp instanceof InstanceFieldRef){
				base = (Local) ((InstanceFieldRef) rightOp).getBase();
				field = ((InstanceFieldRef) rightOp).getField();
			} else {
				ArrayRef ar = (ArrayRef) rightOp;
				base = (Local) ar.getBase();
				field = ArrayElement.v();
				if(ar.getIndex() instanceof IntConstant)
					index = ((IntConstant) ar.getIndex()).value;
			}
			for(Trio<Stmt,Immediate,SootMethod> trio : findAlias(base, field)){
				Stmt stmt = trio.val0;
				Immediate alias = trio.val1;
				SootMethod containerMethod = trio.val2;
				
				if(index >= 0){
					ArrayRef ar = (ArrayRef) ((DefinitionStmt) stmt).getLeftOp();
					if(ar.getIndex() instanceof IntConstant)
						if(((IntConstant) ar.getIndex()).value != index)
							continue;
						else 
							System.out.println("array index matched");
				}
				//System.out.println("alias: "+stmt);
				if(isInteresting(alias)){
					if(alias instanceof Local){
						workList.add(new Trio((Local) alias, stmt, containerMethod));
					} else if(alias instanceof IntConstant)
						reachingDefs.add(((IntConstant) alias).value);
				}
			}
		} else if(rightOp instanceof StaticFieldRef){
			SootField field = ((StaticFieldRef) rightOp).getField();
			for(Trio<Stmt,Immediate,SootMethod> trio : findAlias(field)){
				Stmt stmt = trio.val0;
				Immediate alias = trio.val1;
				SootMethod containerMethod = trio.val2;
				if(isInteresting(alias)){
					if(alias instanceof Local){
						workList.add(new Trio((Local) alias, stmt, containerMethod));
					}
				}
			}
		} else if(rightOp instanceof InvokeExpr){
			Iterator<Edge> edgeIt = callGraph.edgesOutOf(dfnStmt);
			while(edgeIt.hasNext()){
				Edge edge = edgeIt.next();
				if(!edge.isExplicit() && !edge.isThreadRunCall())
					continue;
				SootMethod callee = edge.tgt();
				if(Program.g().isFrameworkClass(callee.getDeclaringClass())){
					//unsoundness
				} else {
					for(Pair<Stmt,Immediate> pair : retsFor(callee)){
						Stmt stmt = pair.val0;
						Immediate r = pair.val1;
						if(isInteresting(r)){
							if(r instanceof Local)
								workList.add(new Trio((Local) r, stmt, callee));
							else if(r instanceof IntConstant)
								reachingDefs.add(((IntConstant) r).value);
						}
					}
				}
			}
		} else if(rightOp instanceof Immediate){
			if(isInteresting((Immediate) rightOp)){
				if(rightOp instanceof Local)
					mLocalList.add(new Pair((Local) rightOp, dfnStmt));
				else if(rightOp instanceof IntConstant)
					reachingDefs.add(((IntConstant) rightOp).value);
			}
		} else if(rightOp instanceof CastExpr){
			CastExpr ce = (CastExpr) rightOp;
			Immediate castOp = (Immediate) ce.getOp();
			if(isInteresting(castOp) && isIntType(ce.getCastType())){
				if(castOp instanceof Local)
					mLocalList.add(new Pair((Local) castOp, dfnStmt));
				else if(castOp instanceof IntConstant)
					reachingDefs.add(((IntConstant) castOp).value);
			}
		} 
	}

	private boolean isIntType(Type type)
	{
		return type.equals(intType) || type.equals(integerType);
	}

	private boolean isInteresting(Type type)
	{
		return isIntType(type) || type.equals(objectType);
	}

	private boolean isInteresting(Immediate i)
	{
		if(i instanceof Constant)
			return i instanceof IntConstant;
		else
			return isInteresting(((Local) i).getType());
	}

	private void init()
	{
		//Program.g().runSpark();
		if(!Scene.v().hasCallGraph())
			Program.g().runCHA(); //because gui-fix change the program
		//this.pag = (PAG) Scene.v().getPointsToAnalysis();
		this.pta = Scene.v().getPointsToAnalysis();
		this.callGraph = Scene.v().getCallGraph();

		Iterator mIt = Program.g().scene().getReachableMethods().listener();
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
			if(!m.isConcrete())
				continue;
			for(Unit unit : m.retrieveActiveBody().getUnits()){
				Stmt stmt = (Stmt) unit;
				if(!stmt.containsFieldRef() && !stmt.containsArrayRef())
					continue;
				Value leftOp = ((DefinitionStmt) stmt).getLeftOp();
				Value rightOp = ((DefinitionStmt) stmt).getRightOp();
				if(leftOp instanceof InstanceFieldRef || leftOp instanceof ArrayRef){
					SparkField field; Local base;
					if(leftOp instanceof InstanceFieldRef){
						InstanceFieldRef ifr = (InstanceFieldRef) leftOp;
						base = (Local) ifr.getBase();
						field = ifr.getField();
						if(!isInteresting(field.getType()))
							continue;
					} else {
						ArrayRef ar = (ArrayRef) leftOp;
						base = (Local) ar.getBase();
						field = ArrayElement.v();
						if(!isInteresting(((ArrayType) base.getType()).getElementType()))
							continue;
					}
					List<Quad<Local,Stmt,Immediate,SootMethod>> quads = fieldToInstanceStores.get(field);
					if(quads == null){
						quads = new ArrayList();
						fieldToInstanceStores.put(field, quads);
					}
					quads.add(new Quad(base, stmt, (Immediate) rightOp, m));
				} else if(leftOp instanceof StaticFieldRef){
					StaticFieldRef sfr = (StaticFieldRef) leftOp;
					SootField field = sfr.getField();
					if(isInteresting(field.getType()))
						continue;
					List<Trio<Stmt,Immediate,SootMethod>> imms = fieldToStaticStores.get(field);
					if(imms == null){
						imms = new ArrayList();
						fieldToStaticStores.put(field, imms);
					}
					imms.add(new Trio(stmt, (Immediate) rightOp, m));
				}
			}
		}
	}


	private Iterable<Trio<Stmt,Immediate,SootMethod>> findAlias(Local local, SparkField f)
	{
		Iterable<Trio<Stmt,Immediate,SootMethod>> ret;	

		Iterable<Quad<Local,Stmt,Immediate,SootMethod>> it = fieldToInstanceStores.get(f);
		if(it == null){
			System.out.println("Warning: No stores found for field "+f);
			ret = Collections.emptyList();
		} else {
			PointsToSet localPt = pta.reachingObjects(local);
			List<Trio<Stmt,Immediate,SootMethod>> aliases = new ArrayList();
			ret = aliases;
			for(Quad<Local,Stmt,Immediate,SootMethod> quad : it){
				Local base = quad.val0;
				Stmt stmt = quad.val1;
				Immediate alias = quad.val2;
				SootMethod containerMethod = quad.val3;
			
				//check if base and local can point to a common object
				PointsToSet basePt = pta.reachingObjects(base);		
				if(localPt.hasNonEmptyIntersection(basePt)){
					aliases.add(new Trio(stmt, alias, containerMethod));
				}
			}
		}		
		return ret;
	}

	private Iterable<Trio<Stmt,Immediate,SootMethod>> findAlias(SootField f)
	{
		Iterable<Trio<Stmt,Immediate,SootMethod>> ret = fieldToStaticStores.get(f);
		if(ret == null)
			ret = Collections.emptySet();
		return ret;
	}

	private Iterable<Pair<Stmt,Immediate>> retsFor(SootMethod m)
	{
		if(!m.isConcrete())
			return Collections.EMPTY_LIST;
		List<Pair<Stmt,Immediate>> rets = new ArrayList();
		for(Unit unit : m.retrieveActiveBody().getUnits()){
			if(!(unit instanceof ReturnStmt))
				continue;
			Immediate retOp = (Immediate) ((ReturnStmt) unit).getOp();
			rets.add(new Pair((Stmt) unit, retOp));
		}
		return rets;
	}



}