package chord.project.analyses.metaback;

/**
 * Represent a query we use in the iterative refinement
 * @author xin
 *
 */
public interface Query {
	/**
	 * Generate a query from string s
	 * @param s
	 */
	public void decode(String s);
	/**
	 * Encode current query as a string
	 * @return the encoded string
	 */
	public String encode();
	
	public String encodeForXML();
	
	public boolean equals(Object o);
	
	public int hashCode();
}