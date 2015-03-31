package chord.analyses.escape.metaback;

import joeq.Compiler.Quad.Quad;
import chord.analyses.heapacc.DomE;
import chord.project.analyses.metaback.Query;
import chord.util.Utils;

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

	public Quad getQuad() {
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
		String s = dom.get(idx).toLocStr();
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
		Quad q =(Quad)dom.get(idx);
		return "EscQuery [e=" + q.toVerboseStr() + ", index="+idx+"]";
	}

}
