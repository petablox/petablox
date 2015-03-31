package chord.analyses.typestate.metaback;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.analyses.metaback.dnf.Variable;

public class TSPVariable implements Variable {

	private int idx;// the index in domV
	private DomV dom;

	public TSPVariable(int i,DomV dom) {
		this.idx = i;
		this.dom = dom;
	}

	public int getIdx() {
		return idx;
	}

	public Register getRegister(){
		return (Register)dom.get(idx);
	}
	
	@Override
	public String encode() {
		return Integer.toString(idx);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idx;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TSPVariable other = (TSPVariable) obj;
		if (idx != other.idx)
			return false;
		return true;
	}

	
	
	@Override
	public String toString() {
		return "Param "+dom.get(idx).toString()+"("+idx+")";
	}
	


}
