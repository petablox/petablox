package petablox.analyses.semantics.taint;

import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

@Petablox(
	name = "taint-java"
)
public class TaintAnalysis extends JavaAnalysis {
    public void run() {
        ClassicProject.g().runTask("taint-dlog");
    }
}
