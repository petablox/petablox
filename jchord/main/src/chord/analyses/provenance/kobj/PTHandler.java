package chord.analyses.provenance.kobj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.provenance.FormatedConstraint;
import chord.project.analyses.provenance.LookUpRule;
import chord.project.analyses.provenance.MaxSatGenerator;
import chord.project.analyses.provenance.ParamTupleConsHandler;
import chord.project.analyses.provenance.Tuple;
import chord.util.tuple.object.Pair;

public class PTHandler implements ParamTupleConsHandler {
	public static int max = 20;
	private Set<String> derivedRs;
	private Set<Tuple> constTuples;
	ProgramRel denyORel;
	ProgramRel denyHRel;
	private boolean ifMono;
	private boolean ifBool;
	
	public PTHandler(boolean ifMono, boolean ifBool){
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
		
		denyORel = (ProgramRel)ClassicProject.g().getTrgt("DenyO");
		denyHRel = (ProgramRel)ClassicProject.g().getTrgt("DenyH");
		this.ifMono = ifMono;
		this.ifBool = ifBool;
	}
	
	@Override
	public int getWeight(Tuple t) {
		String relName = t.getRelName();
		if(relName.equals("DenyO")){
			return 1;
		}
		else
			if(relName.equals("DenyH"))
				return 1;
			else
				throw new RuntimeException("Not a param tuple: "+t);
	}

	@Override
	public Set<FormatedConstraint> getHardCons(int w, Set<Tuple> paramTSet, MaxSatGenerator gen) {
		Set<FormatedConstraint> ret = new HashSet<FormatedConstraint>();
		Map<Quad,List<Tuple>> qOMap = new HashMap<Quad,List<Tuple>>();
		Map<Quad,List<Tuple>> qHMap = new HashMap<Quad,List<Tuple>>();
		for(Tuple t : paramTSet){
			String relName = t.getRelName();
			Map<Quad,List<Tuple>> qToTMap;
			if(relName.equals("DenyH"))
				qToTMap = qHMap;
			else
				qToTMap = qOMap;
			Quad q = (Quad)t.getValue(0);
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
		for(Map.Entry<Quad, List<Tuple>> entry: qOMap.entrySet()){
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
		for(Map.Entry<Quad, List<Tuple>> entry: qHMap.entrySet()){
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
		if(relName.equals("AllowO")){
			int idx = (Integer)t.getValue(1);
			if(ifMono)
				return null;
			if(idx == 0)
				return null;
			Tuple denyO = new Tuple(denyORel,t.getIndices());
			ret.val0 = denyO;
			ret.val1 = false;
			return ret;
		}
		if(relName.equals("DenyO")){
			ret.val0 = t;
			ret.val1 = true;
			if(ifBool){//This is an optimization.when we use boolean domain, we cannot get rid of denyI with k>1
				int idx = (Integer)t.getValue(1);
				if(idx>1)
					return null;
				}
			return ret;
		}
		if(relName.equals("AllowH")){
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
		if(relName.equals("DenyO")){
			int i = (Integer)t.getValue(1);
			if(i!=0)
			return true;
		}
		if(relName.equals("DenyH")){
			int i = (Integer)t.getValue(1);
			if(i!=0&&i!=1)
			return true;
		}
		return false;
	}
}
