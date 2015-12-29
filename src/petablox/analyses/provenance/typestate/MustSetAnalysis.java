package petablox.analyses.provenance.typestate;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.analyses.var.DomV;
import petablox.bddbddb.Rel.RelView;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.integer.IntPair;
import petablox.util.tuple.integer.IntTrio;
import petablox.util.tuple.object.Pair;
import soot.Local;
import soot.SootMethod;
import soot.Unit;

/**
 * Analysis for pre-computing abstract must sets.
 * Recognized properties:
 * 	chord.provenance.mustsetAlgo - Value can either be 0 or 1. Chooses the algorithm to be used
 * 	while constructing the must sets. There is performance vs memory tradeoff between the two
 * 	algorithm. Algo0 has better performance but poorer memory efficiency. 
 * <p>
 * The goal of this analysis is to pre-compute the abstract must sets to be used by a simple 
 * typestate analysis with only variable tracking enabled. These must sets, thus, do not contain
 * access paths that involve fields. However, pre-computing all possible must sets apriori using 
 * all possible combinations of variables available in DomV is not scalable. This analysis therefore
 * computes the required information lazily. We use 3 heuristics:
 * <ul>
 * 	<li>Only variable present in the allow(v) relation are considered for building must sets.</li>
 * 	<li>Only variables that are known to may-alias via 0-cfa are allowed to be present in the same must set.</li>
 * 	<li>Only variables that are local to a method may appear in the same must set.</li>
 * 	<li>Only allocation sites in trackedH are considered when checking may-aliasing of variables
 * <p>
 * This analysis outputs the following domains and relations:
 * <ul>
 *   <li>MS: domain containing all lazily constructed abstract must sets</li>
 *   <li>gen: each (ms1,v,ms2) such that ms2 = ms1 U {v}</li>
 *   <li>kill: each (ms1,v,ms2) such that ms2 = ms1 / {v}</li>
 *   <li>contain: each (v,ms) such that variable v is contained in abstract must set ms</li>
 * </ul>
 * 
 * @author Ravi Mangal
 */
@Petablox(name = "mustSet-java",
consumes = { "VH", "allow", "checkExcludedH", "trackedH" },
produces = { "MS", "gen", "kill", "contain" },
namesOfTypes = { "MS" },
types = { DomMS.class }
		)
public class MustSetAnalysis extends JavaAnalysis {
	private static final Set<Local> epsilonMustSet = Collections.emptySet();

	private Map<Pair<SootMethod,Unit>,Set<Local>> mustSetCandidates;
	private Map<SootMethod,Set<Unit>> mustSetIDs;
	private Set<Local> allowedVar;
	private Set<Unit> excludedH;
	private Set<Unit> trackedH;
	private Set<Set<Local>> addedMS;

	private int mustSetAlgoChoice;

	private DomV domV;
	private DomMS domMS;

	private ProgramRel relVH;
	private ProgramRel relallow;
	private ProgramRel relCheckExcludedH;
	private ProgramRel relTrackedH;

	private ProgramRel relgen;
	private ProgramRel relkill;
	private ProgramRel relcontain;
	
	private List<IntTrio> relgenList;
	private List<IntTrio> relkillList;
	private List<IntPair> relcontainList; //order opposite of relcontain i.e. mustset is the key in this map

