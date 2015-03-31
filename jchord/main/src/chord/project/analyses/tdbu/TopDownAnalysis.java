package chord.project.analyses.tdbu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import chord.program.Loc;
import chord.project.analyses.rhs.IEdge;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.project.analyses.rhs.TraceKind;
import chord.util.tuple.object.Pair;

/**
 * The TopDownAnalysis in TD-BU work. It uses the bottom-up summaries and will
 * fire BU analysis if possible. Extends RHSAnalysis. Trace algorithm not
 * implemented yet.
 * 
 * @author xin
 * 
 * @param <TDPE>
 * @param <TDSE>
 * @param <BUSE>
 */
public abstract class TopDownAnalysis<TDPE extends IEdge, TDSE extends IEdge, BUPE extends BUEdge<TDPE, TDSE>, BUSE extends BUEdge<TDPE, TDSE>>
		extends RHSAnalysis<TDPE, TDSE> {
	private int tdLimit;
//	private Map<jq_Method, Integer> buCountMap;
	private BottomUpAnalysis<TDPE, TDSE, BUPE, BUSE> bu;
	private Set<jq_Method> buMethods;
	private boolean buAllMs;
	private boolean autoAdjustBU;
	private Map<jq_Method,Boolean> tmpMap;

	public TopDownAnalysis(int tdLimit, boolean autoAdjustBU) {
		super();
		this.tdLimit = tdLimit;
		this.bu = null;
//		buCountMap = new HashMap<jq_Method, Integer>();
		this.autoAdjustBU = autoAdjustBU;
		buAllMs = true;
		tmpMap = new HashMap<jq_Method,Boolean>();
	}


	public boolean checkNoTDSEsM(){
		Set<jq_Method> ms = bu.getNoTDSEMs();
		for(jq_Method m:ms)
			if(bu.countEffectiveTDSE(summEdges.get(m))>0)
				return false;
		return true;
	}
	
	public void setBU(BottomUpAnalysis<TDPE, TDSE, BUPE, BUSE> bu) {
		this.bu = bu;
	}

	public void setBUMethods(Set<jq_Method> buMethods) {
		this.buMethods = buMethods;
		buAllMs = false;
	}

	/**
	 * Not used any more. Originally I used it to do some hacky thing to optimize the speed of top-down analysis
	 * @param loc
	 * @param pe
	 * @return
	 */
	protected abstract Set<TDPE> hack(Loc loc,TDPE pe);
	
	@Override
	protected void processInvk(Loc loc, TDPE pe) {
		final Quad q = (Quad) loc.i;
		final Set<jq_Method> targets = getTargets(q);
//		Set<TDPE> hackPEs = hack(loc,pe);
//		if(hackPEs!=null){
//			for(TDPE pe2 : hackPEs)
//				propagatePEtoPE(loc, pe2, pe, null, null);
//			return;
//		}
		if (targets.isEmpty()) {
			final TDPE pe2 = mayMerge ? getPECopy(pe) : pe;
			propagatePEtoPE(loc, pe2, pe, null, null);
		} else {
			for (jq_Method m2 : targets) {
				if (DEBUG){
					System.out.println("\tTarget: " + m2);
				System.out.println("Loc: "+loc+", method: "+m2+", incoming pe: "+pe);}
				if (bu != null) {
					Set<Pair<TDPE, Set<Pair<Loc, TDPE>>>> invokePes = bu
							.getBUInvkPathEdges(q, pe, loc, m2);
					if (invokePes != null && invokePes.size() > 0) {
						for (Pair<TDPE, Set<Pair<Loc, TDPE>>> pe2Pair : invokePes) {
							if(DEBUG){
							System.out.println("PEs got by bu: "+pe2Pair.val0);
							}
							propagatePEtoPE(loc, pe2Pair.val0, pe, m2, null);// currently,
																				// we
							// don't support
							// trace for
							// TD-BU
							for (Pair<Loc, TDPE> instStatePair : pe2Pair.val1) {
								processInstState(instStatePair.val0,
										instStatePair.val1);
							}
						}
						if(DEBUG){
						System.out.println(m2+" handled by BU");
						}
						continue;
					}
				}
				if(DEBUG){
				System.out.println(m2+" unhandled by BU");}
				final TDPE pe2 = getInitPathEdge(q, m2, pe);
				BasicBlock bb2 = m2.getCFG().entry();
				Loc loc2 = new Loc(bb2, -1);
				addPathEdge(loc2, pe2, q, pe, null, null);
				final Set<TDSE> tdSeSet = summEdges.get(m2);// If no BU
															// summaries
															// available, check
															// TD summaries
				if (tdSeSet == null) {
					if (DEBUG)
						System.out.println("\tTD SE set empty");
					continue;
				}
				for (TDSE tdSe : tdSeSet) {
					if (DEBUG)
						System.out.println("\tTesting TD SE: " + tdSe);
					if (propagateSEtoPE(pe, loc, m2, tdSe)) {
						if (DEBUG)
							System.out.println("\tTD Matched");
						if (mustMerge) {
							// this was only SE; stop looking for more
							break;
						}
					} else {
						if (DEBUG)
							System.out.println("\tDid not match");
					}
				}
			}
		}
	}

	/**
	 * A hook for analyses like mustalias/typestate to check the query at the callSite
	 * @param val0
	 * @param val1
	 */
	protected abstract void processInstState(Loc val0, TDPE val1);

	@Override
	protected void processExit(BasicBlock bb, TDPE pe) {
		jq_Method m = bb.getMethod();
		TDSE se = getSummaryEdge(m, pe);
		Set<TDSE> seSet = summEdges.get(m);
		if (DEBUG)
			System.out.println("\tChecking if " + m + " has TDSE: " + se);
		TDSE seToAdd = se;
		if (seSet == null) {
			seSet = new HashSet<TDSE>();
			summEdges.put(m, seSet);
			seSet.add(se);
			if (DEBUG)
				System.out.println("\tNo, adding it as first TDSE");
		} else if (mayMerge) {
			boolean matched = false;
			for (TDSE se2 : seSet) {
				int result = se2.canMerge(se, mustMerge);
				if (result >= 0) {
					if (DEBUG)
						System.out.println("\tNo, but matches TDSE: " + se2);
					boolean changed = se2.mergeWith(se);
					if (DEBUG)
						System.out.println("\tNew SE after merge: " + se2);
					if (!changed) {
						if (DEBUG)
							System.out.println("\tExisting SE did not change");
						if (traceKind != TraceKind.NONE && result == 0)
							updateWSE(m, se2, bb, pe);
						return;
					}
					if (DEBUG)
						System.out.println("\tExisting SE changed");
					// se2 is already in summEdges(m), so no need to add it
					seToAdd = se2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG)
					System.out.println("\tNo, adding");
				seSet.add(se);
			}
		} else if (!seSet.add(se)) {
			if (DEBUG)
				System.out.println("\tYes, not adding");
			if (traceKind != TraceKind.NONE)
				updateWSE(m, se, bb, pe);
			return;
		}
		if (traceKind != TraceKind.NONE) {
			recordWSE(m, seToAdd, bb, pe);
		}
		for (Quad q2 : getCallers(m)) {
			if (DEBUG)
				System.out.println("\tCaller: " + q2 + " in " + q2.getMethod());
			Set<TDPE> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			// make a copy as propagateSEtoPE might add a path edge to this set
			// itself;
			// in this case we could get a ConcurrentModification exception if
			// we don't
			// make a copy.
			List<TDPE> peList = new ArrayList<TDPE>(peSet);
			Loc loc2 = invkQuadToLoc.get(q2);
			for (TDPE pe2 : peList) {
				if (DEBUG)
					System.out.println("\tTesting PE: " + pe2);
				boolean match = propagateSEtoPE(pe2, loc2, m, seToAdd);
				if (match) {
					if (DEBUG)
						System.out.println("\tMatched");
				} else {
					if (DEBUG)
						System.out.println("\tDid not match");
				}
			}
		}
		// Start to check the number of available SEs
		if ((buAllMs||buMethods.contains(m))&&tdLimit>0) {
			if(bu.isMethodAnalyzed(m))
				return;
			int tdSize = bu.countEffectiveTDSE(seSet);
			if (tdSize > tdLimit) {
				int wait = bu.getWait(m);
				if(wait>=0)
				if(tdSize > tdLimit+wait)
				bu.runBU(m, summEdges);
//				Boolean tmp = tmpMap.get(m);
//				if(tmp == null){
//					tmp = bu.isAllRMSSummAvailable(m, summEdges);
//					System.out.println(m);
//					tmpMap.put(m, tmp);
//				}
			}
		}
	}
	
	public void printSummStats(){
		int totalMethods = tmpMap.size();
		System.out.println("Total number of methods: "+totalMethods);
		int avai = 0;
		int unavai = 0;
		for(Map.Entry<jq_Method, Boolean> entry:tmpMap.entrySet())
			if(entry.getValue())
				avai++;
			else unavai++;
		System.out.println("Avai "+avai);
		System.out.println("Unavai "+unavai);
	}

}
