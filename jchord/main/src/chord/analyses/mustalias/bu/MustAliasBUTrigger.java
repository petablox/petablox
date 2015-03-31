package chord.analyses.mustalias.bu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import joeq.Class.jq_Method;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.field.DomF;
import chord.analyses.mustalias.tdbu.FieldBitSet;
import chord.analyses.mustalias.tdbu.MustAliasBUEdge;
import chord.analyses.mustalias.tdbu.Variable;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.tdbu.BottomUpAnalysis;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;
@Chord(name = "bu-mustalias-java", consumes = { "reachableFromM",
"checkExcludedM" })
public class MustAliasBUTrigger extends JavaAnalysis {
	private MustAliasBottomUpAnalysis bu;
	private boolean init;
	private DomH domH;
	private DomV domV;
	private DomF domF;
	private String cipaName,cicgName;
	private CIPAAnalysis cipa;
	private ICICG cicg;
	private boolean printSumms;
	private boolean debug;
	public final static String SE_FILE = "seCount.txt";

	public void init() {
		if (init)
			return;
		init = true;

		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		domF = (DomF) ClassicProject.g().getTrgt("F");
		ClassicProject.g().runTask(domF);
		Variable.domF = domF;
		Variable.domV = domV;
		FieldBitSet.domF = domF;
		
		printSumms = Boolean.getBoolean("chord.mustalias.bu.printSummary");
		debug = Boolean.getBoolean("chord.mustalias.debug");
		BottomUpAnalysis.DEBUG = debug;
		
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
		bu = new MustAliasBottomUpAnalysis(cicg, reachedFromMM);
	}
	
	public void run(){
		init();
		bu.run();
		Map<jq_Method,Set<MustAliasBUEdge>> summMap = bu.getSummaries();
		SortedMap<Integer,Set<jq_Method>> summNumMap = new TreeMap<Integer,Set<jq_Method>>();
		int totalSumNumber = 0;
		for(Map.Entry<jq_Method, Set<MustAliasBUEdge>> entry: summMap.entrySet()){
			int summNum = entry.getValue().size();
			totalSumNumber += summNum;
			Set<jq_Method> ems = summNumMap.get(summNum);
			if(ems == null){
				ems = new ArraySet<jq_Method>();
				summNumMap.put(summNum, ems);
			}
			ems.add(entry.getKey());
		}
		try {
			PrintWriter pw = new PrintWriter(new File(Config.outDirName+File.separator+SE_FILE));
			for(Map.Entry<Integer, Set<jq_Method>> entry : summNumMap.entrySet()){
				int summNum = entry.getKey();
				System.out.println("================Methods with number of summaries = "+summNum+"==================");
				for(jq_Method m : entry.getValue()){
					System.out.println(m);
					pw.println(summNum);
					if(printSumms){
						System.out.println("**********summaries***************");
						Set<MustAliasBUEdge> summSet = summMap.get(m);
						for(MustAliasBUEdge buse : summSet)
							System.out.println(buse);
						System.out.println("**********************************");
					}
				}
			}
			System.out.println("Total number of summaries: "+totalSumNumber);
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
