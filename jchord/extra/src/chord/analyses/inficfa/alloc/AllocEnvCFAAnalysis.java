package chord.analyses.inficfa.alloc;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.inficfa.BitAbstractState;
import chord.analyses.inficfa.BitEdge;
import chord.analyses.inficfa.BitEnv;
import chord.analyses.inficfa.InfiCFARHSAnalysis;
import chord.program.Loc;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.Timer;
import chord.util.Utils;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.program.Program;

@Chord(name = "allocEnvCFA-java", consumes = { "H", "M", "FH", "HFH", "IHM", "THfilter", "rootM", "NonTerminatingM",
		"conNewInstIH", "conNewInstIM", "objNewInstIH", "objNewInstIM", "aryNewInstIH"})
public class AllocEnvCFAAnalysis extends InfiCFARHSAnalysis<BitEdge<Quad>, BitEdge<Quad>> {
	protected static boolean DEBUG = false;
	protected CIPAAnalysis cipa;
	protected ICICG cicg;
	protected InfiCFAAllocQuadVisitor qv;
	private boolean isInit;
	protected BitSet trackedAlloc;
//	protected Map<Pair<Quad,Quad>, Set<jq_Method>> IHtoM;
	protected Map<Pair<Quad,jq_Method>, BitSet> IMtoH;
	protected MutableGraph<Pair<jq_Method,BitAbstractState>> callGraph;
	protected Map<jq_Method, Set<jq_Method>> MtoM;
	protected Set<Pair<jq_Method, jq_Method>> MToMEdges;
	protected Map<jq_Field, BitSet> FHMap;
	protected Map<Pair<Integer, jq_Field>, BitSet> HFHMap;
	protected Map<jq_Type,BitSet> THFilterMap;
	protected Set<jq_Method> rootM;
	protected Set<jq_Method> nonTerminatingM;
	protected Map<Quad,Quad> conNewInstIHMap;
	protected Map<Quad,Quad> objNewInstIHMap;
	protected Map<Quad,Quad> aryNewInstIHMap;
	protected Map<Quad,Set<jq_Method>> conNewInstIMMap;
	protected Map<Quad,Set<jq_Method>> objNewInstIMMap;
	protected Map<Quad,Quad> reflectIHMap;
	protected Map<Quad,Set<jq_Method>> reflectIMMap;
	protected DomH domH;
	protected Map <Pair<BitAbstractState, Quad>, Set<Pair<BitAbstractState, jq_Method>>> CICM;
	protected jq_Reference javaLangObject;
	protected boolean isHeapEditable;
	protected boolean is0CFA;
	protected boolean handleReflection;
	protected boolean useExtraFilters;
	protected Timer timer;
	protected Timer queryGenTimer;
	
