package chord.analyses.typestate.metaback;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.project.analyses.metaback.AbstractJobDispatcher;
import chord.project.analyses.metaback.Abstraction;
import chord.project.analyses.metaback.AbstractionFactory;
import chord.project.analyses.metaback.Query;
import chord.project.analyses.metaback.QueryFactory;
import chord.project.analyses.metaback.dnf.ClauseSizeCMP;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.parallelizer.ParallelAnalysis;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

public class TSJobDispatcher extends AbstractJobDispatcher {
	public final static String MINSEP = "#JMI#";
	public final static String MAJSEP = "#JMA#";
	private final static TSAbsFactory absFac  = TSAbsFactory.getSingleton();
	private final static TSQueryFactory queryFac = TSQueryFactory.getSingleton();
	private List<Pair<Quad,Quad>> queries;
	private DomV domV;
	private DomH domH;
	private DomI domI;
	public TSJobDispatcher(String xmlToHtmlTask, ParallelAnalysis master, List<Pair<Quad,Quad>> queries, DomV domV,DomH domH, DomI domI) {
		super(xmlToHtmlTask, master);
		this.domV = domV;
		this.domH = domH;
		this.domI = domI;
		this.queries = queries;
		TSAbsFactory.setDomV(domV);
		TSQueryFactory.setDomH(domH);
		TSQueryFactory.setDomI(domI);
		TSDNFFactory.setDomV(domV);//It's easy to miss here.....
	}

	@Override
	protected Map<Query, DNF> getInitialANCS() {
		Map<Query,DNF> initANCS = new HashMap<Query,DNF>();
		for(Pair<Quad,Quad> query:queries)
			initANCS.put(new TSQuery(domI.indexOf(query.val0),domI,domH.indexOf(query.val1),domH), DNF.getTrue(new ClauseSizeCMP()));
		return initANCS;
	}

	@Override
	public String getMinorSep() {
		return MINSEP;
	}

	@Override
	public String getMajorSep() {
		return MAJSEP;
	}
	
	@Override
	protected AbstractionFactory getAbsFactory() {
		return absFac;
	}

	@Override
	protected QueryFactory getQueryFactory() {
		return queryFac;
	}

	@Override
	public void saveState() {
		super.saveState();
		//Output the iteration count and H numbers of provenQueries
		PrintWriter iterTout = Utils.openOut(EX.path("iterT.txt"));
		PrintWriter groupTout = Utils.openOut(EX.path("groupT.txt"));
		PrintWriter paramTout = Utils.openOut(EX.path("paramT.txt"));
		PrintWriter tqOut = Utils.openOut(EX.path("tq.txt"));
		int tNum = 0;
		for(Map.Entry<Abstraction, Set<Query>> entry: provenQs.entrySet()){
			TSAbstraction abs = (TSAbstraction)entry.getKey();
			paramTout.println(abs.getTVs().size());
			Set<Query> group = entry.getValue();
			groupTout.println(group.size());
			for(Query q : group){
				tNum++;
				TSQuery tq = (TSQuery)q;
				iterTout.println(iterMap.get(q));
				tqOut.println(tq.getIIdx()+" "+tq.getHIdx());
			}
		}
		iterTout.flush();
		iterTout.close();
		groupTout.flush();
		groupTout.close();
		tqOut.flush();
		tqOut.close();
		paramTout.flush();
		paramTout.close();
		
		PrintWriter iterFout = Utils.openOut(EX.path("iterF.txt"));
		PrintWriter fqOut = Utils.openOut(EX.path("fq.txt"));
		for(Query q: impossiQs){
			TSQuery eq = (TSQuery)q;
			iterFout.println(iterMap.get(q));
			fqOut.println(eq.getIIdx()+" "+eq.getHIdx());
		}
		iterFout.flush();
		iterFout.close();
		fqOut.flush();
		fqOut.close();
		
		PrintWriter sumOut = Utils.openOut(EX.path("new_stats.txt"));
		sumOut.println("newQ:"+queries.size()+",newT:"+tNum+",newF:"+impossiQs.size());
		sumOut.flush();
		sumOut.close();
	}
}
