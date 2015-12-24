package petablox.analyses.escape.metaback;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.util.*;

import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.ICICG;
import petablox.analyses.alloc.DomH;
import petablox.analyses.escape.hybrid.full.Edge;
import petablox.analyses.escape.hybrid.full.ThreadEscapeFullAnalysis;
import petablox.analyses.field.DomF;
import petablox.analyses.heapacc.DomE;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.analyses.var.DomV;
import petablox.program.Loc;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.project.analyses.metaback.AbstractJobDispatcher;
import petablox.project.analyses.metaback.QueryResult;
import petablox.project.analyses.metaback.dnf.ClauseSizeCMP;
import petablox.project.analyses.metaback.dnf.DNF;
import petablox.project.analyses.parallelizer.JobDispatcher;
import petablox.project.analyses.parallelizer.Mode;
import petablox.project.analyses.parallelizer.ParallelAnalysis;
import petablox.project.analyses.parallelizer.Scenario;
import petablox.project.analyses.rhs.BackTraceIterator;
import petablox.project.analyses.rhs.IWrappedPE;
import petablox.project.analyses.rhs.TimeoutException;
import petablox.util.Execution;
import petablox.util.Utils;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;
import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.toolkits.graph.Block;
import soot.util.Chain;

/**
 * chord.ssa = [true|false] (default = true)<br>
 * chord.rhs.timeout = N milliseconds (default = 0, no timeouts)<br>
 * chord.iter-thresc-java.mode = [master|worker] (default = null)<br>
 * chord.iter-thresc-java.optimize = [true|false] (default = false)<br>
 * chord.iter-thresc-java.debug= [true|false] (default = false)<br>
 * chord.iter-thresc-java.disjuncts = [int] (default = 1)<br>
 * chord.iter-thresc-java.query = [int] (default = -1)<br>
 * chord.iter-thresc-java.explode = [int] (default = -1)<br>
 * chord.iter-thresc-java.jobpatch = [int] (default = 100)<br>
 * chord.iter-thresc-java.iterlimit = [int] (default = 1000)<br>
 * chord.iter-thresc-java.xmlToHtmlTask = [String] (default = null)
 * 
 * @author xin
 *
 */
@Petablox(name = "iter-thresc-java",consumes = { "queryE" })
public class IterThrEscAnalysis extends ParallelAnalysis {

	private String xmlToHtmlTask;
	private DomH domH;
	private DomE domE;
	private boolean DEBUG;

	// Fields used by the worker
	private EscAbsFactory absFac = EscAbsFactory.getSingleton();
	private EscQueryFactory qFac = EscQueryFactory.getSingleton();
	private boolean optimizeSumms;
	private int numDisjuncts;
	private int timeout;
	private boolean dnegation;
	private boolean prune;

	private DomM domM;
	private DomI domI;
	private DomV domV;
	private DomF domF;
	private int varId[];
	private TObjectIntHashMap<SootMethod> methToNumVars = new TObjectIntHashMap<SootMethod>();
	private TObjectIntHashMap<SootMethod> methToFstVar = new TObjectIntHashMap<SootMethod>();
	private ICICG cicg;
	private SootMethod mainMethod;
	private SootMethod threadStartMethod;
	private TObjectIntHashMap<Object> quadToRPOid;
	private Map<Unit, Loc> invkQuadToLoc;
	private Map<SootMethod, Set<Unit>> callersMap = new HashMap<SootMethod, Set<Unit>>();
	private Map<Unit, Set<SootMethod>> targetsMap = new HashMap<Unit, Set<SootMethod>>();

	// Fields used by the master
	private EscJobDispatcher dispatcher;
	private int trackedQuery;
	private String trackedQueryFiles;
	private int explode;
	private int jobPatch;
	private int iterLimit;

	@Override
	public void init() {
		X = new Execution("iter-thresc-java");

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
		
		ThrEscForwardAnalysis.setOptimize(optimizeSumms);
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
		
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);
		
		EscAbsFactory.setDomH(domH);
		EscQueryFactory.setDomE(domE);
		
