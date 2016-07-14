package stamp.analyses;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Value;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.toolkits.callgraph.ReachableMethods;

import shord.analyses.VarNode;
import shord.analyses.LocalVarNode;
import shord.analyses.AllocNode;
import shord.analyses.Ctxt;
import shord.analyses.DomC;
import shord.analyses.DomV;
import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.ClassicProject;

import stamp.app.App;
import stamp.util.SHAFileChecksum;

import chord.project.Chord;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Quad;
import chord.bddbddb.Rel.RelView;

import com.google.gson.stream.JsonWriter;

import java.util.*;
import java.io.*;

/*
 * @author Saswat Anand
 */
@Chord(name="iccg-bdd-java",
	   consumes={"fpt", "CICM", "pt", "OutLabelArg", "MI", "MmethArg"})
public class IccgAnalysis extends JavaAnalysis
{
	protected Map<Pair<Ctxt,Stmt>,Set<WidgetList>> cache = new HashMap();
	protected Map<Stmt,SootMethod> invkStmtToMethod = new HashMap();
	protected Map<SootMethod,Set<Integer>> targetMethods = new HashMap();
	protected Map<SootMethod,Map<Ctxt,Set<Pair<Ctxt,Stmt>>>> csCg = new HashMap();
	protected Map<SootMethod,VarNode> uiCallbackToArg = new HashMap();

	protected ProgramRel relPt;
	protected JsonWriter writer;

	protected WidgetControlDependencyAnalysis widgetsAnalysis;

	public void run()
	{
		try{
			startWriter();

			populateTargetMethods(Arrays.asList(new String[]{"!Activity"}));

			populateUiCallbackToArg();

			readMI();
		
			readCICM();

			relPt = (ProgramRel) ClassicProject.g().getTrgt("pt");
			relPt.load();

			widgetsAnalysis = new WidgetControlDependencyAnalysis();
			
			List<Pair<SootMethod,Ctxt>> workList = new ArrayList();
			for(SootMethod target : targetMethods.keySet()){
				System.out.println("contexts of "+target);
				Iterable<Ctxt> allCtxts = allContextsOf(target);
				if(allCtxts == null)
					continue;
				for(Ctxt ctxt : allCtxts){
					System.out.println("context: "+ctxt.toString());
					workList.add(new Pair(target, ctxt));
					traverse(target, ctxt, new ArrayList());
				}				
			}		
			//System.out.println("Total number of paths = "+count);
			
			writeResults(workList);

			relPt.close();

			stopWriter();
			
			dumpCache();
		}catch(IOException e){
			throw new Error(e);
		}
	}
	private int cacheHit = 0;
	protected void traverse(SootMethod callee, 
							Ctxt calleeContext, 
							List<Trio<Ctxt,Stmt,Set<String>>> path)
	{
		System.out.println("Traverse: callee: "+callee.getSignature()+" calleeContext: "+calleeContext);
		Iterable<Pair<Ctxt,Stmt>> callers = callersOf(callee, calleeContext);
		if(callers == null){
			//reached the entry
			cacheResult(Collections.<WidgetList> emptySet(), path, false);
			return;
		}

		for(Pair<Ctxt,Stmt> pair : callers){
			Ctxt callerContext = pair.val0;
			Stmt callSite = pair.val1;
			
			boolean cyclic = false;
			for(Trio<Ctxt,Stmt,Set<String>> trio : path){
				Ctxt ctxt = trio.val0;
				Stmt cs = trio.val1;
				if(cs.equals(callSite) && ctxt.equals(callerContext)){
					cyclic = true;
					break;
				}
			}
			if(cyclic)
				continue;

			//check for cached result
			Set<WidgetList> cachedResult = cache.get(pair);
			if(cachedResult != null){
				System.out.println("cachehit: "+cacheHit++);
				cacheResult(cachedResult, path, false);
				continue;
			}

			//if callSite is conditional dependent
			//on whether a specific widget is clicked
			//then propagate
			Set<String> widgets = null;
			SootMethod caller = invkStmtToMethod.get(callSite);
			String callerSubsig = caller.getSubSignature();
			/*
			if(callerSubsig.equals("void onClick(android.view.View)") ||
			   callerSubsig.equals("void onItemClick(android.widget.AdapterView,android.view.View,int,long)")){
				widgets = widgetsAnalysis.computeWidgetIds(identifyWidgets(callerContext, caller), caller, callSite);
			}
			*/
			if(uiCallbackToArg.get(caller) != null)
				widgets = widgetsAnalysis.computeWidgetIds(identifyWidgets(callerContext, caller), caller, callSite);

			List<Trio<Ctxt,Stmt,Set<String>>> newPath = new ArrayList();
			newPath.addAll(path);
			newPath.add(new Trio(callerContext, callSite, widgets));

			traverse(caller, callerContext, newPath);
		}
	}
	
