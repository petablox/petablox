package chord.analyses.provenance.typestate;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Iterator;

import chord.util.tuple.object.Pair;

import chord.project.analyses.provenance.Model;
import chord.project.analyses.provenance.LookUpRule;
import chord.project.analyses.provenance.ConstraintItem;
import chord.project.analyses.provenance.Tuple;

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
