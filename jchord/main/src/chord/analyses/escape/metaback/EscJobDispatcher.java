package chord.analyses.escape.metaback;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.analyses.heapacc.DomE;
import chord.project.analyses.metaback.AbstractJobDispatcher;
import chord.project.analyses.metaback.Abstraction;
import chord.project.analyses.metaback.AbstractionFactory;
import chord.project.analyses.metaback.Query;
import chord.project.analyses.metaback.QueryFactory;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.parallelizer.ParallelAnalysis;
import chord.util.Utils;

public class EscJobDispatcher extends AbstractJobDispatcher {
	public final static String MINSEP = "#JMI#";
	public final static String MAJSEP = "#JMA#";
	private final static EscAbsFactory absFac  = EscAbsFactory.getSingleton();
	private final static EscQueryFactory queryFac = EscQueryFactory.getSingleton();
	private List<Quad> queryE;
	private DomE domE;
	private DomH domH;
	public EscJobDispatcher(String xmlToHtmlTask, ParallelAnalysis master, List<Quad> queryE, DomE domE,DomH domH) {
		super(xmlToHtmlTask, master);
		this.domE = domE;
		this.domH = domH;
		this.queryE = queryE;
		EscAbsFactory.setDomH(domH);
		EscQueryFactory.setDomE(domE);
		EscDNFFactory.setDomH(domH);//It's easy to miss here.....
	}

	@Override
	protected Map<Query, DNF> getInitialANCS() {
		Map<Query,DNF> initANCS = new HashMap<Query,DNF>();
		for(Quad q:queryE)
			initANCS.put(new EscQuery(domE.indexOf(q),domE), DNF.getTrue(new LNumCMP()));
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
			EscAbstraction abs = (EscAbstraction)entry.getKey();
			paramTout.println(abs.getLHs().size());
			Set<Query> group = entry.getValue();
			groupTout.println(group.size());
			for(Query q : group){
				tNum++;
				iterTout.println(iterMap.get(q));
				tqOut.println(((EscQuery)q).getIdx());
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
			EscQuery eq = (EscQuery)q;
			iterFout.println(iterMap.get(q));
			fqOut.println(eq.getIdx());
		}
		iterFout.flush();
		iterFout.close();
		fqOut.flush();
		fqOut.close();
		
		PrintWriter sumOut = Utils.openOut(EX.path("new_stats.txt"));
		sumOut.println("newQ:"+queryE.size()+",newT:"+tNum+",newF:"+impossiQs.size());
		sumOut.flush();
		sumOut.close();
	}
}
