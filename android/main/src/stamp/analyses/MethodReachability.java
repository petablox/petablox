package stamp.analyses;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.ClassicProject;
import shord.analyses.Ctxt;

import stamp.analyses.ondemand.CallStack;

import chord.project.Chord;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.bddbddb.Rel.RelView;

import com.google.gson.stream.JsonWriter;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@Chord(name="meth-reach-bdd-java",
	   consumes={"CICM", "OutLabelArg"})
public class MethodReachability extends JavaAnalysis
{
	protected Map<Unit,SootMethod> invkUnitToMethod = new HashMap();
	protected Set<SootMethod> targetMethods = new HashSet();
	private ProgramRel relCICM;
	protected JsonWriter writer;
	protected Map<SootMethod,Map<Ctxt,Set<Pair<Ctxt,Stmt>>>> csCg = new HashMap();

	public void run()
	{
		try{
			String stampOutDir = System.getProperty("stamp.out.dir");
			this.writer = new JsonWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "flows.json"))));
			writer.setIndent("  ");
			writer.beginArray();
		}catch(IOException e){
			throw new Error(e);
		}


		populateTargetMethods(Arrays.asList(new String[]{"!Activity"}));

        final ProgramRel relMI = (ProgramRel) ClassicProject.g().getTrgt("MI");		
        relMI.load();
        Iterable<Pair<SootMethod,Unit>> res = relMI.getAry2ValTuples();
        for(Pair<SootMethod,Unit> pair : res) {
            SootMethod meth = pair.val0;
            Unit invk = pair.val1;
			invkUnitToMethod.put(invk, meth);
        }
        relMI.close();

		relCICM = (ProgramRel) ClassicProject.g().getTrgt("CICM");
        relCICM.load();
		System.out.println("starting to read cicm");
		readCICM();
		System.out.println("finished reading cicm");
		relCICM.close();
		
		List<Pair<SootMethod,Ctxt>> workList = new ArrayList();
		for(SootMethod target : targetMethods){
			System.out.println("contexts of "+target);
			Iterable<Ctxt> allCtxts = allContextsOf(target);
			if(allCtxts == null)
				continue;
			for(Ctxt ctxt : allCtxts){
				System.out.println(ctxt.toString());
				//workList.add(new Pair(target, ctxt));
				traverse(target, ctxt, new HashSet(), null);
			}
			
		}		

		try{
			writer.endArray();
			writer.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	protected Object visit(SootMethod caller, 
						   Stmt callStmt, 
						   SootMethod callee, 
						   Object data)
	{
		//System.out.println("visit: "+caller+" "+callStmt+" "+callee);
		CallStack cs = data == null ? new CallStack() : (CallStack) data;
		return cs.append(callStmt, caller);
	}


	protected void visitFinal(Object data)
	{
		System.out.println("visit final");
		CallStack callStack = (CallStack) data;
		
		try{
			writer.beginObject();

			//writer.name("sink").value(callee.getSignature());
						
			writer.name("callstack");
			writer.beginArray();
			for(Pair<Stmt,SootMethod> elem : callStack)
				writer.value(elem.val0 + "@" + elem.val1.getSignature());
			writer.endArray();
			
			/*
			writer.name("flows");
			writer.beginArray();
			for(Trio<Integer,String,Set<String>> e : flows){
				writer.beginObject();
				writer.name("pindex").value(e.val0);
				writer.name("sink-label").value(e.val1);
				writer.name("source-label");
				writer.beginArray();
				for(String w : e.val2)
					writer.value(w);
				writer.endArray();
				writer.endObject();
			}
			writer.endArray();
			*/

			writer.endObject();
		} catch(IOException e){
			throw new Error(e);
		}
	}

	protected void traverse(SootMethod callee, 
							Ctxt calleeContext, 
							Set<SootMethod> visited, 
							Object data)
	{
		Iterable<Pair<Ctxt,Stmt>> callers = callersOf(callee, calleeContext);
		if(callers == null){
			visitFinal(data);
			return;
		}
		for(Pair<Ctxt,Stmt> pair : callers){
			Ctxt callerContext = pair.val0;
			Stmt invkUnit = pair.val1;

			SootMethod caller = invkUnitToMethod.get(invkUnit);
			if(visited.contains(caller))
				return;

			Object newData = visit(caller, invkUnit, callee, data);

			Set<SootMethod> visitedCopy = new HashSet();
			visitedCopy.addAll(visited);
			visitedCopy.add(caller);
			
			traverse(caller, callerContext, visitedCopy, newData);
		}
	}

	void populateTargetMethods(Collection<String> labels)
	{
		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("OutLabelArg");
        rel.load();
		for(String l : labels){
			RelView view = rel.getView();
			view.delete(2); //param index
			view.selectAndDelete(0, l);
			Iterable<SootMethod> meths = view.getAry1ValTuples();
			for(SootMethod m : meths){
				System.out.println("target method: "+m);
				targetMethods.add(m);
			}
		}
		rel.close();
	}

	/*
	private Iterable<Ctxt> allContextsOf(SootMethod m)
	{
		RelView view = relCICM.getView();
		view.delete(0);
		view.delete(1);
		view.selectAndDelete(3, m);
		return view.getAry1ValTuples();

	}

	private Iterable<Pair<Ctxt,Stmt>> callersOf(SootMethod m, Ctxt ctxt)
	{
		RelView view = relCICM.getView();
		view.selectAndDelete(2, ctxt);
		view.selectAndDelete(3, m);
		return view.getAry2ValTuples();

	}*/

	private Iterable<Ctxt> allContextsOf(SootMethod m)
	{
		Map<Ctxt,Set<Pair<Ctxt,Stmt>>> callers = csCg.get(m);
		if(callers == null)
			return null;
		return callers.keySet();
	}

	private Iterable<Pair<Ctxt,Stmt>> callersOf(SootMethod m, Ctxt ctxt)
	{
		Map<Ctxt,Set<Pair<Ctxt,Stmt>>> callers = csCg.get(m);
		if(callers == null)
			return null;
		return callers.get(ctxt);
	}

	private void readCICM()
	{
		Iterable<Quad<Ctxt,Stmt,Ctxt,SootMethod>> callEdges = relCICM.getAry4ValTuples();
		for(Quad<Ctxt,Stmt,Ctxt,SootMethod> quad : callEdges){
			Ctxt callerCtxt = quad.val0;
			Stmt invkStmt = quad.val1;
			Ctxt calleeCtxt = quad.val2;
			SootMethod callee = quad.val3;
			
			Map<Ctxt,Set<Pair<Ctxt,Stmt>>> callers = csCg.get(callee);
			if(callers == null){
				callers = new HashMap();
				csCg.put(callee, callers);
			}
			
			Set<Pair<Ctxt,Stmt>> callSites = callers.get(calleeCtxt);
			if(callSites == null){
				callSites = new HashSet();
				callers.put(calleeCtxt, callSites);
			}
			callSites.add(new Pair(callerCtxt, invkStmt));
		}
	}
}