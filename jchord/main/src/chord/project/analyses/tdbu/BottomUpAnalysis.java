package chord.project.analyses.tdbu;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.ICICG;
import chord.program.Loc;
import chord.project.analyses.rhs.IEdge;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

/**
 * The bottom-up analysis with case dropping using the top-down summaries
 * 
 * @author xin
 * 
 * @param <TDPE>
 * @param <TDSE>
 * @param <BUPE>
 * @param <BUSE>
 */
public abstract class BottomUpAnalysis<TDPE extends IEdge, TDSE extends IEdge, BUPE extends BUEdge<TDPE, TDSE>, BUSE extends BUEdge<TDPE, TDSE>> {
	/**
	 * The call graph
	 */
	protected ICICG callGraph;
	// /**
	// * The map to record how many times BU has been run on each SCC of
	// methods.
	// * The reason we keep this is that we want to run BU on a SCC again and
	// * again if we're analyzing some methods that call the methods inside the
	// * SCC
	// */
	// protected Map<Set<jq_Method>, Integer> buCountMap = new
	// HashMap<Set<jq_Method>, Integer>();
	/**
	 * The map that maps each method to its set of summaries
	 */
	protected Map<jq_Method, Set<BUSE>> summEdges = new HashMap<jq_Method, Set<BUSE>>();

	/**
	 * The strong connected components in the call graph sorted by topological
	 * order
	 */
	protected List<Set<jq_Method>> sccs;

	/**
	 * a map that maps a method to the scc that it belongs
	 */
	protected Map<jq_Method, Set<jq_Method>> sccMap;

	/**
	 * The upper limit of how many cases we're tracking in BU
	 */
	protected TObjectIntHashMap<jq_Method> buCountMap;

	protected TObjectIntHashMap<jq_Method> waitMap;

	protected Set<jq_Method> noTDSEMs;

	protected int BULimit;

	protected int BUPELimit;

	private int waitValue = 10;

	private Map<jq_Method, Set<jq_Method>> rmsMap;
	
	private Map<jq_Method, Set<jq_Method>> callerMap;

	private int BUTimes = 0;
	private int BUMatch = 0;
	private int BUUnmatch = 0;
	private int noCase = 0;
	private int caseExplode = 0;
	private int TDSENotReady = 0;

	public static boolean DEBUG = false;

	/**
	 * A cache to speed up the callee query
	 */
	protected Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();

	/**
	 * The transfer function over quad q
	 * 
	 * @param inEdge
	 * @param q
	 * @return
	 */
	protected abstract Set<BUPE> transfer(BUPE inEdge, Quad q);

	/**
	 * Get the initial set of bupes of method m
	 * 
	 * @param m
	 * @return
	 */
	protected abstract Pair<Loc, Set<BUPE>> getInitialBUEdge(jq_Method m);

	/**
	 * This method is used to handle recursion. Without recursion, methods are
	 * analyzed in topological order. But with recursion, we have to find a way
	 * to break the dependencies among methods. The solution is to provide some
	 * default summaries of the unanalyzed methods. Usually, these summaries are
	 * top.
	 * 
	 * @param m
	 * @return
	 */
	protected abstract Set<BUSE> getDefaultSummaries(jq_Method m);

	/**
	 * 
	 * @param tdses
	 * @return
	 */
	protected abstract CaseTDSEComparator<TDSE> getCaseCMP(Set<TDSE> tdses);

	/**
	 * Lift a bottom-up path edge bupe to a bottom-up summary edge
	 * 
	 * @param bupe
	 * @param m
	 * @return
	 */
	protected abstract BUSE lift(BUPE bupe, jq_Method m);

	protected abstract Constraint getTrue();