	@Override
	public void init() {
		// XXX: do not compute anything here which needs to be re-computed on each call to run() below.

		if (isInit) return;
		isInit = true;

		javaLangObject = Program.g().getClass("java.lang.Object");
		assert (javaLangObject != null);
		
		cipa = (CIPAAnalysis) ClassicProject.g().getTask("cipa-java");
		ClassicProject.g().runTask(cipa);

		CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask("cicg-java");
		ClassicProject.g().runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();

		super.init();
		
		isHeapEditable = Utils.buildBoolProperty("chord.allocEnvCFA.heapEditable", false);
		is0CFA = Utils.buildBoolProperty("chord.allocEnvCFA.0CFA", false);
		handleReflection = Utils.buildBoolProperty("chord.allocEnvCFA.handleReflection", false);
		useExtraFilters = Utils.buildBoolProperty("chord.allocEnvCFA.useExtraFilters", false);
		
		domH = (DomH) ClassicProject.g().getTrgt("H");
		// build set trackedAlloc
		{
			trackedAlloc = new BitSet();
			ProgramRel relTrackedAlloc = (ProgramRel) ClassicProject.g().getTrgt("trackedAlloc");
			ClassicProject.g().runTask(relTrackedAlloc);
			relTrackedAlloc.load();
			Iterable<Quad> tuples = relTrackedAlloc.getAry1ValTuples();
			for (Quad t : tuples){
				trackedAlloc.set(domH.indexOf(t));
			}
			relTrackedAlloc.close();
		}
		
		//built set rootM
		{
			rootM = new HashSet<jq_Method>();
			ProgramRel relRootM = (ProgramRel) ClassicProject.g().getTrgt("rootM");
			relRootM.load();
			Iterable<jq_Method> tuples = relRootM.getAry1ValTuples();
			for (jq_Method t : tuples) {
				rootM.add(t);
			}
			relRootM.close();
		}
		
		//built set NonTerminatingM
		{
			nonTerminatingM = new HashSet<jq_Method>();
			ProgramRel relNonTerminatingM = (ProgramRel) ClassicProject.g().getTrgt("NonTerminatingM");
			relNonTerminatingM.load();
			Iterable<jq_Method> tuples = relNonTerminatingM.getAry1ValTuples();
			for (jq_Method t : tuples) {
				nonTerminatingM.add(t);
			}
			relNonTerminatingM.close();
		}

		//build (invoke instruction, allocSite) to callee map
		//&& build (invoke instruction, callee) to allocSites map
		{
		//	IHtoM = new HashMap<Pair<Quad,Quad>, Set<jq_Method>>();
			IMtoH = new HashMap<Pair<Quad,jq_Method>, BitSet>();
			ProgramRel relIHM = (ProgramRel) ClassicProject.g().getTrgt("IHM");
			relIHM.load();

			Iterable<Trio<Quad, Quad, jq_Method>> tuples = relIHM.getAry3ValTuples();
			for (Trio<Quad, Quad, jq_Method> t : tuples){
		/*		//IHM
				Pair<Quad, Quad> p = new Pair<Quad, Quad>(t.val0, t.val1);
				Set<jq_Method> callees = IHtoM.get(p);
				if (callees == null) {
					callees = new ArraySet<jq_Method>();
					IHtoM.put(p, callees);
				}
				callees.add(t.val2);
		*/		
				//IMH
				Pair<Quad, jq_Method> p2 = new Pair<Quad, jq_Method>(t.val0, t.val2);
				BitSet allocSites = IMtoH.get(p2);
				if (allocSites == null) {
					allocSites = new BitSet();
					IMtoH.put(p2, allocSites);
				}
				allocSites.set(domH.indexOf(t.val1));
			}
			relIHM.close();
		}
		
		//build FHMap
		{
			FHMap = new HashMap<jq_Field, BitSet>();
			if(!isHeapEditable){
				ProgramRel relFH = (ProgramRel) ClassicProject.g().getTrgt("FH");
				relFH.load();

				Iterable<Pair<jq_Field, Quad>> tuples = relFH.getAry2ValTuples();
				for (Pair<jq_Field, Quad> t : tuples){
					BitSet pointsTo = FHMap.get(t.val0);
					if (pointsTo == null) {
						pointsTo = new BitSet();
						FHMap.put(t.val0, pointsTo);
					}
					pointsTo.set(domH.indexOf(t.val1));
				}
				relFH.close();
			}
		}

		//build HFHMap
		{
			HFHMap = new HashMap<Pair<Integer,jq_Field>, BitSet>();
			if(!isHeapEditable){
				ProgramRel relHFH = (ProgramRel) ClassicProject.g().getTrgt("HFH");
				relHFH.load();

				Iterable<Trio<Quad, jq_Field, Quad>> tuples = relHFH.getAry3ValTuples();
				for (Trio<Quad, jq_Field, Quad> t : tuples){
					Pair<Integer, jq_Field> p = new Pair<Integer, jq_Field>(domH.indexOf(t.val0), t.val1);							
					BitSet pointsTo = HFHMap.get(p);
					if (pointsTo == null) {
						pointsTo = new BitSet();
						HFHMap.put(p, pointsTo);
					}
					pointsTo.set(domH.indexOf(t.val2));
				}
				relHFH.close();
			}
		}
		
		//build VTHFilter
		{
			THFilterMap = new HashMap<jq_Type,BitSet>();
			ProgramRel relTHFilter = (ProgramRel) ClassicProject.g().getTrgt("THfilter");
			relTHFilter.load();

			Iterable<Pair<jq_Type, Quad>> tuples = relTHFilter.getAry2ValTuples();
			for (Pair<jq_Type, Quad> t : tuples){
				BitSet filterSites = THFilterMap.get(t.val0);
				if (filterSites == null) {
					filterSites = new BitSet();
					THFilterMap.put(t.val0, filterSites);
				}
				filterSites.set(domH.indexOf(t.val1));
			}
			relTHFilter.close();
		}

		//build conNewInstIH
		{
			conNewInstIHMap = new HashMap<Quad,Quad>();
			if(handleReflection){
				ProgramRel relConNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIH");
				relConNewInstIH.load();

				Iterable<Pair<Quad, Quad>> tuples = relConNewInstIH.getAry2ValTuples();
				for (Pair<Quad, Quad> t : tuples){
					conNewInstIHMap.put(t.val0, t.val1);
				}
				relConNewInstIH.close();
			}
		}

		//build objNewInstIH
		{
			objNewInstIHMap = new HashMap<Quad,Quad>();
			if(handleReflection){
				ProgramRel relObjNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIH");
				relObjNewInstIH.load();

				Iterable<Pair<Quad, Quad>> tuples = relObjNewInstIH.getAry2ValTuples();
				for (Pair<Quad, Quad> t : tuples){
					objNewInstIHMap.put(t.val0, t.val1);
				}
				relObjNewInstIH.close();
			}
		}

		//build aryNewInstIH
		{
			aryNewInstIHMap = new HashMap<Quad,Quad>();
			if(handleReflection){
				ProgramRel relAryNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("aryNewInstIH");
				relAryNewInstIH.load();

				Iterable<Pair<Quad, Quad>> tuples = relAryNewInstIH.getAry2ValTuples();
				for (Pair<Quad, Quad> t : tuples){
					aryNewInstIHMap.put(t.val0, t.val1);
				}
				relAryNewInstIH.close();
			}
		}		
		
		//build conNewInstIM
		{
			conNewInstIMMap = new HashMap<Quad,Set<jq_Method>>();
			if(handleReflection){
				ProgramRel relConNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIM");
				relConNewInstIM.load();

				Iterable<Pair<Quad, jq_Method>> tuples = relConNewInstIM.getAry2ValTuples();
				for (Pair<Quad, jq_Method> t : tuples){
					Set<jq_Method> callees = conNewInstIMMap.get(t.val0);
					if (callees == null) {
						callees = new ArraySet<jq_Method>();
						conNewInstIMMap.put(t.val0, callees);
					}
					callees.add(t.val1);
				}
				relConNewInstIM.close();
			}
		}

		//build objNewInstIM
		{
			objNewInstIMMap = new HashMap<Quad,Set<jq_Method>>();
			if(handleReflection){
				ProgramRel relObjNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIM");
				relObjNewInstIM.load();

				Iterable<Pair<Quad, jq_Method>> tuples = relObjNewInstIM.getAry2ValTuples();
				for (Pair<Quad, jq_Method> t : tuples){
					Set<jq_Method> callees = objNewInstIMMap.get(t.val0);
					if (callees == null) {
						callees = new ArraySet<jq_Method>();
						objNewInstIMMap.put(t.val0, callees);
					}
					callees.add(t.val1);
				}
				relObjNewInstIM.close();
			}
		}

		//build reflectIHMap
		{
			reflectIHMap = new HashMap<Quad,Quad>();
			reflectIHMap.putAll(conNewInstIHMap);
			reflectIHMap.putAll(objNewInstIHMap);
			reflectIHMap.putAll(aryNewInstIHMap);
		}

		//build reflectIMMap
		{
			reflectIMMap = new HashMap<Quad,Set<jq_Method>>();
			reflectIMMap.putAll(conNewInstIMMap);
			reflectIMMap.putAll(objNewInstIMMap);
		}		

		MtoM = new HashMap<jq_Method, Set<jq_Method>>();
		MToMEdges = new HashSet<Pair<jq_Method,jq_Method>>();
		CICM = new HashMap<Pair<BitAbstractState,Quad>, Set<Pair<BitAbstractState,jq_Method>>>();
		qv = new InfiCFAAllocQuadVisitor(THFilterMap, FHMap, HFHMap, trackedAlloc, domH, isHeapEditable, useExtraFilters);
		queryGenTimer = new Timer("allocEnvCFA-QueryGen");
	}

