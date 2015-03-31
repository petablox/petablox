package chord.analyses.logging;

import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.confdep.optnames.DomOpts;
import chord.bddbddb.Rel.RelView;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;


@Chord(
		name = "DumpLogQuadPoints",
		consumes = { "I","logString", "logStmt"}

)
public class ShowLogQuadPtRegex extends JavaAnalysis {

	@Override
	public void run() {
		ClassicProject project = ClassicProject.g();
		PrintWriter out =
			OutDirUtils.newPrintWriter("log_stmt_locations.txt");


		ProgramRel logStrings = (ProgramRel) project.getTrgt("logString"); //i,cst,z
		logStrings.load();
		ProgramRel logQuads = (ProgramRel) project.getTrgt("logStmt"); //just quads
		logQuads.load();

		ProgramRel logVHV = (ProgramRel) project.getTrgt("logVHolds"); //i,cst,z
		logVHV.load();
		ProgramRel logVHU = (ProgramRel) project.getTrgt("logVHoldsU"); //just quads
		logVHU.load();

		DomI domI = (DomI) project.getTrgt("I"); 

		//Do an in-memory sort
		TreeMap<String,String> sortedOpts = new TreeMap<String,String>();
		
		for(Object q: logQuads.getAry1ValTuples()) {
			Quad logCall = (Quad) q;
			int quadID = domI.indexOf(logCall);

			String pointID = logCall.getMethod().getDeclaringClass().getSourceFileName()+": " + logCall.getLineNumber();
			String logRegex = logMsgText(logCall, logStrings, logVHV, logVHU);
			String invokedMethod = Invoke.getMethod(logCall).getMethod().getName().toString().toUpperCase();
			sortedOpts.put(pointID, invokedMethod + " " +  logRegex);
		}
		
		for(Map.Entry<String, String> e: sortedOpts.entrySet()) {
			out.println(e.getValue());
			out.println("printed at: "+ e.getKey());
		}

		logVHV.close();
		logVHU.close();
		logQuads.close();
		logStrings.close();
		out.close();
	}

	public static String logMsgText(Quad logCall, ProgramRel logStrings, ProgramRel logVHV, ProgramRel logVHU) {
		String [] strs = DomOpts.reconcatenate(logCall, logStrings, true, logStmtLen(logCall, logVHV, logVHU));
		StringBuilder sb = new StringBuilder();
		for(String s: strs) {
			sb.append(s);
			sb.append('|');
		}
		sb.deleteCharAt(sb.length() -1);
		return sb.toString();
	}

	private static int logStmtLen(Quad q, ProgramRel logVHoldU, ProgramRel logVHoldV) {

		return Math.max(stmtLen(q, logVHoldU), stmtLen(q, logVHoldV));
	}

	private static int stmtLen(Quad q, ProgramRel logVHold) {
		int maxL = -1;
		RelView componentsAtPt = logVHold.getView();
		componentsAtPt.selectAndDelete(0, q);

		for(Pair<Object, Integer> t: componentsAtPt.<Object,Integer>getAry2ValTuples()) {
			if(t.val1> maxL)
				maxL = t.val1;
		}

		componentsAtPt.free();
		return maxL;
	}

}
