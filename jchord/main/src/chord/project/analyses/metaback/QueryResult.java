package chord.project.analyses.metaback;

import chord.project.analyses.metaback.dnf.DNF;

/**
 * The result of a query
 * @author xin
 *
 */
public interface QueryResult {
	public final int PROVEN = 0;// the query holds
	public final int IMPOSSIBILITY = 1;// the query can't be proven
	public final int REFINE = 2; //the abstraction needs to be refined
	public final int TIMEOUT = 3;//the analysis timed out for this query

	public int getResult();

	/**
	 * Get the correspondent query
	 * @return
	 */
	public Query getQuery();
	
	/**
	 * If the query doesn't hold, return the necessary condition needed to prove
	 * the query. Otherwise, return a TRUE DNF
	 * 
	 * @return the necessary condition
	 */
	public DNF getNC();
	
	public String encode();
}
