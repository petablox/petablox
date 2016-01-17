package petablox.analyses.escape.metaback;

import petablox.analyses.heapacc.DomE;
import petablox.project.analyses.metaback.Query;
import petablox.util.Utils;
import petablox.util.soot.SootUtilities;
import soot.Unit;

public class EscQuery implements Query {
	private int idx;
	private DomE dom;

	public EscQuery(int idx, DomE dom) {
		this.idx = idx;
		this.dom = dom;
	}

	public EscQuery(String s, DomE dom) {
		this.dom = dom;
		this.decode(s);
	}

	public Unit getQuad() {
		return dom.get(idx);
	}

	public int getIdx(){
		return idx;
	}
	
	@Override
	public void decode(String s) {
		this.idx = Integer.parseInt(s);
	}

	@Override
	public String encode() {
		return Integer.toString(idx);
	}

	@Override
	public String encodeForXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<Query Eid=\"E"+idx+"\">");
		String s = SootUtilities.toLocStr(dom.get(idx));
		sb.append(Utils.htmlEscape(s));
		sb.append("</Query>");
		return sb.toString();
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
		EscQuery other = (EscQuery) obj;
		if (idx != other.idx)
			return false;
		return true;
	}

	@Override
	public String toString() {
		Unit q =(Unit)dom.get(idx);
		return "EscQuery [e=" + SootUtilities.toVerboseStr(q) + ", index="+idx+"]";
	}

}