	protected void cacheResult(Pair<Ctxt,Stmt> callSite, WidgetList widgets)
	{
		//check for cyclic widgetList
		boolean cyclic = false;
		//if(widgets != null){
			int widgetListSize = widgets.size();
			outer:
			for(int i = 0; i < widgetListSize; i++){
				for(int j = i+1; j < widgetListSize; j++){
					if(widgets.get(i).equals(widgets.get(j))){
						cyclic = true;
						break outer;
					}
				}
			}
			//}

		//System.out.println("caching "+callSite+" "+(widgets==null));
		Set<WidgetList> ws = cache.get(callSite);
		if(ws == null){
			ws = new HashSet();
			cache.put(callSite, ws);
		}
		if(!cyclic){
			ws.add(widgets);
			//debug(callSite, widgets);
		}
	}

	protected void debug(Pair<Ctxt,Stmt> callSite, WidgetList wl)
	{
		Stmt callStmt = callSite.val1;
		System.out.println("debug: stmt: "+callStmt+"@"+invkStmtToMethod.get(callStmt).getSignature()+" "+
						   "ctxt: "+callSite.val0);
		StringBuilder builder = new StringBuilder();
		builder.append("[ ");
		for(Set<String> widgets : wl){
			builder.append("{");
			for(String id : widgets){
				builder.append(id+", ");
			}
			builder.append("}, ");
		}
		builder.append("]");
		System.out.println(builder.toString());
	}

	protected void cacheResult(Set<WidgetList> suffixes, List<Trio<Ctxt,Stmt,Set<String>>> path, boolean debug)
	{
		int size = path.size();		
		WidgetList prefix = new WidgetList();
		for(int i = size-1; i >= 0; i--){
			Trio<Ctxt,Stmt,Set<String>> trio = path.get(i);
			Ctxt ctxt = trio.val0;
			Stmt invkStmt = trio.val1;
			Set<String> widgets = trio.val2;

			if(widgets != null)
				prefix.add(0, widgets);
				//prefix.add(0, new Trio(ctxt, invkStmt, widgets));
			
			Pair<Ctxt,Stmt> p = new Pair(ctxt,invkStmt);
			if(suffixes.size() > 0){
				for(WidgetList suffix : suffixes){
					WidgetList allWidgets = new WidgetList();
					allWidgets.addAll(prefix);
					allWidgets.addAll(suffix);
					cacheResult(p, allWidgets);
				}
			} else{
				WidgetList allWidgets = new WidgetList();
				if(prefix.isEmpty()){
					//both suffix and prefix empty
					cacheResult(p, allWidgets);
				} else{
					//non-empty prefix, empty suffix
					allWidgets.addAll(prefix);
					cacheResult(p, allWidgets);
				}
			}
		}
		if(debug && prefix.size() == 2) debug(path);
					
	}

	protected void debug(List<Trio<Ctxt,Stmt,Set<String>>> path)
	{
		StringBuilder builder = new StringBuilder();
		int count = 0;
		for(Trio<Ctxt,Stmt,Set<String>> trio : path){
			Ctxt c = trio.val0;
			Stmt callStmt = trio.val1;
			Set<String> ids = trio.val2;

			SootMethod m = invkStmtToMethod.get(callStmt);
			builder.append(count+++". stmt: "+ callStmt+"@"+m.getSignature()+"\n  ctxt: "+c.toString());
			builder.append("\n  widgets: [");
			if(ids != null){
				for(String id : ids)
					builder.append(id+", ");
			}
			builder.append("]\n");
		} 
		System.out.println(builder.toString());
	}

	protected Set<Ctxt> identifyWidgets(Ctxt context, SootMethod m)
	{
		System.out.println("identifyWidgets: ctxt: "+context+" m: "+m.getSignature());
		VarNode vn = uiCallbackToArg.get(m);
		assert vn != null;
		RelView ptView = pointsToSetFor(vn, context);
		Iterable<Ctxt> objs = ptView.getAry1ValTuples();
		Set<Ctxt> ret = new HashSet();
		for(Ctxt obj : objs){
			ret.add(obj);
		}		
		ptView.free();
		return ret;
	}