		if (getMode() == Mode.MASTER)
			initMaster();
		else
			initWorker();
	}

	private void initMaster() {
		ProgramRel relQueryE = (ProgramRel) ClassicProject.g().getTrgt("queryE");
		relQueryE.load();
		Iterable<Unit> tuples = relQueryE.getAry1ValTuples();
		List<Unit> allEs = new ArrayList<Unit>();
		if(trackedQueryFiles!=null)
			allEs = getQueriesFromFiles(trackedQueryFiles);
		else
		for (Unit q : tuples) {
			if(trackedQuery<0||domE.indexOf(q) == trackedQuery)
			allEs.add(q);
		}
//		fillInQuery(allEs);
		dispatcher = new EscJobDispatcher(xmlToHtmlTask,this,allEs,domE,domH);
	}

	private List<Unit> getQueriesFromFiles(String s){
		try{
		List<Unit> allEs = new ArrayList<Unit>();
		String files[] = s.split(",");
		for(String f:files){
			Scanner sc = new Scanner(new File(f.trim()));
			while(sc.hasNext()){
				String line = sc.nextLine().trim();
				if(!line.equals("")){
					allEs.add(domE.get((Integer.parseInt(line))));
				}
			}
		}
		return allEs;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	private void initWorker() {
		Program program = Program.g();
		mainMethod = program.getMainMethod();
		threadStartMethod = program.getThreadStartMethod();
		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		domF = (DomF) ClassicProject.g().getTrgt("F");
		ClassicProject.g().runTask(domF);
		int numV = domV.size();
		varId = new int[numV];
		for (int vIdx = 0; vIdx < numV;) {
			Local v = domV.get(vIdx);
			SootMethod m = domV.getMethod(v);
			List<Local> refVars = new ArrayList<Local>();
			Chain<Local> locals = m.getActiveBody().getLocals();
			Iterator<Local> itr = locals.iterator();
			while(itr.hasNext()){
				Local v1 = itr.next();
				if(v1.getType() instanceof RefLikeType)
					refVars.add(v1);
			}
			int n = refVars.size();
			methToNumVars.put(m, n);
			methToFstVar.put(m, vIdx);
			// System.out.println("Method: " + m);
			for (int i = 0; i < n; i++) {
				varId[vIdx + i] = i;
				// System.out.println("\t" + domV.get(vIdx + i));
			}
			vIdx += n;
		}
		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		cicg = getCallGraph();
		quadToRPOid = new TObjectIntHashMap<Object>();
		invkQuadToLoc = new HashMap<Unit, Loc>();
		for (SootMethod m : cicg.getNodes()) {
			if (m.isAbstract())
				continue;
			CFG cfg = SootUtilities.getCFG(m);
			quadToRPOid.put(cfg.getHeads().get(0), 0);
			int rpoId = 1;
			for (Block bb : cfg.reversePostOrder()) {
				int i = 0;
				Iterator<Unit> itr = bb.getBody().getUnits().iterator();
				while(itr.hasNext()) {
					Unit q = itr.next();
					quadToRPOid.put(q, rpoId++);
					if (SootUtilities.isInvoke(q)) {
						Loc loc = new Loc(q, i);
						invkQuadToLoc.put(q, loc);
					}
					i++;
				}
			}
			quadToRPOid.put(cfg.getTails().get(0), rpoId);
		}
		ThreadEscapeFullAnalysis.setDomF(domF);
	}

	public ICICG getCallGraph() {
		if (cicg == null) {
			CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g()
					.getTrgt("cicg-java");
			ClassicProject.g().runTask(cicgAnalysis);
			cicg = cicgAnalysis.getCallGraph();
		}
		return cicg;
	}

	public SootMethod getMainMethod() {
		return mainMethod;
	}

	public SootMethod getThreadStartMethod() {
		return threadStartMethod;
	}

	public int methToNumVars(SootMethod m) {
		return methToNumVars.get(m);
	}
	
	public int methToFstVar(SootMethod m){
		return methToFstVar.get(m);
	}

	public int getLocalIdx(soot.Value ro) {
		Local r = (Local)ro;
		int vIdx = domV.indexOf(r);
		return varId[vIdx];
	}

	public int getLocalIdx(int domIndex) {
		return varId[domIndex];
	}

	public int getDomVIdx(Local r) {
		return domV.indexOf(r);
	}
	
	public int getDomFIdx(SootField f){
		if(f == null)
			return EscFVariable.ARRAY_ELEMENT;
		else
			return domF.indexOf(f);
	}

	public DomE domE() {
		return domE;
	}

	public DomV domV() {
		return domV;
	}

	public DomH domH(){
		return domH;
	}
	
	public DomF domF(){
		return domF;
	}
	
	@Override
	public void done() {
		// TODO Auto-generated method stub

	}

	@Override
	public JobDispatcher getJobDispatcher() {
		return dispatcher;
	}

	/**
	 * This is where the job is really done for workers
	 */
	@Override
	public String apply(String line) {
		Scenario in = new Scenario(line, EscJobDispatcher.MAJSEP);
		String absStr = in.getIn();
		EscAbstraction abs = (EscAbstraction) absFac.genAbsFromStr(absStr);
		String qStrs[] = Utils.split(in.getOut(), EscJobDispatcher.MINSEP,
				true, true, -1);
		List<EscQueryResult> results = new ArrayList<EscQueryResult>();
		HashSet<Unit> es = new HashSet<Unit>();
		for (String qStr : qStrs) {
			EscQuery q = (EscQuery) qFac.getQueryFromStr(qStr);
			es.add(q.getQuad());
		}
		ThrEscForwardAnalysis forAnalysis = new ThrEscForwardAnalysis(this,
				abs.getLHs(), es);
		forAnalysis.run();
		if (forAnalysis.isTimeOut()) {
			for (Unit q : es) {
				EscQuery query = new EscQuery(domE.indexOf(q),domE);
				EscQueryResult r = new EscQueryResult(query,
						QueryResult.TIMEOUT, null);
				results.add(r);
			}
		} else {
			Set<Unit> Ls = forAnalysis.getLocs();
			for (Unit q : Ls) {
				EscQuery query = new EscQuery(domE.indexOf(q),domE);
				EscQueryResult result = new EscQueryResult(query,
						QueryResult.PROVEN, null);
				results.add(result);
			}
			Set<Unit> Es = forAnalysis.getEscs();
			for (Unit q : Es) {
				EscQuery query = new EscQuery(domE.indexOf(q),domE);
				IWrappedPE<Edge, Edge> wpe = forAnalysis.getEscEdge(q);
				DNF errSuf = getErrSuf(q);
				BackTraceIterator<Edge, Edge> backIter = forAnalysis
						.getBackTraceIterator(wpe);
				backIter.addMethodToSkipList(threadStartMethod);
				MetaBackAnalysis backAnalysis = new MetaBackAnalysis(this,
						errSuf, backIter,  abs.getLIdxes());
				DNF nc;
				try{
				nc = backAnalysis.run();
				}catch(TimeoutException E){
					EscQueryResult result = new EscQueryResult(query,
							QueryResult.TIMEOUT, null);
					results.add(result);
					continue;
				}
				int resultType;
				if(nc.isFalse())
					resultType = QueryResult.IMPOSSIBILITY;
				else
					 resultType = QueryResult.REFINE;
				EscQueryResult result = new EscQueryResult(query,
						resultType, nc);
				results.add(result);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (EscQueryResult r : results) {
			if (sb.length() != 0)
				sb.append(EscJobDispatcher.MINSEP);
			sb.append(r.encode());
		}
		in.setOut(sb.toString());
		in.setType(AbstractJobDispatcher.RESULT);
		return in.encode();
	}

	public DNF getErrSuf(Unit q) {
		if (!(q instanceof AssignStmt)) return null;
		AssignStmt s = (AssignStmt)q;
		soot.Value lhs = s.getLeftOp();
		soot.Value rhs = s.getRightOp();

		soot.Value rx;
		if (rhs instanceof ArrayRef)
			rx = ((ArrayRef)rhs).getBase();
		else if (rhs instanceof InstanceFieldRef)
			rx = ((InstanceFieldRef)rhs).getBase();
		else if (lhs instanceof ArrayRef)
			rx = ((ArrayRef)lhs).getBase();
		else if (lhs instanceof InstanceFieldRef)
			rx = ((InstanceFieldRef)lhs).getBase();
		else
			throw new RuntimeException("Wrong query + " + q);
		EscVVariable escv = null;
		if (rx instanceof Local) {
			int vidx = this.getDomVIdx((Local)rx);
			escv = new EscVVariable(vidx,domV);
		}
		return new DNF(new ClauseSizeCMP(), escv, Value.E());
	}

	public boolean isSkippedMethod(Unit i) {
		Set<SootMethod> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets.isEmpty();
	}

	public boolean isThreadStart(Unit i){
		Set<SootMethod> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets.contains(this.getThreadStartMethod());
	}
	
}
