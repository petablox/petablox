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
import petablox.util.tuple.object.Pair;
import petablox.util.soot.SootUtilities;

/**
 * Converting a context-sensitive call graph to a dot-graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu) 
 *  -- Original context insensitive code
 * @author Sulekha Kulkarni (sulekha.kulkarni@gmail.com) 
 *  -- Modified to be sensitive
 * @author Christian Kalhauge (kalhauge@cs.ucla.edu) 
 *  -- Modified to work with Petablox and Soot
 */
@Petablox(name="cscg2dot-java")
public class CSCG2DotGraphAnalysis extends JavaAnalysis {
	    private DomM domM;
	    private DomC domC;
	    
	    public void run() {
	        ClassicProject project = ClassicProject.g();
	        CSCGAnalysis analysis = (CSCGAnalysis) project.runTask("cscg-java");
	        ICSCG cscg = analysis.getCallGraph();
	        domM = (DomM) project.getTrgt("M");
	        domC = (DomC) project.getTrgt("C");

	        PrintWriter out = OutDirUtils.newPrintWriter("cscg.dot");
	        out.println("digraph G {");
	        for (Pair<Ctxt,SootMethod> p1 : cscg.getNodes()) {
	        	Ctxt c1 = p1.val0;
	        	SootMethod m1 = p1.val1;
	            String id1 = id(m1, c1);
	            out.println("\t" + id1 + " [label=\"" + str(m1) + "\"];");
	            for (Pair<Ctxt, SootMethod> p2 : cscg.getSuccs(p1)) {
	            	Ctxt c2 = p2.val0;
		        	SootMethod m2 = p2.val1;
	                String id2 = id(m2, c2);
	                Set<Unit> labels = cscg.getLabels(p1, p2);
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
	    private String id(SootMethod m, Ctxt c) {
	        return "m" + domM.indexOf(m) + ":" + domC.indexOf(c);
	    }

        private static String str(SootMethod m) {
            SootClass c = m.getDeclaringClass();
            String desc = m.getSignature().toString();
            return c.getName() + "." + desc;
        }
}
