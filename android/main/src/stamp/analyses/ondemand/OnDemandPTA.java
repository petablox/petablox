package stamp.analyses.ondemand;

import soot.G;
import soot.Scene;
import soot.SootMethod;
import soot.SootField;
import soot.Local;
import soot.Type;
import soot.RefLikeType;
import soot.PointsToSet;
import soot.PointsToAnalysis;
import soot.jimple.Stmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.GlobalVarNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.pag.Parm;
import soot.jimple.spark.ondemand.DemandCSPointsTo;
import soot.jimple.spark.ondemand.DemandCSPointsTo.VarAndContext;
import soot.jimple.spark.ondemand.HeuristicType;
import soot.jimple.spark.ondemand.TerminateEarlyException;
import soot.jimple.spark.ondemand.AllocAndContext;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.CallSiteException;
import soot.jimple.spark.ondemand.pautil.AssignEdge;
import soot.jimple.spark.ondemand.pautil.SootUtil;
import soot.jimple.spark.ondemand.pautil.ContextSensitiveInfo;
import soot.jimple.spark.ondemand.genericutil.ImmutableStack;
import soot.jimple.spark.ondemand.genericutil.ArraySet;
import soot.jimple.spark.ondemand.genericutil.Propagator;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.NumberedString;
import soot.toolkits.scalar.Pair;

import java.util.*; 

public class OnDemandPTA extends DemandCSPointsTo
{
	protected Map<VarAndContext,AllocAndContextSet> pointsToCache = new HashMap();
	protected Map<AllocAndContext,Set<VarAndContext>> flowsToCache = new HashMap();
	private int cacheHit = 0;
	private int totalQueries = 0;

	public static OnDemandPTA makeDefault()
	{
		return makeWithBudget(1000000, 50, DEFAULT_LAZY);
	}
	public static OnDemandPTA makeWithBudget(int maxTraversal, int maxPasses, boolean lazy) 
	{
        PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		ContextSensitiveInfo csInfo = new ContextSensitiveInfo(pag);
        return new OnDemandPTA(csInfo, pag, maxTraversal, maxPasses, lazy);
    }

	private OnDemandPTA(ContextSensitiveInfo csInfo, PAG pag, int maxTraversal, int maxPasses, boolean lazy)
	{
		super(csInfo, pag, maxTraversal, maxPasses, lazy);
		init();
	}

	Integer getCallSiteFor(Stmt stmt)
	{
		InvokeExpr ie = stmt.getInvokeExpr();
		return csInfo.getCallSiteFor(ie);
	}

	Set<SootMethod> callTargets(Stmt stmt, ImmutableStack<Integer> callerContext)
	{
		InvokeExpr ie = stmt.getInvokeExpr();

		if(ie instanceof StaticInvokeExpr || ie instanceof SpecialInvokeExpr){
			return Collections.<SootMethod> singleton(ie.getMethod());
		} else if(!pag.virtualCallsToReceivers.containsKey(ie)){
			//single outgoing calledge for a VirtualInvokeExpr or InterfaceInvokeExpr
			Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(stmt);
			SootMethod tgt = it.next().tgt();
			assert !it.hasNext();
			return Collections.<SootMethod> singleton(tgt);
		} else {
			Local rcvr = (Local) ((InstanceInvokeExpr) ie).getBase();
			AllocAndContextSet pt = (AllocAndContextSet) pointsToSetFor(rcvr, callerContext);
			Set<Type> reachingTypes = new HashSet();
			for(AllocAndContext obj : pt)
				reachingTypes.add(obj.alloc.getType());
			NumberedString invokedMethodSubsig = ie.getMethod().getNumberedSubSignature();
			Type rcvrType = rcvr.getType();
			Set<SootMethod> targets = new HashSet();
			for(Type rt : reachingTypes)
				targets.addAll(getCallTargetsForType(rt, invokedMethodSubsig, rcvrType, null));
			return targets;
		}
	}