	@Override
	public void run() {
		init();
		System.out.println("Initialization done");

		if(isHeapEditable){
			FHMap.clear();
			HFHMap.clear();
		}
		MtoM.clear();
		MToMEdges.clear();
		
		int passNum = 0;
		do{
			callGraph = new MutableGraph<Pair<jq_Method,BitAbstractState>>();
			qv.isHeapModified = false;
			timer = new Timer("allocEnvCFA-SinglePass");
			timer.init();
			runPass();
			timer.done();
			System.out.println("RHS Pass " + passNum++ + " done");
			long inclusiveTime = timer.getInclusiveTime();
			System.out.println("Total running time: "
					+ Timer.getTimeStr(inclusiveTime));
		}while(qv.isHeapModified);
		
		queryGenTimer.init();
		
		if (DEBUG) print();
		generateQueries();
		
		queryGenTimer.done();
		long inclusiveTime = queryGenTimer.getInclusiveTime();
		System.out.println("Total time for generating queries: "
				+ Timer.getTimeStr(inclusiveTime));
		
		
		done();
	}

	protected void generateQueries(){
		System.out.println("ENTER generateQueries");

		computeTransitiveClosure(callGraph);

		ProgramDom domM = (ProgramDom) ClassicProject.g().getTrgt("M");
		int numM = domM.size();
		int numReachFromM[] = new int[numM];
		PrintWriter out = OutDirUtils.newPrintWriter("reachMM_CFA2.txt");
		for(jq_Method m : MtoM.keySet()){
			numReachFromM[domM.indexOf(m)] += MtoM.get(m).size();
			for(jq_Method m2: MtoM.get(m)){
				out.println(m + " :: " + m2);
			}
		}
		out.close();
		
		long numPaths = 0;
		PrintWriter out2 = OutDirUtils.newPrintWriter("results_CFA2.txt");
		for (int m = 0; m < numM; m++) {
			out2.println(numReachFromM[m] + " " + domM.get(m));
			numPaths+=numReachFromM[m];
		}
		out2.close();
		
		Set<jq_Method> allReachableM = new HashSet<jq_Method>();
		for(jq_Method m : rootM){
			allReachableM.add(m);
		}
		
		PrintWriter out3 = OutDirUtils.newPrintWriter("edgeMM_CFA2.txt");
		for(Pair<jq_Method,jq_Method> p : MToMEdges){
			allReachableM.add(p.val0);
			allReachableM.add(p.val1);
			out3.println(p.val0 + " :: " + p.val1);
		}
		out3.close();
		
		PrintWriter out4 = OutDirUtils.newPrintWriter("reachableM_CFA2.txt");
		for (jq_Method m : allReachableM) {
			out4.println(m);
		}
		out4.close();
		
		PrintWriter out5 = OutDirUtils.newPrintWriter("summCnt_CFA2.txt");
		for (int m = 0; m < numM; m++) {
			jq_Method currM = (jq_Method) domM.get(m);
			int summCnt = (summEdges.get(currM)==null) ? 0 : summEdges.get(currM).size();
			out5.println(summCnt + " " + currM);
		}
		out5.close();

		System.out.println("CallGraph Statistics: NumPaths= " + numPaths + ", NumEdges= " + MToMEdges.size());
		System.out.println("EXIT generateQueries");
	}

