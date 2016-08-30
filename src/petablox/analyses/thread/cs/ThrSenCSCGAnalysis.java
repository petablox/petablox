package petablox.analyses.thread.cs;

import petablox.analyses.method.DomM;
import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.CSCGAnalysis;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

/**
 * Call graph analysis producing a thread-sensitive and context-insensitive
 * call graph of the program.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "thrsen-cscg-java",
    consumes = { "thrSenRootCM", "thrSenReachableCM", "thrSenCICM", "thrSenCMCM" }
)
public class ThrSenCSCGAnalysis extends CSCGAnalysis {
    public void run() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        relRootCM = (ProgramRel) ClassicProject.g().getTrgt("thrSenRootCM");
        relReachableCM = (ProgramRel) ClassicProject.g().getTrgt("thrSenReachableCM");
        relCICM = (ProgramRel) ClassicProject.g().getTrgt("thrSenCICM");
        relCMCM = (ProgramRel) ClassicProject.g().getTrgt("thrSenCMCM");
    }
}
