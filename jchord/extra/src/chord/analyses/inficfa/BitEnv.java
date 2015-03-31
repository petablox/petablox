package chord.analyses.inficfa;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class BitEnv<E> {

	Map<E, BitSet> envMap;

	public BitEnv(){
		envMap = new HashMap<E, BitSet>();
	}

	public BitEnv(BitEnv<E> env){
		assert(env != null);
		envMap = new HashMap<E, BitSet>(env.envMap);
	}

	public void insert(E v, int index){
		assert(v != null && index >= 0);
		BitSet varEnv  = new BitSet();
		varEnv.set(index);
		envMap.put(v, varEnv);
	}

	public void insert(E v, BitSet t){
		assert(v != null && t != null);
		envMap.put(v, t);
	}

	public void insert(BitEnv<E> env){
		assert(env != null);
		for(E v : env.envMap.keySet()){
			BitSet thatVarEnv = env.envMap.get(v);
			BitSet thisVarEnv = this.envMap.get(v);
			if(thisVarEnv != null){
				BitSet newVarEnv = new BitSet();
				newVarEnv.or(thatVarEnv);
				newVarEnv.or(thisVarEnv);
				this.envMap.put(v, newVarEnv);
			}else{
				this.envMap.put(v, thatVarEnv);
			}
		}

	}

	public BitSet get(E v){
		assert(v != null);
		return envMap.get(v);
	}

	public BitSet remove(E v){
		assert(v != null);
		return envMap.remove(v);
	}

	public boolean isEmpty(){
		return envMap.isEmpty();
	}

	public boolean containsAll(BitEnv<E> that){
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
		BitEnv<E> that = (BitEnv<E>) o;
		return envMap.equals(that.envMap);
	}

}
