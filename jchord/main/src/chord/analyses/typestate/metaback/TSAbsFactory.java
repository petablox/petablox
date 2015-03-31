package chord.analyses.typestate.metaback;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.analyses.metaback.Abstraction;
import chord.project.analyses.metaback.AbstractionFactory;
import chord.project.analyses.metaback.dnf.Clause;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;
import chord.util.Utils;

class TSAbstraction implements Abstraction {
	private TreeSet<Integer> trackedVs;
	private final static String sep = "#abs#";
	private DomV dom;

	public TSAbstraction(DomV dom) {
		trackedVs = new TreeSet<Integer>();
		this.dom = dom;
	}

	public TSAbstraction(Set<Integer> tvs,DomV dom) {
		this.trackedVs = new TreeSet<Integer>(tvs);
		this.dom = dom;
	}
	
	public Set<Register> getTVs() {
		Set<Register> ret = new HashSet<Register>();
		for (int i : trackedVs) {
			ret.add(dom.get(i));
		}
		return ret;
	}

	public Set<Integer> getVIdxes(){
		return trackedVs;
	}
	
	public TSAbstraction(String s,DomV dom) {
		this(dom);
		this.decode(s);
	}

	@Override
	public String encode() {
		StringBuffer ret = new StringBuffer();
		Iterator<Integer> iter = trackedVs.iterator();
		if (iter.hasNext())
			ret.append(iter.next());
		while (iter.hasNext())
			ret.append(sep + iter.next());
		return ret.toString();
	}

	@Override
	public void decode(String s) {
		trackedVs.clear();
		String tokens[] = Utils.split(s, sep, true, true, -1);
		for (String token : tokens) {
			trackedVs.add(Integer.parseInt(token));
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((trackedVs == null) ? 0 : trackedVs.hashCode());
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
		TSAbstraction other = (TSAbstraction) obj;
		if (trackedVs == null) {
			if (other.trackedVs != null)
				return false;
		} else if (!trackedVs.equals(other.trackedVs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i:trackedVs){
			Object o = dom.get(i);
			String s = o.toString();
			sb.append(s+"\n");
		}
		return (sb.length() == 0?sb.toString():sb.substring(0,sb.length()-2));
	}

	@Override
	public String encodeForXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<TVS size=\""+trackedVs.size()+"\">\n");
		for(int i:trackedVs){
			sb.append("<V Vid=\"V"+i+"\">");
			Object o = dom.get(i);
			String s = o.toString();
			sb.append(Utils.htmlEscape(s));
			sb.append("</V>\n");
		}
		sb.append("</TVS>");
		return sb.toString();
	}

	@Override
	public int compareTo(Abstraction o) {
		if(!(o instanceof TSAbstraction))
			throw new RuntimeException("Compared with a different abstraction class!");
		TSAbstraction other = (TSAbstraction)o;
		int ret = this.trackedVs.size() - other.trackedVs.size();
		if(ret == 0){
			Iterator<Integer> i1 = trackedVs.iterator();
			Iterator<Integer> i2 = other.trackedVs.iterator();
			while(i1.hasNext()){
				int hi1 = i1.next();
				int hi2 = i2.next();
				ret = hi1-hi2;
				if(ret == 0)
					continue;
				return ret;
			}
			return 0;
		}else
			return ret;
	}

}

public class TSAbsFactory implements AbstractionFactory {
	private final static TSAbsFactory singleton = new TSAbsFactory();

	public static DomV dom;
	
	public static void setDomV(DomV obj){
		dom = obj;
	}
	
	public static TSAbsFactory getSingleton() {
		return singleton;
	}

	private TSAbsFactory() {
	};

	/**
	 * Generate an abstraction from a given dnf. If dnf.isFalse(), return null
	 * if dnf.isTrue(), return a abstraction representing true
	 */
	@Override
	public Abstraction genAbsFromNC(DNF dnf) {
		if (dnf.isFalse())
			return null;
		if (dnf.isTrue())
			return new TSAbstraction(dom);
		Set<Integer> abs = new HashSet<Integer>();
		Clause c = dnf.getClauses().first();
		for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
			if (TSBoolDomain.T().equals(entry.getValue()))
				abs.add(((TSPVariable) entry.getKey()).getIdx());
		}
		return new TSAbstraction(abs,dom);
	}

	@Override
	public Abstraction genAbsFromStr(String s) {
		TSAbstraction tsAbs = new TSAbstraction(s,dom);
		return tsAbs;
	}

}