	/**
	 * The constructor
	 * TDSEGenMethods are the methods which can generate TDSEs, normally, it should be set to the root methods.But for mustalias, set them
	 * to the method containing tracked 
	 * @param callGraph
	 * @param bULimit
	 */
	public BottomUpAnalysis(ICICG callGraph, int bULimit, int BUPELimit,
			Map<jq_Method, Set<jq_Method>> rmsMap, Set<jq_Method> noTDSEMs) {
		super();
		this.callGraph = callGraph;
		sccs = callGraph.getTopSortedSCCs();
		BULimit = bULimit;
		this.BUPELimit = BUPELimit;
		sccMap = new HashMap<jq_Method, Set<jq_Method>>();
		for (Set<jq_Method> scc : sccs) {
			for (jq_Method m : scc)
				sccMap.put(m, scc);
		}
		this.buCountMap = new TObjectIntHashMap<jq_Method>();
		this.waitMap = new TObjectIntHashMap<jq_Method>();
		this.rmsMap = rmsMap;
		this.noTDSEMs = noTDSEMs;
//		this.callerMap = new HashMap<jq_Method, Set<jq_Method>>();
//		for(jq_Method m:callGraph.getNodes()){
//			Set<jq_Method> callers = new HashSet<jq_Method>();
//			for(Quad q1:callGraph.getCallers(m))
//				callers.add(q1.getMethod());
//			callerMap.put(m, callers);
//		}
	}

	public int getBUTimes() {
		return BUTimes;
	}

	public int getBUMatch() {
		return BUMatch;
	}

	public int getBUUnmatch() {
		return BUUnmatch;
	}

	public int getNoCase() {
		return noCase;
	}

	public int getTDSENotReady() {
		return TDSENotReady;
	}

	public int getCaseExplode() {
		return caseExplode;
	}

	/**
	 * Use BU summary to get the path edges after the function
	 * 
	 * @param clrPE
	 * @param loc
	 * @param tgtM
	 * @return
	 */
	public Set<Pair<TDPE, Set<Pair<Loc, TDPE>>>> getBUInvkPathEdges(Quad q,
			TDPE clrPE, Loc loc, jq_Method tgtM) {
		Set<Pair<TDPE, Set<Pair<Loc, TDPE>>>> ret = new HashSet<Pair<TDPE, Set<Pair<Loc, TDPE>>>>();
		Set<BUSE> buses = summEdges.get(tgtM);
		if (buses == null || buses.size() == 0)
			return null;
		for (BUSE buse : buses) {
			TDPE ape = buse.applyInvoke(q, clrPE, loc, tgtM);
			if (ape != null) {
				Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instStateSet = this
						.getInstStates(tgtM, buse);
				Set<Pair<Loc, TDPE>> tdInstStateSet = new HashSet<Pair<Loc, TDPE>>();
				if (instStateSet != null)
					for (Pair<Loc, BUEdge<TDPE, TDSE>> instPair : instStateSet) {
						TDPE ipe = instPair.val1.applyInvokeWithoutRet(q,
								clrPE, loc, tgtM);
						if (ipe == null) {
							throw new RuntimeException(
									"Something wrong, the final buse with bupes on its path are inconsistent on the constraints!");
						}
						tdInstStateSet.add(new Pair<Loc, TDPE>(instPair.val0,
								ipe));
					}
				ret.add(new Pair<TDPE, Set<Pair<Loc, TDPE>>>(ape,
						tdInstStateSet));
			}
		}
		if(!checkExcludedClrPE(clrPE))
			if (ret != null && ret.size() > 0)
				BUMatch++;
			else
				BUUnmatch++;
		return ret;
	}

	protected abstract boolean checkExcludedClrPE(TDPE clrPE);
	
