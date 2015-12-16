 package chord.project.analyses.provenance;

import java.util.ArrayList;
import java.util.List;
/**
 * Represents a constraint in provenance, which has the following form:
 * headTuple = subTuple1 * subTuple2...subTupleN
 * @author xin
 *
 */
public class ConstraintItem {
	public Tuple headTuple;
	public List<Tuple> subTuples = new ArrayList<Tuple>();
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(headTuple.toString());
		sb.append(":=");
		for(int i = 0; i < subTuples.size(); i ++){
			if(i!=0)
				sb.append("*");
			sb.append(subTuples.get(i));
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((headTuple == null) ? 0 : headTuple.hashCode());
		result = prime * result
				+ ((subTuples == null) ? 0 : subTuples.hashCode());
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
		ConstraintItem other = (ConstraintItem) obj;
		if (headTuple == null) {
			if (other.headTuple != null)
				return false;
		} else if (!headTuple.equals(other.headTuple))
			return false;
		if (subTuples == null) {
			if (other.subTuples != null)
				return false;
		} else if (!subTuples.equals(other.subTuples))
			return false;
		return true;
	}
	
	
}
