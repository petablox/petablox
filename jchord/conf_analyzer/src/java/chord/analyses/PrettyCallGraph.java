package chord.analyses;

import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;

@Chord(
		name = "PrettyCallGraph"
)
public class PrettyCallGraph extends JavaAnalysis {
	
	@Override
	public void run() {
		ClassicProject project = ClassicProject.g();

		project.runTask("cipa-0cfa-arr-dlog");
		project.runTask("findconf-dlog");
		project.runTask("strcomponents-dlog"); 

		
		showCallGraph(project);
	}
	
	public void showCallGraph(ClassicProject project) {
		DomI domI = (DomI) project.getTrgt("I"); 

		/*
		 * NOTE: can't just throw stuff into a map, since keys aren't unique.
		 * Routine to have multiple calls on a line.
		 */
//		TreeMap<String,String> sortedCalls = new TreeMap<String,String>();
		PrintWriter out = OutDirUtils.newPrintWriter("call_dests.txt");

		for(Quad q: domI) {
			String shortFName = q.getMethod().getDeclaringClass().getSourceFileName();
			shortFName = shortFName.substring(shortFName.lastIndexOf('/')+ 1 );
			String pointID = q.getMethod().getDeclaringClass()+"." + q.getMethod().getName()+
			 "(" + shortFName +":" +q.getLineNumber()+")";
			if(pointID.startsWith("conf_analyzer.stubs"))
				continue;
			String destM = Invoke.getMethod(q).getMethod().getName().toString();
			out.println(pointID + " " + destM);
//			sortedCalls.put(pointID, destM);
		}

/*		for(Map.Entry<String, String> e: sortedCalls.entrySet()) {
			out.println(e.getKey()+ " " + e.getValue());
		}*/
		out.close();
		
	}

}