	/**
	 * Run bottom-up analysis using a given method as the root
	 * 
	 * @param root
	 * @param tdSumms
	 */
	public void runBU(jq_Method root, Map<jq_Method, Set<TDSE>> tdSumms) {
		if (DEBUG) {
			System.out.println("Start bu with " + root + " as the root");
		}
		BUTimes++;
		int sccIndex = getSCCIndex(root);
		Set<jq_Method> rootScc = sccs.get(sccIndex);
		// int curCount = Utilities.getCount(buCountMap, rootScc);
		// curCount++;
		// buCountMap.put(rootScc, curCount);
		Set<jq_Method> rms = rmsMap.get(root);
		List<Set<jq_Method>> reachSccs = new ArrayList<Set<jq_Method>>();
		reachSccs.add(rootScc);
		if (rms != null) {
			rms = new HashSet<jq_Method>(rms);
			rms.removeAll(rootScc);
			for (int i = sccIndex + 1; i < sccs.size(); i++) {
				Set<jq_Method> scci = sccs.get(i);
				jq_Method scciM = scci.iterator().next();
				if (rms.contains(scciM)) {
					rms.removeAll(scci);
					// int scciCount = Utilities.getCount(buCountMap, scci);
					// Only analyze the sccs which have few BU runs than current
					// scc
					Set<BUSE> sccSE = summEdges.get(scciM);
					if (sccSE == null || sccSE.isEmpty()) {
						reachSccs.add(scci);
						// buCountMap.put(scci, scciCount+1);
					}
				}
				if (rms.isEmpty())
					break;
			}
		}

		int i = reachSccs.size() - 1;
		try {
			// analyze the sccs in a reverse topological order
			for (; i >= 0; i--) {
				Set<jq_Method> scci = reachSccs.get(i);
				for (jq_Method m : scci) {
					Set<BUSE> summs = summEdges.get(m);
					if (summs == null) {
						summs = new HashSet<BUSE>();
						summEdges.put(m, summs);
					}
					summs.clear();
				}
				boolean changed = true;
				// Repeatedly analyze the scc util the summaries reach a fixed
				// point
//				if (scci.size() < 5)
					while (changed) {
						changed = false;
						for (jq_Method m : scci) {
							Set<TDSE> tdses = tdSumms.get(m);
							if (this.countEffectiveTDSE(tdses) == 0) {
								if (!noTDSEMs.contains(m))
//									tdses = getDefaultTDSESet(m,tdSumms.get(root));
								 throw new TDSummaryNotReadyException();
								else
									tdses = null;
							}
							changed |= runBUonMethod(m, tdses);
						}
						if (!isRecursive(scci.iterator().next()))
							break;
					}
//				else {
//					Set<jq_Method> workList = new ArraySet<jq_Method>(scci);
//					while(!workList.isEmpty()){
//						jq_Method wm = workList.iterator().next();
//						workList.remove(wm);
//						Set<TDSE> tdses = tdSumms.get(wm);
//						if (this.countEffectiveTDSE(tdses) == 0) {
//							if (!noTDSEMs.contains(wm))
//							 throw new TDSummaryNotReadyException();
//							else
//								tdses = null;
//						}
//						changed = runBUonMethod(wm, tdses);
//						if(changed){
//							for(jq_Method caller:callerMap.get(wm))
//								if(scci.contains(caller))
//									workList.add(caller);
//						}
//					}
//				}
			}
		} catch (Exception e) {
			// In the analyze, a case mismatch happens at the invoke site. Give
			// up on all the summaries of current SCC and the SCCs unanalyzed
			if (!((e instanceof NoCaseMatchException)
					|| (e instanceof BUPESizeOverflowException) || (e instanceof TDSummaryNotReadyException))) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			if (e instanceof NoCaseMatchException) {
				noCase++;
			}
			if (e instanceof BUPESizeOverflowException) {
				caseExplode++;
				for (jq_Method m : rootScc) {
				}
			}
			if (e instanceof TDSummaryNotReadyException)
				TDSENotReady++;
			if (DEBUG) {
				System.out.println("Terminate because of exception: " + e);
			}
			// for (; i >= 0; i--) {
			Set<jq_Method> scci = reachSccs.get(i);
			for (jq_Method m : scci) {
				Set<BUSE> summs = summEdges.get(m);
				if (summs == null) {
					summs = new HashSet<BUSE>();
					summEdges.put(m, summs);
				}
				summs.clear();
			}
			for (jq_Method m : rootScc) {
				if (e instanceof TDSummaryNotReadyException) {
					waitMap.put(m, waitMap.get(m) + waitValue);
				} else {
					waitMap.put(m, -1);
				}
			}
			// }
			return;
		}
		// for(jq_Method m: rootScc){
		// increaseBUCount(m);
		// }
	}

	protected abstract Set<TDSE> getDefaultTDSESet(jq_Method m,
			Set<TDSE> rootTDSEs);

	public int getMethodBUCount(jq_Method m) {
		return buCountMap.get(m);
	}

	public int increaseBUCount(jq_Method m) {
		int origin = buCountMap.get(m);
		int ret = origin + 1;
		buCountMap.put(m, ret);
		return ret;
	}

	public Set<Pair<Loc, BUEdge<TDPE, TDSE>>> getInstStates(jq_Method m,
			BUSE buse) {
		return buse.getInstStates();
	}

