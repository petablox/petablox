package chord.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.provenance.FormatedConstraint;
import chord.project.analyses.provenance.LookUpRule;
import chord.project.analyses.provenance.MaxSatGenerator;
import chord.project.analyses.provenance.ParamTupleConsHandler;
import chord.project.analyses.provenance.Tuple;
import chord.util.tuple.object.Pair;

public class PTHandler implements ParamTupleConsHandler {
	private Set<String> derivedRs;
	private Set<Tuple> constTuples;
	private boolean ifMono;
	private ProgramRel denyRel;
	
	public PTHandler(boolean ifMono){
		this.ifMono = ifMono;
		this.constTuples = new HashSet<Tuple>();
		//TODO add constant tuples if necessary
		denyRel = (ProgramRel) ClassicProject.g().getTrgt("deny");
	}
	
	@Override
	public void init(List<LookUpRule> rules) {
		derivedRs = new HashSet<String>();
		for(LookUpRule r : rules)
			derivedRs.add(r.getHeadRelName());
	}

	@Override
	public int getWeight(Tuple t) {
		String relName = t.getRelName();
		if(relName.equals("deny")){
			return 1;
		}
		else
			throw new RuntimeException("Not a param tuple: "+t);
	}

	@Override
	public Set<FormatedConstraint> getHardCons(int w, Set<Tuple> paramTSet, MaxSatGenerator gen) {
		return new HashSet<FormatedConstraint>();
	}

	@Override
	public Pair<Tuple, Boolean> transform(Tuple t) {
		if(constTuples.contains(t))//rootCM(0,0), reachable(0,0)
			return null;
		Pair<Tuple,Boolean> ret = new Pair<Tuple,Boolean>(null,null);
		String relName = t.getRelName();
		if(relName.equals("allow")){
			if(ifMono)
				return null;
			Tuple deny = new Tuple(denyRel,t.getIndices());
			ret.val0 = deny;
			ret.val1 = false;
			return ret;
		}
		if(relName.equals("deny")){
			ret.val0 = t;
			ret.val1 = true;
			return ret;
		}
		if(derivedRs.contains(t.getRelName())){
			ret.val0 = t;
			ret.val1 = true;
			return ret;
		}
		return null;//non parametric inputs
	}

	@Override
	public boolean isParam(Tuple t) {
		String relName = t.getRelName();
		if(relName.equals("deny")){
			return true;
		}
		return false;
	}

}
