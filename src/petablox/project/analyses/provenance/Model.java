package petablox.project.analyses.provenance;

import java.util.List;
import java.util.Set;

import petablox.util.tuple.object.Pair;

public interface Model {
	public void build(List<LookUpRule> rules);
	public int getTotalWeight();
	public int getNumConstraints();
	public Set<Pair<Tuple,Integer>> getWeightedTuples();
}
