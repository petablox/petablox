package chord.project.analyses.metaback;
/**
 * A class to represent the abstraction or parameter of the analysis
 * @author xin
 *
 */
public interface Abstraction extends Comparable<Abstraction>{

	public String encode();

	public void decode(String s);
	
	public boolean equals(Object other);
	
	public int hashCode();
	
	public String encodeForXML();
}