	public void run() {
		mustSetAlgoChoice = Integer.getInteger("chord.provenance.mustsetAlgo", 1);

		domV = (DomV) ClassicProject.g().getTrgt("V");
		domMS = (DomMS) ClassicProject.g().getTrgt("MS");

		relVH = (ProgramRel) ClassicProject.g().getTrgt("VH");
		relallow = (ProgramRel) ClassicProject.g().getTrgt("allow");
		relCheckExcludedH = (ProgramRel) ClassicProject.g().getTrgt("checkExcludedH");
		relTrackedH = (ProgramRel) ClassicProject.g().getTrgt("trackedH");

		relgen = (ProgramRel) ClassicProject.g().getTrgt("gen");
		relkill = (ProgramRel) ClassicProject.g().getTrgt("kill");
		relcontain = (ProgramRel) ClassicProject.g().getTrgt("contain");

	//	addedMS = new HashSet<Set<Local>>();
	//	relgenList = new ArrayList<IntTrio>();
	//	relkillList = new ArrayList<IntTrio>();
	//	relcontainList = new ArrayList<IntPair>();
		
		allowedVar = new HashSet<Local>();
		relallow.load();
		Iterable<Local> tuplesAllow = relallow.getAry1ValTuples();
		for (Local v : tuplesAllow){
			allowedVar.add(v);
		}
		relallow.close();

		/* excludedH = new HashSet<Unit>();
        relCheckExcludedH.load();
        Iterable<Unit> tuplesExcludedH = relCheckExcludedH.getAry1ValTuples();
		for (Unit q : tuplesExcludedH){
			excludedH.add(q);
		}
        relCheckExcludedH.close();
		 */
		trackedH = new HashSet<Unit>();
		relTrackedH.load();
		Iterable<Unit> tuplesTrackedH = relTrackedH.getAry1ValTuples();
		for (Unit q : tuplesTrackedH){
			trackedH.add(q);
		}
		relTrackedH.close();

		mustSetCandidates = new HashMap<Pair<SootMethod,Unit>, Set<Local>>();
		mustSetIDs = new HashMap<SootMethod, Set<Unit>>();

	/*	if(domMS.size() != 0){
			//Implies that this is not the first time that domMS has been populated
			relgen.load();
			IntTrioIterable itr = relgen.getAry3IntTuples();
			for(IntTrio t : itr){
				relgenList.add(t);
			}
			relgen.close();
			
			relkill.load();
			itr = relkill.getAry3IntTuples();
			for(IntTrio t : itr){
				relkillList.add(t);
			}
			relkill.close();
			
			relcontain.load();
			IntPairIterable itr2 = relcontain.getAry2IntTuples();
			for(IntPair t : itr2){
				relcontainList.add(t);
			}
			relcontain.close();
		}
	*/	
		domMS.add(epsilonMustSet);
	//	if(domMS.add(epsilonMustSet))
	//		addedMS.add(epsilonMustSet);

		relVH.load();

		// Do the heavy crunching
		doAnalysis();

		relVH.close();

		int numMS = domMS.size();

		relgen.zero();
		relkill.zero();
		relcontain.zero();

		for (int msIdx = 0; msIdx < numMS; msIdx++) {
	//	for (Set<Local> newMS : addedMS) {
			Set<Local> newMS = domMS.get(msIdx);
			for(Local v : newMS){
				relcontain.add(v,newMS);
				relgen.add(newMS,v,newMS);

				Set<Local> prevMS = new HashSet<Local>(newMS);
				prevMS.remove(v);
				if(prevMS.size() == 0){
					relgen.add(epsilonMustSet,v,newMS);
					relkill.add(newMS,v,epsilonMustSet);
				}else{
					relgen.add(prevMS,v,newMS);
					relkill.add(newMS,v,prevMS);
				}
			}
		}
		
	/*	for(IntTrio t : relgenList){
			relgen.add(t.idx0, t.idx1, t.idx2);
		}
		for(IntTrio t : relkillList){
			relkill.add(t.idx0, t.idx1, t.idx2);
		}
		for(IntPair t : relcontainList){
			relcontain.add(t.idx0, t.idx1);
		}
	*/	
		relgen.save();
		relkill.save();
		relcontain.save();

	//	assert (domMS.size() == numMS);

	}

	private Iterable<Unit> getPointsTo(Local var) {
		RelView view = relVH.getView();
		view.selectAndDelete(0, var);
		return view.getAry1ValTuples();
	}

	private void doAnalysis() {

		for(Local v : allowedVar){
			SootMethod m = domV.getMethod(v);
			Iterable<Unit> pts = getPointsTo(v);
			for (Unit inst : pts) {
				//if(excludedH.contains(inst)) continue;
				if(!trackedH.contains(inst)) continue;
				Pair<SootMethod, Unit> mustSetID = new Pair<SootMethod, Unit>(m, inst);

				Set<Unit> mustSetIDUnits = mustSetIDs.get(mustSetID.val0);
				if(mustSetIDUnits == null){
					mustSetIDUnits = new HashSet<Unit>();
					mustSetIDs.put(mustSetID.val0, mustSetIDUnits);
				}
				mustSetIDUnits.add(mustSetID.val1);

				Set<Local> mustSetCandidate = mustSetCandidates.get(mustSetID);
				if(mustSetCandidate == null){
					mustSetCandidate = new HashSet<Local>();
					mustSetCandidates.put(mustSetID, mustSetCandidate);
				}
				mustSetCandidate.add(v);
			}
		}
		System.out.println("Generated " + mustSetCandidates.size() + " must set groups");

		if(mustSetAlgoChoice != 0){
			for(Pair<SootMethod, Unit> p : mustSetCandidates.keySet()){
				Set<Local> mustSetCandidate = mustSetCandidates.get(p);
				System.out.println("Generating for candidateID " + p + " with number " +
						"of variables=" +  mustSetCandidate.size());
				generatePowerSet(mustSetCandidate.toArray());
			}
		}else{
			for(SootMethod m : mustSetIDs.keySet()){
				Set<Local> mustSetComboCandidate = new HashSet<Local>();
				int numPartitions = mustSetIDs.get(m).size();
				Map<Local,BitSet> partitionMembership = new HashMap<Local, BitSet>();
				int partitionID = 0;

				for(Unit q : mustSetIDs.get(m)){
					Set<Local> mustSetCandidate = mustSetCandidates.get(new Pair<SootMethod, Unit>(m, q));
					mustSetComboCandidate.addAll(mustSetCandidate);
					for(Local var : mustSetCandidate){
						BitSet partitionBitSet = partitionMembership.get(var);
						if(partitionBitSet == null){
							partitionBitSet = new BitSet(numPartitions);
							partitionMembership.put(var, partitionBitSet);
						}
						partitionBitSet.set(partitionID);
					}
					partitionID++;
				}
				Object[] mustSetComboCandidateArr = mustSetComboCandidate.toArray();
				BitSet[] partitionMembershipArr = new BitSet[mustSetComboCandidate.size()];
				int index = 0;
				for(Object o : mustSetComboCandidateArr){
					partitionMembershipArr[index] = partitionMembership.get((Local)o);
					index++;
				}
				System.out.println("For method " + m + ", number of partitions=" + numPartitions + " & number " +
						"of variables=" +  mustSetComboCandidate.size());

				generatePowerSet(mustSetComboCandidateArr, partitionMembershipArr, numPartitions);
			}
		}

		domMS.save();
	}

