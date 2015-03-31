package chord.analyses.escape.metaback;

import java.util.HashSet;
import java.util.Set;

import chord.analyses.escape.hybrid.full.FldObj;
import chord.analyses.escape.hybrid.full.Obj;
import chord.project.analyses.metaback.dnf.Domain;

public class Value implements Domain {
	private String v;
	private final static Value N = new Value("N");
	private final static Value L = new Value("L");
	private final static Value E = new Value("E");

	public final static Value N() {
		return N;
	}
	
	public final static Value L() {
		return L;
	}
	
	public final static Value E() {
		return E;
	}
	
	public Set<Domain> space(){
		Set<Domain> space = new HashSet<Domain>();
		space.add(N);
		space.add(E);
		space.add(L);
		return space;
	}
	
	private Value() { }

	private Value(String v) {
		this.v = v;
	}

	public static Value getValue(String s) {
		if (s.equals("N"))
			return N;
		if (s.equals("L"))
			return L;
		if (s.equals("E"))
			return E;
		throw new RuntimeException("Unknown value: "+s);
	}
	
	@Override
	public int size() {
		return 3;
	}

	@Override
	public boolean equals(Domain other) {
		return this == other;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return this==obj;
	}

	@Override
	public String toString() {
		return v;
	}

	@Override
	public String encode() {
		return v;
	}

	public static Value objToValue(Obj obj) {
		switch(obj) {
		case EMTY:
			return N;
		case ONLY_ESC:
			return E;
		case ONLY_LOC:
			return L;
		}
		throw new RuntimeException("Unacceptable convert: "+obj);
	}
	
	public static Value fldToValue(FldObj f) {
		if (f.isEsc && f.isLoc)
			throw new RuntimeException("Currently we don't support BOTH!");
		if (!f.isEsc && !f.isLoc)
			return N;
		if (f.isEsc)
			return E;
		if (f.isLoc)
			return L;
		throw new RuntimeException("Impossible to reach here with "+f);
	}
}
