package petablox.analyses.escape.metaback;

import petablox.analyses.alloc.DomH;
import petablox.project.analyses.metaback.dnf.Variable;
import petablox.util.soot.SootUtilities;
import soot.Unit;

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
	
	public EscHVariable(Unit q, DomH dom){
		this.idx = dom.indexOf(q);
		this.dom = dom;
	}

	public Unit getH(){
		return (Unit)dom.get(idx);
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
		Unit alloc = (Unit)dom.get(idx);
		return SootUtilities.toVerboseStr(alloc);
	}

}
