package petablox.analyses.escape.metaback;

import petablox.analyses.var.DomV;
import petablox.project.analyses.metaback.dnf.Variable;
import soot.Local;

public class EscVVariable implements Variable {
	private int idx;// the index in domV
	private DomV dom;
	private int context;

	public EscVVariable(int i,DomV dom) {
		this.idx = i;
		this.dom = dom;
		context = 0;
	}

	public int getIdx() {
		return idx;
	}

	public Local getRegister(){
		return (Local)dom.get(idx);
	}
	
	@Override
	public String encode() {
		return Integer.toString(idx);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + context;
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
		EscVVariable other = (EscVVariable) obj;
		if (context != other.context)
			return false;
		if (idx != other.idx)
			return false;
		return true;
	}

	public EscVVariable getIncreased(){
		EscVVariable ret = new EscVVariable(idx,dom);
		ret.context = this.context+1;
		return ret;
	}
	
	public EscVVariable getDecreased(){
		EscVVariable ret = new EscVVariable(idx,dom);
		ret.context = this.context - 1;
		return ret;
	}
	
	public int getContext(){
		return context;
	}
	
	@Override
	public String toString() {
		return dom.get(idx).toString()+"("+idx+","+context+")";
	}
	

}
