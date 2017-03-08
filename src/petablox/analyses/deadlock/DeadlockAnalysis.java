package petablox.analyses.deadlock;

import java.io.PrintWriter;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import soot.SootMethod;
import soot.Unit;

import petablox.project.Config;
import petablox.program.Program;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.OutDirUtils;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;

import petablox.util.ArraySet;
import petablox.util.graph.IPathVisitor;
import petablox.util.graph.ShortestPathBuilder;
import petablox.analyses.alias.CIObj;
import petablox.analyses.alias.ICICG;
import petablox.analyses.thread.ThrSenCICGAnalysis;
import petablox.analyses.alias.DomO;
import petablox.analyses.alloc.DomH;
import petablox.bddbddb.Rel.RelView;
import petablox.analyses.thread.DomA;
import petablox.analyses.invk.DomI;
import petablox.analyses.lock.DomL;
import petablox.analyses.method.DomM;
import petablox.util.SetUtils;
import petablox.util.soot.SootUtilities;

/**
 * Static deadlock analysis.
 * <p>
 * Outputs relation 'deadlock' containing each tuple (a1,l1,l2,a2,l3,l4) denoting a possible
 * deadlock between abstract thread a1, which acquires a lock at l1 followed by a lock at l2,
 * and abstract thread a2, which acquires a lock at l3 followed by a lock at l4.
 * <p>
 * Recognized system properties:
 * <ul>
 *   <li>petablox.deadlock.exclude.escaping (default is false).</li>
 *   <li>petablox.deadlock.exclude.parallel (default is false).</li>
 *   <li>petablox.deadlock.exclude.nonreent (default is false).</li>
 *   <li>petablox.deadlock.exclude.nongrded (default is false).</li>
 *   <li>petablox.print.results (default is false).</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name="deadlock-java", consumes = { "syncLH" })
public class DeadlockAnalysis extends JavaAnalysis {
    private DomA domA;
    private DomH domH;
    private DomI domI;
    private DomL domL;
    private DomM domM;
    private ProgramRel relDeadlock;
    private ProgramRel relSyncLH;
    private ICICG thrSenCICG;
    private final Map<SootMethod, Set<SootMethod>> MMmap = new HashMap<SootMethod, Set<SootMethod>>();

    public void run() {
        boolean excludeParallel = Boolean.getBoolean("petablox.deadlock.exclude.parallel");
        boolean excludeEscaping = Boolean.getBoolean("petablox.deadlock.exclude.escaping");
        boolean excludeNonreent = Boolean.getBoolean("petablox.deadlock.exclude.nonreent");
        boolean excludeNongrded = Boolean.getBoolean("petablox.deadlock.exclude.nongrded");

        domA = (DomA) ClassicProject.g().getTrgt("A");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domL = (DomL) ClassicProject.g().getTrgt("L");
        domM = (DomM) ClassicProject.g().getTrgt("M");
        
        relDeadlock = (ProgramRel) ClassicProject.g().getTrgt("deadlock");
        relSyncLH   = (ProgramRel) ClassicProject.g().getTrgt("syncLH");

        ThrSenCICGAnalysis thrSenCICGAnalysis =
            (ThrSenCICGAnalysis) ClassicProject.g().getTrgt("thrsen-cicg-java");
        ClassicProject.g().runTask(thrSenCICGAnalysis);
        thrSenCICG = thrSenCICGAnalysis.getCallGraph();

        if (excludeParallel)
            ClassicProject.g().runTask("deadlock-parallel-exclude-dlog");
        else
            ClassicProject.g().runTask("deadlock-parallel-include-dlog");
        if (excludeEscaping)
            ClassicProject.g().runTask("deadlock-escaping-exclude-dlog");
        else
            ClassicProject.g().runTask("deadlock-escaping-include-dlog");
        if (excludeNonreent)
            ClassicProject.g().runTask("deadlock-nonreent-exclude-dlog");
        else
            ClassicProject.g().runTask("deadlock-nonreent-include-dlog");
        if (excludeNongrded)
            ClassicProject.g().runTask("deadlock-nongrded-exclude-dlog");
        else
            ClassicProject.g().runTask("deadlock-nongrded-include-dlog");
        ClassicProject.g().runTask("deadlock-dlog");

        if (Config.printResults){
            printResults();
        }
    }

    private CIObj getPointsTo(int lIdx) {
        RelView view = relSyncLH.getView();
        view.selectAndDelete(0, lIdx);
        Iterable<Object> objs = view.getAry1ValTuples();
        Set<Unit> pts = SetUtils.newSet(view.size());
        for (Object o : objs)
            pts.add((Unit) o);
        view.free();
        return new CIObj(pts);
    }
    
    private void printResults() {
        final DomO domO = new DomO();
        domO.setName("O");
        
        PrintWriter out;

        relDeadlock.load();
        relSyncLH.load();

        out = OutDirUtils.newPrintWriter("deadlocklist.xml");
        out.println("<deadlocklist>");
        for (Object[] tuple : relDeadlock.getAryNValTuples()) {
            SootMethod t1Val = (SootMethod) tuple[0];
            Unit l1Val = (Unit) tuple[1];
            Unit l2Val = (Unit) tuple[2];
            SootMethod t2Val = (SootMethod) tuple[3];
            Unit l3Val = (Unit) tuple[4];
            Unit l4Val = (Unit) tuple[5];
            int l1 = domL.indexOf(l1Val);
            int l2 = domL.indexOf(l2Val);
            int l3 = domL.indexOf(l3Val);
            int l4 = domL.indexOf(l4Val);
            // require l1,l2 <= l3,l4 and if not switch
            if (l1 > l3 || (l1 == l3 && l2 > l4)) {
                {
                    int tmp;
                    tmp = l1; l1 = l3; l3 = tmp;
                    tmp = l2; l2 = l4; l4 = tmp;
                }
                {
                    Unit tmp;
                    tmp = l1Val; l1Val = l3Val; l3Val = tmp;
                    tmp = l2Val; l2Val = l4Val; l4Val = tmp;
                }
                {
                    SootMethod tmp;
                    tmp = t1Val; t1Val = t2Val; t2Val = tmp;
                }
            }
            int t1 = domA.indexOf(t1Val);
            int t2 = domA.indexOf(t2Val);
            int t1m = domM.indexOf(t1Val);
            int t2m = domM.indexOf(t2Val);
            SootMethod m1Val = SootUtilities.getMethod(l1Val);
            SootMethod m2Val = SootUtilities.getMethod(l2Val);
            SootMethod m3Val = SootUtilities.getMethod(l3Val);
            SootMethod m4Val = SootUtilities.getMethod(l4Val);
            int m1 = domM.indexOf(m1Val);
            int m2 = domM.indexOf(m2Val);
            int m3 = domM.indexOf(m3Val);
            int m4 = domM.indexOf(m4Val);
            CIObj o1Val = getPointsTo(l1);
            CIObj o2Val = getPointsTo(l2);
            CIObj o3Val = getPointsTo(l3);
            CIObj o4Val = getPointsTo(l4);
            int o1 = domO.getOrAdd(o1Val);
            int o2 = domO.getOrAdd(o2Val);
            int o3 = domO.getOrAdd(o3Val);
            int o4 = domO.getOrAdd(o4Val);
            addToMMmap(t1Val, m1Val);
            addToMMmap(t2Val, m3Val);
            addToMMmap(m1Val, m2Val);
            addToMMmap(m3Val, m4Val);
            out.println("<deadlock " +
                "group=\"" + l1 + "_" + l2 + "_" + l3 + "_" + l4 + "\" " +
                "T1id=\"A" + t1 + "\" T2id=\"A" + t2 + "\" " +
                "M1id=\"M" + m1 + "\" L1id=\"L" + l1 + "\" O1id=\"O" + o1 + "\" " +
                "M2id=\"M" + m2 + "\" L2id=\"L" + l2 + "\" O2id=\"O" + o2 + "\" " +
                "M3id=\"M" + m3 + "\" L3id=\"L" + l3 + "\" O3id=\"O" + o3 + "\" " +
                "M4id=\"M" + m4 + "\" L4id=\"L" + l4 + "\" O4id=\"O" + o4 + "\"/>");
        }
        relDeadlock.close();
        relSyncLH.close();
        out.println("</deadlocklist>");
        out.close();        
        
        IPathVisitor<SootMethod> visitor = new IPathVisitor<SootMethod>() {
            public String visit(SootMethod srcM, SootMethod dstM) {
                Set<Unit> insts = thrSenCICG.getLabels(srcM, dstM);
                for (Unit inst : insts) {
                    return "<elem Iid=\"I" + domI.indexOf(inst) + "\"/>";
                }
                return "";
            }
        };

        out = OutDirUtils.newPrintWriter("MMlist.xml");
        out.println("<MMlist>");
        
        for (SootMethod m1 : MMmap.keySet()) {
            int mIdx1 = domM.indexOf(m1);
            Set<SootMethod> mSet = MMmap.get(m1);
            ShortestPathBuilder<SootMethod> builder = new ShortestPathBuilder(thrSenCICG, m1, visitor);
            for (SootMethod m2 : mSet) {
                int mIdx2 = domM.indexOf(m2);
                out.println("<MM M1id=\"M" + mIdx1 + "\" M2id=\"M" + mIdx2 + "\">");
                String path = builder.getShortestPathTo(m2);
                out.println("<path>");
                out.println(path);
                out.println("</path>");
                out.println("</MM>");
            }
        }
        out.println("</MMlist>");
        out.close();
        
        domO.saveToXMLFile();
        domA.saveToXMLFile();
        domH.saveToXMLFile();
        domI.saveToXMLFile();
        domM.saveToXMLFile();
        domL.saveToXMLFile();

        OutDirUtils.copyResourceByName("web/style.css");
        OutDirUtils.copyResourceByName("petablox/analyses/method/Mlist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/method/M.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/lock/Llist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alloc/Hlist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alloc/H.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/invk/Ilist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/invk/I.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/thread/Alist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/thread/A.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/alias/Olist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alias/O.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/deadlock/web/results.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/deadlock/web/results.xml");
        OutDirUtils.copyResourceByName("petablox/analyses/deadlock/web/group.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/deadlock/web/paths.xsl");

        OutDirUtils.runSaxon("results.xml", "group.xsl");
        OutDirUtils.runSaxon("results.xml", "paths.xsl");

        Program.g().HTMLizeJavaSrcFiles();
    }

    private void addToMMmap(SootMethod m1, SootMethod m2) {
        Set<SootMethod> s = MMmap.get(m1);
        if (s == null) {
            s = new ArraySet<SootMethod>();
            MMmap.put(m1, s);
        }
        s.add(m2);
    }
}