	protected void computeTransitiveClosure(MutableGraph<Pair<jq_Method,BitAbstractState>> callGraph){
		Set<Pair<jq_Method, BitAbstractState>> allNodes = callGraph.getNodes();
		ArrayList<Pair<jq_Method, BitAbstractState>> nodesArr = new ArrayList<Pair<jq_Method,BitAbstractState>>(allNodes);

		int numNodes = allNodes.size();

		boolean adjMat[][] = new boolean[allNodes.size()][allNodes.size()];
		boolean pathMat[][] = new boolean[allNodes.size()][allNodes.size()];

		for(Pair<jq_Method, BitAbstractState> node : allNodes){
			int i = nodesArr.indexOf(node);
			for(Pair<jq_Method, BitAbstractState> succNode : callGraph.getSuccs(node)){
				MToMEdges.add(new Pair<jq_Method,jq_Method>(node.val0,succNode.val0));
				int j = nodesArr.indexOf(succNode);
				adjMat[i][j] = true;
				pathMat[i][j] = true;
			}
		}

		for(int k = 0; k < numNodes; k++){
			for(int i = 0;i < numNodes; i++){
				if(pathMat[i][k]){
					for(int j = 0; j < numNodes; j++){
						if(pathMat[k][j])
							pathMat[i][j] = true;
					}
				}
			}
		}

		for(int i = 0; i < numNodes; i++){
			Pair<jq_Method, BitAbstractState> srcNode = nodesArr.get(i);
			Set<jq_Method> mList = MtoM.get(srcNode.val0);
			if(mList == null){
				mList = new HashSet<jq_Method>();
				MtoM.put(srcNode.val0, mList);
			}

			for(int j = 0;j < numNodes; j++){
				if(pathMat[i][j]){
					mList.add(nodesArr.get(j).val0);
				}
			}
		}
	}

/***********************************CallGraph Handling**********************************************/
	@Override
	public ICICG getCallGraph() {
		return cicg;
	}

