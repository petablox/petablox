package chord.analyses.escape.metaback;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Comparator;
import java.util.Map;

import chord.project.analyses.metaback.dnf.Clause;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;

public class LNumCMP implements Comparator<Clause> {
	private TObjectIntHashMap<Clause> cToI = new TObjectIntHashMap<Clause>();

	@Override
	public int compare(Clause o1, Clause o2) {
		if (o1.equals(o2))
			return 0;
		int LNum1 = 0;
		int LNum2 = 0;
		for (Map.Entry<Variable, Domain> entry : o1.getLiterals().entrySet()) {
			if (entry.getValue() == Value.L())
				LNum1++;
		}
		for (Map.Entry<Variable, Domain> entry : o2.getLiterals().entrySet()) {
			if (entry.getValue() == Value.L())
				LNum2++;
		}
		int result = LNum1 - LNum2;
		if (result==0) {
			int i1 = cToI.get(o1);
			if (i1==0) {
				i1 = cToI.size()+1;
				cToI.put(o1, i1);}
			int i2 = cToI.get(o2);
			if (i2==0) {
				i2 = cToI.size()+1;
				cToI.put(o2, i2);	
			}
			return i1 - i2;
		}
		return result;
	}
}
