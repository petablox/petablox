package chord.project.analyses.tdbu;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class CaseTDSEComparator<TDSE> implements Comparator<Constraint> {
	private Set<TDSE> tdses;
	private Map<Constraint,Integer> cache;
	private TObjectIntHashMap<Constraint> cToI = new TObjectIntHashMap<Constraint>();
	
	public CaseTDSEComparator(Set<TDSE> tdses) {
		super();
		this.tdses = tdses;
		cache = new HashMap<Constraint,Integer>();
	    cToI = new TObjectIntHashMap<Constraint>();
	}

	@Override
	public int compare(Constraint o1, Constraint o2) {
		if(o1.equals(o2))
			return 0;
		Integer c1c = cache.get(o1);
		Integer c2c = cache.get(o2);
		int c1 = 0;
		int c2 = 0;
		if(c1c!=null)
			c1 = c1c.intValue();
		if(c2c!=null)
			c2 = c2c.intValue();
		if(c1 == 0 || c2 ==0)
			if(tdses!=null)
		for(TDSE se: tdses){
			if(c1c ==null && statisfy(o1,se))
				c1++;
			if(c2c ==null && statisfy(o2,se))
				c2++;
		}
		cache.put(o1, c1);
		cache.put(o2, c2);
		if(c1 != c2)
			return c1 - c2;
		int i1 = cToI.get(o1);
		if (i1==0) {
			i1 = cToI.size()+1;
			cToI.put(o1, i1);}
		int i2 = cToI.get(o2);
		if (i2==0) {
			i2 = cToI.size()+1;
			cToI.put(o2, i2);
		}
		return i1-i2;
	}

	protected abstract boolean statisfy(Constraint constraint, TDSE tdse);
	
}
