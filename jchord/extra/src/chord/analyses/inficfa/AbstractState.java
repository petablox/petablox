package chord.analyses.inficfa;

import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.ArraySet;

public class AbstractState<F> {
	
	public final Env<Register,F> envLocal;
	public final Set<F> returnVarEnv;

	public AbstractState(Env<Register,F> envLocal) {
		this(envLocal, new ArraySet<F>());
	}

	public AbstractState(Env<Register,F> envLocal, Set<F> retVarEnv) {
		assert (envLocal != null && retVarEnv != null);
		this.envLocal = envLocal;
		returnVarEnv = retVarEnv;
	}

	@Override
	public int hashCode() {
		return envLocal.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof AbstractState) {
			AbstractState that = (AbstractState) obj;
			return envLocal.equals(that.envLocal) && returnVarEnv.equals(that.returnVarEnv);
		}
		return false;
	}

	@Override
	public String toString() {
		String ret = "returnVarEnv=" + (returnVarEnv.isEmpty() ? "EMPTY" : "{");
		for(F t : returnVarEnv)
			ret += t + ",";
		ret += "},tcLocal=" + (envLocal.isEmpty() ? "EMPTY, " : "");
		for (Register v : envLocal.envMap.keySet()){
			ret += "["+v+",{";
			for(F t : envLocal.envMap.get(v))
				ret += t + ",";
			ret += "}],";
		}
		
		return ret;
	}
}
