package chord.analyses.escape.metaback;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.project.analyses.metaback.Abstraction;
import chord.project.analyses.metaback.AbstractionFactory;
import chord.project.analyses.metaback.dnf.Clause;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;
import chord.util.Utils;

class EscAbstraction implements Abstraction {
	private TreeSet<Integer> LHs;
	private final static String sep = "#abs#";
	private DomH dom;

	public EscAbstraction(DomH dom) {
		LHs = new TreeSet<Integer>();
		this.dom = dom;
	}

	public EscAbstraction(Set<Integer> ehs,DomH dom) {
		this.LHs = new TreeSet<Integer>(ehs);
		this.dom = dom;
	}
	
	public Set<Quad> getLHs() {
		Set<Quad> ret = new HashSet<Quad>();
		for (int i : LHs) {
			ret.add((Quad)dom.get(i));
		}
		return ret;
	}

	public Set<Integer> getLIdxes(){
		return LHs;
	}
	
	public EscAbstraction(String s,DomH dom) {
		this(dom);
		this.decode(s);
	}

	@Override
	public String encode() {
		StringBuffer ret = new StringBuffer();
		Iterator<Integer> iter = LHs.iterator();
		if (iter.hasNext())
			ret.append(iter.next());
		while (iter.hasNext())
			ret.append(sep + iter.next());
		return ret.toString();
	}

	@Override
	public void decode(String s) {
		LHs.clear();
		String tokens[] = Utils.split(s, sep, true, true, -1);
		for (String token : tokens) {
			LHs.add(Integer.parseInt(token));
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((LHs == null) ? 0 : LHs.hashCode());
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
		EscAbstraction other = (EscAbstraction) obj;
		if (LHs == null) {
			if (other.LHs != null)
				return false;
		} else if (!LHs.equals(other.LHs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i:LHs){
			Object o = dom.get(i);
			String s = (o instanceof Quad) ? ((Quad) o).toVerboseStr() : o.toString();
			sb.append(s+"\n");
		}
		return (sb.length() == 0?sb.toString():sb.substring(0,sb.length()-2));
	}

	@Override
	public String encodeForXML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<LHS size=\""+LHs.size()+"\">\n");
		for(int i:LHs){
			sb.append("<H Hid=\"H"+i+"\">");
			Object o = dom.get(i);
			String s = (o instanceof Quad) ? ((Quad) o).toLocStr() : o.toString();
			sb.append(Utils.htmlEscape(s));
			sb.append("</H>\n");
		}
		sb.append("</LHS>");
		return sb.toString();
	}

	@Override
	public int compareTo(Abstraction o) {
		if(!(o instanceof EscAbstraction))
			throw new RuntimeException("Compared with a different abstraction class!");
		EscAbstraction other = (EscAbstraction)o;
		int ret = this.LHs.size() - other.LHs.size();
		if(ret == 0){
			Iterator<Integer> i1 = LHs.iterator();
			Iterator<Integer> i2 = other.LHs.iterator();
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

public class EscAbsFactory implements AbstractionFactory {
	private final static EscAbsFactory singleton = new EscAbsFactory();

	public static DomH dom;
	
	public static void setDomH(DomH obj){
		dom = obj;
	}
	
	public static EscAbsFactory getSingleton() {
		return singleton;
	}

	private EscAbsFactory() {
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
			return new EscAbstraction(dom);
		Set<Integer> abs = new HashSet<Integer>();
		Clause c = dnf.getClauses().first();
		for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
			if (Value.L().equals(entry.getValue()))
				abs.add(((EscHVariable) entry.getKey()).getIdx());
		}
		return new EscAbstraction(abs,dom);
	}

	@Override
	public Abstraction genAbsFromStr(String s) {
		EscAbstraction escAbs = new EscAbstraction(s,dom);
		return escAbs;
	}

}
