package petablox.analyses.provenance.kcfa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alloc.DomH;
import petablox.analyses.argret.DomK;
import petablox.analyses.invk.DomI;
import petablox.bddbddb.Rel.IntAryNIterable;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.ITask;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.project.analyses.provenance.DefaultModel;
import petablox.project.analyses.provenance.MaxSatGenerator;
import petablox.project.analyses.provenance.Model;
import petablox.project.analyses.provenance.Tuple;
import soot.Unit;

/**
 * A general class to run experiments based on k-cfa analysis.
 * -Dpetablox.provenance.client=<polysite/downcast/datarace>: specify the client to use
 * -Dpetablox.provenance.cfa2=<true/false>: specify whether to run only with the queries solvable using 2-CFA
 * -Dpetablox.provenance.queryOption=<all/separate/single>: specify the way to solve queries
 * -Dpetablox.provenance.heap=<true/false>: specify whether to turn on heap-cloning
 * -Dpetablox.provenance.mono=<true/false>: specify whether to monotonically grow the k values
 * -Dpetablox.provenance.queryWeight=<Integer>: specify the weight we want to use for queries; if -1, treat them as hard constraints.
 * -Dpetablox.provenance.model=<default/kcfa>: specify what model to use to bias the refinement. Default: default(no bias)
 * If 0, use the sum(input weight) + 1
 * -Dpetablox.provenance.boolDomain=<true/false>: specify whether we want to use boolean domain based kcfa
 * -Dpetablox.provenance.invkK=<2>: if we use boolean domain, what is the k value we want for invoke sites
 * -Dpetablox.provenance.allocK=<2>: if we use boolean domain, what is the k value we want for alloc sites 
 * @author xin
 * 
 */
@Petablox(name = "kcfa-refiner")
public class KCFARefiner extends JavaAnalysis {
	DomI domI;
	DomH domH;
	DomK domK;
	Map<Tuple, Map<Unit, Integer>> absMap;
	Set<Tuple> unresolvedQs = new HashSet<Tuple>();
	Set<Tuple> impossiQs = new HashSet<Tuple>();
	MaxSatGenerator gen;
	String[] configFiles;

	ProgramRel IKRel;
	ProgramRel HKRel;
	ProgramRel OKRel;
	ProgramRel allowIRel;
	ProgramRel denyIRel;
	ProgramRel allowHRel;
	ProgramRel denyHRel;
	ProgramRel queryRel;
	List<ITask> tasks;
	PrintWriter debugPW;
	PrintWriter statPW;

	int client; // 0 polysite, 1 downcast, 2 datarace

	String clientFile;
	String clientConfigPath;
	String queryRelName;
	String modelStr;
	
	boolean ifCfa2;
	boolean ifHeap;
	boolean ifMono;
	boolean ifBool;
	int invkK;
	int allocK;

	static int iterLimit = 100;//max number of refinement iterations allowed
	static int max = 20; //max number of k value for both IK and HK
	
	int queryWeight;

