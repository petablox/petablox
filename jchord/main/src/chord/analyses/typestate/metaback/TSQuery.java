package chord.analyses.typestate.metaback;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.project.analyses.metaback.Query;
import chord.util.Utils;

public class TSQuery implements Query {
private int iIdx;
private DomI domI;
private int hIdx;
private DomH domH;
public final static String SEP = "#Q#";

public TSQuery(int iIdx, DomI domI, int hIdx, DomH domH){
	this.hIdx = hIdx;
	this.domH = domH;
	this.iIdx = iIdx;
	this.domI = domI;
}

	@Override
	public void decode(String s) {
		String nums[] = s.split(SEP);
		this.iIdx = Integer.parseInt(nums[0]);
		this.hIdx = Integer.parseInt(nums[1]);
	}

	@Override
	public String encode() {
		StringBuffer sb = new StringBuffer();
		sb.append(iIdx);
		sb.append(SEP);
		sb.append(hIdx);
		return sb.toString();
	}

	@Override
	public String encodeForXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<Query Iid=\"I"+iIdx+"\" Hid=\"H"+hIdx+"\">\n");
		sb.append(Utils.htmlEscape(domI.get(iIdx).toLocStr())+"\n");
		sb.append("#########");
		sb.append(Utils.htmlEscape(((Quad)domH.get(hIdx)).toLocStr())+"\n");
		sb.append("</Query>");
		return sb.toString();
	}

	@Override
	public String toString() {
		return domI.get(iIdx).toString()+" "+domH.get(hIdx).toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hIdx;
		result = prime * result + iIdx;
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
		TSQuery other = (TSQuery) obj;
		if (hIdx != other.hIdx)
			return false;
		if (iIdx != other.iIdx)
			return false;
		return true;
	}

	public Quad getI(){
		return domI.get(iIdx);
	}
	
	public int getIIdx(){
		return iIdx;
	}
	
	public int getHIdx(){
		return hIdx;
	}
	
	public Quad getH(){
		return (Quad)domH.get(hIdx);
	}
}
