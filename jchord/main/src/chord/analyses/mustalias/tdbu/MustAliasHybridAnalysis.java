package chord.analyses.mustalias.tdbu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.field.DomF;
import chord.analyses.invk.DomI;
import chord.analyses.typestate.Edge;
import chord.analyses.var.DomV;
import chord.program.Loc;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.tdbu.BottomUpAnalysis;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

/**
 * The hybrid analysis which combines topdown analysis and bottomup analysis for
 * mustalias analysis
 * 
 * @author xin
 * 
 */
@Chord(name = "hybrid-mustalias-java", consumes = { "reachableFromM",
		"checkExcludedM" })
public class MustAliasHybridAnalysis extends JavaAnalysis {
	private MustAliasTopDownAnalysis td;
	private MustAliasBottomUpAnalysis bu;
	private PrePassAnalysis pre;
	private DomH domH;
	private DomI domI;
	private DomV domV;
	private DomF domF;
	private int tdLimit;
	private int buLimit;
	private int bupeLimit;
	private int trackedSiteNum;
	private Set<Quad> trackedSites = new HashSet<Quad>();
	private boolean init;
	private boolean autoAdjustBU = false;
	private boolean jumpEmpty = false;
	private boolean DEBUG = false;
	private boolean statistics = false;
	private boolean buAllMethods;
	public static CIPAAnalysis cipa;
	public static ICICG cicg;
	protected String cipaName, cicgName;
	public final static int defTdLimit = 50;
	public final static int defBuLimit = 1;
	public final static int defBupeLimit = Integer.MAX_VALUE;
	public final static int defTrackedSites = Integer.MAX_VALUE;

	public MustAliasHybridAnalysis() {
		init = false;
	}

	public void init() {
		if (init)
			return;
		init = true;
		tdLimit = Integer.getInteger("chord.mustalias.tdlimit", defTdLimit);
		buLimit = Integer.getInteger("chord.mustalias.bulimit", defBuLimit);
		bupeLimit = Integer.getInteger("chord.mustalias.bupelimit",
				defBupeLimit);
		trackedSiteNum = Integer.getInteger("chord.mustalias.trackedsites",
				defTrackedSites);
		statistics = Boolean.getBoolean("chord.mustalias.statistics");
		autoAdjustBU = Boolean.getBoolean("chord.mustalias.autoadjust");
		jumpEmpty = Boolean.getBoolean("chord.mustalias.jumpempty");
		DEBUG = Boolean.getBoolean("chord.mustalias.debug");
		String buAllms = System.getProperty("chord.mustalias.buallms", "false");
		if (buAllms.equals("false"))
			buAllMethods = false;
		else
			buAllMethods = true;

		BottomUpAnalysis.DEBUG = DEBUG;

		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		domF = (DomF) ClassicProject.g().getTrgt("F");
		ClassicProject.g().runTask(domF);
		FieldBitSet.domF = domF;
		Variable.domF = domF;
		Variable.domV = domV;

		fillTrackedSites();

		cipaName = System.getProperty("chord.mustalias.cipa", "cipa-java");
		cipa = (CIPAAnalysis) ClassicProject.g().getTask(cipaName);
		ClassicProject.g().runTask(cipa);

		cicgName = System.getProperty("chord.mustalias.cicg", "cicg-java");
		CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask(
				cicgName);
		ClassicProject.g().runTask(cicgAnalysis);
        cicg = cicgAnalysis.getCallGraph();
		
		ProgramRel relReachedFromMM = (ProgramRel) ClassicProject.g().getTrgt(
				"reachableFromM");
		relReachedFromMM.load();
		Map<jq_Method, Set<jq_Method>> reachedFromMM = new HashMap<jq_Method, Set<jq_Method>>();
		Iterable<Pair<jq_Method, jq_Method>> tuples = relReachedFromMM
				.getAry2ValTuples();
		for (Pair<jq_Method, jq_Method> p : tuples) {
			Set<jq_Method> methods = reachedFromMM.get(p.val0);
			if (methods != null)
				methods.add(p.val1);
			else {
				methods = new HashSet<jq_Method>();
				methods.add(p.val1);
				reachedFromMM.put(p.val0, methods);
			}
		}
		relReachedFromMM.close();
		
		Set<jq_Method> rmsFrRoots = getRmsFrRoots(cicg.getRoots(),reachedFromMM);
		
		pre = new PrePassAnalysis(trackedSites,rmsFrRoots);
		pre.run();
		Set<jq_Method> noTDSEMs = pre.getNoFullSummsMethods();
		td = new MustAliasTopDownAnalysis(tdLimit, autoAdjustBU, jumpEmpty,
				buAllMethods, trackedSites,rmsFrRoots);
		td.init();
		bu = new MustAliasBottomUpAnalysis(td.getCallGraph(), buLimit,
				bupeLimit, reachedFromMM, noTDSEMs);
		td.setBU(bu);
	}

	private Set<jq_Method> getRmsFrRoots(Set<jq_Method> roots,Map<jq_Method,Set<jq_Method>> rmsMap){
		Set<jq_Method> ret = new HashSet<jq_Method>();
		ret.addAll(roots);
		for(jq_Method r: roots){
			Set<jq_Method> rms = rmsMap.get(r);
			if(rms!=null)
				ret.addAll(rms);
		}
		return ret;
	}
	
