package chord.project.analyses.metaback.dnf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import chord.util.Utils;

/**
 * The clause of a DNF, which is a conjunctive clause.
 * 
 * @author xin
 * 
 */
public class Clause implements Cloneable {
	private Map<Variable, Domain> literals;
	private boolean ifTrue;
	private static String CSEP = "#AND#";
	private static String LSEP = "#=#";

	/**
	 * Construct a true clause
	 */
	public Clause() {
		literals = new HashMap<Variable, Domain>();
		ifTrue = true;
	}

	public Clause(boolean ifTrue) {
		literals = new HashMap<Variable, Domain>();
		this.ifTrue = ifTrue;
	}

	public Clause(Map<Variable, Domain> literals) {
		this.literals = new HashMap<Variable, Domain>(literals);
		if (literals.size() == 0)
			ifTrue = true;
		else
			ifTrue = false;
	}

	public Clause(Map<Variable, Domain> literals, boolean ifTrue) {
		this.literals = new HashMap<Variable, Domain>(literals);
		this.ifTrue = ifTrue;
	}

	public Clause(DNFFactory fac, String line) {
		String tokens[] = Utils.split(line, CSEP, true, true, -1);
		this.ifTrue = Boolean.parseBoolean(tokens[0]);
		literals = new HashMap<Variable, Domain>();
		for (int i = 1; i < tokens.length; i++) {
			String[] literal = Utils.split(tokens[i], LSEP, true, true, -1);
			literals.put(fac.genVarFromStr(literal[0]),
					fac.genDomainFromStr(literal[1]));
		}
	}

	public static void setCSEP(String sep) {
		CSEP = sep;
	}

	public static void setLSEP(String sep) {
		LSEP = sep;
	}

	/**
	 * Return whether this Clause represents FAlSE. This == FAlSE iff
	 * literals.size()==0 && ifTrue = false
	 * 
	 * @return
	 */
	public boolean isFalse() {
		return literals.isEmpty() && ifTrue == false;
	}

	public boolean isTrue() {
		return literals.isEmpty() && ifTrue;
	}

	/**
	 * Add o=v to this clause, the effect is like a intersection. It has no
	 * effect on a false Clause
	 * 
	 * @param o
	 * @param v
	 */
	public void addLiteral(Variable o, Domain v) {
		if (this.isFalse())
			return;
		Domain existValue = literals.get(o);
		if (existValue != null)
			if (existValue.equals(v))
				return;
			else {
				literals.clear();
				ifTrue = false;
				return;
			}
		literals.put(o, v);
	}

	public Map<Variable, Domain> getLiterals() {
		return Collections.unmodifiableMap(literals);
	}

	public Clause intersect(Clause other) {
		if (this.isFalse() || other.isFalse())
			return new Clause(false);
		if (this.isTrue())
			return other.clone();
		if (other.isTrue())
			return this.clone();
		Clause ret = this.clone();
		for (Map.Entry<Variable, Domain> entry : other.literals.entrySet()) {
			if (ret.isFalse()) {
				break;
			}
			ret.addLiteral(entry.getKey(), entry.getValue());
		}
		return ret;
	}

	public boolean contains(Clause other) {
		if (this.isTrue())
			return true;
		if (this.isFalse())
			if (other.isFalse())
				return true;
			else
				return false;
		if (other.isTrue())
			return false;
		if(other.isFalse())
			return true;
		if (this.literals.size() > other.literals.size())
			return false;
		else
			for (Map.Entry<Variable, Domain> entry : literals.entrySet()) {
				Domain v = other.literals.get(entry.getKey());
				if (v == null)
					return false;
				else if (!entry.getValue().equals(v))
					return false;
			}
		return true;
	}

	public Clause clone() {
		Clause ret = new Clause(this.literals, this.ifTrue);
		return ret;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ifTrue ? 1231 : 1237);
		result = prime * result
				+ ((literals == null) ? 0 : literals.hashCode());
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
		Clause other = (Clause) obj;
		if (ifTrue != other.ifTrue)
			return false;
		if (literals == null) {
			if (other.literals != null)
				return false;
		} else if (!literals.equals(other.literals))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(this.isTrue())
			return "true";
		if(this.isFalse())
			return "false";
		StringBuffer sb = new StringBuffer();
		boolean ifFirst = true;
		for (Map.Entry<Variable, Domain> l : literals.entrySet()) {
			if (ifFirst)
				ifFirst = false;
			else
				sb.append(" and ");
			sb.append(l.getKey());
			sb.append(" = ");
			sb.append(l.getValue());
		}
		return sb.toString();
	}

	public int size() {
		return literals.size();
	}

	/**
	 * Encode current Clause as a String. Format:ifTrueC[SEPVariableLSEPDomain]?
	 * 
	 * @return
	 */
	public String encode() {
		StringBuffer sb = new StringBuffer();
		sb.append(ifTrue);
		for (Map.Entry<Variable, Domain> l : literals.entrySet()) {
			sb.append(CSEP);
			sb.append(l.getKey().encode());
			sb.append(LSEP);
			sb.append(l.getValue().encode());
		}
		return sb.toString();
	}
}