	public VarNode varNode(Local l)
	{
		VarNode v = pag.findLocalVarNode(l);
		return v;
	}

    /**
     * Computes the refined set of reaching objects for l.                                                            
     * Returns <code>null</code> if refinement failed.
     */
    public PointsToSet pointsToSetFor(Local l, ImmutableStack<Integer> context) {
        VarNode v = pag.findLocalVarNode(l);
        if (v == null) {
			//no reaching objects
			return EmptyPointsToSet.v();
        }
		return pointsToSetFor(new VarAndContext(v, context));
	}

	public PointsToSet pointsToSetFor(VarAndContext vc)
	{
		totalQueries++;
		if(pointsToCache.containsKey(vc)){
			cacheHit++;
			if(cacheHit % 1000 == 0) System.out.println("points-to cache stat: "+cacheHit+ " "+totalQueries);
			return pointsToCache.get(vc);
		}

        // must reset the refinement heuristic for each query
        this.fieldCheckHeuristic = HeuristicType.getHeuristic(heuristicType, pag.getTypeManager(), getMaxPasses());
        doPointsTo = true;
        numPasses = 0;
        AllocAndContextSet contextSensitiveResult = null;
        while (true) {
            numPasses++;
            if (DEBUG_PASS != -1 && numPasses > DEBUG_PASS) {
                break;
            }
            if (numPasses > maxPasses) {
                break;
            }
            if (DEBUG) {
                G.v().out.println("PASS " + numPasses);
                G.v().out.println(fieldCheckHeuristic);
            }
            clearState();
            pointsTo = new AllocAndContextSet();
            try {
                refineP2Set(vc, null);
                contextSensitiveResult = pointsTo;
            } catch (TerminateEarlyException e) {
				System.out.println("TerminateEarlyException "+e.getMessage());
            }
            if (!fieldCheckHeuristic.runNewPass()) {
                break;
            }
        }
		pointsToCache.put(vc, contextSensitiveResult);
        return contextSensitiveResult;
    }

	public Set<VarAndContext> flowsToSetFor(AllocAndContext allocAndContext) 
	{
		totalQueries++;
		if(flowsToCache.containsKey(allocAndContext)){
			cacheHit++;
			if(cacheHit % 1000 == 0) System.out.println("flows-to cache stat: "+cacheHit+ " "+totalQueries);
			return flowsToCache.get(allocAndContext);
		}

		this.fieldCheckHeuristic = HeuristicType.getHeuristic(heuristicType, pag.getTypeManager(), getMaxPasses());
		numPasses = 0;
		Set<VarAndContext> smallest = null;
		while (true) {
			numPasses++;
			if (DEBUG_PASS != -1 && numPasses > DEBUG_PASS) {
				return smallest;
			}
			if (numPasses > maxPasses) {
				return smallest;
			}
			if (DEBUG) {
				G.v().out.println("PASS " + numPasses);
				G.v().out.println(fieldCheckHeuristic);
			}
			clearState();
			Set<VarAndContext> result = null;
			try {
				result = flowsToSetHelper(allocAndContext);
			} catch (TerminateEarlyException e) {
				System.out.println("TerminateEarlyException "+e.getMessage());
			}
			if (result != null) {
				if (smallest == null || result.size() < smallest.size()) {
					smallest = result;
				}
			}
			if (!fieldCheckHeuristic.runNewPass()) {
				break;
			}
		}
		flowsToCache.put(allocAndContext, smallest);
        return smallest;
	}

