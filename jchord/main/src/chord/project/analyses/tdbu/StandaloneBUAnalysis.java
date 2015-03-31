package chord.project.analyses.tdbu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
import chord.util.Timer;
import chord.util.tuple.object.Pair;

public abstract class StandaloneBUAnalysis<TDPE extends IEdge,TDSE extends IEdge,BUPE extends BUEdge<TDPE,TDSE>,BUSE extends BUEdge<TDPE,TDSE>> 
extends BottomUpAnalysis<TDPE, TDSE , BUPE, BUSE> 
{
	protected final String CHORD_BU_MERGE = "chord.bottom-up.merge"; //[naive|pjoin]
	enum JOIN{pjoin,naive};
	JOIN joinkind;

	public StandaloneBUAnalysis(ICICG callGraph,Map<jq_Method, Set<jq_Method>> rmsMap) {
		super(callGraph, Integer.MAX_VALUE, Integer.MAX_VALUE, rmsMap, null);
	}

	public void run(){
		String joinKind = System.getProperty(CHORD_BU_MERGE, "naive");
		if(joinKind.equals("pjoin"))
			joinkind = JOIN.pjoin;
		else
			joinkind = JOIN.naive;
		Set<jq_Method> roots = callGraph.getRoots();
		for(jq_Method m : roots)
			runBU(m,null);
	}

	@Override
	public void runBU(jq_Method root, Map<jq_Method, Set<TDSE>> tdSumms) {
		if (DEBUG) {
			System.out.println("Start bu with " + root + " as the root");
		}
		int sccIndex = getSCCIndex(root);
		Set<jq_Method> rootScc = sccs.get(sccIndex);
		Set<jq_Method> rms = super.getSCC(root);
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
					Set<BUSE> sccSE = summEdges.get(scciM);
					if (sccSE == null || sccSE.isEmpty()) {
						reachSccs.add(scci);
					}
				}
				if (rms.isEmpty())
					break;
			}
		}

		int i = reachSccs.size() - 1;
		try {
			// analyze the sccs in a reverse topological order
			out:for (; i >= 0; i--) {
				Set<jq_Method> scci = reachSccs.get(i);
				System.out.println("Analyzing SCC: "+scci);
				Timer tempTimer = new Timer("temp");
				tempTimer.init();
				for (jq_Method m : scci) {
					Set<BUSE> summs = summEdges.get(m);
					if (summs == null) {
						summs = new HashSet<BUSE>();
						summEdges.put(m, summs);
					}
					if(!summs.isEmpty()) //avoid reanalyzing
						continue out;
				}
				boolean changed = true;
				// Repeatedly analyze the scc util the summaries reach a fixed
				// point
				while (changed) {
					changed = false;
					for (jq_Method m : scci) {
						changed |= runBUonMethod(m, null);
					}
					if (!isRecursive(scci.iterator().next()))
						break;
				}
				tempTimer.done();
				long it = tempTimer.getInclusiveTime();
				System.out.println("Running time: "+Timer.getTimeStr(it));
				if(it >1000*60)
					System.out.println("Critical!");
			}
		} catch (NoCaseMatchException e) {
			throw new RuntimeException(e);
		} catch (BUPESizeOverflowException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected boolean runBUonMethod(jq_Method m, Set<TDSE> tdses) throws NoCaseMatchException, BUPESizeOverflowException {
		if (DEBUG) {
			System.out.println("Run BU on " + m);
		}
		Queue<Pair<Loc, BUPE>> workList = new LinkedList<Pair<Loc, BUPE>>();
		Map<Inst, Set<BUPE>> pathEdges = new HashMap<Inst, Set<BUPE>>();
		MethodTuple mt = new MethodTuple();
		mt.worklist = workList;
		mt.pathEdges = pathEdges;
		mt.m = m;

		Pair<Loc, Set<BUPE>> initialPESet = getInitialBUEdge(m);

		addPathEdge(initialPESet.val0, initialPESet.val1, null, null, null, mt);

		while (!workList.isEmpty()) {
			Pair<Loc, BUPE> pair = workList.poll();
			Loc loc = pair.val0;
			Inst i = loc.i;
			BUPE bupe = pair.val1;
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
//						this.removeCase(mt, bupe.getConstraint());
					}
					propagatePEtoPE(loc, peSet, bupe, null, mt);
				}
			}
		}
		BasicBlock exit = m.getCFG().exit();
		Set<BUPE> bupesToLift = mt.pathEdges.get(exit);
		Set<BUSE> newSumms = new HashSet<BUSE>();
		if(bupesToLift!=null)
			for (BUPE bupe : bupesToLift) {
				BUSE buse = lift(bupe, m);
				if (buse != null)
					newSumms.add(buse);
			}
		Set<BUSE> oldSumms = summEdges.get(m);
		if (DEBUG) {
			System.out.println("old summs: " + oldSumms);
			System.out.println("New summs: " + newSumms);
		}
		boolean ifAdded = false;
		if(joinkind == JOIN.naive)
			ifAdded |= oldSumms.addAll(newSumms);
		else{
			for(BUSE buse : newSumms){

				Set<BUSE> buseToRm = new ArraySet<BUSE>();
				boolean ifContain = false;
				for(BUSE exBuse : oldSumms){
					int mv = exBuse.canMerge(buse);
					if(mv >=0){
						ifContain = true;
						break;
					}
					if(mv == -1)
						buseToRm.add(exBuse);
				}
				if(!ifContain){
					ifAdded |= oldSumms.add(buse);
				}
				oldSumms.removeAll(buseToRm);
			}
		}
		return ifAdded;
	}


	@Override
	protected void processInvk(Loc loc, BUPE bupe, MethodTuple mt) throws NoCaseMatchException {
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
				if (seSet.size() == 0) {
					Set<jq_Method> scc = sccMap.get(mt.m);
					if (!scc.contains(m2))
						throw new RuntimeException(
								"Something wrong in the method analysis order?");
					// The callee is inside the scc, has to introduce default
					// summaries to break the dependency
					seSet = getDefaultSummaries(m2);
				}
				Set<BUPE> bupesToAdd = new HashSet<BUPE>();
				for (BUSE buse : seSet) {
					Set<BUPE> bupeSet1 = (Set<BUPE>) buse.applyInvoke(q, bupe,
							loc, m2, mt.trackedCases, BULimit);
					if (bupeSet1 != null && !bupeSet1.isEmpty()) {
						for (BUPE bupe1 : bupeSet1)
							bupesToAdd.add(bupe1);
					}
				}
//				if (bupesToAdd.isEmpty() && !seSet.isEmpty()){
//					throw new RuntimeException("Something wrong while apply the BU summary!");
//				}
				propagatePEtoPE(loc, bupesToAdd, bupe, null, mt);
			}
		}
	}


	@Override
	protected void addPathEdge(Loc loc, Set<BUPE> bupes, Loc preLoc, BUPE preBUPE, Set<Pair<Loc, BUEdge<TDPE, TDSE>>> instStateSet, MethodTuple mt) {
		if (DEBUG) {
			System.out.println("Try to add bupe pairs: ");
			System.out.println("Loc=" + loc + "," + bupes);
		}
		for (BUPE bupe : bupes) {
			bupe = (BUPE)bupe.checkValid(loc.i.getMethod());
			if(bupe == null)
				continue;
			Set<BUPE> bupes1 = mt.pathEdges.get(loc.i);
			if (bupes1 == null) {
				bupes1 = new HashSet<BUPE>();
				mt.pathEdges.put(loc.i, bupes1);
			}
			boolean added = false;
			if(joinkind == JOIN.naive)
				added = bupes1.add(bupe);
			else{
				Set<BUPE> bupeToRm = new ArraySet<BUPE>();
				boolean ifContain = false;
				for(BUPE exBupe : bupes1){
					int mv = exBupe.canMerge(bupe);
					if(mv >=0){
						ifContain = true;
						break;
					}
					if(mv == -1)
						bupeToRm.add(exBupe);
				}
				if(!ifContain){
					added = bupes1.add(bupe);
				}
				bupes1.removeAll(bupeToRm);
			}
			if (added) {
				mt.worklist.add(new Pair<Loc, BUPE>(loc, bupe));
			}
		}

	}


	@Override
	protected CaseTDSEComparator<TDSE> getCaseCMP(Set<TDSE> tdses) {
		return null;
	}

	@Override
	protected boolean checkExcludedClrPE(TDPE clrPE) {
		return false;
	}

	@Override
	protected Set<TDSE> getDefaultTDSESet(jq_Method m, Set<TDSE> rootTDSEs) {
		return null;
	}

	@Override
	public int countEffectiveTDSE(Set<TDSE> seSet) {
		return 0;
	}


}
