package chord.analyses.inficfa.type;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import joeq.Class.jq_Array;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Reference.jq_NullType;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.inficfa.AbstractState;
import chord.analyses.inficfa.BitAbstractState;
import chord.analyses.inficfa.Edge;
import chord.analyses.inficfa.Env;
import chord.analyses.inficfa.InfiCFARHSAnalysis;
import chord.program.Loc;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.util.ArraySet;
import chord.util.Timer;
import chord.util.Utils;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.program.Program;

@Chord(name = "typeEnvCFA-java", consumes = { "M", "HT", "VH", "FH", "HFH", "ITM", "sub", "rootM", "NonTerminatingM",
		"conNewInstIH", "conNewInstIM", "objNewInstIH", "objNewInstIM", "aryNewInstIH"})
public class TypeEnvCFAAnalysis extends InfiCFARHSAnalysis<Edge<jq_Type>, Edge<jq_Type>> {
	protected static boolean DEBUG = false;
	protected CIPAAnalysis cipa;
	protected ICICG cicg;
	protected MyQuadVisitor qv = new MyQuadVisitor();
	private boolean isInit;
	protected Set<Register> trackedVar;
//	protected Map<Pair<Quad,jq_Type>, jq_Method> ITtoM;
	protected Map<Pair<Quad,jq_Method>, Set<jq_Type>> IMtoT;
	protected MutableGraph<Pair<jq_Method,AbstractState<jq_Type>>> callGraph;
	protected Map<jq_Method, Set<jq_Method>> MtoM;
	protected Set<Pair<jq_Method, jq_Method>> MToMEdges;
	protected Map<Register, Set<Quad>> VHMap;
	protected Map<jq_Field, Set<Quad>> FHMap;
	protected Map<Pair<Quad, jq_Field>, Set<Quad>> HFHMap;
	protected Map<Quad, Set<jq_Type>> HTMap;
	protected HashMap<jq_Type,Set<jq_Type>> TTFilterMap;
	protected Map<Quad,Quad> conNewInstIHMap;
	protected Map<Quad,Quad> objNewInstIHMap;
	protected Map<Quad,Quad> aryNewInstIHMap;
	protected Map<Quad,Set<jq_Method>> conNewInstIMMap;
	protected Map<Quad,Set<jq_Method>> objNewInstIMMap;
	protected Map<Quad,Quad> reflectIHMap;
	protected Map<Quad,Set<jq_Method>> reflectIMMap;
	protected Map <Pair<AbstractState<jq_Type>, Quad>, Set<Pair<AbstractState<jq_Type>, jq_Method>>> CICM;
	protected jq_Reference javaLangObject;
	protected Set<jq_Method> rootM;
	protected Set<jq_Method> nonTerminatingM;
	protected boolean handleReflection;
	protected boolean useExtraFilters;
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
		
		handleReflection = Utils.buildBoolProperty("chord.typeEnvCFA.handleReflection", false);
		useExtraFilters = Utils.buildBoolProperty("chord.typeEnvCFA.useExtraFilters", false);
		
		// build set trackedVar
		{
			trackedVar = new HashSet<Register>();
			ProgramRel relTrackedVar = (ProgramRel) ClassicProject.g().getTrgt("trackedVar");
			ClassicProject.g().runTask(relTrackedVar);
			relTrackedVar.load();
			for (Object var : relTrackedVar.getAry1ValTuples()) {
				trackedVar.add((Register) var);
			}
			relTrackedVar.close();
		}

		//built set rootM
		{
			rootM = new HashSet<jq_Method>();
			ProgramRel relRootM = (ProgramRel) ClassicProject.g().getTrgt("rootM");
			relRootM.load();
			for (Object var : relRootM.getAry1ValTuples()) {
				rootM.add((jq_Method) var);
			}
			relRootM.close();
		}

		//built set NonTerminatingM
		{
			nonTerminatingM = new HashSet<jq_Method>();
			ProgramRel relNonTerminatingM = (ProgramRel) ClassicProject.g().getTrgt("NonTerminatingM");
			relNonTerminatingM.load();
			for (Object var : relNonTerminatingM.getAry1ValTuples()) {
				nonTerminatingM.add((jq_Method) var);
			}
			relNonTerminatingM.close();
		}		
		
