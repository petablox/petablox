package stamp.analyses.ondemand;

import soot.SootMethod;
import soot.MethodOrMethodContext;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.RefLikeType;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.ondemand.genericutil.ImmutableStack;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.DemandCSPointsTo.VarAndContext;

import shord.program.Program;

import chord.project.Chord; 
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import com.google.gson.stream.JsonWriter;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@Chord(name="taint-ondemand-java")
public class TaintAnalysis extends MethodReachabilityAnalysis
{
	protected TaintManager taintManager;
	protected JsonWriter writer;

	protected void setup()
	{
		Program.g().runSpark("merge-stringbuffer:false");
		this.taintManager = new TaintManager(this.dpta);
		setup(OnDemandPTA.makeDefault());

		this.taintManager = new TaintManager(dpta);

		try{
			String stampOutDir = System.getProperty("stamp.out.dir");
			this.writer = new JsonWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "flows.json"))));
			writer.setIndent("  ");
			writer.beginArray();
		}catch(IOException e){
			throw new Error(e);
		}

		taintManager.readAnnotations();
		targetMethods.addAll(taintManager.sinkMethods());
	}

	protected void done()
	{
		try{
			writer.endArray();
			writer.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}

	public void run()
	{
		setup();
		super.run();
		done();
	}

	protected void visitFinal(SootMethod caller, 
							  Stmt callStmt, 
							  SootMethod callee, 
							  ImmutableStack<Integer> calleeContext, 
							  Object data)
	{
		List<Trio<Integer,String,Set<String>>> flows = computeTaintFlows(callee, calleeContext);
		CallStack callStack = (CallStack) visit(caller, callStmt, callee, calleeContext, data);
		
		try{
			writer.beginObject();

			writer.name("sink").value(callee.getSignature());
						
			writer.name("callstack");
			writer.beginArray();
			for(Pair<Stmt,SootMethod> elem : callStack)
				writer.value(elem.val0 + "@" + elem.val1.getSignature());
			writer.endArray();
			
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

			writer.endObject();
		} catch(IOException e){
			throw new Error(e);
		}
	}
	
	protected List<Trio<Integer,String,Set<String>>> computeTaintFlows(SootMethod sink, ImmutableStack<Integer> sinkContext)
	{
		Body body = sink.retrieveActiveBody();
		List<Trio<Integer,String,Set<String>>> result = new ArrayList();

		for(Map.Entry<Integer,Set<String>> sinkParam : taintManager.sinkParamsOf(sink).entrySet()){
			int pCount = sinkParam.getKey();
			for(String sinkLabel : sinkParam.getValue()){
				Local param;
				if(!sink.isStatic())
					param = pCount == 0 ? body.getThisLocal() : body.getParameterLocal(pCount-1);
				else
					param = body.getParameterLocal(pCount);

				Set<String> taints = computeTaintSetFor(param, sinkContext);
				if(!taints.isEmpty())
					result.add(new Trio(pCount, sinkLabel, taints));
			}
		}

		return result;
	}

	protected Set<String> computeTaintSetFor(Local loc, ImmutableStack<Integer> locContext)
	{
		Set<String> taints = new HashSet();
		if(!(loc.getType() instanceof RefLikeType)){
			//TODO
			return taints;
		}

		List<VarAndContext> varsWL = new ArrayList();
		Set<VarAndContext> varsVisited = new HashSet();
		varsWL.add(new VarAndContext(dpta.varNode(loc), locContext));

		List<AllocAndContext> objectsWL = new ArrayList();
		Set<AllocAndContext> objectsVisited = new HashSet();

		do{
			while(!varsWL.isEmpty()){
				VarAndContext vc = varsWL.remove(0);
				if(varsVisited.contains(vc))
					continue;
				varsVisited.add(vc);
				
				AllocAndContextSet pt = (AllocAndContextSet) dpta.pointsToSetFor(vc);
				if(pt == null)
					System.out.println("Warning: Points-to set for "+vc.var+" is null.");
				else
					for(AllocAndContext obj : pt)
						objectsWL.add(obj);
			}
			
			while(!objectsWL.isEmpty()){
				AllocAndContext oc = objectsWL.remove(0);
				if(objectsVisited.contains(oc))
					continue;
				objectsVisited.add(oc); 
				
				Set<String> ts = taintManager.getTaint(oc.alloc);
				if(ts != null)
					taints.addAll(ts);
				
				Set<VarAndContext> vcs = dpta.flowsToSetFor(oc);
				if(vcs == null)
					System.out.println("Warning: Flows-to set for "+ oc.alloc+" is null.");
				else
					for(VarAndContext vc : vcs){
						VarNode dest = vc.var;
						ImmutableStack<Integer> destContext = vc.context;
						
						SootMethod method = dpta.transferEndPoint(dest);
						if(method == null)
							continue;

						Collection<LocalVarNode> sources = taintManager.findTaintTransferSourceFor(dest);
						if(sources == null){
							//System.out.println("possibly missing models. endpoint: "+
							//				   ((LocalVarNode) dest).getVariable()+" in method: "+method.getSignature());
							continue;
						}
						for(LocalVarNode src : sources)
							varsWL.add(new VarAndContext(src, destContext));
					}
			}
		} while(!varsWL.isEmpty());

		return taints;
	}
}