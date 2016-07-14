package stamp.analyses;

import soot.Scene;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Immediate;
import soot.jimple.Stmt;
import soot.jimple.IntConstant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.spark.pag.SparkField;

import shord.analyses.Ctxt;
import shord.analyses.AllocNode;

import shord.project.analyses.ProgramRel;
import shord.project.ClassicProject;

import chord.util.tuple.object.Pair;
import chord.bddbddb.Rel.RelView;

import java.util.*;

public class WidgetIdentifierAnalysis
{
	private Map<Ctxt,Set<Ctxt>> outgoing = new HashMap();
	private Map<Ctxt,Set<Ctxt>> incoming = new HashMap();
	private Map<Ctxt,String> widgetToResourceId = new HashMap();

	//a widget may be mapped to multiple id's because of over-approximation
	private Map<Ctxt,Set<Integer>> widgetToId = new HashMap();

	private Traverser fwd = new Traverser(outgoing, true);
	private Traverser bwd = new Traverser(incoming, false);
	
	void prepare()
	{
		buildGraph();
		mapWidgetsToResourceIds();
		mapWidgetsToIds();
	}

	Set<Integer> findId(Ctxt widgetObj)
	{
		return widgetToId.get(widgetObj);
	}

	String findResourceId(Ctxt widgetObj)
	{
		String id = widgetToResourceId.get(widgetObj);
		if(id != null)
			return id;
		
		List<Ctxt> result = fwd.findPath(widgetObj);
		if(result != null){
			int size = result.size();
			StringBuilder builder = new StringBuilder();
			for(int i = 0; i < (size-1); i++){
				Ctxt w = result.get(i);
				AllocNode an = (AllocNode) w.getElems()[0];
				builder.append(an.getType().toString()+" > ");
			}
			id = widgetToResourceId.get(result.get(size-1));
			builder.append(id);
			return builder.toString();
		}
		
		result = bwd.findPath(widgetObj);
		if(result != null){
			int size = result.size();
			StringBuilder builder = new StringBuilder();
			id = widgetToResourceId.get(result.get(0));
			builder.append(id + " > ");
			for(int i = 1; i < size; i++){
				Ctxt w = result.get(i);
				AllocNode an = (AllocNode) w.getElems()[0];
				builder.append(an.getType().toString());
				if(i < (size-1))
					builder.append(" > ");					
			}
			return builder.toString();
		}
		
		return null;
	}

	private class Traverser
	{
		Map<Ctxt,Set<Ctxt>> graph;
		boolean forward;

		Traverser(Map<Ctxt,Set<Ctxt>> graph, boolean forward)
		{
			this.graph = graph;
			this.forward = forward;
		}

		List<Ctxt> findPath(Ctxt node)
		{
			List<Ctxt> result = traverse(node, new ArrayList());
			if(result == null)
				return null;
			if(!forward)
				Collections.reverse(result);
			return result;
		}
		
		private List<Ctxt> traverse(Ctxt node, List<Ctxt> path)
		{
			if(path.contains(node))
				return null;
			path.add(node);
			String id = widgetToResourceId.get(node);
			if(id != null)
				return path;
			Set<Ctxt> succs = graph.get(node);
			if(succs == null)
				return null; //dead end
			for(Ctxt succ : succs){
				List<Ctxt> pathCopy = new ArrayList();
				pathCopy.addAll(path);
				List<Ctxt> succResult = traverse(succ, pathCopy); 
				if(succResult != null)
					return succResult;
			}
			return null;
		}
	}

	protected void buildGraph()
	{
        ProgramRel relFpt = (ProgramRel) ClassicProject.g().getTrgt("fpt");		
        relFpt.load();

		SootField childFld = Scene.v().getSootClass("android.view.ViewGroup").getFieldByName("child");

		RelView view = relFpt.getView();
		view.selectAndDelete(1, childFld);
		Iterable<Pair<Ctxt,Ctxt>> iter = view.getAry2ValTuples();
		for(Pair<Ctxt,Ctxt> pair : iter){
			Ctxt from = pair.val0;
			Ctxt to = pair.val1;
			
			Set<Ctxt> outs = outgoing.get(from);
			if(outs == null){
				outs = new HashSet();
				outgoing.put(from, outs);
			}
			outs.add(to);
			
			Set<Ctxt> ins = incoming.get(to);
			if(ins == null){
				ins = new HashSet();
				incoming.put(to, ins);
			}
			ins.add(from);
		}
		view.free();
		relFpt.close();
	}

	private void mapWidgetsToResourceIds()
	{
        ProgramRel relFpt = (ProgramRel) ClassicProject.g().getTrgt("fpt");		
        relFpt.load();
 
		RelView view = relFpt.getView();
		view.delete(0);
		Iterable<Pair<SparkField,Ctxt>> iter = view.getAry2ValTuples();
		for(Pair<SparkField,Ctxt> pair : iter){
			if(!(pair.val0 instanceof SootField))
				continue;
			SootField fld = (SootField) pair.val0;
			Ctxt obj = pair.val1;
			String className = fld.getDeclaringClass().getName();
			if(!className.startsWith("stamp.harness.LayoutInflater$"))
				continue;
			String fldSubsig = fld.getSubSignature();
			widgetToResourceId.put(obj, fldSubsig);
		}
		view.free();
		relFpt.close();
	}
	
	private void mapWidgetsToIds()
	{
		ProgramRel relCICM = (ProgramRel) ClassicProject.g().getTrgt("CICM");
        relCICM.load();
		RelView view = relCICM.getView();
		view.delete(0);
		SootMethod setIdMeth = Scene.v().getMethod("<android.view.View: void setId(int)>");
		view.selectAndDelete(3, setIdMeth);
		Iterable<Pair<Stmt,Ctxt>> it = view.getAry2ValTuples();
		for(Pair<Stmt,Ctxt> pair : it){
			Stmt invkStmt = pair.val0;
			Ctxt tgtCtxt = pair.val1;
			InstanceInvokeExpr ie = (InstanceInvokeExpr) invkStmt.getInvokeExpr();
			Local base = (Local) ie.getBase();
			Immediate id = (Immediate) ie.getArg(0);
			if(!(id instanceof IntConstant)){
				System.out.println("Unexpected. arg is not constant "+invkStmt);
				continue;
			}
			Set<Integer> ids = widgetToId.get(tgtCtxt);
			if(ids == null)
				widgetToId.put(tgtCtxt, (ids = new HashSet()));
			ids.add(((IntConstant) id).value);
		}
		view.free();
		relCICM.close();
	}
}