	protected boolean isRecursive(jq_Method m) {
		Set<jq_Method> rms = rmsMap.get(m);
		if (rms == null || rms.size() == 0)
			return false;
		return true;
	}
	
	protected Set<jq_Method> getSCC(jq_Method m){
		return rmsMap.get(m);
	}

	protected int getSCCIndex(jq_Method m) {
		for (int i = 0; i < sccs.size(); i++) {
			Set<jq_Method> scc = sccs.get(i);
			if (scc.contains(m))
				return i;
		}
		throw new RuntimeException(m + " is not contained in the call graph!");
	}

	public abstract int countEffectiveTDSE(Set<TDSE> seSet);

	/**
	 * Run bottom-up analysis on a single method. Worklist and path edge set are
	 * all allocated locally, in case we want to parallel bu in the future
	 * 
	 * @param m
	 * @param ses
	 * @throws NoCaseMatchException
	 * @throws BUPESizeOverflowException
	 */
	protected boolean runBUonMethod(jq_Method m, Set<TDSE> tdses)
			throws NoCaseMatchException, BUPESizeOverflowException {
		if (DEBUG) {
			System.out.println("Run BU on " + m);
		}
		if (tdses == null) {
			if (DEBUG) {
				System.out.println("Broken method: " + m);
			}
			return false;
		}
		Queue<Pair<Loc, BUPE>> workList = new LinkedList<Pair<Loc, BUPE>>();
		Map<Inst, Set<BUPE>> pathEdges = new HashMap<Inst, Set<BUPE>>();
		SortedSet<Constraint> constraints = new SortedArraySet<Constraint>(
				getCaseCMP(tdses));
		constraints.add(getTrue());
		MethodTuple mt = new MethodTuple();
		mt.worklist = workList;
		mt.pathEdges = pathEdges;
		mt.trackedCases = constraints;
		mt.tdses = tdses;
		mt.m = m;

		Pair<Loc, Set<BUPE>> initialPESet = getInitialBUEdge(m);

		addPathEdge(initialPESet.val0, initialPESet.val1, null, null, null, mt);

		while (!workList.isEmpty()) {
			Pair<Loc, BUPE> pair = workList.poll();
			Loc loc = pair.val0;
			Inst i = loc.i;
			BUPE bupe = pair.val1;
			boolean matched = false;
			for (Constraint tracked : mt.trackedCases)
				if (!tracked.intersect(bupe.getConstraint()).isFalse()) {
					matched = true;
					break;
				}
			if (!matched)
				continue;
			if (bupe.size() > BUPELimit)
				throw new BUPESizeOverflowException();
			if (DEBUG)
				System.out.println("Processing loc: " + loc + " BUPE: " + bupe);
			if (i instanceof BasicBlock) {
				// i is either method entry basic block, method exit basic
				// block, or an empty basic block
				BasicBlock bb = (BasicBlock) i;
				if (bb.isEntry()) {
					processEntry(loc, bupe, mt);
				} else if (bb.isExit()) {
					processExit(loc, bupe, mt);
				} else {// empty basic block
					propagatePEtoPE(loc, createSingleton(bupe), bupe, null, mt);
				}
			} else {
				Quad q = (Quad) i;
				// invoke or misc quad
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					processInvk(loc, bupe, mt);
				} else {
					Set<BUPE> peSet = transfer(bupe, q);
					if (peSet.isEmpty()){
						this.removeCase(mt, bupe.getConstraint());
					}
					propagatePEtoPE(loc, peSet, bupe, null, mt);
				}
			}
		}
		// We cannot guarantee the order that each path edge reaches exit,so we
		// need to process the edges to match the final split cases
		BasicBlock exit = m.getCFG().exit();
		Set<BUPE> bupesToLift = new HashSet<BUPE>();
		Set<BUPE> exitEdges = mt.pathEdges.get(exit);
		if (exitEdges != null)
			for (Constraint cons : mt.trackedCases)
				for (BUPE bupe : exitEdges) {
					Constraint cons1 = cons.intersect(bupe.getConstraint());
					if (cons1.isFalse())
						continue;
					bupesToLift.add((BUPE) bupe.changeConstraint(cons1));
				}
		Set<BUSE> newSumms = new HashSet<BUSE>();
		for (BUPE bupe : bupesToLift) {
			BUSE buse = lift(bupe, m);
			if (buse != null)
				newSumms.add(buse);
		}
		Set<BUSE> oldSumms = summEdges.get(m);
		newSumms = join(newSumms,oldSumms);
		if (DEBUG) {
			System.out.println("old summs: " + oldSumms);
			System.out.println("New summs: " + newSumms);
		}
		if (oldSumms.equals(newSumms))
			return false;
		if(DEBUG){
			System.out.println("TDs: "+tdses);
			System.out.println("BU: "+newSumms);
			System.out.println();
		}
		summEdges.put(m, newSumms);
		return true;
	}
	
	private Set<BUSE> join(Set<BUSE> l, Set<BUSE> r){
		if(l.isEmpty())
			return new HashSet<BUSE>(r);
		if(r.isEmpty())
			return new HashSet<BUSE>(l);
		Set<BUSE> ret = new HashSet<BUSE>();
		for(BUSE se1 : l){
			for(BUSE se2 : r){
				Constraint c = se1.getConstraint().intersect(se2.getConstraint());
				if(!c.isFalse()){
					ret.add((BUSE)se1.changeConstraint(c));
					ret.add((BUSE)se2.changeConstraint(c));
				}
			}
		}
		return ret;
	}

	/**
	 * BU fails to generate result for case cons(unimplemented part). Give up on this case
	 * @param mt
	 * @param cons
	 * @throws NoCaseMatchException
	 */
	private void removeCase(MethodTuple mt, Constraint cons) throws NoCaseMatchException{
		if(mt.trackedCases.size() == 1)
			throw new NoCaseMatchException();
		Set<Constraint> casesToRm = new HashSet<Constraint>();
		for(Constraint cons1: mt.trackedCases)
			if(cons.contains(cons1))
				casesToRm.add(cons1);
		mt.trackedCases.removeAll(casesToRm);
		if(mt.trackedCases.size() == 0)
			throw new NoCaseMatchException();
	}
	
	/**
	 * A helper method that creates a set which only contains bupe
	 * 
	 * @param bupe
	 * @return
	 */
	protected Set<BUPE> createSingleton(BUPE bupe) {
		Set<BUPE> ret = new HashSet<BUPE>();
		ret.add(bupe);
		return ret;
	}

	
	private boolean isReachableFromEntry(Inst i){
		Set<BasicBlock> visitedBB = new ArraySet<BasicBlock>();
		BasicBlock start = i.getBasicBlock();
		Queue<BasicBlock> workList = new LinkedList<BasicBlock>();
		workList.add(start);
		visitedBB.add(start);
		while (!workList.isEmpty()) {
			BasicBlock bb = workList.poll();
			for (BasicBlock pbb : bb.getPredecessors()) {
				if (pbb.isEntry())
					return true;
				if (visitedBB.add(pbb))
					workList.add(pbb);
			}
		}
		return false;
	}
		
	public boolean isMethodAnalyzed(jq_Method m) {
		Set<BUSE> summs = summEdges.get(m);
		if (summs == null || summs.size() == 0)
			return false;
		return true;
	}

	public boolean isAllRMSSummAvailable(jq_Method m,
			Map<jq_Method, Set<TDSE>> tdseMap) {
		Set<jq_Method> rms = rmsMap.get(m);
		for (jq_Method m1 : rms) {
			Set<TDSE> tdseSet = tdseMap.get(m1);
			if (this.countEffectiveTDSE(tdseSet) == 0) {
				System.out.println(m);
				System.out.println(this.getSCCIndex(m));
				return false;
			}
		}
		return true;
	}

	/**
	 * Propagate bupes to the program point after loc
	 * 
	 * @param loc
	 * @param bupes
	 * @param mt
	 */
	protected void propagatePEtoPE(Loc loc, Set<BUPE> bupes, BUPE preBUPE,
			Set<Pair<Loc, BUEdge<TDPE, TDSE>>> preSeInstStates, MethodTuple mt) {
		int qIdx = loc.qIdx;
		Inst i = loc.i;
		BasicBlock bb = i.getBasicBlock();
		if (qIdx != bb.size() - 1) {
			int q2Idx = qIdx + 1;
			Quad q2 = bb.getQuad(q2Idx);
			Loc loc2 = new Loc(q2, q2Idx);
			addPathEdge(loc2, bupes, loc, preBUPE, preSeInstStates, mt);
			return;
		}
		for (BasicBlock bb2 : bb.getSuccessors()) {
			Inst i2;
			int q2Idx;
			if (bb2.size() == 0) {
				i2 = (BasicBlock) bb2;
				q2Idx = -1;
			} else {
				i2 = bb2.getQuad(0);
				q2Idx = 0;
			}
			Loc loc2 = new Loc(i2, q2Idx);
			addPathEdge(loc2, bupes, loc, preBUPE, preSeInstStates, mt);
		}
	}

	/**
	 * Well, not sure if I need this. Summaries are generated at the end of
	 * runBUonMethod
	 * 
	 * @param bb
	 * @param bupe
	 * @param mt
	 */
	protected void processExit(Loc loc, BUPE bupe, MethodTuple mt) {

	}

	/**
	 * The method to handle invoke
	 * 
	 * @param loc
	 * @param bupe
	 * @param mt
	 * @throws NoCaseMatchException
	 */
	protected void processInvk(Loc loc, BUPE bupe, MethodTuple mt)
			throws NoCaseMatchException {
		final Quad q = (Quad) loc.i;
		final Set<jq_Method> targets = getTargets(q);
		if (targets.isEmpty()) {
			propagatePEtoPE(loc, createSingleton(bupe), bupe, null, mt);
		} else {
			for (jq_Method m2 : targets) {
				Set<BUSE> seSet = summEdges.get(m2);
				if (seSet == null) {// Well, I've created an empty set for every
									// method at least
					throw new RuntimeException(
							"The methods are sorted in topological order, how can this happen?");
				}
				if (seSet.size() == 0 && !noTDSEMs.contains(m2)) {
					Set<jq_Method> scc = sccMap.get(mt.m);
					if (!scc.contains(m2))
						throw new RuntimeException(
								"Something wrong in the method analysis order?");
					// The callee is inside the scc, has to introduce default
					// summaries to break the dependency
					seSet = getDefaultSummaries(m2);
				}
				Set<BUPE> bupesToAdd = new HashSet<BUPE>();
				Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instStatesToAdd = new HashSet<Pair<Loc, BUEdge<TDPE, TDSE>>>();
				for (BUSE buse : seSet) {
					Set<BUPE> bupeSet1 = (Set<BUPE>) buse.applyInvoke(q, bupe,
							loc, m2, mt.trackedCases, BULimit);
					if (bupeSet1 != null && !bupeSet1.isEmpty()) {
						for (BUPE bupe1 : bupeSet1)
							bupesToAdd.add(bupe1);
					}
					Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instStateSet = buse
							.getInstStates();
					if (instStateSet != null)
						for (Pair<Loc, BUEdge<TDPE, TDSE>> instPair : instStateSet) {
							Set<BUEdge<TDPE, TDSE>> bupeSet2 = instPair.val1
									.applyInvokeWithoutRet(q, bupe, loc, m2,
											mt.trackedCases, BULimit);
							for (BUEdge<TDPE, TDSE> bupe2 : bupeSet2)
								instStatesToAdd
										.add(new Pair<Loc, BUEdge<TDPE, TDSE>>(
												instPair.val0, (BUPE) bupe2));
						}
				}
				if (bupesToAdd.isEmpty() && !seSet.isEmpty()){
					this.removeCase(mt, bupe.getConstraint());
					return;
				}
				propagatePEtoPE(loc, bupesToAdd, bupe, instStatesToAdd, mt);
			}
		}
	}

	protected final Set<jq_Method> getTargets(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = callGraph.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets;
	}

	protected void processEntry(Loc loc, BUPE bupe, MethodTuple tp) {
		propagatePEtoPE(loc, createSingleton(bupe), bupe, null, tp);
	}

	/**
	 * The method where magic happens. It has to do: 1. Split the current
	 * tracked cases according to bupes. For a bupe, if bupe.constraint doesn't
	 * intersect with any of the current tracked case, simply drop it. Other
	 * wise, split the current tracked case using the intersection 2. Add the
	 * bupe in bupes to the worklist with adjusted constraint (must satisfy the
	 * current tracked cases) 3. If the tracked cases exceeds a certain limit,
	 * drop some according to the set limit
	 * 
	 * @param loc
	 * @param bupe
	 * @param tdses
	 * @param pathEdges
	 * @param workList
	 */
	protected void addPathEdge(Loc loc, Set<BUPE> bupes, Loc preLoc,
			BUPE preBUPE, Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instStateSet,
			MethodTuple mt) {
		Set<Constraint> casesToSplit = new ArraySet<Constraint>();
		Set<Constraint> newCases = new ArraySet<Constraint>();
		Set<BUPE> bupeToAdd = new ArraySet<BUPE>();
		if (DEBUG) {
			System.out.println("Try to add bupe pairs: ");
			System.out.println("Loc=" + loc + "," + bupes);
			System.out.println("Current tracked cases: " + mt.trackedCases);
		}
		for (Constraint dnf : mt.trackedCases) {
			for (BUPE bupe : bupes) {
				bupe = (BUPE) bupe.checkValid(mt.m);
				if (bupe == null)
					continue;
				Constraint nc = bupe.getConstraint().intersect(dnf);
				if (nc.isFalse())
					continue;
				newCases.add(nc);
				casesToSplit.add(dnf);
				bupe = (BUPE) bupe.changeConstraint(nc);
				bupeToAdd.add(bupe);
			}
		}
		mt.trackedCases.removeAll(casesToSplit);
		mt.trackedCases.addAll(newCases);
		if (DEBUG) {
			System.out.println("Tracked cases afterwards: " + mt.trackedCases);
		}
		while (mt.trackedCases.size() > BULimit) {
			mt.trackedCases.remove(mt.trackedCases.first());
		}
		for (BUPE bupe : bupeToAdd) {
			if (mt.trackedCases.contains(bupe.getConstraint())) {
				Set<BUPE> bupes1 = mt.pathEdges.get(loc.i);
				if (bupes1 == null) {
					bupes1 = new HashSet<BUPE>();
					mt.pathEdges.put(loc.i, bupes1);
				}
				if (bupes1.add(bupe)) {
					mt.worklist.add(new Pair<Loc, BUPE>(loc, bupe));
					// if (preLoc != null) {// for Entry
					// Pair<Loc, BUEdge<TDPE, TDSE>> prePair = new Pair<Loc,
					// BUEdge<TDPE, TDSE>>(
					// preLoc, preBUPE);
					// Pair<Loc, BUPE> curPair = new Pair<Loc, BUPE>(loc, bupe);
					// Set<Pair<Loc, BUEdge<TDPE, TDSE>>> curInstStates = bupe
					// .getInstStates();
					// Set<Pair<Loc, BUEdge<TDPE, TDSE>>> preInstStates =
					// preBUPE
					// .getInstStates();
					// if (preInstStates != null)
					// curInstStates.addAll(preInstStates);
					// if (instStateSet != null) {
					// for (Pair<Loc, BUEdge<TDPE, TDSE>> instPair :
					// instStateSet) {
					// if (instPair.val1.getConstraint().contains(
					// bupe.getConstraint()))
					// curInstStates.add(instPair);
					// }
					// }
					// if (trackedLocs.contains(preLoc)) {
					// // curInstStates.add(prePair);
					// addWithoutInstStates(curInstStates, prePair);
					// }
					// }
				}
			}
		}

	}

	private void addWithoutInstStates(
			Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instSet,
			Pair<Loc, BUEdge<TDPE, TDSE>> instPair) {
		BUEdge<TDPE, TDSE> peToAdd = instPair.val1.cloneWithoutInstStates();
		instSet.add(new Pair<Loc, BUEdge<TDPE, TDSE>>(instPair.val0, peToAdd));
	}

	class MethodTuple {
		public jq_Method m;
		public Set<TDSE> tdses;
		public Map<Inst, Set<BUPE>> pathEdges;
		public Queue<Pair<Loc, BUPE>> worklist;
		public SortedSet<Constraint> trackedCases;
	}

	public int getWait(jq_Method m) {
		return waitMap.get(m);
	}

	public Set<jq_Method> getNoTDSEMs() {
		return noTDSEMs;
	}

}