	private Set<VarAndContext> flowsToSetHelper(AllocAndContext allocAndContext) {
		Set<VarAndContext> ret = new ArraySet<VarAndContext>();

		try {
			HashSet<VarAndContext> marked = new HashSet<VarAndContext>();
			soot.jimple.spark.ondemand.genericutil.Stack<VarAndContext> worklist = new soot.jimple.spark.ondemand.genericutil.Stack<VarAndContext>();
			Propagator<VarAndContext> p = new Propagator<VarAndContext>(marked,
					worklist);
			AllocNode alloc = allocAndContext.alloc;
			ImmutableStack<Integer> allocContext = allocAndContext.context;
			Node[] newBarNodes = pag.allocLookup(alloc);
			for (int i = 0; i < newBarNodes.length; i++) {
				VarNode v = (VarNode) newBarNodes[i];
				VarAndContext vc = new VarAndContext(v, allocContext);
				ret.add(vc);
				p.prop(vc);
			}
			while (!worklist.isEmpty()) {
				if(!incrementNodesTraversed()) return ret;
				VarAndContext curVarAndContext = worklist.pop();
				if (DEBUG) {
					debugPrint("looking at " + curVarAndContext);
				}
				VarNode curVar = curVarAndContext.var;
				ImmutableStack<Integer> curContext = curVarAndContext.context;
				ret.add(curVarAndContext);
				// assign
				Collection<AssignEdge> assignEdges = filterAssigns(curVar,
						curContext, false, true);
				for (AssignEdge assignEdge : assignEdges) {
					VarNode dst = assignEdge.getDst();
					ImmutableStack<Integer> newContext = curContext;
					if (assignEdge.isReturnEdge()) {
						if (!curContext.isEmpty()) {
							if (!callEdgeInSCC(assignEdge)) {
								assert assignEdge.getCallSite().equals(
										curContext.peek()) : assignEdge + " "
										+ curContext;
								newContext = curContext.pop();
							} else {
								newContext = popRecursiveCallSites(curContext);
							}
						}
					} else if (assignEdge.isParamEdge()) {
						if (DEBUG)
							debugPrint("entering call site "
									+ assignEdge.getCallSite());
						// if (!isRecursive(curContext, assignEdge)) {
						// newContext = curContext.push(assignEdge
						// .getCallSite());
						// }
						try{
							newContext = pushWithRecursionCheck(curContext,
																assignEdge);
						}catch(TerminateEarlyException e){
							//SA: dont bail out completely. generate less unsound result 
							continue;
						}
					}
					if (assignEdge.isReturnEdge() && curContext.isEmpty()
							&& csInfo.isVirtCall(assignEdge.getCallSite())) {
						Set<SootMethod> targets = refineCallSite(assignEdge
								.getCallSite(), newContext);
						if (!targets.contains(((LocalVarNode) assignEdge
								.getDst()).getMethod())) {
							continue;
						}
					}
					if (dst instanceof GlobalVarNode) {
						newContext = EMPTY_CALLSTACK;
					}
					p.prop(new VarAndContext(dst, newContext));
				}
				// putfield_bars
				Set<VarNode> matchTargets = vMatches.vMatchLookup(curVar);
				Node[] pfTargets = pag.storeLookup(curVar);
				for (int i = 0; i < pfTargets.length; i++) {
					FieldRefNode frNode = (FieldRefNode) pfTargets[i];
					final VarNode storeBase = frNode.getBase();
					SparkField field = frNode.getField();
					// Pair<VarNode, FieldRefNode> putfield = new Pair<VarNode,
					// FieldRefNode>(curVar, frNode);
					for (Pair<VarNode, VarNode> load : fieldToLoads.get(field)) {
						final VarNode loadBase = load.getO2();
						final PointsToSetInternal loadBaseP2Set = loadBase
								.getP2Set();
						final PointsToSetInternal storeBaseP2Set = storeBase
								.getP2Set();
						final VarNode matchTgt = load.getO1();
						if (matchTargets.contains(matchTgt)) {
							if (DEBUG) {
								debugPrint("match source " + matchTgt);
							}
							PointsToSetInternal intersection = SootUtil
									.constructIntersection(storeBaseP2Set,
											loadBaseP2Set, pag);

							boolean checkField = fieldCheckHeuristic
									.validateMatchesForField(field);
							if (checkField) {
								AllocAndContextSet sharedAllocContexts = findContextsForAllocs(
										new VarAndContext(storeBase, curContext),
										intersection);
								for (AllocAndContext curAllocAndContext : sharedAllocContexts) {
									CallingContextSet upContexts;
									if (fieldCheckHeuristic
											.validFromBothEnds(field)) {
										upContexts = findUpContextsForVar(
												curAllocAndContext,
												new VarContextAndUp(loadBase,
														EMPTY_CALLSTACK,
														EMPTY_CALLSTACK));
									} else {
										upContexts = findVarContextsFromAlloc(
												curAllocAndContext, loadBase);
									}
									for (ImmutableStack<Integer> upContext : upContexts) {
										p.prop(new VarAndContext(matchTgt,
												upContext));
									}
								}
							} else {
								p.prop(new VarAndContext(matchTgt,
										EMPTY_CALLSTACK));
							}
							// h.handleMatchSrc(matchSrc, intersection,
							// storeBase,
							// loadBase, varAndContext, checkGetfield);
							// if (h.terminate())
							// return;
						}
					}

				}
			}
			return ret;
		} catch (CallSiteException e) {
			allocAndContextCache.remove(allocAndContext);
			throw e;
		}
	}
	