	@Override
	protected boolean jumpToMethodEnd(Quad q, jq_Method m, BitEdge<Quad> predPe, BitEdge<Quad> pe){
    	if(nonTerminatingM.contains(m))
    		return true;
    	return false;
    }
	
	@Override
	protected Set<Quad> getCallers(jq_Method m) {
		Set<Quad> callers = callersMap.get(m);
	    if (callers == null) {
	        callers = new ArraySet<Quad>();
	        callersMap.put(m, callers);
	    }
	    return callers;
    }
	
	@Override
	protected Set<jq_Method> getTargets(Quad i, BitEdge<Quad> pe) {
		assert(pe.dstNode != null);
		Set<jq_Method> targets = super.getTargets(i, pe);
		Set<jq_Method> correctTargets = new ArraySet<jq_Method>();
		
		//Remove incorrect targets
		for(jq_Method m : targets){
			if(isCorrectTarget(i, m, pe, new BitSet()))
				correctTargets.add(m);
		}
		
		//Handle reflection
		Set<jq_Method> reflectTargets = reflectIMMap.get(i);
		if(reflectTargets != null)
			correctTargets.addAll(reflectTargets);
		
		//Update callersMap
		for(jq_Method m : correctTargets){
			Set<Quad> callers = callersMap.get(m);
			if (callers == null) {
				callers = new ArraySet<Quad>();
				callersMap.put(m, callers);
			}
			callers.add(i);
		}
		return correctTargets;
	}
	
	//Returns if the provided target is a correct one for the given edge and callsite. If it is and if the 
	//method is neither static nor special, the set callerVarPtsToFiltered is populated with the ptsTo
	//information for "this" argument of the callee
	protected boolean isCorrectTarget(Quad i, jq_Method m, BitEdge<Quad> pe, BitSet callerVarPtsToFiltered){
		assert(callerVarPtsToFiltered != null);
		
		//Handle reflection
		Set<jq_Method> reflectTargets = reflectIMMap.get(i);
		if(reflectTargets != null && reflectTargets.contains(m)){
			callerVarPtsToFiltered.clear();
			callerVarPtsToFiltered.set(domH.indexOf(reflectIHMap.get(i)));
			return true;
		}
		
		boolean isStaticInvk = false, isSpecInvk = false, isCorrectTarget = false;
		Operator op = i.getOperator();
		if(op instanceof InvokeStatic){
			if(m.isStatic())
				isStaticInvk = true;
			else
				isSpecInvk = true;
		}

		if(isStaticInvk){
			isCorrectTarget = true;
		}else{
			Register callerVar = Invoke.getParam(i, 0).getRegister();
			BitSet callerVarPtsTo = pe.dstNode.envLocal.get(callerVar);
			if(callerVarPtsTo != null){
				if(isSpecInvk){
					isCorrectTarget = true;
				}else{
					callerVarPtsToFiltered.clear();
					BitSet filterSet = IMtoH.get(new Pair<Quad, jq_Method>(i, m));
					if(filterSet != null){	
						callerVarPtsToFiltered.or(callerVarPtsTo);
						callerVarPtsToFiltered.and(filterSet);
					}
					if(callerVarPtsTo.get(0)) callerVarPtsToFiltered.set(0);
					if(!callerVarPtsToFiltered.isEmpty()) isCorrectTarget = true;
				}
			}
		}
		return isCorrectTarget;
	}

/*************************************************************************************************/	
	@Override
	public Set<Pair<Loc, BitEdge<Quad>>> getInitPathEdges() {
		Set<Pair<Loc, BitEdge<Quad>>> initPEs = new ArraySet<Pair<Loc, BitEdge<Quad>>>();
		for(jq_Method m : rootM){
			BasicBlock bb = m.getCFG().entry();
			Loc loc = new Loc(bb, -1);
			Pair<Loc, BitEdge<Quad>> pair = new Pair<Loc, BitEdge<Quad>>(loc, new BitEdge<Quad>());
			if (DEBUG) System.out.println("getInitPathEdges: Added " + pair);
			initPEs.add(pair);

			callGraph.insertRoot(new Pair<jq_Method, BitAbstractState>(m, pair.val1.srcNode));
		}
		//jq_Method m = Program.g().getMainMethod();
		return initPEs;
	}
	
	@Override
	public BitEdge<Quad> getSkipMethodEdge(Quad q, BitEdge<Quad> pe) {
		assert(pe.dstNode != null);
		if (DEBUG) System.out.println("ENTER getSkipMethodEdge: q=" + q + " pe=" + pe);

		BitEnv<Register> newEnv = null;
		Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q).getRegister() : null;