	private void fillTrackedSites() {
		String fileName = trackedSiteNum + "ta";
		File input = new File(fileName);
		ArraySet<Integer> trackedIdxes = new ArraySet<Integer>();
		ProgramRel relCheckExcludedT = (ProgramRel) ClassicProject.g().getTrgt(
				"checkExcludedT");
		ClassicProject.g().runTask(relCheckExcludedT);
		relCheckExcludedT.load();
		int numH = domH.getLastA() + 1;
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			if (q.getOperator() instanceof New) {
				jq_Class c = q.getMethod().getDeclaringClass();
				String qs = q.toString();
				if(!(qs.contains("java.lang.StringBuilder")||qs.contains("java.lang.StringBuffer")))//well string operations are not interesting
				if (!relCheckExcludedT.contains(c)) {
					trackedIdxes.add(hIdx);
				}
			}
		}
		if (trackedIdxes.size() > trackedSiteNum) {
			ArraySet<Integer> oldtrackedIdxes = trackedIdxes;
			trackedIdxes = new ArraySet<Integer>();
			if (input.exists()) {
				try {
					Scanner sc = new Scanner(input);
					while (sc.hasNext()) {
						String line = sc.nextLine();
						if (!line.equals("")) {
							trackedIdxes.add(Integer.parseInt(line));
						}
					}
				} catch (FileNotFoundException e) {
				}
			} else {
				while (trackedIdxes.size() < trackedSiteNum) {
					int rand = (int) (Math.random() *oldtrackedIdxes.size()) ;
					trackedIdxes.add(oldtrackedIdxes.get(rand));
				}
				PrintWriter pw;
				try {
					pw = new PrintWriter(input);
					for (int i : trackedIdxes)
						pw.println(i);
					pw.flush();
					pw.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		for (int i : trackedIdxes) {
			Quad q = (Quad) domH.get(i);
	//		System.out.println("Tracking: " + q);
			trackedSites.add(q);
		}
		relCheckExcludedT.close();
	}

	@Override
	public void run() {
		init();
		System.out.println("TD Limit: " + tdLimit);
		System.out.println("BU Limit: " + buLimit);
		System.out.println("BUPE Limit: " + bupeLimit);
		System.out.println("Auto adjustment: " + autoAdjustBU);
		System.out.println("Jump empty: " + jumpEmpty);
		System.out.println("Statistics: " + statistics);
		System.out.println("BU run on all methods: " + buAllMethods);
		System.out.println("Max number of tracked alloc sites: " + trackedSiteNum);
		td.run();
		if (statistics) {
			System.out.println("Times of BU has run: " + bu.getBUTimes());
			System.out.println("Times of BUSE matches: " + bu.getBUMatch());
			System.out.println("Times of BUSE unmatches: " + bu.getBUUnmatch());
			System.out.println("Times of BUPE size explodes: "
					+ bu.getCaseExplode());
			System.out.println("Times of TDSE not ready: "
					+ bu.getTDSENotReady());
			System.out.println("Times of no match case: " + bu.getNoCase());
		}
		Map<jq_Method, Set<Edge>> ses = td.getAllSEs();
		int seNum = 0;
		TreeMap<Integer,Set<jq_Method>> seCounts = new TreeMap<Integer,Set<jq_Method>>();
		for (Map.Entry<jq_Method, Set<Edge>> entry : ses.entrySet()) {
			if(DEBUG)
			System.out.println(entry.getKey() + "," + entry.getValue().size()
					+ "," + bu.isMethodAnalyzed(entry.getKey()));
			int entrySeNum = bu.countEffectiveTDSE(entry.getValue());
			Set<jq_Method> mSet = seCounts.get(entrySeNum);
			if(mSet == null){
				mSet = new HashSet<jq_Method>();
				seCounts.put(entrySeNum, mSet);
			}
			mSet.add(entry.getKey());
			seNum += entrySeNum;
		}
		System.out.println("Total TD SE numbers: " + seNum);
		System.out.println("Total BU SE numbers: "+bu.getTotalBUSENumber());
		PrintWriter scOut = OutDirUtils.newPrintWriter("seCounts.txt");
		for(Map.Entry<Integer, Set<jq_Method>> entry:seCounts.entrySet()){
			System.out.println("#####SE number: "+entry.getKey()+", method number: "+entry.getValue().size()+"######");
			for(jq_Method m : entry.getValue()){
				System.out.println(m);
				scOut.println(entry.getKey());
				}
			System.out.println();
		}
		scOut.flush();
		scOut.close();
		if (td.checkNoTDSEsM())
			System.out.println("The prepass really works!");
		else
			System.out.println("Something wrong with the prepass!");
		if (DEBUG)
			bu.printSummaries(System.out);
		PrintWriter out = OutDirUtils.newPrintWriter("results.xml");
		Set<Pair<Quad, Quad>> provedQueries = td.getProvedQueries();
		Set<Pair<Quad, Quad>> errQueries = td.getErrQueries();
		out.println("<results>");
		out.println("<proven num=\"" + provedQueries.size() + "\">");
		for (Pair<Quad, Quad> pq : provedQueries) {
			out.println("<query>");
			out.println("<i Iid=\"" + domI.indexOf(pq.val0) + "\">");
			out.println(pq.val0.toByteLocStr());
			out.println("</i>");
			out.println("<h Hid=\"" + domH.indexOf(pq.val1) + "\">");
			out.println(pq.val1.toByteLocStr());
			out.println("</h>");
			out.println("</query>");
		}
		out.println("</proven>");
		out.println("<err num=\"" + errQueries.size() + "\">");
		for (Pair<Quad, Quad> pq : errQueries) {
			out.println("<query>");
			out.println("<i Iid=\"" + domI.indexOf(pq.val0) + "\">");
			out.println(pq.val0.toByteLocStr());
			out.println("</i>");
			out.println("<h Hid=\"" + domH.indexOf(pq.val1) + "\">");
			out.println(pq.val1.toByteLocStr());
			out.println("</h>");
			out.println("</query>");
		}
		out.println("</err>");
		out.println("</results>");
		out.flush();
		out.close();
	}

}
