package stamp.analyses.ondemand;

import soot.Scene;
import soot.SootMethod;
import soot.MethodOrMethodContext;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.RefLikeType;
import soot.PointsToSet;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.ondemand.genericutil.ImmutableStack;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.DemandCSPointsTo.VarAndContext;

import shord.project.analyses.JavaAnalysis;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
public abstract class MethodReachabilityAnalysis extends JavaAnalysis
{
	protected OnDemandPTA dpta;
	protected Map<SootMethod,List<Edge>> callEdges = new HashMap();
	protected Set<SootMethod> targetMethods = new HashSet();

	protected void setup(OnDemandPTA dpta)
	{
		this.dpta = dpta;
	}

	public void run()
	{
		List<SootMethod> workList = new ArrayList();
		try{
			String stampOutDir = System.getProperty("stamp.out.dir");
			PrintWriter reachableMethodsWriter = new PrintWriter(new BufferedWriter(
											 new FileWriter(new File(stampOutDir, "reachablemethods.txt"))));
			for(Iterator<MethodOrMethodContext> it = Scene.v().getReachableMethods().listener(); it.hasNext();){
				SootMethod m = (SootMethod) it.next();
				reachableMethodsWriter.println(m);
				if(targetMethods.contains(m)){
					System.out.println("target method: "+m);			
					workList.add(m);
				}
			}
			reachableMethodsWriter.close();
		}catch(IOException e){
			throw new Error(e);
		}
		
		CallGraph cg = Scene.v().getCallGraph();
		Set<SootMethod> roots = new HashSet();
		Set<SootMethod> visited = new HashSet();
		while(!workList.isEmpty()){
			SootMethod tgt = workList.remove(0);
			if(visited.contains(tgt))
				continue;
			visited.add(tgt);
			Iterator<Edge> edgeIt = cg.edgesInto(tgt);
			boolean root = true;
			while(edgeIt.hasNext()){
				Edge edge = edgeIt.next();
				if(!edge.isExplicit())
					continue;
				root = false;
				Stmt stmt = edge.srcStmt();
				SootMethod src = (SootMethod) edge.src();
				workList.add(src);
				System.out.println("adding to worklist. target: "+tgt+" callstmt: "+stmt+"@"+src);
				List<Edge> outgoingEdges = callEdges.get(src);
				if(outgoingEdges == null){
					outgoingEdges = new ArrayList();
					callEdges.put(src, outgoingEdges);
				}
				outgoingEdges.add(edge);
				//System.out.println("success: "+src+" "+stmt+" "+tgt);
				//System.out.println("XY: "+src+" "+tgt);
			}
			if(root){
				//ignore methods that override Thread.run methods
				//that have only thread_call type incoming calledges in spark's callgraph
				String subsig = tgt.getSubSignature();
				if(tgt.isStatic())
					if(subsig.equals("void <clinit>()") || subsig.equals("void main(java.lang.String[])"))
						roots.add(tgt);
			}
		}
		
		for(SootMethod rootMethod : roots){
			System.out.println("rootMethod: "+rootMethod);
			if(targetMethods.contains(rootMethod)) 
				visitFinal(null, null, rootMethod, dpta.emptyStack(), null);
			else
				traverse(rootMethod, dpta.emptyStack(), new HashSet(), null);
		}
	}

	protected Object visit(SootMethod caller, 
						   Stmt callStmt, 
						   SootMethod callee, 
						   ImmutableStack<Integer> calleeContext, 
						   Object data)
	{
		CallStack cs = data == null ? new CallStack() : (CallStack) data;
		return cs.append(callStmt, caller);
	}

	protected abstract void visitFinal(SootMethod caller, 
									   Stmt callStmt, 
									   SootMethod callee, 
									   ImmutableStack<Integer> calleeContext, 
									   Object data);

	protected void traverse(SootMethod caller, 
							ImmutableStack<Integer> callerContext, 
							Set<SootMethod> visited, 
							Object data)
	{
		Map<Stmt,List<Edge>> callSiteToEdges = new HashMap();
		
		{
			List<Edge> outgoingEdges = callEdges.get(caller);
			if(outgoingEdges == null)
				return;
			
			for(Edge e : outgoingEdges){
				if(visited.contains(e.tgt()))
					continue;
				
				Stmt callStmt = e.srcStmt();
				List<Edge> es = callSiteToEdges.get(callStmt);
				if(es == null){
					es = new ArrayList();
					callSiteToEdges.put(callStmt, es);
				}
				es.add(e);
			}
		}

		for(Map.Entry<Stmt,List<Edge>> entry : callSiteToEdges.entrySet()){
			Stmt callStmt = entry.getKey();
			List<Edge> outgoingEdges = entry.getValue();

			System.out.println("Query: "+ callStmt + "@" + caller + " callerContext: "+callerContext);
			Set<SootMethod> targets = dpta.callTargets(callStmt, callerContext);

			for(Edge e : outgoingEdges){
				SootMethod callee = e.tgt();		
				if(!targets.contains(callee)){
					System.out.println("invalid calledge to "+callee);
					continue;
				}
				Integer callSite = dpta.getCallSiteFor(callStmt);
				if(callSite == null){
					//probably a static method with no params
					//unsound
					continue;
				}
				ImmutableStack<Integer> calleeContext = callerContext.push(callSite);

				if(targetMethods.contains(callee)) {
					visitFinal(caller, callStmt, callee, calleeContext, data);
				}
				else {
					Object newData = visit(caller, callStmt, callee, calleeContext, data);
				
					Set<SootMethod> visitedCopy = new HashSet();
					visitedCopy.addAll(visited);
					visitedCopy.add(callee);
					
					traverse(callee, calleeContext, visitedCopy, newData);
				}
			}
		}
	}
}