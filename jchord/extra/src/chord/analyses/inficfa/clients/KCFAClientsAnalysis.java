package chord.analyses.inficfa.clients;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.ProgramDom;
import chord.bddbddb.Rel.IntPairIterable;
import chord.util.Timer;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.object.Pair;
import chord.project.OutDirUtils;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

@Chord(name="kCFAClients-java",
	consumes = { "M" }
)
public class KCFAClientsAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		ProgramDom domM = (ProgramDom) ClassicProject.g().getTrgt("M");
		int numM = domM.size();
		
		ClassicProject.g().runTask("cipa-0cfa-noreflect-dlog");
		
		String ctxtKind = System.getProperty("chord.kcfaclients.ctxtkind", "ci");
		int kVal = Integer.getInteger("chord.kcfaclients.kval", 0);
		System.setProperty("chord.ctxt.kind", ctxtKind);
		System.setProperty("chord.kcfa.k", String.valueOf(kVal));
		
		if(ctxtKind.equalsIgnoreCase("ci") && kVal == 0)
			ClassicProject.g().runTask("cspa-0cfa-dlog");
		else if(ctxtKind.equalsIgnoreCase("cs"))
			ClassicProject.g().runTask("cspa-kcfa-noreflect-dlog");
		else
			throw new RuntimeException("Incorrect values for the properties");
		
		
		//Generate various queries
		Timer queryGenTimer = new Timer("kCFA-QueryGen");
		queryGenTimer.init();
		
		Set<jq_Method> reachableM_0cfa = new HashSet<jq_Method>();
		Set<jq_Method> rootM = new HashSet<jq_Method>();
		
		ProgramRel rel1 = (ProgramRel) ClassicProject.g().getTrgt("rootM");
		rel1.load();
		Iterable<jq_Method> tuples1 = rel1.getAry1ValTuples();
		for (jq_Method m : tuples1) {
			reachableM_0cfa.add(m);
			rootM.add(m);
		}
		rel1.close();
		
		long allReachQueriesCnt = 0;
		long allReachQueriesFromRootCnt = 0;
		
		ProgramRel rel9 = (ProgramRel) ClassicProject.g().getTrgt("reachableM");
		rel9.load();
		Iterable<jq_Method> tuples9 = rel9.getAry1ValTuples();
		for (jq_Method m1 : tuples9) {
			for (jq_Method m2 : tuples9) {
				if(!rootM.contains(m2)){
					if(!rootM.contains(m1))
						allReachQueriesCnt++;
					else
						allReachQueriesFromRootCnt++;
				}
			}
		}
		rel9.close();
		System.out.println("Total ReachMM Queries: " + ((allReachQueriesCnt/2) + allReachQueriesFromRootCnt));
		
		
		ClassicProject.g().runTask("reach-dlog");
		
		int[] numReachM = new int[numM];
		ProgramRel rel2 = (ProgramRel) ClassicProject.g().getTrgt("reachMM");
		rel2.load();
		IntPairIterable tuples2 = rel2.getAry2IntTuples();
		PrintWriter out1 = OutDirUtils.newPrintWriter("reachMM_0cfa.txt");
		for (IntPair tuple : tuples2) {
			int m = tuple.idx0;
			numReachM[m]++;
			jq_Method m0 = (jq_Method) domM.get(tuple.idx0);
			jq_Method m1 = (jq_Method) domM.get(tuple.idx1);
		//	reachableM_0cfa.add(m0);
			reachableM_0cfa.add(m1);
			out1.println(m0 + " :: " + m1);
		}
		rel2.close();
		out1.close();
		
		PrintWriter out2 = OutDirUtils.newPrintWriter("results_0cfa.txt");
		for (int m = 0; m < numM; m++) {
			out2.println(numReachM[m] + " " + domM.get(m));
		}
		out2.close();
		
		ProgramRel rel3 = (ProgramRel) ClassicProject.g().getTrgt("edgeMM");
		rel3.load();
		Iterable<Pair<jq_Method, jq_Method>> tuples3 = rel3.getAry2ValTuples();
		PrintWriter out3 = OutDirUtils.newPrintWriter("edgeMM_0cfa.txt");
		for (Pair<jq_Method, jq_Method> tuple : tuples3) {
			out3.println(tuple.val0 + " :: " + tuple.val1);
		}
		rel3.close();
		out3.close();
		
		PrintWriter out4 = OutDirUtils.newPrintWriter("reachableM_0cfa.txt");
		for (jq_Method m : reachableM_0cfa) {
			out4.println(m);
		}
		out4.close();
		
		int[] cloneCntM = new int[numM];
		ProgramRel rel4 = (ProgramRel) ClassicProject.g().getTrgt("CM");
		rel4.load();
		IntPairIterable tuples4 = rel4.getAry2IntTuples();
		for (IntPair tuple : tuples4) {
			int m = tuple.idx1;
			cloneCntM[m]++;
		}
		rel4.close();
		
		PrintWriter out5 = OutDirUtils.newPrintWriter("results_0cfa_Clones.txt");
		for (int m = 0; m < numM; m++) {
			out5.println(cloneCntM[m] + " " + domM.get(m));
		}
		out5.close();
		
		ClassicProject.g().runTask("monosite-dlog");
		
		ProgramRel rel5 = (ProgramRel) ClassicProject.g().getTrgt("allQueries");
		rel5.load();
		Iterable<Quad> tuples5 = rel5.getAry1ValTuples();
		PrintWriter out6 = OutDirUtils.newPrintWriter("allMonositeQueries_0cfa.txt");
		for (Quad tuple : tuples5) {
			out6.println(tuple);
		}
		rel5.close();
		out6.close();
		
		ProgramRel rel6 = (ProgramRel) ClassicProject.g().getTrgt("polySite");
		rel6.load();
		Iterable<Quad> tuples6 = rel6.getAry1ValTuples();
		PrintWriter out7 = OutDirUtils.newPrintWriter("failedMonositeQueries_0cfa.txt");
		for (Quad tuple : tuples6) {
			out7.println(tuple);
		}
		rel6.close();
		out7.close();
		
		
		ClassicProject.g().runTask("downcast-dlog");
		
		ProgramRel rel7 = (ProgramRel) ClassicProject.g().getTrgt("downcast");
		rel7.load();
		Iterable<Pair<Register, jq_Type>> tuples7 = rel7.getAry2ValTuples();
		PrintWriter out8 = OutDirUtils.newPrintWriter("allDowncastQueries_0cfa.txt");
		for (Pair<Register, jq_Type> tuple : tuples7) {
			out8.println(tuple.val0 + " " + tuple.val1);
		}
		rel7.close();
		out8.close();
		
		ProgramRel rel8 = (ProgramRel) ClassicProject.g().getTrgt("unsafeDowncast");
		rel8.load();
		Iterable<Pair<Register, jq_Type>> tuples8 = rel8.getAry2ValTuples();
		PrintWriter out9 = OutDirUtils.newPrintWriter("unsafeDowncastQueries_0cfa.txt");
		for (Pair<Register, jq_Type> tuple : tuples8) {
			out9.println(tuple.val0 + " " + tuple.val1);
		}
		rel8.close();
		out9.close();
		
		queryGenTimer.done();
		long inclusiveTime = queryGenTimer.getInclusiveTime();
		System.out.println("Total time for generating queries: "
				+ Timer.getTimeStr(inclusiveTime));
	}
}

