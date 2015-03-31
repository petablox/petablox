package chord.project.analyses.metaback;
/**
 * A helper class to decode Query and QueryResult
 * @author xin
 *
 */
public interface QueryFactory {
	public QueryResult genResultFromStr(String s);

	public Query getQueryFromStr(String s);
}
