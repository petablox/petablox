package chord.analyses.escape.metaback;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.escape.hybrid.full.Edge;
import chord.analyses.escape.hybrid.full.ThreadEscapeFullAnalysis;
import chord.analyses.field.DomF;
import chord.analyses.heapacc.DomE;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.Loc;
import chord.program.Program;
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
@Chord(name = "iter-thresc-java",consumes = { "queryE" })
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
	private TObjectIntHashMap<jq_Method> methToNumVars = new TObjectIntHashMap<jq_Method>();
	private TObjectIntHashMap<jq_Method> methToFstVar = new TObjectIntHashMap<jq_Method>();
	private ICICG cicg;
	private jq_Method mainMethod;
	private jq_Method threadStartMethod;
	private TObjectIntHashMap<Inst> quadToRPOid;
	private Map<Quad, Loc> invkQuadToLoc;
	private Map<jq_Method, Set<Quad>> callersMap = new HashMap<jq_Method, Set<Quad>>();
	private Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();

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
		Iterable<Quad> tuples = relQueryE.getAry1ValTuples();
		List<Quad> allEs = new ArrayList<Quad>();
		if(trackedQueryFiles!=null)
			allEs = getQueriesFromFiles(trackedQueryFiles);
		else
		for (Quad q : tuples) {
			if(trackedQuery<0||domE.indexOf(q) == trackedQuery)
			allEs.add(q);
		}
//		fillInQuery(allEs);
		dispatcher = new EscJobDispatcher(xmlToHtmlTask,this,allEs,domE,domH);
	}

	private List<Quad> getQueriesFromFiles(String s){
		try{
		List<Quad> allEs = new ArrayList<Quad>();
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
			Register v = domV.get(vIdx);
			jq_Method m = domV.getMethod(v);
			int n = m.getLiveRefVars().size();
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
		quadToRPOid = new TObjectIntHashMap<Inst>();
		invkQuadToLoc = new HashMap<Quad, Loc>();
		for (jq_Method m : cicg.getNodes()) {
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			quadToRPOid.put(cfg.entry(), 0);
			int rpoId = 1;
			for (BasicBlock bb : cfg.reversePostOrder()) {
				for (int i = 0; i < bb.size(); i++) {
					Quad q = bb.getQuad(i);
					quadToRPOid.put(q, rpoId++);
					if (q.getOperator() instanceof Invoke) {
						Loc loc = new Loc(q, i);
						invkQuadToLoc.put(q, loc);
					}
				}
			}
			quadToRPOid.put(cfg.exit(), rpoId);
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

	public jq_Method getMainMethod() {
		return mainMethod;
	}

	public jq_Method getThreadStartMethod() {
		return threadStartMethod;
	}

	public int methToNumVars(jq_Method m) {
		return methToNumVars.get(m);
	}
	
	public int methToFstVar(jq_Method m){
		return methToFstVar.get(m);
	}

	public int getLocalIdx(RegisterOperand ro) {
		Register r = ro.getRegister();
		int vIdx = domV.indexOf(r);
		return varId[vIdx];
	}

	public int getLocalIdx(int domIndex) {
		return varId[domIndex];
	}

	public int getDomVIdx(Register r) {
		return domV.indexOf(r);
	}
	
	public int getDomFIdx(jq_Field f){
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
		HashSet<Quad> es = new HashSet<Quad>();
		for (String qStr : qStrs) {
			EscQuery q = (EscQuery) qFac.getQueryFromStr(qStr);
			es.add(q.getQuad());
		}
		ThrEscForwardAnalysis forAnalysis = new ThrEscForwardAnalysis(this,
				abs.getLHs(), es);
		forAnalysis.run();
		if (forAnalysis.isTimeOut()) {
			for (Quad q : es) {
				EscQuery query = new EscQuery(domE.indexOf(q),domE);
				EscQueryResult r = new EscQueryResult(query,
						QueryResult.TIMEOUT, null);
				results.add(r);
			}
		} else {
			Set<Quad> Ls = forAnalysis.getLocs();
			for (Quad q : Ls) {
				EscQuery query = new EscQuery(domE.indexOf(q),domE);
				EscQueryResult result = new EscQueryResult(query,
						QueryResult.PROVEN, null);
				results.add(result);
			}
			Set<Quad> Es = forAnalysis.getEscs();
			for (Quad q : Es) {
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

	private void fillInQuery(List<Quad> queries){
		queries.clear();
		queries.add(domE.get(20514));
		queries.add(domE.get(20531));
		queries.add(domE.get(20533));
		queries.add(domE.get(20532));
		queries.add(domE.get(20529));
		queries.add(domE.get(20156));
		queries.add(domE.get(20157));
		queries.add(domE.get(20534));
		queries.add(domE.get(19992));
		queries.add(domE.get(20530));
		queries.add(domE.get(19993));
	}
	
	public DNF getErrSuf(Quad q) {
		Operator ro = q.getOperator();
		RegisterOperand rx;
		if (ro instanceof AStore)
			rx = (RegisterOperand) AStore.getBase(q);
		else if (ro instanceof ALoad)
			rx = (RegisterOperand) ALoad.getBase(q);
		else if (ro instanceof Getfield)
			rx = (RegisterOperand) Getfield.getBase(q);
		else if (ro instanceof Putfield)
			rx = (RegisterOperand) Putfield.getBase(q);
		else
			throw new RuntimeException("Wrong query + " + q);
		int vidx = this.getDomVIdx(rx.getRegister());
		EscVVariable escv = new EscVVariable(vidx,domV);
		return new DNF(new ClauseSizeCMP(), escv, Value.E());
	}

	public boolean isSkippedMethod(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets.isEmpty();
	}
	
	public boolean isThreadStart(Quad i){
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets.contains(this.getThreadStartMethod());
	}
	
}
