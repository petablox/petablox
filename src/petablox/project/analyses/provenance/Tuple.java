package petablox.project.analyses.provenance;

import java.util.Arrays;

import petablox.bddbddb.Dom;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

/**
 * Represents a tuple in the program relation
 * 
 * @author xin
 * 
 */
public class Tuple {
    private ProgramRel relation;
    private Dom[] domains;
    private int[] domIndices;
    public static ProgramRel relMV = null;

    public Tuple(ProgramRel relation, int[] indices) {
        this.relation = relation;
        domains = relation.getDoms();
        this.domIndices = indices;
    }
    public boolean isSpurious(){
        for(int i = 0 ; i < domains.length; i++)
            if(domIndices[i] < 0 || domIndices[i] >= domains[i].size())
                return true;
        return false;
    }

    /**
     * Assume s has the following form: VH(2,3)
     * 
     * @param s
     */
    public Tuple(String s) {
        String splits1[] = s.split("\\("); 
        String rName = splits1[0];
        String indexString = splits1[1].replace(")", "");
        String splits2[] = indexString.split(",");
        relation = (ProgramRel)ClassicProject.g().getTrgt(rName);
        //	relation.load();
        domains = relation.getDoms();
        domIndices = new int[splits2.length];
        for(int i = 0 ; i < splits2.length; i++){
            domIndices[i] = Integer.parseInt(splits2[i]);
        }
    }

    public Dom[] getDomains(){
        return this.domains;
    }

    public Object getValue(int idx){
        return this.domains[idx].get(domIndices[idx]);
    }

    public ProgramRel getRel(){
        return relation;
    }

    public String getRelName() {
        return relation.getName();
    }

    public int[] getIndices() {
        return domIndices;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(domIndices);
        result = prime * result
            + ((relation == null) ? 0 : relation.hashCode());
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
        Tuple other = (Tuple) obj;
        if (relation == null) {
            if (other.relation != null)
                return false;
        } else if (!relation.equals(other.relation))
            return false;
        if (!Arrays.equals(domIndices, other.domIndices))
            return false;
        return true;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("");
        sb.append(relation.getName());
        sb.append("(");
        for(int i = 0; i < domIndices.length; i++){
            if(i!=0)
                sb.append(',');
            sb.append(domIndices[i]);
        }
        sb.append(")");
        return sb.toString();
    }


    public String toSummaryString(String sep){
        StringBuilder sb = new StringBuilder("");
        sb.append(relation.getName());
        for(int i = 0; i < domIndices.length; i++){
            sb.append(sep);
            sb.append(domains[i].toUniqueString(domIndices[i]));
        }
        return sb.toString();
    }

    public String toVerboseString(){
        StringBuilder sb = new StringBuilder("");
        sb.append(this.toString());
        sb.append(" : ");
        for(int i = 0; i < domIndices.length; i++){
            if(i!=0)
                sb.append(',');
            sb.append(domains[i].get(domIndices[i]));
        }
        return sb.toString();
    }
}
