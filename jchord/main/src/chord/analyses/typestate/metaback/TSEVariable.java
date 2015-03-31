package chord.analyses.typestate.metaback;

import chord.project.analyses.metaback.dnf.Variable;

public class TSEVariable implements Variable {

	private final static TSEVariable singleton = new TSEVariable();
	
	private TSEVariable(){}
	
	public static TSEVariable getSingleton(){
		return singleton;
	}
	
	@Override
	public String encode() {
		return "edgetype";
	}

	@Override
	public String toString(){
		return encode();
	}
	
}
