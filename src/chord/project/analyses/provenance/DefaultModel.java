package chord.project.analyses.provenance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chord.util.tuple.object.Pair;

/**
 * The default model for constraint generation without any bias
 * @author xin
 *
 */
public class DefaultModel implements Model {

	@Override
	public void build(List<LookUpRule> rules) {

	}

	@Override
	public int getTotalWeight() {
		return 0;
	}

	@Override
	public int getNumConstraints() {
		return 0;
	}

	@Override
	public Set<Pair<Tuple, Integer>> getWeightedTuples() {
		return new HashSet<Pair<Tuple, Integer>>();
	}

}