	protected RelView pointsToSetFor(VarNode vn, Ctxt context)
	{
		RelView view = relPt.getView();
		view.selectAndDelete(0, context);
		view.selectAndDelete(1, vn);
		return view;
	}

	void populateTargetMethods(Collection<String> labels)
	{
		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("OutLabelArg");
        rel.load();
		for(String l : labels){
			RelView view = rel.getView();
			view.selectAndDelete(0, l);
			Iterable<Pair<SootMethod,Integer>> it = view.getAry2ValTuples();
			for(Pair<SootMethod,Integer> pair : it){
				SootMethod m = pair.val0;
				Integer p = pair.val1;
				System.out.println("target method: "+m);
				Set<Integer> params = targetMethods.get(m);
				if(params == null){
					params = new HashSet();
					targetMethods.put(m, params);
				}
				params.add(p);
			}
		}
		rel.close();
	}

	private Iterable<Ctxt> allContextsOf(SootMethod m)
	{
		Map<Ctxt,Set<Pair<Ctxt,Stmt>>> callers = csCg.get(m);
		if(callers == null)
			return null;
		return callers.keySet();
	}

	private void populateUiCallbackToArg()
	{
		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("MmethArg");
        rel.load();
		Program prog = Program.g();
		ReachableMethods reachableMethods = prog.scene().getReachableMethods();
		UiCallbacks uiCallbacks = new UiCallbacks();
		for(Map.Entry<SootMethod,Integer> entry : uiCallbacks.allCallbacks().entrySet()){
			SootMethod callback = entry.getKey();
			Integer paramIndex = entry.getValue();
			if(!reachableMethods.contains(callback) || prog.exclude(callback))
				continue;
			//Rrishi
			RelView view = rel.getView();
			view.selectAndDelete(0, callback);
			view.selectAndDelete(1, paramIndex);
			Iterable<VarNode> it = view.getAry1ValTuples();
			VarNode vn = it.iterator().next();
			uiCallbackToArg.put(callback, vn);
			view.free();
		}
		rel.close();
	}

	private Iterable<Pair<Ctxt,Stmt>> callersOf(SootMethod m, Ctxt ctxt)
	{
		/*
		Pair<SootMethod,Ctxt> p = new Pair(m,ctxt);
		if(!queries.add(p))
			repeatedQueryCount++;
		totalQueryCount++;
		if(totalQueryCount % 10000 == 0)
			System.out.println("totalQueryCount = "+totalQueryCount+" repeatedQueryCount = "+repeatedQueryCount);
		*/
		Map<Ctxt,Set<Pair<Ctxt,Stmt>>> callers = csCg.get(m);
		if(callers == null)
			return null;
		return callers.get(ctxt);
	}

	private void readCICM()
	{
		ProgramRel relCICM = (ProgramRel) ClassicProject.g().getTrgt("CICM");
        relCICM.load();
		System.out.println("starting to read cicm");

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

		System.out.println("finished reading cicm");
		relCICM.close();
	}
	
	private void readMI()
	{
        ProgramRel relMI = (ProgramRel) ClassicProject.g().getTrgt("MI");		
        relMI.load();
        Iterable<Pair<SootMethod,Stmt>> res = relMI.getAry2ValTuples();
        for(Pair<SootMethod,Stmt> pair : res) {
            SootMethod meth = pair.val0;
            Stmt invk = pair.val1;
			invkStmtToMethod.put(invk, meth);
        }
        relMI.close();
	}

	static class WidgetList extends ArrayList<Set<String>>//ArrayList<Trio<Ctxt,Stmt,Set<Ctxt>>>
	{
	}

	/*
	Set<String> computeTaints(Ctxt ctxt, Stmt callSite, SootMethod target)
	{
		Set<String> result = new HashSet();
		InvokeExpr ie = callSite.getInvokeExpr();
		Set<Integer> params = targets.get(target);
		for(Integer p : params){
			Value arg;
			if(ie instanceof InstanceInvokeExpr)
				arg = p == 0 ? ((InstanceInvokeExpr) ie).getBase() : ie.getArgument(p-1);
			else
				arg = ie.getArgument(p);
			if(arg instanceof Constant)
				continue;
			
		}
	}
	*/

