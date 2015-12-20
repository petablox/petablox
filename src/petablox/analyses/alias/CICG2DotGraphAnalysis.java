package petablox.analyses.alias;

import java.io.PrintWriter;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.OutDirUtils;
import petablox.project.analyses.JavaAnalysis;
import petablox.util.soot.SootUtilities;

/**
 * Converting a context-insensitive call graph to a dot-graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name="cicg2dot-java")
public class CICG2DotGraphAnalysis extends JavaAnalysis {
    private DomM domM;
    public void run() {
        ClassicProject project = ClassicProject.g();
        CICGAnalysis analysis = (CICGAnalysis) project.runTask("cicg-java");
        ICICG cicg = analysis.getCallGraph();
        domM = (DomM) project.getTrgt("M");

        PrintWriter out = OutDirUtils.newPrintWriter("cicg.dot");
        out.println("digraph G {");
        for (SootMethod m1 : cicg.getNodes()) {
            String id1 = id(m1);
            out.println("\t" + id1 + " [label=\"" + str(m1) + "\"];");
            for (SootMethod m2 : cicg.getSuccs(m1)) {
                String id2 = id(m2);
                Set<Unit> labels = cicg.getLabels(m1, m2);
                for (Unit q : labels) {
                    String el = SootUtilities.toJavaLocStr(q);
                    out.println("\t" + id1 + " -> " + id2 + " [label=\"" + el + "\"];");
                }
            }
        }
        out.println("}");
        out.close();

        analysis.free();
    }
    private String id(SootMethod m) {
        return "m" + domM.indexOf(m);
    }
    private static String str(SootMethod m) {
        SootClass c = m.getDeclaringClass();
        String desc = m.getSignature().toString();
        return c.getName() + "." + desc;
    }
}

