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
@Chord(name="taint-ondemand-2-java")
public class TaintAnalysis2 extends MethodReachabilityAnalysis
{
	protected TaintManager taintManager;
	protected JsonWriter writer;
	protected ForwardTaintPropagation fwdTaintPropagation;

	public TaintAnalysis2()
	{
	}

	public void run()
	{
		setup();

        Date start = new Date();
		fwdTaintPropagation.run();
		Date end = new Date();
		reportTime("Forward taint propagation", start, end);

		targetMethods.clear();
		targetMethods.addAll(taintManager.sinkMethods());
		
		start = new Date();
		super.run();
		end = new Date();
		reportTime("Finding sink contexts", start, end);

		done();
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

	protected void setup()
	{
		Program.g().runSpark("merge-stringbuffer:false");
		setup(OnDemandPTA.makeDefault());

		TaintManager tm = new TaintManager(this.dpta);
		tm.readAnnotations();
		this.taintManager = tm;

		ForwardTaintPropagation ftp = new ForwardTaintPropagation();
		ftp.setup(this.dpta, this.taintManager);
		this.fwdTaintPropagation = ftp;

		try{
			String stampOutDir = System.getProperty("stamp.out.dir");
			this.writer = new JsonWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "flows.json"))));
			writer.setIndent("  ");
			writer.beginArray();
		}catch(IOException e){
			throw new Error(e);
		}
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

		for(Map.Entry<Integer,Set<String>> e : taintManager.sinkParamsOf(sink).entrySet()){
			int pCount = e.getKey();
			Set<String> sinkLabels = e.getValue();

			for(String sinkLabel : sinkLabels){
				Local param;
				if(!sink.isStatic())
					param = pCount == 0 ? body.getThisLocal() : body.getParameterLocal(pCount-1);
				else
					param = body.getParameterLocal(pCount);

				Set<String> taints = fwdTaintPropagation.computeTaintSetFor(param, sinkContext);
				if(!taints.isEmpty())
					result.add(new Trio(pCount, sinkLabel, taints));
			}
		}
		return result;
	}

	protected Set<String> computeTaintSet(SootMethod sink, ImmutableStack<Integer> sinkContext, int paramIndex)
	{
		Body body = sink.retrieveActiveBody();
		Local param;
		if(!sink.isStatic())
			param = paramIndex == 0 ? body.getThisLocal() : body.getParameterLocal(paramIndex-1);
		else
			param = body.getParameterLocal(paramIndex);
		return fwdTaintPropagation.computeTaintSetFor(param, sinkContext);
	}

    protected static void reportTime( String desc, Date start, Date end ) {
        long time = end.getTime()-start.getTime();
        System.out.println( "[TaintAnalysis] "+desc+" in "+time/1000+"."+(time/100)%10+" seconds." );
    }

}