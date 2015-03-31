package chord.analyses.typestate.metaback;

import java.util.Set;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.analyses.metaback.dnf.Domain;
import chord.util.ArraySet;
import chord.util.DomBitSet;

/**
 * Use a universal domain for ts, inMS and param.
 * Specially, ts = init for T, ts = error for F.
 * @author xin
 *
 */
public class TSBoolDomain implements Domain {
	private final static TSBoolDomain T = new TSBoolDomain();
	private final static TSBoolDomain F = new TSBoolDomain();
	private final static Set<Domain> space;
	static {
		space = new ArraySet<Domain>();
		space.add(T);
		space.add(F);
	}

	public static TSBoolDomain get(boolean b){
		if(b)
			return T;
		else
			return F;
	}
	
	private TSBoolDomain() {

	}

	public static TSBoolDomain T(){
		return T;
	}
	
	public static TSBoolDomain F(){
		return F;
	}
	
	@Override
	public int size() {
		return 2;
	}

	@Override
	public boolean equals(Domain other) {
		return this == other;
	}

	@Override
	public String encode() {
		if (this == T)
			return "T";
		else
			return "F";
	}

	@Override
	public String toString() {
		return encode();
	}
	
	@Override
	public Set<Domain> space() {
		return space;
	}

	public static TSBoolDomain objToValue(Set<Register> ms, TSVVariable v){
		if(ms.contains(v.getRegister()))
			return T;
		return F;
	}
	
}
