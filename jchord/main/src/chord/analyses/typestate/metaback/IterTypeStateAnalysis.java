package chord.analyses.typestate.metaback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.type.DomT;
import chord.analyses.var.DomV;
import chord.bddbddb.Rel.PairIterable;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.metaback.AbstractJobDispatcher;
import chord.project.analyses.metaback.QueryResult;
import chord.project.analyses.metaback.dnf.ClauseSizeCMP;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.parallelizer.JobDispatcher;
import chord.project.analyses.parallelizer.Mode;
import chord.project.analyses.parallelizer.ParallelAnalysis;
import chord.project.analyses.parallelizer.Scenario;
import chord.project.analyses.rhs.BackTraceIterator;
import chord.project.analyses.rhs.IWrappedPE;
import chord.project.analyses.rhs.TimeoutException;
import chord.util.Execution;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

/**
 * chord.ssa = [true|false] (default = true)<br>
 * chord.rhs.timeout = N milliseconds (default = 0, no timeouts)<br>
 * chord.iter-typestate-java.mode = [master|worker] (default = null)<br>
 * chord.iter-typestate-java.optimize = [true|false] (default = false)<br>
 * chord.iter-typestate-java.debug= [true|false] (default = false)<br>
 * chord.iter-typestate-java.disjuncts = [int] (default = 1)<br>
 * chord.iter-typestate-java.explode = [int] (default = -1)<br>
 * chord.iter-typestate-java.jobpatch = [int] (default = 100)<br>
 * chord.iter-typestate-java.iterlimit = [int] (default = 1000)<br>
 * chord.iter-typestate-java.xmlToHtmlTask = [String] (default = null)<br>
 * chord.iter-typestate-java.cicg = [String] (default = cicg-java)<br>
 * chord.iter-typestate-java.cipa = [String] (default = cipa-java)
 * chord.iter-typestate-java.onlyTrackedTypes = [true|false] (default = true; Implies that only SAFE paper types are used for queries)<br>
 * 
 * @author xin
 * 
 */
@Chord(name = "iter-typestate-java")
public class IterTypeStateAnalysis extends ParallelAnalysis {

	private String xmlToHtmlTask;
	private DomH domH;
	private boolean DEBUG;

	// Fields used by the worker
	private TSAbsFactory absFac = TSAbsFactory.getSingleton();
	private TSQueryFactory qFac = TSQueryFactory.getSingleton();
	private boolean optimizeSumms;
	private int numDisjuncts;
	private int timeout;
	private boolean dnegation;
	private boolean prune;
	private boolean isOnlyTrackedTypes;

	private DomI domI;
	private DomV domV;
	private String cipaName;
	private String cicgName;
	private ICICG cicg;
	private CIPAAnalysis cipa;
	private jq_Method mainMethod;
	private Map<jq_Method, Set<Quad>> callersMap = new HashMap<jq_Method, Set<Quad>>();
	private Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();

	// Fields used by the master
	private TSJobDispatcher dispatcher;
	private Set<Quad> checkIncludedI;
	private Set<Quad> trackedSites;
	private List<Pair<Quad, Quad>> queries;
	private int trackedQuery;
	private String trackedQueryFiles;
	private int explode;
	private int jobPatch;
	private int iterLimit;

	// Fields used by the worker
	private ForwardAnalysis worker;

	@Override
	public void init() {
		X = new Execution("iter-typestate-java");

		this.xmlToHtmlTask = X.getStringArg("xmlToHtmlTask", null);
		this.optimizeSumms = X.getBooleanArg("optimize", false);
		this.numDisjuncts = X.getIntArg("disjuncts", 1);
		this.DEBUG = X.getBooleanArg("debug", false);
		this.timeout = X.getIntArg("timeout", -1);
		this.trackedQuery = X.getIntArg("query", -1);
		this.trackedQueryFiles = X.getStringArg("queryFiles", null);
		this.explode = X.getIntArg("explode", -1);
		this.jobPatch = X.getIntArg("jobpatch", 100);
		this.dnegation = X.getBooleanArg("negate", true);
		this.prune = X.getBooleanArg("prune", true);
		this.iterLimit = X.getIntArg("iterlimit", 1000);
		this.cipaName = X.getStringArg("cipa", "my-cipa-java");
		this.cicgName = X.getStringArg("cicg", "cicg-java");
		this.isOnlyTrackedTypes = X.getBooleanArg("onlyTrackedTypes", true);
		
		MetaBackAnalysis.setDEBUG(DEBUG);
		MetaBackAnalysis.setErrSufSize(numDisjuncts);
		MetaBackAnalysis.setOptimizeSumms(optimizeSumms);
		MetaBackAnalysis.setTimeout(timeout);
		MetaBackAnalysis.setDNegation(dnegation);
		MetaBackAnalysis.setPrune(prune);
		AbstractJobDispatcher.setDEBUG(DEBUG);
		AbstractJobDispatcher.setExplode(explode);
		AbstractJobDispatcher.setJobPatchSize(jobPatch);
		AbstractJobDispatcher.setIterLimit(iterLimit);

		cipa = (CIPAAnalysis) ClassicProject.g().getTask(cipaName);
		ClassicProject.g().runTask(cipa);

		CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask(
				cicgName);
		ClassicProject.g().runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();

		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);

		domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);

		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);

		System.setProperty("chord.rhs.merge", "pjoin");
		System.setProperty("chord.rhs.trace", "shortest");

		TSAbsFactory.setDomV(domV);
		TSQueryFactory.setDomI(domI);
		TSQueryFactory.setDomH(domH);

		checkIncludedI = new HashSet<Quad>();

		ClassicProject.g().runTask("checkIncludedI-dlog");
		ProgramRel relI = (ProgramRel) ClassicProject.g().getTrgt(
				"checkIncludedI");
		relI.load();
		Iterable<Quad> tuples = relI.getAry1ValTuples();
		for (Quad q : tuples)
			checkIncludedI.add(q);
		relI.close();

		if (getMode() == Mode.MASTER)
			initMaster();
		else
			initWorker();
	}

	private void initMaster() {

		fillTrackedSites();
		fillQueries();
		dispatcher = new TSJobDispatcher(xmlToHtmlTask, this, queries, domV,
				domH, domI);
	}

	private void fillTrackedSites() {
		trackedSites = new HashSet<Quad>();
	/*	ProgramRel relCheckExcludedT = (ProgramRel) ClassicProject.g().getTrgt(
				"checkExcludedT");
		ClassicProject.g().runTask(relCheckExcludedT);
		relCheckExcludedT.load();
		int numH = domH.getLastA() + 1;
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			if (q.getOperator() instanceof New) {
				jq_Class c = q.getMethod().getDeclaringClass();
				String qs = q.toString();
				if (!(qs.contains("java.lang.StringBuilder")
						|| qs.contains("java.lang.StringBuffer") || qs
							.contains("java.lang.String")))// well string
															// operations are
															// not interesting
					if (!relCheckExcludedT.contains(c)) {
						trackedSites.add(q);
					}
			}
		}
		relCheckExcludedT.close();
	*/
		Set<jq_Type> trackedTypes = new HashSet<jq_Type>();

		if(isOnlyTrackedTypes){
			String[] trackedTypesStr = getTrackedTypes();

			ProgramRel relSub = (ProgramRel) ClassicProject.g().getTrgt("sub");
			ClassicProject.g().runTask(relSub);
			relSub.load();
			DomT domT = (DomT) ClassicProject.g().getTrgt("T");


			for(String trackedTypeStr : trackedTypesStr){
				jq_Type trackedType = jq_Type.parseType(trackedTypeStr);
				trackedTypes.add(trackedType);

				if(domT.contains(trackedType)){
					for (jq_Type type : domT) {
						if (relSub.contains(type, trackedType))
							trackedTypes.add(type);
					}
				}
			}
			relSub.close();
		}
		
		ClassicProject.g().runTask("checkExcludedH-dlog");
		ProgramRel relCheckExcludedH = (ProgramRel) ClassicProject.g().getTrgt("checkExcludedH");
		relCheckExcludedH.load();
		int numH = domH.getLastA() + 1;
		for(int hIdx = 1; hIdx < numH; hIdx++){
			Quad q = (Quad) domH.get(hIdx);
			if(!relCheckExcludedH.contains(q)){
				String qType = DomH.getType(q);
				if (!(qType.startsWith("java.lang.String") || qType.startsWith("java.lang.StringBuilder") 
						|| qType.startsWith("java.lang.StringBuffer") || qType.contains("Exception"))){
					if ((q.getOperator() instanceof New && trackedTypes.contains(New.getType(q).getType()))
							|| (q.getOperator() instanceof NewArray && trackedTypes.contains(NewArray.getType(q).getType()))
							|| (q.getOperator() instanceof MultiNewArray && trackedTypes.contains(MultiNewArray.getType(q).getType()))
							|| !isOnlyTrackedTypes) {
						trackedSites.add(q);
					}
					
				}
			}
		}
	}

	private String[] getTrackedTypes(){
			return new String[]{"java.util.Enumeration", "java.io.InputStream", 
					"java.util.Iterator", "java.security.KeyStore", "java.io.PrintStream", 
					"java.io.PrintWriter", "java.security.Signature", "java.net.Socket",
					"java.util.Stack", "java.net.URLConnection", "java.util.Vector"};
	}
	private void fillQueries() {
		if (this.queries == null) {
			queries = new ArrayList<Pair<Quad, Quad>>();
			for (Quad q : checkIncludedI) {
				//if (!q.toString().contains("<init>")&&Invoke.getParamList(q).length()>0) {
				if ((q.getOperator() instanceof InvokeVirtual 
						|| q.getOperator() instanceof InvokeInterface)) {
					Register v = Invoke.getParam(q, 0).getRegister();
					try {
						OUT: for (Quad h : cipa.pointsTo(v).pts) {
							if (trackedSites.contains(h)) {
								for (jq_Method m : cicg.getTargets(q)) {
									if (isInterestingMethod(m, h, q)) {
										queries.add(new Pair<Quad, Quad>(q, h));
										continue OUT;
									}
								}
							}
						}
					} catch (RuntimeException e) {
						continue;
					}
				}
			}
		}
	}

	private boolean isMethodDeclaredInClass(Quad invoke, Quad h) {
		jq_Type hType = New.getType(h).getType();
		for (jq_Method m : cicg.getTargets(invoke)) {
			if (m.getDeclaringClass().equals(hType))
				return true;
		}
		return false;
	}

	private static boolean isInterestingSite(Operator o) {
		return o instanceof Invoke && !(o instanceof InvokeStatic);
	}

	private void initWorker() {
	}

	public jq_Method getMainMethod() {
		return mainMethod;
	}

	public int getDomVIdx(Register r) {
		return domV.indexOf(r);
	}

	public boolean isInterestingMethod(jq_Method m, Quad allocSite, Quad invoke) {
		if (m.isStatic() || m.toString().equals("<init>:()V@java.lang.Object"))
			return false;
		if (checkIncludedI.contains(invoke)) {
			return true;
		}
		return false;
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub

	}

	/**
	 * This is where the job is really done for workers
	 */
	@Override
	public String apply(String line) {
		Scenario in = new Scenario(line, TSJobDispatcher.MAJSEP);
		String absStr = in.getIn();
		TSAbstraction abs = (TSAbstraction) absFac.genAbsFromStr(absStr);
		String qStrs[] = Utils.split(in.getOut(), TSJobDispatcher.MINSEP, true,
				true, -1);
		List<TSQueryResult> results = new ArrayList<TSQueryResult>();
		List<TSQuery> tmpQueries = new ArrayList<TSQuery>();
		for (String qStr : qStrs) {
			TSQuery q = (TSQuery) qFac.getQueryFromStr(qStr);
			tmpQueries.add(q);
		}
		ForwardAnalysis forAnalysis = new ForwardAnalysis(this);
		forAnalysis.setCICG(cicg);
		forAnalysis.setCIPA(cipa);
		forAnalysis.setDomV(domV);
		forAnalysis.setTrackedAPs(abs.getVIdxes());
		forAnalysis.setQueries(tmpQueries);
		forAnalysis.run();
		if (forAnalysis.isTimeOut()) {
			for (TSQuery query : tmpQueries) {
				TSQueryResult r = new TSQueryResult(query, QueryResult.TIMEOUT,
						null);
				results.add(r);
			}
		} else {
			for (Pair<Quad, Quad> pq : forAnalysis.getProvenQueries()) {
				TSQuery query = new TSQuery(domI.indexOf(pq.val0), domI,
						domH.indexOf(pq.val1), domH);
				TSQueryResult result = new TSQueryResult(query,
						QueryResult.PROVEN, null);
				results.add(result);
			}
			for (Pair<Quad, Quad> eq : forAnalysis.getErrQueries()) {
				TSQuery query = new TSQuery(domI.indexOf(eq.val0), domI,
						domH.indexOf(eq.val1), domH);
				IWrappedPE<Edge, Edge> wpe = forAnalysis.getErrWPE(eq.val0,
						eq.val1);
				DNF errSuf = getErrSuf(eq);
				BackTraceIterator<Edge, Edge> backIter = forAnalysis
						.getBackTraceIterator(wpe);
				MetaBackAnalysis backAnalysis = new MetaBackAnalysis(this,
						errSuf, backIter, abs.getVIdxes());
				DNF nc;
				try {
					nc = backAnalysis.run();
				} catch (TimeoutException E) {
					TSQueryResult result = new TSQueryResult(query,
							QueryResult.TIMEOUT, null);
					results.add(result);
					continue;
				}
				int resultType;
				if (nc.isFalse())
					resultType = QueryResult.IMPOSSIBILITY;
				else
					resultType = QueryResult.REFINE;
				TSQueryResult result = new TSQueryResult(query, resultType, nc);
				results.add(result);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (TSQueryResult r : results) {
			if (sb.length() != 0)
				sb.append(TSJobDispatcher.MINSEP);
			sb.append(r.encode());
		}
		in.setOut(sb.toString());
		in.setType(AbstractJobDispatcher.RESULT);
		return in.encode();
	}

	public DNF getErrSuf(Pair<Quad, Quad> query) {
		TSVariable tsv = TSVariable.getSingleton();
		TSEVariable esv = TSEVariable.getSingleton();
		DNF dnf1 = new DNF(new ClauseSizeCMP(), tsv, TSBoolDomain.F());
		DNF dnf2 = new DNF(new ClauseSizeCMP(), esv, TSBoolDomain.T());
		DNF dnf = dnf1.intersect(dnf2);
		return dnf;
	}

	public boolean isSkippedMethod(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets.isEmpty();
	}

	@Override
	public JobDispatcher getJobDispatcher() {
		return dispatcher;
	}

	public DomV domV() {
		return domV;
	}

	public DomH domH() {
		return domH;
	}

	public DomI domI() {
		return domI;
	}

	public boolean mayPointTo(Register v, Quad h) {
		return Helper.mayPointsTo(v, h, cipa);
	}

	public CIPAAnalysis getCIPA() {
		return cipa;
	}

}
