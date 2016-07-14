package stamp.fixcode;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Body;
import soot.Unit;
import soot.Local;
import soot.Value;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.InvokeExpr;
import soot.util.Chain;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import shord.project.analyses.JavaAnalysis;
import shord.program.Program;

import chord.project.Chord;

import java.util.Iterator;

/*
 * @author Saswat Anand
*/
@Chord(name="inline-fix")
public class Inliner extends JavaAnalysis
{
	private Body body;
	private Chain<Unit> units;
	private Chain<Local> locals;
	private CallGraph callGraph;

	public void run()
	{
		if(!Scene.v().hasCallGraph())
			Program.g().runCHA();
		callGraph = Scene.v().getCallGraph();
		
		Program prog = Program.g();
		for(SootClass klass : prog.getClasses()){
			if(prog.isFrameworkClass(klass))
				continue;
			for(SootMethod method : klass.getMethods()){
				if(!method.isConcrete())
					continue;
				this.body = method.retrieveActiveBody();
				this.units = body.getUnits();
				this.locals = body.getLocals();
				process();
			}
		}
	}
	
	private void process()
	{
		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
			
			if(runOnUiThread(stmt))
				continue;
		}
	}
	
	private boolean runOnUiThread(Stmt stmt)
	{
		if(!stmt.containsInvokeExpr())
			return false;
		
		InvokeExpr ie = stmt.getInvokeExpr();
		String calleeSubsig = ie.getMethod().getSubSignature();
		if(!calleeSubsig.equals("void runOnUiThread(java.lang.Runnable)"))
		   return false;

		Iterator<Edge> edgeIt = callGraph.edgesOutOf(stmt);
		SootMethod target = null;
		while(edgeIt.hasNext()){
			if(target == null)
				target = (SootMethod) edgeIt.next().getTgt();
			else{
				//multple outgoing edges
				System.out.println("TODO: multiple outgoing edges from "+stmt);
				target = null;
				break;
			}
		}
		if(target == null)
			return false;
		String targetClassName = target.getDeclaringClass().getName();
		if(!targetClassName.equals("android.app.Activity"))
			return false;
		Value arg = ie.getArg(0);
		if(!(arg instanceof Local))
			return false;
		SootMethodRef runMethodRef = Scene.v().getMethod("<java.lang.Runnable: void run()>").makeRef();
		Stmt invkStmt = Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr((Local) arg, runMethodRef));
		units.insertBefore(invkStmt, stmt);
		units.remove(stmt);
		return true;
	}
}