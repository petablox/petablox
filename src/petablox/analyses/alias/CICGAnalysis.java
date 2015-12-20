package petablox.analyses.alias;

import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

/**
 * Context-insensitive call graph analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "cicg-java",
    consumes = { "rootM", "reachableM", "IM", "MM" }
)
public class CICGAnalysis extends JavaAnalysis {
    protected DomM domM;
    protected ProgramRel relRootM;
    protected ProgramRel relReachableM;
    protected ProgramRel relIM;
    protected ProgramRel relMM;
    protected CICG callGraph;
    public void run() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        relRootM = (ProgramRel) ClassicProject.g().getTrgt("rootM");
        relReachableM = (ProgramRel) ClassicProject.g().getTrgt("reachableM");
        relIM = (ProgramRel) ClassicProject.g().getTrgt("IM");
        relMM = (ProgramRel) ClassicProject.g().getTrgt("MM");
    }
    /**
     * Provides the program's context-insensitive call graph.
     * 
     * @return The program's context-insensitive call graph.
     */
    public ICICG getCallGraph() {
        if (callGraph == null) {
            callGraph = new CICG(domM, relRootM, relReachableM, relIM, relMM);
        }
        return callGraph;
    }
    /**
     * Frees relations used by this program analysis if they are in memory.
     * <p>
     * This method must be called after clients are done exercising the interface of this analysis.
     */
    public void free() {
        if (callGraph != null)
            callGraph.free();
    }
}

