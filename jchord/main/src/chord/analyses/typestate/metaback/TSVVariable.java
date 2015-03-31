package chord.analyses.typestate.metaback;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.analyses.metaback.dnf.Variable;

public class TSVVariable implements Variable {
	private int idx;// the index in domV
	private DomV dom;
	private int context;

	public TSVVariable(int i,DomV dom) {
		this.idx = i;
		this.dom = dom;
		context = 0;
	}

	public TSVVariable(Register r, DomV dom){
		this.idx = dom.indexOf(r);
		this.dom = dom;
		context = 0;
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
		TSVVariable other = (TSVVariable) obj;
		if (context != other.context)
			return false;
		if (idx != other.idx)
			return false;
		return true;
	}

	public TSVVariable getIncreased(){
		TSVVariable ret = new TSVVariable(idx,dom);
		ret.context = this.context+1;
		return ret;
	}
	
	public TSVVariable getDecreased(){
		TSVVariable ret = new TSVVariable(idx,dom);
		ret.context = this.context - 1;
		return ret;
	}
	
	public int getContext(){
		return context;
	}
	
	@Override
	public String toString() {
		return "inMS "+dom.get(idx).toString()+"("+idx+","+context+")";
	}
	

}