	@Override
	public void run() {
		try {
			debugPW = new PrintWriter(new File(Config.outDirName + File.separator + "debug.txt"));
			statPW = new PrintWriter(new File(Config.outDirName+File.separator+"stat.txt"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		String client = System.getProperty("petablox.provenance.client");
		if (client.equals("polysite")) {
			this.client = 0;
			clientFile = "polysite-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/monosite/polysite-dlog_XZ89_.config";
			queryRelName = "polySite";
		} else if (client.equals("downcast")) {
			this.client = 1;
			clientFile = "pro-downcast-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/downcast/pro-downcast-dlog_XZ89_.config";
			queryRelName = "unsafeDowncast";
		} else if (client.equals("datarace")){
			this.client = 2;
			clientFile = "pro-datarace-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/race/pro-datarace-dlog_XZ89_.config";
			queryRelName = "racePairs";
		}else
			throw new RuntimeException("Unknown client: " + this.client);

		//The analyses we need to run
		tasks = new ArrayList<ITask>();
		tasks.add(ClassicProject.g().getTask("cipa-0cfa-dlog"));
		tasks.add(ClassicProject.g().getTask("simple-pro-ctxts-java"));
		tasks.add(ClassicProject.g().getTask("pro-argCopy-dlog"));
		tasks.add(ClassicProject.g().getTask("kcfa-bit-init-dlog_XZ89_"));
		tasks.add(ClassicProject.g().getTask("pro-cspa-kcfa-dlog_XZ89_"));
		tasks.add(ClassicProject.g().getTask(clientFile));

		System.setProperty("petablox.ctxt.kind", "cs");
		System.setProperty("petablox.kobj.khighest", "" + max);
		System.setProperty("petablox.kcfa.khighest", "" + max);
		String chordMain = System.getenv("CHORD_MAIN");
		String kinitConfig = chordMain + File.separator + "src/chord/analyses/provenance/kcfa/kcfa-bit-init-dlog_XZ89_.config";
		String kcfaConfig = chordMain + File.separator + "src/chord/analyses/provenance/kcfa/pro-cspa-kcfa-dlog_XZ89_.config";
		String clientConfig = chordMain + File.separator + clientConfigPath;
		configFiles = new String[]{ kinitConfig, kcfaConfig, clientConfig };

		OKRel = (ProgramRel) ClassicProject.g().getTrgt("OK");
		
		IKRel = (ProgramRel) ClassicProject.g().getTrgt("IK");
		HKRel = (ProgramRel) ClassicProject.g().getTrgt("HK");
		allowIRel = (ProgramRel) ClassicProject.g().getTrgt("AllowI");
		denyIRel = (ProgramRel) ClassicProject.g().getTrgt("DenyI");
		allowHRel = (ProgramRel) ClassicProject.g().getTrgt("AllowH");
		denyHRel = (ProgramRel) ClassicProject.g().getTrgt("DenyH");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domK = (DomK) ClassicProject.g().getTrgt("K");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		queryRel = (ProgramRel) ClassicProject.g().getTrgt(queryRelName);
		ClassicProject.g().runTask(domI);
		ClassicProject.g().runTask(domK);
		ClassicProject.g().runTask(domH);
		absMap = new HashMap<Tuple, Map<Unit, Integer>>();
		
		OKRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			OKRel.add(H,0);
		}
		OKRel.save();	

		String opt = System.getProperty("petablox.provenance.queryOption", "all");
		ifCfa2 = Boolean.getBoolean("petablox.provenance.cfa2");
		ifHeap = Boolean.getBoolean("petablox.provenance.heap");
		ifMono = Boolean.getBoolean("petablox.provenance.mono");
		queryWeight = Integer.getInteger("petablox.provenance.queryWeight", MaxSatGenerator.QUERY_HARD);
		ifBool = Boolean.getBoolean("petablox.provenance.boolDomain");
		invkK = Integer.getInteger("petablox.provenance.invkK",2);
		allocK = Integer.getInteger("petablox.provenance.allocK",2);
		
		modelStr = System.getProperty("petablox.provenance.model", "default");
		
		System.out.println("petablox.provenance.queryOption = "+opt);
		System.out.println("petablox.provenance.cfa2 = "+ifCfa2);
		System.out.println("petablox.provenance.mono = "+ifMono);
		System.out.println("petablox.provenance.queryWeight = "+queryWeight);
		System.out.println("petablox.provenance.boolDomain = "+ifBool);
		System.out.println("petablox.provenance.invkK = "+invkK);
		System.out.println("petablox.provenance.allocK = "+allocK);
		System.out.println("petablox.provenance.model = "+modelStr);
		
		//Initialize the queries
		unresolvedQs = runClientWithK(0,1);
		if(ifCfa2){//if we want to solve 2-CFA queries only
			Set<Tuple> cfa2qs = runClientWithK(2,1);
			unresolvedQs.removeAll(cfa2qs);
		}
		for (Tuple t : unresolvedQs) {// put empty abstraction map for each query
			absMap.put(t, new HashMap<Unit, Integer>());
		}
		if (opt.equals("all")) {
			runAll();
		}
		if (opt.equals("separate")) {
			runSeparate();
		}
		if (opt.equals("single")) {
			String queryString = System.getProperty("petablox.provenance.query");
			Tuple t = new Tuple(queryString);
			runSingle(t);
		}
		if (opt.equals("group")) {
			runGroup();
		}
		debugPW.flush();
		debugPW.close();
		statPW.flush();
		statPW.close();
	}
	
	private MaxSatGenerator createMaxSatGenerator(PTHandler ptHandler, int queryWeight) {
		Model model;
		if(modelStr.equals("kcfa"))
			model = new KCFAModel(ptHandler);
		else
			if(modelStr.endsWith("default"))
				model = new DefaultModel();
			else
				throw new RuntimeException("A model must be specified to bias the refinement!");
		MaxSatGenerator g = new MaxSatGenerator(configFiles, queryRelName, ptHandler, model, queryWeight);
		return g;
	}

	private void runAll() {
		//Set up MaxSatGenerator
		gen = createMaxSatGenerator(new PTHandler(ifHeap, ifMono, ifBool), queryWeight);
		gen.DEBUG = false;
		PTHandler.max = max;
		int numIter = 0;
		int kcfaImp = 0;
		int totalQs = unresolvedQs.size();
		while (unresolvedQs.size() != 0) {
			if(ifMono)
				gen = createMaxSatGenerator(new PTHandler(ifHeap, ifMono, ifBool), queryWeight);
			printlnInfo("===============================================");
			printlnInfo("===============================================");
			printlnInfo("Iteration: " + numIter + " unresolved queries size: " + unresolvedQs.size());
			for (Tuple t : unresolvedQs) {
				printlnInfo(t.toVerboseString());
			}
			int unresolNum = unresolvedQs.size();
			int impossiNum = impossiQs.size();
			int provenNum = totalQs-unresolNum - impossiNum;
			statPW.println(numIter+" "+unresolNum+" "+provenNum+" "+impossiNum+" "+kcfaImp);
			statPW.flush();
			printlnInfo("++++++++++++++++++++++++++++++++++++++++++++++");
			printlnInfo("Abstraction used: ");
			Map<Unit, Integer> abs = absMap.get(unresolvedQs.iterator().next());
			int totalKs = 0;
			for(Map.Entry<Unit, Integer> entry : abs.entrySet())
				totalKs += entry.getValue();
			printlnInfo("Total k values: "+totalKs);
			printlnInfo(abs.toString());
			runAnalysis(abs);
			Set<Tuple> hardQueries = updateAllQs(numIter);
			if(hardQueries.size() != 0){
//				MaxSatGenerator current = gen;
				printlnInfo("********************************");
				printlnInfo("Some queries might be impossible (indeed impossible using complete max sat solver:");
				printlnInfo(hardQueries.toString());
//				if(!ifMono)
//					updateAbsMap(hardQueries,new HashSet<Tuple>());
//				for(Tuple t : hardQueries)
//					runSingle(t);
				if(ifBool){
					gen = null;//save some memory
					System.out.println("Let's check if they're impossible because of the boolean domain limitaion");
					//Attempt to solve in one run
					MaxSatGenerator tempGen = createMaxSatGenerator(new PTHandler(ifHeap,ifMono,false), MaxSatGenerator.QUERY_MAX);
					tempGen.update(hardQueries);
					Set<Tuple> tupleToEli = tempGen.solve(hardQueries, -1+"");
					tupleToEli.retainAll(hardQueries);
					System.out.println("Queries unsovable using KCFA: "+tupleToEli);
					kcfaImp += tupleToEli.size();
				}
				impossiQs.addAll(hardQueries);
				printlnInfo("********************************");
				printlnInfo("Coming back to the normal loop");
//				gen = current; // some recover, not entirely necessary
			}
			numIter++;
		}
		int impossiNum = impossiQs.size();
		int provNum = 0;
		printlnInfo("Impossible Qs: " + impossiQs);
		System.out.println("Proven Qs: ");
		for (Map.Entry<Tuple, Map<Unit, Integer>> entry : absMap.entrySet()) {
			Tuple t = entry.getKey();
			if (!impossiQs.contains(t)) {
				printlnInfo("Query: " + t + ", " + entry.getValue());
				provNum++;
			}
		}
		System.out.println("Proven num: "+provNum);
		System.out.println("Impossi num: "+impossiNum);
		System.out.println("KCFA impossi num: "+kcfaImp);
		statPW.println(numIter+" 0 "+provNum+" "+impossiNum+" "+kcfaImp);
		statPW.flush();
	}

	private Set<Tuple> updateAllQs(int numIter) {
		queryRel.load();
		Set<Tuple> nrqs = new HashSet<Tuple>();
		IntAryNIterable iter = queryRel.getAryNIntTuples();
		for (int[] indices : iter) {
			nrqs.add(new Tuple(queryRel, indices));
		}
		unresolvedQs.retainAll(nrqs);
		gen.update(unresolvedQs);
		Set<Tuple> tupleToEli = gen.solve(unresolvedQs, numIter+"");
		if (tupleToEli == null) {
			impossiQs.addAll(unresolvedQs);
			unresolvedQs.clear();
		}
		Set<Tuple> ret = new HashSet<Tuple>();
		for(Tuple t : tupleToEli){
			if(t.getRelName().equals(queryRelName))
				ret.add(t);
		}
		unresolvedQs.removeAll(ret);// remove the queries from the group, we'll deal with them individually
		updateAbsMap(unresolvedQs, tupleToEli);
		return ret;
	}

	private void updateAbsMap(Set<Tuple> qts, Set<Tuple> tupleToEli) {
		for (Tuple t : qts) {
			Map<Unit, Integer> abs = absMap.get(t);
			if (abs == null) {
				abs = new HashMap<Unit, Integer>();
				absMap.put(t, abs);
			}
			if(!ifMono)
				abs.clear();
			for (Tuple t1 : tupleToEli) {
				if (t1.getRelName().equals("DenyI")) {
					Unit I = (Unit) t1.getValue(0);
					Integer K = (Integer) t1.getValue(1);
					if(ifBool)
						K = invkK;
					Integer ek = abs.get(I);
					if(ek == null)
						abs.put(I, K);
					else
						if(K > ek)
							abs.put(I, K);
				} else if (t1.getRelName().equals("DenyH")) {
					Unit H = (Unit) t1.getValue(0);
					Integer K = (Integer) t1.getValue(1);
					if(ifBool)
						K = allocK;
					Integer ek = abs.get(H);
					if(ek == null)
						abs.put(H, K);
					else
						if(K > ek)
							abs.put(H, K);
				} else
					if(!t1.getRelName().equals(this.queryRelName))
						throw new RuntimeException("Unexpected param tuple: " + t1);
			}
		}
	}

	private void setIK(Unit q, int k){
		IKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowIRel.add(q,i);
		}
		for(int i = k+1; i <= max; i++){
			denyIRel.add(q,i);
		}
	}

	private void setHK(Unit q, int k){
		HKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowHRel.add(q,i);
		}
		for(int i = k+1; i <= max; i++){
			denyHRel.add(q,i);
		}
	}
	