		//build (invoke instruction, callerType) to callee map
		//&& build (invoke instruction, callee) to callerTypes map
		{
		//	ITtoM = new HashMap<Pair<Quad,jq_Type>, jq_Method>();
			IMtoT = new HashMap<Pair<Quad,jq_Method>, Set<jq_Type>>();
			ProgramRel relITM = (ProgramRel) ClassicProject.g().getTrgt("ITM");
			relITM.load();

			Iterable<Trio<Quad, jq_Type, jq_Method>> tuples = relITM.getAry3ValTuples();
			for (Trio<Quad, jq_Type, jq_Method> t : tuples){
			/*	Pair<Quad, jq_Type> p = new Pair<Quad, jq_Type>(t.val0, t.val1);
				jq_Method callee = ITtoM.get(p);
				if (callee == null) {
					ITtoM.put(p, t.val2);
				}else
					assert(callee == t.val2);
			*/	
				Pair<Quad, jq_Method> p2 = new Pair<Quad, jq_Method>(t.val0, t.val2);
				Set<jq_Type> types = IMtoT.get(p2);
				if (types == null) {
					types = new ArraySet<jq_Type>();
					IMtoT.put(p2, types);
				}
				types.add(t.val1);				
			}
			relITM.close();
		}

		//build VHMap
		{
			VHMap = new HashMap<Register, Set<Quad>>();
			ProgramRel relVH = (ProgramRel) ClassicProject.g().getTrgt("VH");
			relVH.load();

			Iterable<Pair<Register, Quad>> tuples = relVH.getAry2ValTuples();
			for (Pair<Register, Quad> t : tuples){
				Set<Quad> pointsTo = VHMap.get(t.val0);
				if (pointsTo == null) {
					pointsTo = new ArraySet<Quad>();
					VHMap.put(t.val0, pointsTo);
				}
				pointsTo.add(t.val1);
			}
			relVH.close();
		}

		//build FHMap
		{
			FHMap = new HashMap<jq_Field, Set<Quad>>();
			ProgramRel relFH = (ProgramRel) ClassicProject.g().getTrgt("FH");
			relFH.load();

			Iterable<Pair<jq_Field, Quad>> tuples = relFH.getAry2ValTuples();
			for (Pair<jq_Field, Quad> t : tuples){
				Set<Quad> pointsTo = FHMap.get(t.val0);
				if (pointsTo == null) {
					pointsTo = new ArraySet<Quad>();
					FHMap.put(t.val0, pointsTo);
				}
				pointsTo.add(t.val1);
			}
			relFH.close();
		}

		//build HFHMap
		{
			HFHMap = new HashMap<Pair<Quad,jq_Field>, Set<Quad>>();
			ProgramRel relHFH = (ProgramRel) ClassicProject.g().getTrgt("HFH");
			relHFH.load();

			Iterable<Trio<Quad, jq_Field, Quad>> tuples = relHFH.getAry3ValTuples();
			for (Trio<Quad, jq_Field, Quad> t : tuples){
				Pair<Quad, jq_Field> p = new Pair<Quad, jq_Field>(t.val0, t.val1);
				Set<Quad> pointsTo = HFHMap.get(p);
				if (pointsTo == null) {
					pointsTo = new ArraySet<Quad>();
					HFHMap.put(p, pointsTo);
				}
				pointsTo.add(t.val2);
			}
			relHFH.close();
		}

		//build HTMap
		{
			HTMap = new HashMap<Quad, Set<jq_Type>>();
			ProgramRel relHT = (ProgramRel) ClassicProject.g().getTrgt("HT");
			relHT.load();

			Iterable<Pair<Quad, jq_Type>> tuples = relHT.getAry2ValTuples();
			for (Pair<Quad, jq_Type> t : tuples){
				Set<jq_Type> allocType = HTMap.get(t.val0);
				if (allocType == null) {
					allocType = new ArraySet<jq_Type>();
					HTMap.put(t.val0, allocType);
				}
				allocType.add(t.val1);
			}
			relHT.close();
		}
		
		//build VTTFilter
		{
			TTFilterMap = new HashMap<jq_Type,Set<jq_Type>>();
			ProgramRel relTTFilter = (ProgramRel) ClassicProject.g().getTrgt("sub");
			relTTFilter.load();

			Iterable<Pair<jq_Type, jq_Type>> tuples = relTTFilter.getAry2ValTuples();
			for (Pair<jq_Type, jq_Type> t : tuples){
				Set<jq_Type> filterTypes = TTFilterMap.get(t.val1);
				if (filterTypes == null) {
					filterTypes = new ArraySet<jq_Type>();
					TTFilterMap.put(t.val1, filterTypes);
					//filterTypes.add(null); //No need to add this since sub relation already
					//has jq_NullType.NULL_TYPE as subtype of all types
				}
				filterTypes.add(t.val0);
			}
			relTTFilter.close();
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
		CICM = new HashMap<Pair<AbstractState<jq_Type>,Quad>, Set<Pair<AbstractState<jq_Type>,jq_Method>>>();
		queryGenTimer = new Timer("allocEnvCFA-QueryGen");
	}

