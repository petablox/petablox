package stamp.analysis;

import soot.Unit;
import soot.Local;
import soot.Value;
import soot.Scene;
import soot.SootMethod;
import soot.SootClass;
import soot.SootField;
import soot.jimple.Stmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.spark.ondemand.genericutil.ImmutableStack;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.AllocAndContextSet;

import stamp.analyses.ondemand.TaintAnalysis2;
import stamp.analyses.ondemand.CallStack;
import stamp.analyses.ondemand.TaintManager;

import chord.project.Chord;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@Chord(name="iccg-ondemand-2-java")
public class IccgOnDemand2 extends TaintAnalysis2
{
	protected void setup() 
	{
		super.setup();
		taintManager.setSinkLabels(Arrays.asList(new String[]{"!Activity"})); 
		taintManager.setSourceLabels(Arrays.asList(new String[]{})); 
		//addWidgetTaint();
	}

	private static class IccPath
	{
		List<Set<String>> widgets;
		CallStack callStack;

		IccPath(CallStack callStack, List<Set<String>> widgets)
		{
			this.callStack = callStack;
			this.widgets = widgets;
		}
		
		IccPath()
		{
			this(new CallStack(), new ArrayList());
		}
		
		IccPath append(Stmt callStmt, SootMethod caller, Set<String> ws)
		{
			CallStack callStackCopy = this.callStack.append(callStmt, caller);

			List<Set<String>> widgetsCopy = new ArrayList();
			widgetsCopy.addAll(this.widgets);
			if(ws != null)
				widgetsCopy.add(ws);
			
			return new IccPath(callStackCopy, widgetsCopy);
		}
	}
	
	protected Object visit(SootMethod caller, 
						   Stmt callStmt, 
						   SootMethod callee, 
						   ImmutableStack<Integer> calleeContext, 
						   Object data)
	{
		Set<String> ws = null;
		if(callee.getSubSignature().equals("void onClick(android.view.View)")){
			Set<String> taint = computeTaintSet(callee, calleeContext, 1);
			if(!taint.isEmpty()){
				if(ws == null)
					ws = new HashSet();
				ws.addAll(taint);
			}
		}
		if(data == null)
			return new IccPath().append(callStmt, caller, ws);
		else
			return ((IccPath) data).append(callStmt, caller, ws);
	}

	protected void visitFinal(SootMethod caller, 
							  Stmt callStmt, 
							  SootMethod callee, 
							  ImmutableStack<Integer> calleeContext, 
							  Object data)
	{
		List<Trio<Integer,String,Set<String>>> flows = computeTaintFlows(callee, calleeContext);
		IccPath iccPath = (IccPath) visit(caller, callStmt, callee, calleeContext, data);

		try{
			writer.beginObject();
			
			CallStack callStack = iccPath.callStack;
			String srcAct = callStack.elemAt(1).val1.getDeclaringClass().getName();
			writer.name("src").value(srcAct);
			
			writer.name("callstack");
			writer.beginArray();
			for(Pair<Stmt,SootMethod> elem : callStack)
				writer.value(elem.val0 + "@" + elem.val1.getSignature());
			writer.endArray();
			
			writer.name("widget-control");
			writer.beginArray();
			for(Set<String> ws : iccPath.widgets){
				writer.beginArray();
				for(String w : ws)
					writer.value(w);
				writer.endArray();
			}
			writer.endArray();
			
			writer.name("icc-meth").value(callee.getSignature());

			writer.name("widget-data");
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
	
	protected void addWidgetTaint()
	{
		SootClass gClass = Scene.v().getSootClass("stamp.harness.G");
		SootMethod gClinit = gClass.getMethod("void <clinit>()");
		for(Unit unit : gClinit.retrieveActiveBody().getUnits()){
			Stmt stmt = (Stmt) unit;
			if(!(stmt instanceof DefinitionStmt))
				continue;
			Value left = ((DefinitionStmt) stmt).getLeftOp();
			if(!(left instanceof StaticFieldRef))
				continue;
			SootField f = ((StaticFieldRef) left).getField();
			if(!f.getDeclaringClass().equals(gClass))
				continue;
			Local right = (Local) ((DefinitionStmt) stmt).getRightOp();
			taintManager.setTaint(right, f.getSubSignature());
		}
		
		for(Map.Entry<soot.jimple.spark.pag.AllocNode,Set<String>> e : taintManager.allocNodeToTaints.entrySet()){
			System.out.print("Added taint label to "+e.getKey()+": ");
			for(String label : e.getValue())
				System.out.print(label+", ");
			System.out.println("");
		}
	}
}