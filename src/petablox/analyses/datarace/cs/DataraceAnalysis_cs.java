package petablox.analyses.datarace.cs;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alias.CIObj;
import petablox.analyses.alias.DomO;
import petablox.analyses.alias.ICICG;
import petablox.analyses.alias.ICSCG;
import petablox.analyses.thread.ThrSenCICGAnalysis;
import petablox.analyses.alloc.DomH;
import petablox.analyses.thread.DomA;
import petablox.analyses.thread.cs.DomAS;
import petablox.analyses.thread.cs.ThrSenCSCGAnalysis;
import petablox.bddbddb.Rel.RelView;
import petablox.analyses.field.DomF;
import petablox.analyses.heapacc.DomE;
import petablox.analyses.invk.DomI;
import petablox.analyses.lock.DomL;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.OutDirUtils;
import petablox.project.Config;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramDom;
import petablox.project.analyses.ProgramRel;
import petablox.util.ArraySet;
import petablox.util.SetUtils;
import petablox.util.graph.IPathVisitor;
import petablox.util.graph.ShortestPathBuilder;
import petablox.util.tuple.object.Pair;

/**
 * Static datarace analysis.
 * <p>
 * Outputs relation 'datarace' containing each tuple (a1,e1,a2,e2) denoting a possible race between abstract threads
 * a1 and a2 executing accesses e1 and e2, respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 *   <li>petablox.datarace.exclude.init (default is true): Suppress checking races on accesses in constructors.</li>
 *   <li>petablox.datarace.exclude.eqth (default is true): Suppress checking races between the same abstract thread.</li>
 *   <li>petablox.datarace.exclude.escaping (default is false): Suppress the thread-escape analysis stage.</li>
 *   <li>petablox.datarace.exclude.parallel (default is false): Suppress the may-happen-in-parallel analysis stage.</li>
 *   <li>petablox.datarace.exclude.nongrded (default is false): Suppress the lockset analysis stage.</li>
 *   <li>petablox.print.results (default is false): Print race results in HTML.</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name="datarace-cs-java")
public class DataraceAnalysis_cs extends JavaAnalysis {
    private DomM domM;
    private DomI domI;
    private DomF domF;
    private DomE domE;
    private DomAS domAS;
    private DomH domH;
    private DomL domL;
    private ThrSenCSCGAnalysis thrSenCSCGAnalysis;

    private void init() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domF = (DomF) ClassicProject.g().getTrgt("F");
        domE = (DomE) ClassicProject.g().getTrgt("E");
        domAS = (DomAS) ClassicProject.g().getTrgt("AS");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domL = (DomL) ClassicProject.g().getTrgt("L");
        thrSenCSCGAnalysis = (ThrSenCSCGAnalysis) ClassicProject.g().getTrgt("thrsen-cscg-java");
    }

    public void run() {
        boolean excludeParallel = Boolean.getBoolean("petablox.datarace.exclude.parallel");
        boolean excludeEscaping = Boolean.getBoolean("petablox.datarace.exclude.escaping");
        boolean excludeNongrded = Boolean.getBoolean("petablox.datarace.exclude.nongrded");

        init();
        
        if (excludeNongrded)
            ClassicProject.g().runTask("datarace-nongrded-exclude-cs-dlog");
        else
            ClassicProject.g().runTask("datarace-nongrded-include-cs-dlog");
        if (excludeParallel)
            ClassicProject.g().runTask("datarace-parallel-exclude-cs-dlog");
        else
            ClassicProject.g().runTask("datarace-parallel-include-cs-dlog");
        if (excludeEscaping)
            ClassicProject.g().runTask("datarace-escaping-exclude-cs-dlog");
        else
            ClassicProject.g().runTask("datarace-escaping-include-cs-dlog");
        
      //  ClassicProject.g().runTask("datarace-cs-init-dlog");
      //  ClassicProject.g().runTask("datarace-cs-noneg-dlog");
        ClassicProject.g().runTask("datarace-cs-dlog");
        
        if (Config.printResults)
            printResults();
    }

    private void printResults() {}
}
