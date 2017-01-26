package petablox.analyses.cg;

import java.io.PrintWriter;
import java.util.Set;

import petablox.analyses.method.DomM;
import petablox.project.OutDirUtils;
import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.ICICG;
import petablox.project.ITask;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.util.soot.SootUtilities;
import soot.SootMethod;
import soot.Unit;

@Petablox(name="cg-java")
public class CallGraph extends JavaAnalysis {
	private ITask cipa;
	private ICICG cicg;
	
	public void run() {
		// cipa = ClassicProject.g().getTask("cipa-0cfa-dlog");
		// ClassicProject.g().runTask(cipa);
		CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g().getTask("cicg-java");
		ClassicProject.g().runTask(cicgAnalysis);
		cicg = cicgAnalysis.getCallGraph();
		
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");

		PrintWriter out = OutDirUtils.newPrintWriter("cicg.txt");
		for (SootMethod m1 : cicg.getNodes()) {
			int id1 = domM.indexOf(m1);
			out.print(id1);
			for (SootMethod m2 : cicg.getSuccs(m1)) {
				int id2 = domM.indexOf(m2);
				out.print(" " + id2);
				/****
				Set<Unit> labels = cicg.getLabels(m1, m2);
				for (Unit q : labels) {
					String el = SootUtilities.toJavaLocStr(q);
				}
				****/
			}
			out.println("");
		}
		out.close();
		
		PrintWriter mname = OutDirUtils.newPrintWriter("name.txt");
		for (int i = 0; i < domM.size(); i++)
			mname.println(domM.get(i));
		mname.close();
	}
}