		if(tgtRetReg != null ){			
			newEnv = new BitEnv<Register>(pe.dstNode.envLocal);
			//There is no real need to handle reflection inside getSkipMethodEdge since any sites with reflection
			//will always have a target method. For aryNewInst type instruction, the target would be
			//the static method newInstance() in java.lang.reflect.Array. However, to be extra safe we do handle it here.
			Quad reflectSite = reflectIHMap.get(q);
			if(reflectSite != null){
				jq_Type tgtRetRegType = Invoke.getDest(q).getType();
				tgtRetRegType = tgtRetRegType != null ? tgtRetRegType : javaLangObject;
				BitSet dstFiltered = new BitSet();
				dstFiltered.set(domH.indexOf(reflectSite));
				BitSet filterSet = THFilterMap.get(tgtRetRegType);
				if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				if(!dstFiltered.isEmpty())
					newEnv.insert(tgtRetReg, dstFiltered);
				else if(newEnv.remove(tgtRetReg) == null)
					newEnv = pe.dstNode.envLocal;
			}else if(newEnv.remove(tgtRetReg) == null)
				newEnv = pe.dstNode.envLocal;
		}else{
			newEnv = pe.dstNode.envLocal;
		}
		
		BitAbstractState newDst = (newEnv==pe.dstNode.envLocal) ? pe.dstNode : new BitAbstractState(newEnv);
		BitEdge<Quad> newEdge = new BitEdge<Quad>(pe.srcNode, newDst);

