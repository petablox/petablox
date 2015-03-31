package chord.analyses.typestate.metaback;

import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.project.analyses.metaback.Query;
import chord.project.analyses.metaback.QueryFactory;
import chord.project.analyses.metaback.QueryResult;
import chord.project.analyses.metaback.dnf.ClauseSizeCMP;
import chord.project.analyses.metaback.dnf.DNF;
import chord.util.Utils;

public class TSQueryFactory implements QueryFactory {
	private final static TSQueryFactory singleton = new TSQueryFactory();
	private static DomI domI;
	private static DomH domH;
	
	public static void setDomI(DomI dom){
		domI = dom;
	}
	
	public static void setDomH(DomH dom){
		domH = dom;
	}
	
	private TSQueryFactory(){}
	
	public static TSQueryFactory getSingleton(){
		return singleton;
	}
	
	
	@Override
	public QueryResult genResultFromStr(String s) {
		String tokens[] = Utils.split(s, TSQueryResult.SEP, true, true, -1);
		TSQuery q = (TSQuery)this.getQueryFromStr(tokens[0]);
		int result = Integer.parseInt(tokens[1]);
		DNF nc =null;
		if(result==QueryResult.REFINE&&tokens.length>=3)
		nc = new DNF(new ClauseSizeCMP(),TSDNFFactory.getSingleton(),tokens[2]);
		return new TSQueryResult(q,result,nc);
	}

	@Override
	public Query getQueryFromStr(String s) {
		TSQuery ret = new TSQuery(0,domI,0,domH);
		ret.decode(s);
		return ret;
	}

}