	/**
	 * Generates the power set from the given set by using a binary counter
	 * and adds all corresponding sets to the MS domain
	 * Example: S = {a,b,c}
	 * P(S) = {[], [c], [b], [b, c], [a], [a, c], [a, b], [a, b, c]}
	 */
	private void generatePowerSet(Object[] set) {

		//create the empty power set
		//HashSet<Set<Local>> power = new HashSet<Set<Local>>();

		//get the number of elements in the set
		int elements = set.length;
		BitSet bset = new BitSet(elements + 1);

		//Since we dont want to add the empty set to DomMS again,
		//we start the bit counter from 1
		bset.set(0); 

		while(!bset.get(elements)){
			HashSet<Local> innerSet = new HashSet<Local>();
			for(int i = 0; i < elements; i++)
			{
				if(bset.get(i))
					innerSet.add((Local) set[i]);
			}
			//increment bset (simple adder)
			for(int i = 0; i < bset.size(); i++) 
			{
				if(!bset.get(i))
				{
					bset.set(i);
					break;
				}else
					bset.clear(i);
			}
			//add the new set to the power set
			//power.add(innerSet);
			domMS.add(innerSet);
		//	if(domMS.add(innerSet))
		//		addedMS.add(innerSet);
		}
	}

	private void generatePowerSet(Object[] set, BitSet[] partitionMembership, int numPartitions) {

		//create the empty power set
		//HashSet<Set<Local>> power = new HashSet<Set<Local>>();

		//get the number of elements in the set
		int elements = set.length;
		BitSet bset = new BitSet(elements + 1);

		BitSet allSet = new BitSet(numPartitions);
		allSet.set(0, numPartitions);

		//Since we dont want to add the empty set to DomMS again,
		//we start the bit counter from 1
		bset.set(0); 

		while(!bset.get(elements)){
			BitSet validChoice = new BitSet(numPartitions);
			validChoice.or(allSet);
			HashSet<Local> innerSet = new HashSet<Local>();
			for(int i = 0; i < elements; i++)
			{
				if(bset.get(i)){
					validChoice.and(partitionMembership[i]);
					innerSet.add((Local) set[i]);
				}
			}
			//increment bset (simple adder)
			for(int i = 0; i < bset.size(); i++) 
			{
				if(!bset.get(i))
				{
					bset.set(i);
					break;
				}else
					bset.clear(i);
			}
			if(!validChoice.isEmpty()){
				//add the new set to the power set
				//power.add(innerSet);
				domMS.add(innerSet);
			//	if(domMS.add(innerSet))
			//		addedMS.add(innerSet);
			}
		}
	}

	/*   private Set<Set<Local>> generatePowerSetRecursive(Set<Local> originalSet) {
	    Set<Set<Local>> sets = new HashSet<Set<Local>>();
	    if (originalSet.isEmpty()) {
	    	sets.add(new HashSet<Local>());
	    	return sets;
	    }
	    List<Local> list = new ArrayList<Local>(originalSet);
	    Local head = list.get(0);
	    Set<Local> rest = new HashSet<Local>(list.subList(1, list.size()));
	    if(domMS.contains(rest))

	    for (Set<Local> set : generatePowerSetRecursive(rest)) {
	    	Set<Local> newSet = new HashSet<Local>();
	    	newSet.add(head);
	    	newSet.addAll(set);
	    	sets.add(newSet);
	    	sets.add(set);
	    }		
	    return sets;
	}
	 */	
}