	public PointsToSetInternal ciPointsToSetFor(Local local)
	{
		return (PointsToSetInternal) pag.reachingObjects(local);
	}
	
	public ImmutableStack<Integer> emptyStack()
	{
		return EMPTY_CALLSTACK;
	}
	
	public Node retVarNode(SootMethod m)
	{
		if(!(m.getReturnType() instanceof RefLikeType)
		   || !Scene.v().getReachableMethods().contains(m))
			return null;
		LocalVarNode retNode = pag.findLocalVarNode(Parm.v(m, PointsToAnalysis.RETURN_NODE));
		/*
		Node[] retVarNodes = pag.simpleInvLookup(retNode);
		return retVarNodes;
		*/
		return retNode;
	}
	
	public Node parameterNode(SootMethod m, int index)
	{
		if(!Scene.v().getReachableMethods().contains(m))
			return null;
		if(!m.isStatic()){
			if(index == 0){
				LocalVarNode thisNode = pag.findLocalVarNode(new Pair(m, PointsToAnalysis.THIS_NODE));
				/*
				Node[] toNodes = pag.simpleLookup(thisNode);
				int nodeIndex = -1;
				//StringBuilder builder = new StringBuilder();
				for(int i = 0; i < toNodes.length; i++){
					Node node = toNodes[i];
					if(node instanceof LocalVarNode && ((LocalVarNode) node).getMethod().equals(m)){
						assert nodeIndex < 0;
						nodeIndex = i;
					}
					//builder.append(", " + node.toString());
				}
				return toNodes[nodeIndex];
				*/
				return thisNode;
			} else
				index--;
		}
		if(!(m.getParameterType(index) instanceof RefLikeType))
			return null;
		LocalVarNode paramNode = pag.findLocalVarNode(new Pair<SootMethod,Integer>(m, new Integer(index)));
		/*
		Node[] paramVarNodes = pag.simpleLookup(paramNode);
		assert paramVarNodes.length == 1;
		return paramVarNodes[0];
		*/
		return paramNode;
	}
	
	public SootMethod transferEndPoint(VarNode vn)
	{
		if(!(vn instanceof LocalVarNode))
			return null;
		LocalVarNode lvn = (LocalVarNode) vn;
		SootMethod m = lvn.getMethod();
		Object var = lvn.getVariable();
		if(var instanceof Local)
			return null;
		if(var instanceof Pair){
			Pair p = (Pair) var;
			if(!p.getO1().equals(m))
				return null;
			Object o2 = p.getO2();
			if(o2.equals(PointsToAnalysis.THIS_NODE) ||  o2 instanceof Integer)
				return m;
		} else if(Parm.v(m, PointsToAnalysis.RETURN_NODE).equals(var))
			return m;
		return null;
	}
	
	public AllocNode allocNodeFor(SootMethod m, NewExpr ne)
	{
		return pag.makeAllocNode(ne, ne.getType(), m);
	}
}