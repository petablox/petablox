package chord.analyses.escape.metaback;

import chord.project.analyses.metaback.Query;
import chord.project.analyses.metaback.QueryResult;
import chord.project.analyses.metaback.dnf.DNF;

public class EscQueryResult implements QueryResult {
	private EscQuery q;
	private int result;
	private DNF nc;
	public final static String SEP = "#R#";

	public EscQueryResult(EscQuery q, int result, DNF nc) {
		super();
		this.q = q;
		this.result = result;
		this.nc = nc;
	}

	@Override
	public int getResult() {
		return result;
	}

	@Override
	public Query getQuery() {
		return q;
	}

	@Override
	public DNF getNC() {
		return nc;
	}

	@Override
	public String encode() {
		StringBuffer sb = new StringBuffer();
		sb.append(q.encode());
		sb.append(SEP);
		sb.append(result);
		sb.append(SEP);
		if(nc!=null)
			sb.append(nc.encode());
		else
			sb.append("");
		return sb.toString();
	}

}
