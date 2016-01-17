package petablox.project.analyses.metaback.dnf;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Comparator;

/**
 * A comparator which cmpares the size of clauses
 * @author xin
 *
 */
public class ClauseSizeCMP implements Comparator<Clause>{
private TObjectIntHashMap<Clause> cToI = new TObjectIntHashMap<Clause>();
	@Override
	public int compare(Clause o1, Clause o2) {
		if(o1.equals(o2))
			return 0;
		int result = o1.size() - o2.size();
		if(result==0){
			int i1 = cToI.get(o1);
			if(i1==0){
				i1 = cToI.size()+1;
				cToI.put(o1, i1);}
			int i2 = cToI.get(o2);
			if(i2==0){
				i2 = cToI.size()+1;
				cToI.put(o2, i2);	
			}
			return i1 - i2;
			}
			return result;
	}

}
