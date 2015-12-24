package petablox.analyses.escape.metaback;

import petablox.analyses.field.DomF;
import petablox.project.analyses.metaback.dnf.Variable;
import soot.SootField;

public class EscFVariable implements Variable {
	private int idx;  // f is NULl if it is an element of an array
	private DomF dom;
	public final static int ARRAY_ELEMENT = 0;

	public EscFVariable(int idx, DomF dom) {
		this.idx = idx;
		this.dom = dom;
	}

	public EscFVariable(SootField f, DomF dom) {
		this.dom = dom;
		if (f != null)
			this.idx = dom.indexOf(f);
		else
			this.idx = ARRAY_ELEMENT;
	}

	public SootField getF() {
		if (idx != ARRAY_ELEMENT)
			return dom.get(idx);
		else
			return null;
	}

	public int getIdx() {
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
		EscFVariable other = (EscFVariable) obj;
		if (idx != other.idx)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (idx != ARRAY_ELEMENT){
			SootField f = (SootField)dom.get(idx);
			return f.toString();
		}
		else
			return "ArrayElement";
	}
}
