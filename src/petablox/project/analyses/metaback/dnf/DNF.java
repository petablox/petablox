package petablox.project.analyses.metaback.dnf;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import petablox.util.Utils;

/**
 * Represents a disjunctive normal form
 * 
 * @author xin
 * 
 */
public class DNF implements Cloneable {
	private SortedSet<Clause> clauses;
	private boolean ifTrue;
	private static String SEP = "#OR#";

	/**
	 * Create a FALSE DNF with given comparator
	 * 
	 * @param cmp
	 */
	public DNF(Comparator<Clause> cmp) {
		clauses = new TreeSet<Clause>(cmp);
		ifTrue = false;
	}

	public DNF(Comparator<Clause> cmp, boolean ifTrue) {
		this.clauses = new TreeSet<Clause>(cmp);
		this.ifTrue = ifTrue;
	}
	/**
	 * Construct a DNF with a single clause: v=d
	 * @param cmp
	 * @param v
	 * @param d
	 */
	public DNF(Comparator<Clause> cmp,Variable v, Domain d){
		clauses = new TreeSet<Clause>(cmp);
		ifTrue = false;
		Clause c = new Clause();
		c.addLiteral(v, d);
		clauses.add(c);
	}

	public DNF(Comparator<Clause> cmp, DNFFactory fac, String line) {
		String tokens[] = Utils.split(line, SEP, true, true, -1);
		ifTrue = Boolean.parseBoolean(tokens[0]);
		clauses = new TreeSet<Clause>(cmp);
		for(int i = 1;i < tokens.length;i++){
			Clause c = new Clause(fac,tokens[i]);
			clauses.add(c);
		}
	}

	public static void setSEP(String sep) {
		SEP = sep;
	}

	public static DNF getTrue(Comparator<Clause> cmp) {
		DNF ret = new DNF(cmp);
		ret.ifTrue = true;
		return ret;
	}

	public static DNF getFalse(Comparator<Clause> cmp) {
		DNF ret = new DNF(cmp);
		ret.ifTrue = false;
		return ret;
	}

	public DNF(SortedSet<Clause> clauses, boolean ifTrue) {
		this.clauses = new TreeSet<Clause>(clauses);
		this.ifTrue = ifTrue;
	}

	public DNF(SortedSet<Clause> clauses) {
		this.clauses = new TreeSet<Clause>(clauses);
		if (clauses.size() == 0)
			this.ifTrue = false;
		else
			this.ifTrue = true;
	}

	/**
	 * How to abbreviate the disjunction is a problem here. Currently I only use
	 * a simple optimization: detecting containing relations between clauses.
	 * 
	 * @param s
	 */
	public void addClause(Clause s) {
		if(this.isTrue())//Here actually add is like a join function
			return;
		if (s.isFalse())
			return;
		if (s.isTrue()) {
			clauses.clear();
			ifTrue = true;
			return;
		}
		Set<Clause> clsRmv = new HashSet<Clause>();
		for (Clause s1 : clauses)
			if (s1.contains(s))
				return;
			else if (s.contains(s1))
				clsRmv.add(s1);
		clauses.removeAll(clsRmv);
		clauses.add(s);
		ifTrue = false;
	}

	public DNF intersect(DNF other) {
		if (this.isFalse() || other.isFalse())
			return new DNF(this.getCMP());
		if (this.isTrue())
			return other.clone();
		if (other.isTrue())
			return this.clone();
		DNF ret = new DNF(this.getCMP());
		for (Clause c1 : clauses)
			for (Clause c2 : other.clauses) {
				Clause c3 = c1.intersect(c2);
				if (!c3.isFalse())
					ret.addClause(c3);
			}
		return ret;
	}

	public DNF prune(int size, Clause c) {
		if(this.isTrue())
			return this.clone();
		if(this.isFalse())
			return this.clone();
		SortedSet<Clause> ncs = new TreeSet<Clause>(clauses.comparator());
		boolean ifMatch = false;
		for (Clause c1 : clauses) {
			if (c1.contains(c)) {
				ifMatch = true;
				ncs.add(c1);
			} else {
				if (ifMatch)
					ncs.add(c1);
				else if (ncs.size() < size - 1)
					ncs.add(c1);
			}
			if (ncs.size() == size)
				break;
		}
		if(!ifMatch)
			return DNF.getFalse(this.getCMP());
		return new DNF(ncs);
	}

	public Comparator<Clause> getCMP() {
		return (Comparator<Clause>) clauses.comparator();
	}

	public boolean isFalse() {
		return clauses.isEmpty() && ifTrue == false;
	}

	public boolean isTrue() {
		return clauses.isEmpty() && ifTrue;
	}

	public Clause first() {
		return clauses.first();
	}

	public DNF trancate(int size) {
		if (clauses.size() <= size)
			return this.clone();
		Clause[] tcs = (Clause[]) Arrays.copyOf(clauses.toArray(), size);
		DNF ret = new DNF(this.getCMP());
		for (Clause cl : tcs)
			ret.clauses.add(cl);
		return ret;
	}

	public DNF join(DNF other) {
		if (this.isTrue() || other.isFalse())
			return this.clone();
		if (other.isTrue() || this.isFalse())
			return other.clone();
		DNF ret = this.clone();
		for (Clause c : other.clauses) {
			if (ret.isTrue())
				return ret;
			ret.addClause(c);
		}
		return ret;
	}

	// TODO This is not a deep clone, could there be bugs?
	public DNF clone() {
		return new DNF(this.clauses, ifTrue);
	}

	public int size() {
		return clauses.size();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clauses == null) ? 0 : clauses.hashCode());
		result = prime * result + (ifTrue ? 1231 : 1237);
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
		DNF other = (DNF) obj;
		if (clauses == null) {
			if (other.clauses != null)
				return false;
		} else if (!clauses.equals(other.clauses))
			return false;
		if (ifTrue != other.ifTrue)
			return false;
		return true;
	}

	public SortedSet<Clause> getClauses() {
		return clauses;
	}

	@Override
	public String toString() {
		if(this.isTrue())
			return "true";
		if(this.isFalse())
			return "false";
		StringBuffer sb = new StringBuffer();
		boolean ifFirst = true;
		sb.append("(");
		for (Clause c : clauses) {
			if(!ifFirst)
			sb.append(") or (");
			else
				ifFirst = false;
			sb.append(c.toString());
		}
		sb.append(")");
		return sb.toString();
	}

	public String encode() {
		StringBuffer sb = new StringBuffer();
		sb.append(ifTrue);
		for (Clause c : clauses) {
			sb.append(SEP);
			sb.append(c.encode());
		}
		return sb.toString();
	}

}
