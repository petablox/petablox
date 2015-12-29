package petablox.analyses.provenance.kcfa;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import petablox.project.analyses.provenance.ConstraintItem;
import petablox.project.analyses.provenance.LookUpRule;
import petablox.project.analyses.provenance.Model;
import petablox.project.analyses.provenance.Tuple;
import petablox.util.tuple.object.Pair;

public class KCFAModel implements Model {
	Set<Tuple> tupleSet;
	int weight;
	PTHandler ptHandler;

	public KCFAModel(PTHandler ptHandler) {
		this.ptHandler = ptHandler;
		this.tupleSet = new HashSet<Tuple>();
		weight = 10;
	}

	private boolean isLikelyTrueTuple(Tuple t) { 
		return t.getRel().toString().startsWith("reachable");
	} 

	public void build(List<LookUpRule> rules) {
		int x;
		tupleSet.clear();
		for (LookUpRule r : rules) {
			Iterator<ConstraintItem> iter = r.getAllConstrIterator();
			while (iter.hasNext()) {
				ConstraintItem it = iter.next();
				Pair<Tuple, Boolean> ht = ptHandler.transform(it.headTuple);
				if (ht != null && isLikelyTrueTuple(ht.val0))
					tupleSet.add(ht.val0);
			}
		}
	}

	public int getTotalWeight() {
		return (weight * tupleSet.size());
	}
	
	public int getNumConstraints() {
		return tupleSet.size();
	}

	public Set<Pair<Tuple,Integer>> getWeightedTuples() {
		Set<Pair<Tuple,Integer>> res = new HashSet<Pair<Tuple,Integer>>();
		for (Tuple t : tupleSet) {
			res.add(new Pair<Tuple,Integer>(t, weight));
		}	
		return res;
	}
}