	/**
	 * Run the analysis with the abstraction map specified in the parameter
	 * @param abs
	 */
	private void runAnalysis(Map<Unit, Integer> abs) {
		IKRel.zero();
		allowIRel.zero();
		denyIRel.zero();
		for (int i = 0; i < domI.size(); i++) {
			Unit I = (Unit) domI.get(i);
			Integer k = abs.get(I);
			if (k == null)
				k = 0;
			setIK(I,k);
		}
		IKRel.save();
		allowIRel.save();
		denyIRel.save();
		
		HKRel.zero();
		allowHRel.zero();
		denyHRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			Integer k = abs.get(H);
			if (k == null)
				k = 1;
			setHK(H,k);
		}
		HKRel.save();
		allowHRel.save();
		denyHRel.save();
		for (ITask t : tasks) {
			ClassicProject.g().resetTaskDone(t);
			ClassicProject.g().runTask(t);
		}
	}

	private void runSeparate() {
		for (Tuple q : unresolvedQs) {
			runSingle(q);
		}
	}


	private boolean updateQuery(Tuple t, int numIter) {
		queryRel.load();
		int colNum = t.getIndices().length;
		boolean containment = false;
		if (colNum == 1)
			containment = queryRel.contains(t.getValue(0));
		if (colNum == 2)
			containment = queryRel.contains(t.getValue(0), t.getValue(1));
		if (containment) {
			Set<Tuple> tmp = new HashSet<Tuple>();
			tmp.add(t);
			gen.update(tmp);
			Set<Tuple> tupleToEli = gen.solve(tmp,t.toString()+numIter);
			if (tupleToEli == null) {
				impossiQs.add(t);
				return true;
			}
			updateAbsMap(tmp, tupleToEli);
			return false;
		}
		return true;
	}

	private Set<Tuple> runClientWithK(int ik,int hk) {
		IKRel.zero();
		allowIRel.zero();
		denyIRel.zero();
		for (int i = 0; i < domI.size(); i++) {
			Unit I = (Unit) domI.get(i);
			setIK(I,ik);
		}
		IKRel.save();
		allowIRel.save();
		denyIRel.save();
		HKRel.zero();
		allowHRel.zero();
		denyHRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Unit H = (Unit) domH.get(i);
			setHK(H,hk);
		}
		HKRel.save();
		allowHRel.save();
		denyHRel.save();
		for (ITask t : tasks) {
			ClassicProject.g().resetTaskDone(t);
			ClassicProject.g().runTask(t);
		}
		queryRel.load();
		Set<Tuple> ret = new HashSet<Tuple>();
		for (int[] idx : queryRel.getAryNIntTuples())
			ret.add(new Tuple(queryRel, idx));
		return ret;
	}

	private void runSingle(Tuple q) {
		printlnInfo("Processing query: " + q);
		int numIter = 0;
		gen = createMaxSatGenerator(new PTHandler(ifHeap,ifMono,ifBool), MaxSatGenerator.QUERY_HARD);
		gen.DEBUG = false;
		while (true) {
			if(ifMono)
				gen = createMaxSatGenerator(new PTHandler(ifHeap, ifMono, ifBool), MaxSatGenerator.QUERY_HARD);
			printlnInfo("===============================================");
			printlnInfo("===============================================");
			printlnInfo("Iteration: " + numIter);
			Map<Unit, Integer> abs = absMap.get(q);
			printlnInfo("Abstraction used: ");
			printlnInfo(abs.toString());
			runAnalysis(abs);
			if (updateQuery(q,numIter)) {
				if (impossiQs.contains(q)) {
					printlnInfo("Impossible");
				} else {
					printlnInfo("Proven");
				}
				break;
			}
			numIter++;
			if (numIter > iterLimit) {
				printlnInfo("Too many iteration for " + q);
				break;
			}
		}
	}

	private void runGroup() {

	}

	private void printlnInfo(String s) {
		System.out.println(s);
		debugPW.println(s);
	}

	private void printInfo(String s) {
		System.out.print(s);
		debugPW.print(s);
	}

}