	void writeResults(List<Pair<SootMethod,Ctxt>> workList) throws IOException
	{
		Map<Ctxt,Set<SootMethod>> ctxtToTargetMethods = new HashMap();
		for(Pair<SootMethod,Ctxt> pair : workList){
			SootMethod target = pair.val0;
			Ctxt ctxt = pair.val1;
			Set<SootMethod> ts = ctxtToTargetMethods.get(ctxt);
			if(ts == null){
				ts = new HashSet();
				ctxtToTargetMethods.put(ctxt, ts);
			}
			ts.add(target);
		}

		int pathCount = 0;
		for(Map.Entry<Ctxt,Set<SootMethod>> entry : ctxtToTargetMethods.entrySet()){
			Ctxt targetCtxt = entry.getKey();
			Set<SootMethod> targets = entry.getValue();
			
			Map<WidgetList,Set<Stmt>> widgetListToCallsites = new HashMap();
			
			for(SootMethod target : targets){
				Iterable<Pair<Ctxt,Stmt>> callers = callersOf(target, targetCtxt);
				if(callers == null)
					continue;
				for(Pair<Ctxt,Stmt> caller : callers){
					//check for cached result
					System.out.println("Querying "+caller);
					Set<WidgetList> widgetListSet = cache.get(caller);
					for(WidgetList wl : widgetListSet){
						Set<Stmt> callsites = widgetListToCallsites.get(wl);
						if(callsites == null){
							callsites = new HashSet();
							widgetListToCallsites.put(wl, callsites);
						}
						callsites.add(caller.val1);
					}
				}
			}

			if(widgetListToCallsites.size() == 0)
				continue;
			
			writer.beginObject();
			//writer.name("target");
			//writer.value(target.getSignature());
			
			writer.name("source");
			Object[] ctxtElems = targetCtxt.getElems();
			assert ctxtElems.length == 1;
			AllocNode an = (AllocNode) ctxtElems[0];
			writer.value(an.getType().toString());
			
			//writer.name("control");
			writer.name("paths");
			writer.beginArray();
			for(Map.Entry<WidgetList,Set<Stmt>> entry1 : widgetListToCallsites.entrySet()){
				writer.beginObject();
				writer.name("id").value(pathCount++);
				writer.name("length").value(entry1.getKey().size());
				writer.name("widgets");
				writer.beginArray();
				for(Set<String> widgets : entry1.getKey()){
					writer.beginArray();
					for(String id : widgets)
						writer.value(id);
					writer.endArray();
				}
				writer.endArray();

				writer.name("launchsite");
				writer.beginArray();
				for(Stmt callsite : entry1.getValue())
					writer.value(invkStmtToMethod.get(callsite).getSignature()+"@"+callsite);
				writer.endArray();

				writer.endObject();
			}
			writer.endArray();
			writer.endObject();
		}
	}
		
	private void dumpCache() throws IOException
	{
		String stampOutDir = System.getProperty("stamp.out.dir");
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "cache.txt"))));
		for(Map.Entry<Pair<Ctxt,Stmt>,Set<WidgetList>> entry : cache.entrySet()){
			Pair<Ctxt,Stmt> callSite = entry.getKey();
			Set<WidgetList> setOfWidgetList = entry.getValue();
			Stmt stmt = callSite.val1;
			writer.print(stmt+"@"+invkStmtToMethod.get(stmt).getSignature()+" ctxt: "+callSite.val0+" widgets: ");
			writer.print("{");
			for(WidgetList wl : setOfWidgetList){
				writer.print("[");
				for(Set<String> ws : wl){
					writer.print("{");
					for(String w : ws)
						writer.print(w+", ");
					writer.print("}, ");
				}
				writer.print("], ");
			}
			writer.print("}");
			writer.println("");
		}
		writer.close();
	}
	
	private void startWriter() throws IOException
	{ 
		String stampOutDir = System.getProperty("stamp.out.dir");
		this.writer = new JsonWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "flows.json"))));
		writer.setIndent("  ");
		writer.beginObject();
		App app = Program.g().app();
		writer.name("pkg").value(app.getPackageName());
		writer.name("version").value(app.getVersion());
		String apkPath = app.apkPath();
		writer.name("sha").value(SHAFileChecksum.compute(apkPath));
		writer.name("paths");
		writer.beginArray();
	}
	
	private void stopWriter() throws IOException
	{
		writer.endArray();
		writer.endObject();
		writer.close();
	}
}
