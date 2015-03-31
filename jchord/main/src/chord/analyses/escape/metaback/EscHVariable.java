package chord.analyses.escape.metaback;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.project.analyses.metaback.dnf.Variable;
/**
 * A variable to represent the allocation site
 * @author xin
 *
 */
public class EscHVariable implements Variable {
	private int idx;
	private DomH dom;

	public EscHVariable(int idx, DomH dom) {
		this.idx = idx;
		this.dom = dom;
	}
	
	public EscHVariable(Quad q, DomH dom){
		this.idx = dom.indexOf(q);
		this.dom = dom;
	}

	public Quad getH(){
		return (Quad)dom.get(idx);
	}
	
	public int getIdx(){
		return idx;
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
		EscHVariable other = (EscHVariable) obj;
		if (idx != other.idx)
			return false;
		return true;
	}

	@Override
	public String toString() {
		Quad alloc = (Quad)dom.get(idx);
		return alloc.toVerboseStr();
	}

}
