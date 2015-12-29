package petablox.analyses.provenance.kcfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.project.analyses.provenance.FormatedConstraint;
import petablox.project.analyses.provenance.LookUpRule;
import petablox.project.analyses.provenance.MaxSatGenerator;
import petablox.project.analyses.provenance.ParamTupleConsHandler;
import petablox.project.analyses.provenance.Tuple;
import petablox.util.tuple.object.Pair;
import soot.Unit;

public class PTHandler implements ParamTupleConsHandler {
	public static int max = 20;
	private Set<String> derivedRs;
	private Set<Tuple> constTuples;
	ProgramRel denyIRel;
	ProgramRel denyHRel;
	private boolean heap;
	private boolean ifMono;
	private boolean ifBool;
	
	public PTHandler(boolean heap, boolean ifMono, boolean ifBool){
		constTuples = new HashSet<Tuple>();
		//rootCM(0,0) and reachableCM(0,0) are two constants
		ProgramRel reachableCM = (ProgramRel) ClassicProject.g().getTrgt("reachableCM");
		ProgramRel rootCM = (ProgramRel) ClassicProject.g().getTrgt("rootCM");
		int[] a1 = { 0, 0 };
		Tuple t1 = new Tuple(reachableCM, a1);
		int[] a2 = { 0, 0 };
		Tuple t2 = new Tuple(rootCM, a2);
		constTuples.add(t1);
		constTuples.add(t2);
		
		denyIRel = (ProgramRel)ClassicProject.g().getTrgt("DenyI");
		denyHRel = (ProgramRel)ClassicProject.g().getTrgt("DenyH");
		this.heap = heap;
		this.ifMono = ifMono;
		this.ifBool = ifBool;
	}
	
	@Override
	public int getWeight(Tuple t) {
		String relName = t.getRelName();
		if(relName.equals("DenyI")){
			return 1;
		}
		else
			if(heap&&relName.equals("DenyH"))
				return 1;
			else
				throw new RuntimeException("Not a param tuple: "+t);
	}

	@Override
	public Set<FormatedConstraint> getHardCons(int w, Set<Tuple> paramTSet, MaxSatGenerator gen) {
		Set<FormatedConstraint> ret = new HashSet<FormatedConstraint>();
		Map<Unit,List<Tuple>> qToTMap = new HashMap<Unit,List<Tuple>>();
		for(Tuple t : paramTSet){
			Unit q = (Unit)t.getValue(0);
			List<Tuple> tList = qToTMap.get(q);
			if(tList == null){
				tList = new ArrayList<Tuple>();
				qToTMap.put(q, tList);
			}
			int i = 0;
			for(i = 0; i < tList.size(); i++)
				if((Integer)tList.get(i).getValue(1) >= (Integer)t.getValue(1))
					break;
			tList.add(i, t);
		}
		for(Map.Entry<Unit, List<Tuple>> entry: qToTMap.entrySet()){
			List<Tuple> l = entry.getValue();
			for(int i = 0; i < l.size() - 1; i++){
				Tuple t1 = l.get(i);
				Tuple t2 = l.get(i+1);
				int i1 = gen.getOrAddTupleIdx(t1);
				int i2 = gen.getOrAddTupleIdx(t2);
				int cons[] = new int[2];
				cons[0] = 0-i1;
				cons[1] = i2;
				ret.add(new FormatedConstraint(w,cons));
			}
		}
		return ret;
	}

	@Override
	public void init(List<LookUpRule> rules) {
		derivedRs = new HashSet<String>();
		for(LookUpRule r : rules)
			derivedRs.add(r.getHeadRelName());
	}

	@Override
	public Pair<Tuple, Boolean> transform(Tuple t) {
		if(constTuples.contains(t))//rootCM(0,0), reachable(0,0)
			return null;
		Pair<Tuple,Boolean> ret = new Pair<Tuple,Boolean>(null,null);
		String relName = t.getRelName();
		if(relName.equals("AllowI")){
			int idx = (Integer)t.getValue(1);
			if(ifMono)
				return null;
			if(idx == 0)
				return null;
			Tuple denyI = new Tuple(denyIRel,t.getIndices());
			ret.val0 = denyI;
			ret.val1 = false;
			return ret;
		}
		if(relName.equals("DenyI")){
			ret.val0 = t;
			ret.val1 = true;
			if(ifBool){//when we use boolean domain, we cannot get rid of denyI with k>1
				int idx = (Integer)t.getValue(1);
				if(idx>1)
					return null;
				}
			return ret;
		}
		if(relName.equals("AllowH")){
			if(!heap)
				return null;
			if(ifMono)
				return null;
			int idx = (Integer)t.getValue(1);
			if(idx == 0)
				return null;
			if(idx == 1)
				return null;
			Tuple denyH = new Tuple(denyHRel,t.getIndices());
			ret.val0 = denyH;
			ret.val1 = false;
			return ret;
		}
		if(relName.equals("DenyH")){
			if(!heap)
				return null;
			if(ifBool){//when we use boolean domain, we cannot get rid of denyH with k>2
				int idx = (Integer)t.getValue(1);
				if(idx>2)
					return null;
				}
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
		if(relName.equals("DenyI")){
			int i = (Integer)t.getValue(1);
			if(i!=0)
			return true;
		}
		if(heap && relName.equals("DenyH")){
			int i = (Integer)t.getValue(1);
			if(i!=0&&i!=1)
			return true;
		}
		return false;
	}


}
