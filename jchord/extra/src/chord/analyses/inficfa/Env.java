package chord.analyses.inficfa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import joeq.Class.jq_Type;
import chord.util.ArraySet;

public class Env<E,F> {

	Map<E, Set<F>> envMap;

	public Env(){
		envMap = new HashMap<E, Set<F>>();
	}

	public Env(Env<E,F> env){
		assert(env != null);
		envMap = new HashMap<E, Set<F>>(env.envMap);
	}

	public void insert(E v, F t){
		assert(v != null && t != null);
		Set<F> varEnv  = new ArraySet<F>();
		varEnv.add(t);
		envMap.put(v, varEnv);
	}

	public void insert(E v, Set<F> t){
		assert(v != null && t != null);
		envMap.put(v, t);
	}

	public void insert(Env<E,F> env){
		assert(env != null);
		for(E v : env.envMap.keySet()){
			Set<F> thatVarEnv = env.envMap.get(v);
			Set<F> thisVarEnv = this.envMap.get(v);
			if(thisVarEnv != null){
				Set<F> newVarEnv = new ArraySet<F>(thatVarEnv);
				newVarEnv.addAll(thisVarEnv);
				this.envMap.put(v, newVarEnv);
			}else{
				this.envMap.put(v, thatVarEnv);
			}
		}

	}

	public Set<F> get(E v){
		assert(v != null);
		return envMap.get(v);
	}

	public Set<F> remove(E v){
		assert(v != null);
		return envMap.remove(v);
	}

	public boolean isEmpty(){
		return envMap.isEmpty();
	}

	public boolean containsAll(Env<E,F> that){
		return false;
	}

	@Override
	public int hashCode(){
		return envMap.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if (getClass() != o.getClass())
			return false;
		Env<E,F> that = (Env<E,F>) o;
		return envMap.equals(that.envMap);
	}

}
