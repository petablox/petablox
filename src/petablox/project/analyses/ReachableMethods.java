package petablox.project.analyses;

import java.io.PrintWriter;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

import petablox.analyses.alias.CICGAnalysis;
import petablox.analyses.alias.ICICG;

import petablox.project.OutDirUtils;

/*
 * petablox.printrel.dir      directory where all the .txt files containing the rels will be dumped.
 */

/**
 * Create a list of reachable methods
 *
 * @author Christian Kalhauge (kalhauge@cs.ucla.edu)
 *  --  initial commit based of cicg2dot.
 */
@Petablox(name="reachable-methods")
public class ReachableMethods extends JavaAnalysis {

  public void run() {
    ClassicProject project = ClassicProject.g();
    CICGAnalysis analysis = (CICGAnalysis) project.runTask("cicg-java");
    ICICG cicg = analysis.getCallGraph();

    PrintWriter out = OutDirUtils.newPrintWriter("reachable-methods.txt");
    for (SootMethod m : cicg.getNodes()) {
      out.println(m.getSignature());
    }
    out.close();
    analysis.free();
  }
}
