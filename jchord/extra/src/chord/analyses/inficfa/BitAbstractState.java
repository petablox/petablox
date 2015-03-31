package chord.analyses.inficfa;

import java.util.BitSet;

import joeq.Compiler.Quad.RegisterFactory.Register;

public class BitAbstractState {
	
	public final BitEnv<Register> envLocal;
	public final BitSet returnVarEnv;

	public BitAbstractState(BitEnv<Register> envLocal) {
		this(envLocal, new BitSet());
	}

	public BitAbstractState(BitEnv<Register> envLocal, BitSet retVarEnv) {
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
		if (obj instanceof BitAbstractState) {
			BitAbstractState that = (BitAbstractState) obj;
			return envLocal.equals(that.envLocal) && returnVarEnv.equals(that.returnVarEnv);
		}
		return false;
	}

	@Override
	public String toString() {
		String ret = "returnVarEnv=" + (returnVarEnv.isEmpty() ? "EMPTY" : "{");
		ret += returnVarEnv.cardinality();
		ret += "},tcLocal=" + (envLocal.isEmpty() ? "EMPTY, " : "");
		for (Register v : envLocal.envMap.keySet()){
			ret += "["+v+",{";
			ret += envLocal.envMap.get(v).cardinality() + "}],";
		}
		
		return ret;
	}
}
