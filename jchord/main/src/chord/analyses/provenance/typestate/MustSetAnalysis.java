package chord.analyses.provenance.typestate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.bddbddb.Rel.IntPairIterable;
import chord.bddbddb.Rel.IntTrioIterable;
import chord.bddbddb.Rel.RelView;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.object.Pair;

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
@Chord(name = "mustSet-java",
consumes = { "VH", "allow", "checkExcludedH", "trackedH" },
produces = { "MS", "gen", "kill", "contain" },
namesOfTypes = { "MS" },
types = { DomMS.class }
		)
public class MustSetAnalysis extends JavaAnalysis {
	private static final Set<Register> epsilonMustSet = Collections.emptySet();

	private Map<Pair<jq_Method,Quad>,Set<Register>> mustSetCandidates;
	private Map<jq_Method,Set<Quad>> mustSetIDs;
	private Set<Register> allowedVar;
	private Set<Quad> excludedH;
	private Set<Quad> trackedH;
	private Set<Set<Register>> addedMS;

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

	//	addedMS = new HashSet<Set<Register>>();
	//	relgenList = new ArrayList<IntTrio>();
	//	relkillList = new ArrayList<IntTrio>();
	//	relcontainList = new ArrayList<IntPair>();
		
		allowedVar = new HashSet<RegisterFactory.Register>();
		relallow.load();
		Iterable<Register> tuplesAllow = relallow.getAry1ValTuples();
		for (Register v : tuplesAllow){
			allowedVar.add(v);
		}
		relallow.close();

		/* excludedH = new HashSet<Quad>();
        relCheckExcludedH.load();
        Iterable<Quad> tuplesExcludedH = relCheckExcludedH.getAry1ValTuples();
		for (Quad q : tuplesExcludedH){
			excludedH.add(q);
		}
        relCheckExcludedH.close();
		 */
		trackedH = new HashSet<Quad>();
		relTrackedH.load();
		Iterable<Quad> tuplesTrackedH = relTrackedH.getAry1ValTuples();
		for (Quad q : tuplesTrackedH){
			trackedH.add(q);
		}
		relTrackedH.close();

		mustSetCandidates = new HashMap<Pair<jq_Method,Quad>, Set<Register>>();
		mustSetIDs = new HashMap<jq_Method, Set<Quad>>();

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
	//	for (Set<Register> newMS : addedMS) {
			Set<Register> newMS = domMS.get(msIdx);
			for(Register v : newMS){
				relcontain.add(v,newMS);
				relgen.add(newMS,v,newMS);

				Set<Register> prevMS = new HashSet<RegisterFactory.Register>(newMS);
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

	private Iterable<Quad> getPointsTo(Register var) {
		RelView view = relVH.getView();
		view.selectAndDelete(0, var);
		return view.getAry1ValTuples();
	}

	private void doAnalysis() {

		for(Register v : allowedVar){
			jq_Method m = domV.getMethod(v);
			Iterable<Quad> pts = getPointsTo(v);
			for (Quad inst : pts) {
				//if(excludedH.contains(inst)) continue;
				if(!trackedH.contains(inst)) continue;
				Pair<jq_Method, Quad> mustSetID = new Pair<jq_Method, Quad>(m, inst);

				Set<Quad> mustSetIDQuads = mustSetIDs.get(mustSetID.val0);
				if(mustSetIDQuads == null){
					mustSetIDQuads = new HashSet<Quad>();
					mustSetIDs.put(mustSetID.val0, mustSetIDQuads);
				}
				mustSetIDQuads.add(mustSetID.val1);

				Set<Register> mustSetCandidate = mustSetCandidates.get(mustSetID);
				if(mustSetCandidate == null){
					mustSetCandidate = new HashSet<RegisterFactory.Register>();
					mustSetCandidates.put(mustSetID, mustSetCandidate);
				}
				mustSetCandidate.add(v);
			}
		}
		System.out.println("Generated " + mustSetCandidates.size() + " must set groups");

		if(mustSetAlgoChoice != 0){
			for(Pair<jq_Method, Quad> p : mustSetCandidates.keySet()){
				Set<Register> mustSetCandidate = mustSetCandidates.get(p);
				System.out.println("Generating for candidateID " + p + " with number " +
						"of variables=" +  mustSetCandidate.size());
				generatePowerSet(mustSetCandidate.toArray());
			}
		}else{
			for(jq_Method m : mustSetIDs.keySet()){
				Set<Register> mustSetComboCandidate = new HashSet<RegisterFactory.Register>();
				int numPartitions = mustSetIDs.get(m).size();
				Map<Register,BitSet> partitionMembership = new HashMap<RegisterFactory.Register, BitSet>();
				int partitionID = 0;

				for(Quad q : mustSetIDs.get(m)){
					Set<Register> mustSetCandidate = mustSetCandidates.get(new Pair<jq_Method, Quad>(m, q));
					mustSetComboCandidate.addAll(mustSetCandidate);
					for(Register var : mustSetCandidate){
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
					partitionMembershipArr[index] = partitionMembership.get((Register)o);
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
		//HashSet<Set<Register>> power = new HashSet<Set<Register>>();

		//get the number of elements in the set
		int elements = set.length;
		BitSet bset = new BitSet(elements + 1);

		//Since we dont want to add the empty set to DomMS again,
		//we start the bit counter from 1
		bset.set(0); 

		while(!bset.get(elements)){
			HashSet<Register> innerSet = new HashSet<RegisterFactory.Register>();
			for(int i = 0; i < elements; i++)
			{
				if(bset.get(i))
					innerSet.add((Register) set[i]);
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
		//HashSet<Set<Register>> power = new HashSet<Set<Register>>();

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
			HashSet<Register> innerSet = new HashSet<RegisterFactory.Register>();
			for(int i = 0; i < elements; i++)
			{
				if(bset.get(i)){
					validChoice.and(partitionMembership[i]);
					innerSet.add((Register) set[i]);
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

	/*   private Set<Set<Register>> generatePowerSetRecursive(Set<Register> originalSet) {
	    Set<Set<Register>> sets = new HashSet<Set<Register>>();
	    if (originalSet.isEmpty()) {
	    	sets.add(new HashSet<Register>());
	    	return sets;
	    }
	    List<Register> list = new ArrayList<Register>(originalSet);
	    Register head = list.get(0);
	    Set<Register> rest = new HashSet<Register>(list.subList(1, list.size()));
	    if(domMS.contains(rest))

	    for (Set<Register> set : generatePowerSetRecursive(rest)) {
	    	Set<Register> newSet = new HashSet<Register>();
	    	newSet.add(head);
	    	newSet.addAll(set);
	    	sets.add(newSet);
	    	sets.add(set);
	    }		
	    return sets;
	}
	 */	
}
