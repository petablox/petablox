package petablox.analyses.thread;

import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.method.DomM;
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
    name = "thrsen-cicg-java",
    consumes = { "thrSenRootM", "thrSenReachableM", "thrSenIM", "thrSenMM" }
)
public class ThrSenCICGAnalysis extends CICGAnalysis {
    public void run() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        relRootM = (ProgramRel) ClassicProject.g().getTrgt("thrSenRootM");
        relReachableM = (ProgramRel) ClassicProject.g().getTrgt("thrSenReachableM");
        relIM = (ProgramRel) ClassicProject.g().getTrgt("thrSenIM");
        relMM = (ProgramRel) ClassicProject.g().getTrgt("thrSenMM");
    }
}
