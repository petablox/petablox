package petablox.project.analyses.provenance;

import java.util.List;


public class FormatedConstraint{
	int weight;
	int[] constraint;
	public FormatedConstraint(int weight, int[] constraint){
		this.weight = weight;
		this.constraint = constraint;
	}
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(weight);
		for(int i : constraint)
			sb.append(" "+i);
		sb.append(" 0");
		return sb.toString();
	}
	
	public String toExplicitString(List<Tuple> tuplePool){
		StringBuffer sb = new StringBuffer();
		for(int i : constraint){
			if(i > 0)
				sb.append(tuplePool.get(i).toString());
			else
				sb.append("!"+tuplePool.get(0-i).toString());
				sb.append(" \\/ ");
		}
		return sb.toString();
	}
	
	public int[] getConstraint(){
		return this.constraint;
	}
}