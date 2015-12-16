package chord.project.analyses.provenance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import chord.bddbddb.Rel.RelView;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;

/**
 * A rule generated from a single rule in the datalog. It represents a set of
 * constraints generated from the rule.
 * 
 * @author xin
 * 
 */
public class LookUpRule {
	private String instrName;
	private Term headTerm;
	private List<Term> subGoalTerm = new ArrayList<Term>();
	private ProgramRel instRelation;
	private boolean ifNeg = false;

	public LookUpRule(String line) {
		Scanner sc = new Scanner(line);
		instrName = sc.next();
		headTerm = readTerm(sc);
		while (sc.hasNext()) {
			subGoalTerm.add(readTerm(sc));
		}
		instRelation = (ProgramRel) ClassicProject.g().getTrgt(instrName);
		instRelation.load();
	}

	public void update(){
		instRelation.load();
	}
	
	public String getHeadRelName() {
		return headTerm.name;
	}

	/**
	 * Whether t is on the lhs of the rule
	 * 
	 * @param t
	 * @return
	 */
	public boolean match(Tuple t) {
		if (t.getRelName().equals(headTerm.name))
			return true;
		return false;
	}

	/**
	 * To get all the constraints related to t in current rule
	 * 
	 * @param t
	 * @return
	 */
	public List<ConstraintItem> lookUp(Tuple t) {
		List<ConstraintItem> ret = new ArrayList<ConstraintItem>();
		Iterator<ConstraintItem> iter = this.getConstrIterForTuple(t);
		while (iter.hasNext()) {
			ret.add(iter.next());
		}
		return ret;
	}

	public Iterator<ConstraintItem> getConstrIterForTuple(Tuple t) {
		if (!this.match(t))
			throw new RuntimeException(t + " does not matche the head term of current rule.");
		return new ConstraintItemIterator(t);
	}

	public Iterator<ConstraintItem> getAllConstrIterator() {
		return new ConstraintItemIterator(null);
	}

	/**
	 * Generate the tuple from a view with selectAndDelete on
	 * @param headTuple
	 * @param parInstRel
	 * @param t
	 * @return
	 */
	private Tuple getTuple(Tuple headTuple, int[] parInstRel, Term t) {
		int[] instrRel = new int[instRelation.getDoms().length];
		int c = 0;
		int tupleIdx[] = headTuple.getIndices();
		OUT: for (int i = 0; i < instrRel.length; i++) {
			for (int j = 0; j < headTerm.attrIdx.size(); j++)
				if (!headTerm.isConstant.get(j))
					if (headTerm.attrIdx.get(j) == i) {
						instrRel[i] = tupleIdx[j];
						continue OUT;
					}
			instrRel[i] = parInstRel[c];
			c++;
		}
		return getTuple(instrRel, t);
	}

	private Tuple getTuple(int[] instrRel, Term t) {
		ProgramRel tRel = (ProgramRel) ClassicProject.g().getTrgt(t.name);
		int indices[] = new int[t.attrIdx.size()];
		for (int i = 0; i < t.attrIdx.size(); i++)
			if (t.isConstant.get(i))
				indices[i] = t.attrIdx.get(i);
			else
				indices[i] = instrRel[t.attrIdx.get(i)];
		Tuple ret = new Tuple(tRel, indices);
		return ret;
	}

	private Term readTerm(Scanner sc) {
		Term ret = new Term();
		ret.name = sc.next();
		int attrNum = Integer.parseInt(sc.next());
		for (int i = 0; i < attrNum; i++) {
			String item = sc.next();
			if (item.startsWith("_")) {
				item = item.substring(1);
				ret.attrIdx.add(Integer.parseInt(item));
				ret.isConstant.add(true);
			} else if (item.equals("*")) {
				ret.attrIdx.add(-1);
				ret.isConstant.add(true);
			} else {
				ret.attrIdx.add(Integer.parseInt(item));
				ret.isConstant.add(false);
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(instrName + ": ");
		sb.append(headTerm);
		return "LookUpRule [instrName=" + instrName + ", headTerm=" + headTerm + ", subGoalTerm=" + subGoalTerm + ", instRelation=" + instRelation
				+ "]";
	}

	class ConstraintItemIterator implements Iterator<ConstraintItem> {
		Iterator<int[]> iter;
		Tuple t;

		/**
		 * Create the iterator to get ConstraintItems related to t, if t ==
		 * null, return all the ConstraintItems
		 * 
		 * @param t
		 */
		ConstraintItemIterator(Tuple t) {
			if (t == null)
				iter = instRelation.getAryNIntTuples().iterator();
			else {
				this.t = t;
				RelView view = instRelation.getView();
				int[] indicies = t.getIndices();
				for (int i = 0; i < headTerm.attrIdx.size(); i++) {
					if (!headTerm.isConstant.get(i))
						view.selectAndDelete(headTerm.attrIdx.get(i).intValue(), indicies[i]);
				}
				iter = view.getAryNIntTuples().iterator();
			}
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public ConstraintItem next() {
			int[] instRel = iter.next();
			ConstraintItem item = new ConstraintItem();
			if (t != null)
				item.headTuple = t;
			else
				item.headTuple = getTuple(instRel, headTerm);
			for (Term term : subGoalTerm) {
				if (term.name.startsWith("!")) {
					if (!ifNeg)
						System.out.println("Negation detected in the datalog rules. Pay attention for unwanted errors.");
					ifNeg = true;
					continue;
				}
				Tuple sgt = null;
				if(t != null)
				 sgt = getTuple(item.headTuple, instRel, term);
				else
				 sgt = getTuple(instRel,term);	
				item.subTuples.add(sgt);
			}
			return item;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}

class Term {
	public String name;
	public List<Integer> attrIdx = new ArrayList<Integer>();
	public List<Boolean> isConstant = new ArrayList<Boolean>();

	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        for (int i = 0; i < attrIdx.size(); i++) {
			if (i != 0)
				sb.append(",");
			if (isConstant.get(i))
				sb.append("_");
			sb.append(attrIdx.get(i));
        }
        sb.append(")");
        return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attrIdx == null) ? 0 : attrIdx.hashCode());
		result = prime * result + ((isConstant == null) ? 0 : isConstant.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Term other = (Term) obj;
		if (attrIdx == null) {
			if (other.attrIdx != null)
				return false;
		} else if (!attrIdx.equals(other.attrIdx))
			return false;
		if (isConstant == null) {
			if (other.isConstant != null)
				return false;
		} else if (!isConstant.equals(other.isConstant))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
