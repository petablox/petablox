package petablox.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import petablox.project.analyses.provenance.LookUpRule;
import petablox.project.analyses.provenance.Model;
import petablox.project.analyses.provenance.Tuple;
import petablox.util.tuple.object.Pair;

public class TypeStateModel implements Model {
        PTHandler ptHandler;

        public TypeStateModel(PTHandler ptHandler) {
                this.ptHandler = ptHandler;
        }

        public void build(List<LookUpRule> rules) { 
		return;
	}

        public int getTotalWeight() {
                return 0;
        }

        public int getNumConstraints() {
                return 0;
        }

        public Set<Pair<Tuple,Integer>> getWeightedTuples() {
                Set<Pair<Tuple,Integer>> res = new HashSet<Pair<Tuple,Integer>>();
                return res;
        }
}
