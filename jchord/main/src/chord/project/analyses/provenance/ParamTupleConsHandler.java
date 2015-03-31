package chord.project.analyses.provenance;

import java.util.List;
import java.util.Set;

import chord.util.tuple.object.Pair;

public interface ParamTupleConsHandler {
	public void init(List<LookUpRule> rules);
	public int getWeight(Tuple t);
	public Set<FormatedConstraint> getHardCons(int w, Set<Tuple> paramTSet, MaxSatGenerator gen);
	/**
	 * A hacky method to transform Tuple t to another tuple. If it returns <t,true> it's transformed to
	 * t, if <t,false> is returned, it is transformed to !t
	 * Return null, if this tuple cannot be eliminated
	 * @param t
	 * @return
	 */
	public Pair<Tuple,Boolean> transform(Tuple t);
	public boolean isParam(Tuple t);
}