	@Override
	public void run() {
		init();
		MtoM.clear();
		MToMEdges.clear();
		callGraph = new MutableGraph<Pair<jq_Method,AbstractState<jq_Type>>>();
		runPass();
		
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
		PrintWriter out2 = OutDirUtils.newPrintWriter("reachMM_CFA2.txt");
		for(jq_Method m : MtoM.keySet()){
			numReachFromM[domM.indexOf(m)] += MtoM.get(m).size();
			for(jq_Method m2: MtoM.get(m)){
				out2.println(m + " :: " + m2);
			}
		}
		out2.close();

		long numPaths = 0;
		PrintWriter out = OutDirUtils.newPrintWriter("results_CFA2.txt");
		for (int m = 0; m < numM; m++) {
			out.println(numReachFromM[m] + " " + domM.get(m));
			numPaths+=numReachFromM[m];
		}
		out.close();

		System.out.println("NumPaths: " + numPaths + ", NumEdges: " + MToMEdges.size());
		System.out.println("EXIT generateQueries");
	}

	protected void computeTransitiveClosure(MutableGraph<Pair<jq_Method,AbstractState<jq_Type>>> callGraph){
		Set<Pair<jq_Method, AbstractState<jq_Type>>> allNodes = callGraph.getNodes();
		ArrayList<Pair<jq_Method, AbstractState<jq_Type>>> nodesArr = new ArrayList<Pair<jq_Method,AbstractState<jq_Type>>>(allNodes);

		int numNodes = allNodes.size();

		boolean adjMat[][] = new boolean[allNodes.size()][allNodes.size()];
		boolean pathMat[][] = new boolean[allNodes.size()][allNodes.size()];

		for(Pair<jq_Method, AbstractState<jq_Type>> node : allNodes){
			int i = nodesArr.indexOf(node);
			for(Pair<jq_Method, AbstractState<jq_Type>> succNode : callGraph.getSuccs(node)){
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
			Pair<jq_Method, AbstractState<jq_Type>> srcNode = nodesArr.get(i);
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
	protected boolean jumpToMethodEnd(Quad q, jq_Method m, Edge<jq_Type> predPe, Edge<jq_Type> pe){
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
	protected Set<jq_Method> getTargets(Quad i, Edge<jq_Type> pe) {
		assert(pe.dstNode != null);
		Set<jq_Method> targets = super.getTargets(i, pe);
		Set<jq_Method> correctTargets = new ArraySet<jq_Method>();
		
		//Remove incorrect targets
		for(jq_Method m : targets){
			if(isCorrectTarget(i, m, pe, new ArraySet<jq_Type>()))
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
	//method is neither static nor special, the set callerVarTypesFiltered is populated with the types
	//information for "this" argument of the callee
	protected boolean isCorrectTarget(Quad i, jq_Method m, Edge<jq_Type> pe, Set<jq_Type> callerVarTypesFiltered){
		assert(callerVarTypesFiltered != null);

		//Handle reflection
		Set<jq_Method> reflectTargets = reflectIMMap.get(i);
		if(reflectTargets != null && reflectTargets.contains(m)){
			callerVarTypesFiltered.clear();
			callerVarTypesFiltered.addAll(HTMap.get(reflectIHMap.get(i)));
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
			Set<jq_Type> callerVarTypes = pe.dstNode.envLocal.get(callerVar);
			if(callerVarTypes != null){
				if(isSpecInvk){
					isCorrectTarget = true;
				}else{
					callerVarTypesFiltered.clear();
					Set<jq_Type> filterSet = IMtoT.get(new Pair<Quad, jq_Method>(i, m));
					if(filterSet != null){	
						callerVarTypesFiltered.addAll(callerVarTypes);
						callerVarTypesFiltered.retainAll(filterSet);
					}
					if(callerVarTypes.contains(jq_NullType.NULL_TYPE)) callerVarTypesFiltered.add(jq_NullType.NULL_TYPE);
					if(!callerVarTypesFiltered.isEmpty()) isCorrectTarget = true;
				}
			}
		}
		return isCorrectTarget;
	}

/*************************************************************************************************/	
	@Override
	public Set<Pair<Loc, Edge<jq_Type>>> getInitPathEdges() {
		Set<Pair<Loc, Edge<jq_Type>>> initPEs = new ArraySet<Pair<Loc, Edge<jq_Type>>>();
		for(jq_Method m : rootM){
			BasicBlock bb = m.getCFG().entry();
			Loc loc = new Loc(bb, -1);
			Pair<Loc, Edge<jq_Type>> pair = new Pair<Loc, Edge<jq_Type>>(loc, new Edge<jq_Type>());
			if (DEBUG) System.out.println("getInitPathEdges: Added " + pair);
			initPEs.add(pair);

			callGraph.insertRoot(new Pair<jq_Method, AbstractState<jq_Type>>(m, pair.val1.srcNode));
		}
		//jq_Method m = Program.g().getMainMethod();
		return initPEs;
	}
	
	@Override
	public Edge<jq_Type> getSkipMethodEdge(Quad q, Edge<jq_Type> pe) {
		assert(pe.dstNode != null);
		if (DEBUG) System.out.println("ENTER getSkipMethodEdge: q=" + q + " pe=" + pe);

		Env<Register,jq_Type> newTC = null;
		Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q).getRegister() : null;

	
		if(tgtRetReg != null ){
			newTC = new Env<Register,jq_Type>(pe.dstNode.envLocal);
			if(trackedVar.contains(tgtRetReg)){
				//Handle reflection
				Quad reflectSite = reflectIHMap.get(q);
				if(reflectSite != null){
					jq_Type tgtRetVarType = Invoke.getDest(q).getType();
					tgtRetVarType = tgtRetVarType != null ? tgtRetVarType : javaLangObject;
					Set<jq_Type> dstFiltered = new ArraySet<jq_Type>();
					dstFiltered.addAll(HTMap.get(reflectSite));
					Set<jq_Type> filterSet = TTFilterMap.get(tgtRetVarType);
					if(filterSet != null) dstFiltered.retainAll(filterSet); else dstFiltered.clear();
					if(!dstFiltered.isEmpty()){
						newTC.insert(tgtRetReg, dstFiltered);
					}else if(newTC.remove(tgtRetReg) == null){
						newTC = pe.dstNode.envLocal;
					}
				}else if(newTC.remove(tgtRetReg) == null)
					newTC = pe.dstNode.envLocal;
			}else{
				Set<jq_Type> tgtRetRegType = new ArraySet<jq_Type>();
				tgtRetRegType.add(jq_NullType.NULL_TYPE);
				newTC.insert(tgtRetReg, tgtRetRegType);
			}
		}else{
			newTC = pe.dstNode.envLocal;
		}

		AbstractState<jq_Type> newDst = (newTC==pe.dstNode.envLocal) ? pe.dstNode : new AbstractState<jq_Type>(newTC);
		Edge<jq_Type> newEdge = new Edge<jq_Type>(pe.srcNode, newDst);

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
	public Edge<jq_Type> getInitPathEdge(Quad q, jq_Method m, Edge<jq_Type> pe) {
		assert(pe.dstNode != null);
		if (DEBUG) System.out.println("ENTER getInitPathEdge: q=" + q + " m=" + m + " pe=" + pe);

		Env<Register,jq_Type> newTC = generateInitSrcState(q, m, pe, null);

		AbstractState<jq_Type> newSrc = new AbstractState<jq_Type>(newTC);
		AbstractState<jq_Type> newDst = new AbstractState<jq_Type>(newTC);
		Edge<jq_Type> newEdge = new Edge<jq_Type>(newSrc, newDst);

		Pair<jq_Method, AbstractState<jq_Type>> cgSrcNode =  new Pair<jq_Method, AbstractState<jq_Type>>(q.getMethod(), pe.srcNode);
		Pair<jq_Method, AbstractState<jq_Type>> cgDstNode = new Pair<jq_Method, AbstractState<jq_Type>>(m, newSrc);
		callGraph.insertEdge(cgSrcNode, cgDstNode);

		
		Pair<AbstractState<jq_Type>, Quad> CICMSrc =  new Pair<AbstractState<jq_Type>, Quad>(pe.srcNode, q);
		Set<Pair<AbstractState<jq_Type>, jq_Method>> CICMDstSet = CICM.get(CICMSrc);
		if(CICMDstSet == null){
			CICMDstSet = new HashSet<Pair<AbstractState<jq_Type>,jq_Method>>();
			CICM.put(CICMSrc, CICMDstSet);
		}
		CICMDstSet.add(new Pair<AbstractState<jq_Type>, jq_Method>(newSrc, m));
		
		if (DEBUG) System.out.println("LEAVE getInitPathEdge: " + newEdge);
		return newEdge;
	}

	//Generate new src state for a callee m at callsite q given input edge pe. The set callerVarTypesFiltered is
	//populated with the type information, if available, for "this" argument of the callee
	protected Env<Register,jq_Type> generateInitSrcState(Quad q, jq_Method m, Edge<jq_Type> pe, Set<jq_Type> callerVarTypesFiltered){
		assert(pe.dstNode != null);
		boolean isStatic = false, isReflect = false;

		Operator op = q.getOperator();
		if(op instanceof InvokeStatic)
			isStatic =  true;

		//Handle reflection
		Set<jq_Method> reflectTargets = reflectIMMap.get(q);
		if(reflectTargets != null && reflectTargets.contains(m)){
			isReflect = true;
			if(callerVarTypesFiltered == null){
				callerVarTypesFiltered = new ArraySet<jq_Type>();
				callerVarTypesFiltered.addAll(HTMap.get(reflectIHMap.get(q)));
			}
			
		}else{
			if(!isStatic && callerVarTypesFiltered == null && useExtraFilters){
				Register callerVar = Invoke.getParam(q, 0).getRegister();
				Set<jq_Type> callerVarTypes = pe.dstNode.envLocal.get(callerVar);
				assert(callerVarTypes != null); //This function shouldn't be invoked unless its the correct target 
				callerVarTypesFiltered = new ArraySet<jq_Type>();
				Set<jq_Type> filterSet = IMtoT.get(new Pair<Quad, jq_Method>(q, m));
				if(filterSet != null){	
					callerVarTypesFiltered.addAll(callerVarTypes);
					callerVarTypesFiltered.retainAll(filterSet);
				}
				if(callerVarTypes.contains(jq_NullType.NULL_TYPE)) callerVarTypesFiltered.add(jq_NullType.NULL_TYPE);
				assert(!callerVarTypesFiltered.isEmpty()); //This function shouldn't be invoked unless its the correct target
			}
		}
		
		AbstractState<jq_Type> oldDst = pe.dstNode;
		Env<Register,jq_Type> newTC = new Env<Register,jq_Type>();

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
			Set<jq_Type> paramVarTypes;
			if(!trackedVar.contains(formalReg)){
				paramVarTypes = new ArraySet<jq_Type>();
				paramVarTypes.add(jq_NullType.NULL_TYPE);
			}else if(i == 0 && ((!isStatic && useExtraFilters) || isReflect)){
				paramVarTypes = callerVarTypesFiltered;
			}else if(i == 1 && isReflect && conNewInstIMMap.get(q)!=null){
				paramVarTypes = null;
				Set<Quad> basePointsTo = VHMap.get(actualReg);
				if(basePointsTo != null){
					for(Quad quad : basePointsTo){
						Pair<Quad, jq_Field> pair = new Pair<Quad, jq_Field>(quad, null);
						Set<Quad> fieldPointsTo = HFHMap.get(pair);
						if(fieldPointsTo != null){
							if(paramVarTypes == null) paramVarTypes = new ArraySet<jq_Type>();
							for(Quad quad2 : fieldPointsTo)
								paramVarTypes.addAll(HTMap.get(quad2));
						}
					}
				}
			}else if(isReflect){
				paramVarTypes = null;
			}else
				paramVarTypes = oldDst.envLocal.get(actualReg);

			if(paramVarTypes != null){
				Set<jq_Type> dstFiltered = new ArraySet<jq_Type>(paramVarTypes);
				Set<jq_Type> filterSet = TTFilterMap.get(paramTypes[i]);
				if(filterSet != null) dstFiltered.retainAll(filterSet); else dstFiltered.clear();
				if(!dstFiltered.isEmpty())
					newTC.insert(formalReg, dstFiltered);
			}

		}

		return newTC;
	}

	@Override
	public Edge<jq_Type> getMiscPathEdge(Quad q, Edge<jq_Type> pe) {
		if (DEBUG) System.out.println("ENTER getMiscPathEdge: q=" + q + " pe=" + pe);

		qv.istate = pe.dstNode;
		qv.ostate = pe.dstNode;
		assert(qv.ostate != null && qv.istate != null);
		// may modify only qv.ostate
		q.accept(qv);
		assert(qv.ostate != null);
		// XXX: DO NOT REUSE incoming PE (merge does strong updates)
		Edge<jq_Type> newEdge = new Edge<jq_Type>(pe.srcNode, qv.ostate);

		if (DEBUG) System.out.println("LEAVE getMiscPathEdge: ret=" + newEdge);
		return newEdge;
	}

	@Override
	public Edge<jq_Type> getInvkPathEdge(Quad q, Edge<jq_Type> clrPE, jq_Method m, Edge<jq_Type> tgtSE) {
		assert(clrPE.dstNode != null && tgtSE.dstNode != null);
		if (DEBUG) System.out.println("ENTER getInvkPathEdge: q=" + q + " clrPE=" + clrPE + " m=" + m + " tgtSE=" + tgtSE);

		Set<jq_Type> callerVarTypesFiltered = new ArraySet<jq_Type>();

		//Following check is necessary since each callsite, though a correct one for method m,
		//could have many incoming edges that can't invoked this method
		if(!isCorrectTarget(q, m, clrPE, callerVarTypesFiltered)){
			if (DEBUG) System.out.println("LEAVE getInvkPathEdge: null (Incorrect target for clrPE)");
			return null;
		}else{

			// Compare localTCs; they should be equal in order to apply summary
			// Build this local tmpTC as follows:
			Env<Register,jq_Type> tmpTC = generateInitSrcState(q, m, clrPE, callerVarTypesFiltered);

			if (!(tgtSE.srcNode.envLocal.equals(tmpTC))) {
				if (DEBUG) System.out.println("LEAVE getInvkPathEdge: null (type contexts don't match)");
				return null;
			}

			Env<Register,jq_Type> newTC = null;
			Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q).getRegister() : null;
			if(tgtRetReg != null){
				newTC = new Env<Register,jq_Type>(clrPE.dstNode.envLocal);
				if(trackedVar.contains(tgtRetReg)){
					jq_Type tgtRetVarType = Invoke.getDest(q).getType();
					tgtRetVarType = tgtRetVarType != null ? tgtRetVarType : javaLangObject;
					Set<jq_Type> dstFiltered = new ArraySet<jq_Type>(tgtSE.dstNode.returnVarEnv);
					
					//Handle reflection
					Quad reflectSite = reflectIHMap.get(q);
					if(reflectSite != null)
						dstFiltered.addAll(HTMap.get(reflectSite));
					
					Set<jq_Type> filterSet = TTFilterMap.get(tgtRetVarType);
					if(filterSet != null) dstFiltered.retainAll(filterSet); else dstFiltered.clear();
					if(!dstFiltered.isEmpty()){
						newTC.insert(tgtRetReg, dstFiltered);
					}else{
						if(newTC.remove(tgtRetReg) == null)
							newTC = clrPE.dstNode.envLocal;
					}
				}else{
					Set<jq_Type> tgtRetRegType = new ArraySet<jq_Type>();
					tgtRetRegType.add(jq_NullType.NULL_TYPE);
					newTC.insert(tgtRetReg, tgtRetRegType);
				}
			}else{
				newTC = clrPE.dstNode.envLocal;
			}

			AbstractState<jq_Type> newDst = (newTC==clrPE.dstNode.envLocal) ? clrPE.dstNode : new AbstractState<jq_Type>(newTC);				
			Edge<jq_Type> newEdge = new Edge<jq_Type>(clrPE.srcNode, newDst);

			if (DEBUG) System.out.println("LEAVE getInvkPathEdge: " + newEdge);
			return newEdge;
		}
	}

	@Override
	public Edge<jq_Type> getPECopy(Edge<jq_Type> pe) { return getCopy(pe); }

	@Override
	public Edge<jq_Type> getSECopy(Edge<jq_Type> se) { return getCopy(se); }

	protected Edge<jq_Type> getCopy(Edge<jq_Type> pe) {
		assert(pe.dstNode != null && pe.srcNode != null);
		if (DEBUG) System.out.println("Called Copy with: " + pe);
		return new Edge<jq_Type>(pe.srcNode, pe.dstNode);
	}

	@Override
	public Edge<jq_Type> getSummaryEdge(jq_Method m, Edge<jq_Type> pe) {
		assert(pe.srcNode != null && pe.dstNode != null);
		if (DEBUG) System.out.println("\nCalled getSummaryEdge: m=" + m + " pe=" + pe);
		return getCopy(pe);
	}

	public class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		public AbstractState<jq_Type> istate;    // immutable, will never be null
		public AbstractState<jq_Type> ostate;    // mutable, initially ostate == istate

		@Override
		public void visitCheckCast(Quad q) {
			Register dstR = CheckCast.getDest(q).getRegister();
			jq_Type dstVarType = CheckCast.getType(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);

			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}

			if (CheckCast.getSrc(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) CheckCast.getSrc(q)).getRegister();
				Set<jq_Type> srcRTypes = istate.envLocal.get(srcR);
				if(srcRTypes != null){				
					Set<jq_Type> dstFiltered = new ArraySet<jq_Type>(srcRTypes);
					Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
					if(filterSet != null) dstFiltered.retainAll(filterSet); else dstFiltered.clear();
					if(!dstFiltered.isEmpty()){
						newLocalTC.insert(dstR, dstFiltered);
						ostate = new AbstractState<jq_Type>(newLocalTC);
						return;
					}
				}	
			}

			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitMove(Quad q) {
			Register dstR = Move.getDest(q).getRegister();
			jq_Type dstVarType = Move.getDest(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);
			
			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}
			
			if (Move.getSrc(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Move.getSrc(q)).getRegister();
				Set<jq_Type> srcRTypes = istate.envLocal.get(srcR);
				if(srcRTypes != null){
					Set<jq_Type> dstFiltered = new ArraySet<jq_Type>(srcRTypes);
					Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
					if(filterSet != null) dstFiltered.retainAll(filterSet); else dstFiltered.clear();
					if(!dstFiltered.isEmpty()){
						newLocalTC.insert(dstR, dstFiltered);
						ostate = new AbstractState<jq_Type>(newLocalTC);
						return;
					}
				}	
			}
			
			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitPhi(Quad q) {
			assert false : "Use no PHI version of quad code!";
			Register dstR = Phi.getDest(q).getRegister();
			jq_Type dstVarType = Phi.getDest(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);
			
			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}
			
			ParamListOperand ros = Phi.getSrcs(q);
			int n = ros.length();

			Set<jq_Type> dstRTypes = null;

			for (int i = 0; i < n; i++) {
				RegisterOperand ro = ros.get(i);
				if (ro == null) continue;
				Register srcR = ((RegisterOperand) ro).getRegister();
				Set<jq_Type> srcRTypes = istate.envLocal.get(srcR);

				if(srcRTypes != null){
					if(dstRTypes == null) dstRTypes = new ArraySet<jq_Type>();
					dstRTypes.addAll(srcRTypes);
				}
			}
			
			if(dstRTypes != null){
				Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
				if(filterSet != null) dstRTypes.retainAll(filterSet); else dstRTypes.clear();
				if(!dstRTypes.isEmpty()){
					newLocalTC.insert(dstR, dstRTypes);
					ostate = new AbstractState<jq_Type>(newLocalTC);
					return;
				}
			}
			
			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitNew(Quad q) {
			Register dstR = New.getDest(q).getRegister();
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);

			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
			}else
				newLocalTC.insert(dstR, New.getType(q).getType());

			ostate = new AbstractState<jq_Type>(newLocalTC);

		}

		@Override
		public void visitNewArray(Quad q) {
			Register dstR = NewArray.getDest(q).getRegister();
		
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);

			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
			}else
				newLocalTC.insert(dstR, NewArray.getType(q).getType());

			ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitMultiNewArray(Quad q) {
			Register dstR = MultiNewArray.getDest(q).getRegister();
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);

			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
			}else
				newLocalTC.insert(dstR, MultiNewArray.getType(q).getType());

			ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		//Can't reason about array element types from the array type but still need to 
		//populate the dstR entry with 0-cfa types for soundness reasons(consider joins)
		@Override
		public void visitALoad(Quad q) {
			Register dstR = ALoad.getDest(q).getRegister();
			jq_Type dstVarType = ALoad.getDest(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);
			
			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}
			
			Set<jq_Type> dstRTypes = null;
			
			if (ALoad.getBase(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) ALoad.getBase(q)).getRegister();
				Set<Quad> basePointsTo = VHMap.get(srcR);
				
				if(basePointsTo != null){
					for(Quad quad : basePointsTo){
						Pair<Quad, jq_Field> pair = new Pair<Quad, jq_Field>(quad, null);
						Set<Quad> fieldPointsTo = HFHMap.get(pair);
						if(fieldPointsTo != null){
							if(dstRTypes == null) dstRTypes = new ArraySet<jq_Type>();
							for(Quad quad2 : fieldPointsTo)
								dstRTypes.addAll(HTMap.get(quad2));
						}
					}
				}
			}

			if(dstRTypes != null){
				Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
				if(filterSet != null) dstRTypes.retainAll(filterSet); else dstRTypes.clear();
				if(!dstRTypes.isEmpty()){
					newLocalTC.insert(dstR, dstRTypes);
					ostate = new AbstractState<jq_Type>(newLocalTC);
					return;
				}
			}
			
			
			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitAStore(Quad q) {
			return;		
		}

		//Not tracking globals, so get info via 0-cfa
		@Override
		public void visitGetstatic(Quad q) {
			Register dstR = Getstatic.getDest(q).getRegister();
			jq_Type dstVarType = Getstatic.getDest(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;
			
			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);
			
			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}
			
			jq_Field srcF = Getstatic.getField(q).getField();
			Set<Quad> pointsTo = FHMap.get(srcF);
			Set<jq_Type> srcRTypes = null;

			if(pointsTo != null){
				srcRTypes = new ArraySet<jq_Type>();
				for(Quad quad : pointsTo)
					srcRTypes.addAll(HTMap.get(quad));
			}
			
			if(srcRTypes != null){
				Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
				if(filterSet != null) srcRTypes.retainAll(filterSet); else srcRTypes.clear();
				if(!srcRTypes.isEmpty()){
					newLocalTC.insert(dstR, srcRTypes);
					ostate = new AbstractState<jq_Type>(newLocalTC);
					return;
				}
			}
			
			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		//Not tracking globals
		@Override
		public void visitPutstatic(Quad q) { }

		//Not tracking heap
		@Override
		public void visitPutfield(Quad q) {	}

		@Override
		public void visitGetfield(Quad q) {
			Register dstR = Getfield.getDest(q).getRegister();
			jq_Type dstVarType = Getfield.getDest(q).getType();
			dstVarType = dstVarType != null ? dstVarType : javaLangObject;

			Env<Register,jq_Type> newLocalTC = new Env<Register,jq_Type>(istate.envLocal);
			
			if(!trackedVar.contains(dstR)){
				Set<jq_Type> dstRTypes = new ArraySet<jq_Type>();
				dstRTypes.add(jq_NullType.NULL_TYPE);
				newLocalTC.insert(dstR,dstRTypes);
				ostate = new AbstractState<jq_Type>(newLocalTC);
				return;
			}
			
			Set<jq_Type> dstRTypes = null;

			if (Getfield.getBase(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Getfield.getBase(q)).getRegister();
				jq_Field srcF = Getfield.getField(q).getField();
				Set<Quad> basePointsTo = VHMap.get(srcR);
				
				if(basePointsTo != null){
					for(Quad quad : basePointsTo){
						Pair<Quad, jq_Field> pair = new Pair<Quad, jq_Field>(quad, srcF);
						Set<Quad> fieldPointsTo = HFHMap.get(pair);
						if(fieldPointsTo != null){
							if(dstRTypes == null) dstRTypes = new ArraySet<jq_Type>();
							for(Quad quad2 : fieldPointsTo)
								dstRTypes.addAll(HTMap.get(quad2));
						}
					}
				}
			}

			if(dstRTypes != null){
				Set<jq_Type> filterSet = TTFilterMap.get(dstVarType);
				if(filterSet != null) dstRTypes.retainAll(filterSet); else dstRTypes.clear();
				if(!dstRTypes.isEmpty()){
					newLocalTC.insert(dstR, dstRTypes);
					ostate = new AbstractState<jq_Type>(newLocalTC);
					return;
				}
			}
			
			if(newLocalTC.remove(dstR) != null)
				ostate = new AbstractState<jq_Type>(newLocalTC);
		}

		@Override
		public void visitReturn(Quad q) {
			if (q.getOperator() instanceof THROW_A)
				return;
			
			if (Return.getSrc(q) instanceof RegisterOperand) {
				Register tgtR = ((RegisterOperand) (Return.getSrc(q))).getRegister();
				Set<jq_Type> tgtRTypes = istate.envLocal.get(tgtR);
				if(tgtRTypes != null)
					ostate = new AbstractState<jq_Type>(istate.envLocal, tgtRTypes);
			}
		}
	}
}
