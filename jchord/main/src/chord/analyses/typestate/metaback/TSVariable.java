package chord.analyses.typestate.metaback;

import chord.project.analyses.metaback.dnf.Variable;

public class TSVariable implements Variable {
	private final static TSVariable singleton = new TSVariable();

	private TSVariable() {
	}

	public static TSVariable getSingleton(){
		return singleton;
	}
	
	@Override
	public String encode() {
		return "TS";
	}

	@Override
	public String toString(){
		return encode();
	}
	
}