		if (DEBUG) System.out.println("LEAVE getSkipMethodEdge: " + newEdge);
		return newEdge;
	}
	
	/*
	 * If incoming path edge 'pe' is of the form <AS', AS> then
	 * create and return new path edge in callee of the form <AS1,AS2> where
	 * AS1 and AS2 are as follows:
	 *   1. AS1 = AS2 
	 *   2. LocalMap of AS1 contains entries for all v such that v is an actual argument of the method (now
	 *   replaced by the corresponding formal argument)
	 */
	@Override
	public BitEdge<Quad> getInitPathEdge(Quad q, jq_Method m, BitEdge<Quad> pe) {
		assert(pe.dstNode != null);
		if (DEBUG) System.out.println("ENTER getInitPathEdge: q=" + q + " m=" + m + " pe=" + pe);

		BitEnv<Register> newAC = generateInitSrcState(q, m, pe, null);
		BitAbstractState newSrc;
		
		//If 0cfa, use empty src state for all initial edges of methods. This ensures that edges always
		//have the same src state and as a consequence, always merge.
		if(is0CFA)
			newSrc = new BitAbstractState(new BitEnv<Register>());
		else
			newSrc = new BitAbstractState(newAC);
		
		BitAbstractState newDst = new BitAbstractState(newAC);
		BitEdge<Quad> newEdge = new BitEdge<Quad>(newSrc, newDst);

		Pair<jq_Method, BitAbstractState> cgSrcNode =  new Pair<jq_Method, BitAbstractState>(q.getMethod(), pe.srcNode);
		Pair<jq_Method, BitAbstractState> cgDstNode = new Pair<jq_Method, BitAbstractState>(m, newSrc);
		callGraph.insertEdge(cgSrcNode, cgDstNode);

		
		Pair<BitAbstractState, Quad> CICMSrc =  new Pair<BitAbstractState, Quad>(pe.srcNode, q);
		Set<Pair<BitAbstractState, jq_Method>> CICMDstSet = CICM.get(CICMSrc);
		if(CICMDstSet == null){
			CICMDstSet = new HashSet<Pair<BitAbstractState,jq_Method>>();
			CICM.put(CICMSrc, CICMDstSet);
		}
		CICMDstSet.add(new Pair<BitAbstractState, jq_Method>(newSrc, m));
		
		
		if (DEBUG) System.out.println("LEAVE getInitPathEdge: " + newEdge);
		return newEdge;
	}

	//Generate new src state for a callee m at callsite q given input edge pe. The set callerVarPtsToFiltered is
	//populated with the ptsTo information, if available, for "this" argument of the callee
	protected BitEnv<Register> generateInitSrcState(Quad q, jq_Method m, BitEdge<Quad> pe, BitSet callerVarPtsToFiltered){
		assert(pe.dstNode != null);
		boolean isStatic = false, isReflect = false;
		
		Operator op = q.getOperator();
		if(op instanceof InvokeStatic)
			isStatic = true;
		
		//Handle reflection
		Set<jq_Method> reflectTargets = reflectIMMap.get(q);
		if(reflectTargets != null && reflectTargets.contains(m)){
			isReflect = true;
			if(callerVarPtsToFiltered == null){
				callerVarPtsToFiltered = new BitSet();
				callerVarPtsToFiltered.set(domH.indexOf(reflectIHMap.get(q)));
			}
		}else{
			if(!isStatic && callerVarPtsToFiltered == null && useExtraFilters){
				Register callerVar = Invoke.getParam(q, 0).getRegister();
				BitSet callerVarPtsTo = pe.dstNode.envLocal.get(callerVar);
				assert(callerVarPtsTo != null); //This function shouldn't be invoked unless its the correct target 
				callerVarPtsToFiltered = new BitSet();
				BitSet filterSet = IMtoH.get(new Pair<Quad, jq_Method>(q, m));
				if(filterSet != null){	
					callerVarPtsToFiltered.or(callerVarPtsTo);
					callerVarPtsToFiltered.and(filterSet);
				}
				if(callerVarPtsTo.get(0)) callerVarPtsToFiltered.set(0);
				assert(!callerVarPtsToFiltered.isEmpty()); //This function shouldn't be invoked unless its the correct target
			}
		}

		BitAbstractState oldDst = pe.dstNode;
		BitEnv<Register> newTC = new BitEnv<Register>();

		// Build newTC as follows:
		// for each <r1,{t1,t2,...,tn}> in the oldTC where r1 is an actual arg of q, add <r2,{t1,t2,...,tn}> where r2 is
		// the corresponding formal arg of m.
		// However, to handle the "this"/0th argument, for the corresponding <r1,{t1,t2,...,tn}> in the oldTC, add <r2,{tn}> where
		// tn is the type that causes the target method to be invoked
		// 
		ParamListOperand args = Invoke.getParamList(q);
		RegisterFactory rf = m.getCFG().getRegisterFactory();
		jq_Type[] paramTypes = m.getParamTypes();
		for (int i = 0; i < args.length(); i++) {
			Register actualReg = args.get(i).getRegister();
			Register formalReg = rf.get(i);
			BitSet paramVarPtsTo;
			if(i == 0 && ((!isStatic && useExtraFilters)  || isReflect)){
				paramVarPtsTo = callerVarPtsToFiltered;
			}else if(i == 1 && isReflect && conNewInstIMMap.get(q)!=null){
				BitSet actualVarPtsTo = oldDst.envLocal.get(actualReg);
				paramVarPtsTo = null;
				if(actualVarPtsTo != null){
					for (int quad = actualVarPtsTo.nextSetBit(0); quad >= 0; quad = actualVarPtsTo.nextSetBit(quad+1)) {
						if(quad == 0){
							if(paramVarPtsTo == null) paramVarPtsTo = new BitSet();
							paramVarPtsTo.set(0);
						}else{
							Pair<Integer, jq_Field> pair = new Pair<Integer, jq_Field>(quad, null);
							BitSet fieldPointsTo = HFHMap.get(pair);
							if(fieldPointsTo != null){
								if(paramVarPtsTo == null) paramVarPtsTo = new BitSet();
								paramVarPtsTo.or(fieldPointsTo);
							}
						}
					}
				}
				if(paramVarPtsTo != null){
					//If the heap is not editable, the pointsTo information in the heap
					//is obtained via 0cfa and might contain untracked allocSites. This
					//necessitates checking if all the sites added to paramVarPtsTo are tracked
					// and if not, adding the null site to paramVarPtsTo
					int cardinality = paramVarPtsTo.cardinality();
					paramVarPtsTo.and(trackedAlloc);
					cardinality = cardinality - paramVarPtsTo.cardinality();
					if(cardinality > 0)
						paramVarPtsTo.set(0);
				}
			}else if(isReflect){
				paramVarPtsTo = null;
			}else
				paramVarPtsTo = oldDst.envLocal.get(actualReg);
				
			if(paramVarPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(paramVarPtsTo);
				BitSet filterSet = THFilterMap.get(paramTypes[i]);
				if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				if(!dstFiltered.isEmpty())
					newTC.insert(formalReg, dstFiltered);
			}
		}

		return newTC;
	}

	@Override
	public BitEdge<Quad> getMiscPathEdge(Quad q, BitEdge<Quad> pe) {
		if (DEBUG) System.out.println("ENTER getMiscPathEdge: q=" + q + " pe=" + pe);
		
		qv.istate = pe.dstNode;
		qv.ostate = pe.dstNode;
		assert(qv.ostate != null && qv.istate != null);
		// may modify only qv.ostate
		q.accept(qv);
		assert(qv.ostate != null);
		// XXX: DO NOT REUSE incoming PE (merge does strong updates)
		BitEdge<Quad> newEdge = new BitEdge<Quad>(pe.srcNode, qv.ostate);

		if (DEBUG) System.out.println("LEAVE getMiscPathEdge: ret=" + newEdge);
		return newEdge;
	}

	@Override
	public BitEdge<Quad> getInvkPathEdge(Quad q, BitEdge<Quad> clrPE, jq_Method m, BitEdge<Quad> tgtSE) {
		assert(clrPE.dstNode != null && tgtSE.dstNode != null);
		if (DEBUG) System.out.println("ENTER getInvkPathEdge: q=" + q + " clrPE=" + clrPE + " m=" + m + " tgtSE=" + tgtSE);

		//If 0cfa:
		//1. No need to check if the target is correct. Its assured that all the callsites queried actually invoked 
		//	this method since we update the callersMap on the fly. Also, at each site there will be just one pathedge
		//2. No need to compare clrPE and tgtSE states. With 0cfa, any tgtSE matches any clrPE.
		if(!is0CFA){
			//Following check is necessary since each callsite, though a correct one for method m,
			//could have many incoming edges that can't invoked this method
			BitSet callerVarPtsToFiltered = new BitSet();
			if(!isCorrectTarget(q, m, clrPE, callerVarPtsToFiltered)){
				if (DEBUG) System.out.println("LEAVE getInvkPathEdge: null (Incorrect target for clrPE)");
				return null;
			}
			// Compare envLocals; they should be equal in order to apply summary
			// Build this local tmpEnv as follows:
			BitEnv<Register> tmpEnv = generateInitSrcState(q, m, clrPE,callerVarPtsToFiltered);

			if (!(tgtSE.srcNode.envLocal.equals(tmpEnv))) {
				if (DEBUG) System.out.println("LEAVE getInvkPathEdge: null (contexts don't match)");
				return null;
			}
		}

		BitEnv<Register> newEnv = null;
		Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q).getRegister() : null;
		if(tgtRetReg != null ){
			jq_Type tgtRetRegType = Invoke.getDest(q).getType();
			tgtRetRegType = tgtRetRegType != null ? tgtRetRegType : javaLangObject;
			BitSet dstFiltered = new BitSet();
			
			//Handle reflection
			Quad reflectSite = reflectIHMap.get(q);
			if(reflectSite != null)
				dstFiltered.set(domH.indexOf(reflectSite));
			
			dstFiltered.or(tgtSE.dstNode.returnVarEnv);
			BitSet filterSet = THFilterMap.get(tgtRetRegType);
			if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
			newEnv = new BitEnv<Register>(clrPE.dstNode.envLocal);

			if(!dstFiltered.isEmpty()){
				newEnv.insert(tgtRetReg, dstFiltered);
			}else{
				if(newEnv.remove(tgtRetReg) == null)
					newEnv = clrPE.dstNode.envLocal;
			}
		}else{
			newEnv = clrPE.dstNode.envLocal;
		}

		BitAbstractState newDst = (newEnv==clrPE.dstNode.envLocal) ? clrPE.dstNode : new BitAbstractState(newEnv); 
		BitEdge<Quad> newEdge = new BitEdge<Quad>(clrPE.srcNode, newDst);

		if (DEBUG) System.out.println("LEAVE getInvkPathEdge: " + newEdge);
		return newEdge;
		

	}

	@Override
	public BitEdge<Quad> getPECopy(BitEdge<Quad> pe) { return getCopy(pe); }

	@Override
	public BitEdge<Quad> getSECopy(BitEdge<Quad> se) { return getCopy(se); }

	protected BitEdge<Quad> getCopy(BitEdge<Quad> pe) {
		assert(pe.srcNode != null && pe.dstNode != null);
		if (DEBUG) System.out.println("Called Copy with: " + pe);
		return new BitEdge<Quad>(pe.srcNode, pe.dstNode);
	}

	@Override
	public BitEdge<Quad> getSummaryEdge(jq_Method m, BitEdge<Quad> pe) {
		assert(pe.srcNode != null && pe.dstNode != null);
		if (DEBUG) System.out.println("\nCalled getSummaryEdge: m=" + m + " pe=" + pe);
		return getCopy(pe);
	}
}