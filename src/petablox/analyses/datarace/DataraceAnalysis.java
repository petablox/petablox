package petablox.analyses.datarace;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alias.CIObj;
import petablox.analyses.alias.DomO;
import petablox.analyses.alias.ICICG;
import petablox.analyses.thread.ThrSenCICGAnalysis;
import petablox.analyses.alloc.DomH;
import petablox.analyses.thread.DomA;
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
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

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
@Petablox(name="datarace-java")
public class DataraceAnalysis extends JavaAnalysis {
    private DomM domM;
    private DomI domI;
    private DomF domF;
    private DomE domE;
    private DomA domA;
    private DomH domH;
    private DomL domL;
    private ThrSenCICGAnalysis thrSenCICGAnalysis;

    private void init() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domF = (DomF) ClassicProject.g().getTrgt("F");
        domE = (DomE) ClassicProject.g().getTrgt("E");
        domA = (DomA) ClassicProject.g().getTrgt("A");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domL = (DomL) ClassicProject.g().getTrgt("L");
        thrSenCICGAnalysis = (ThrSenCICGAnalysis) ClassicProject.g().getTrgt("thrsen-cicg-java");
    }

    public void run() {
        boolean excludeParallel = Boolean.getBoolean("petablox.datarace.exclude.parallel");
        boolean excludeEscaping = Boolean.getBoolean("petablox.datarace.exclude.escaping");
        boolean excludeNongrded = Boolean.getBoolean("petablox.datarace.exclude.nongrded");

        init();

        if (excludeParallel)
            ClassicProject.g().runTask("datarace-parallel-exclude-dlog");
        else
            ClassicProject.g().runTask("datarace-parallel-include-dlog");
        if (excludeEscaping)
            ClassicProject.g().runTask("datarace-escaping-exclude-dlog");
        else
            ClassicProject.g().runTask("datarace-escaping-include-dlog");
        if (excludeNongrded)
            ClassicProject.g().runTask("datarace-nongrded-exclude-dlog");
        else
            ClassicProject.g().runTask("datarace-nongrded-include-dlog");
        ClassicProject.g().runTask("datarace-dlog");
        
        if (Config.printResults)
            printResults();
    }

    private void printResults() {
        ClassicProject.g().runTask(thrSenCICGAnalysis);
        final ICICG thrSenCICG = thrSenCICGAnalysis.getCallGraph();
        final ProgramDom<Pair<SootMethod, Unit>> domTE = new ProgramDom<Pair<SootMethod, Unit>>();
        domTE.setName("TE");
        final DomO domO = new DomO();
        domO.setName("O");

        PrintWriter out;

        out = OutDirUtils.newPrintWriter("dataracelist.xml");
        out.println("<dataracelist>");
        final ProgramRel relUltimateRace = (ProgramRel) ClassicProject.g().getTrgt("ultimateRace");
        relUltimateRace.load();
        final ProgramRel relRaceEEH = (ProgramRel) ClassicProject.g().getTrgt("raceEEH");
        relRaceEEH.load();
        final Iterable<petablox.util.tuple.object.Quad<SootMethod, Unit, SootMethod, Unit>> tuples =
            relUltimateRace.getAry4ValTuples();
        for (petablox.util.tuple.object.Quad<SootMethod, Unit, SootMethod, Unit> tuple : tuples) {
            int te1 = domTE.getOrAdd(new Pair<SootMethod, Unit>(tuple.val0, tuple.val1));
            int te2 = domTE.getOrAdd(new Pair<SootMethod, Unit>(tuple.val2, tuple.val3));
            RelView view = relRaceEEH.getView();
            view.selectAndDelete(0, tuple.val1);
            view.selectAndDelete(1, tuple.val3);
            Set<Unit> pts = new ArraySet<Unit>(view.size());
            Iterable<Object> res = view.getAry1ValTuples();
            for (Object o : res)
                pts.add((Unit) o);
            view.free();
            int o = domO.getOrAdd(new CIObj(pts));
            SootField fld = null;
            if(tuple.val1 instanceof Stmt){
                Stmt s = (Stmt) tuple.val1;
                if(s.containsFieldRef())
                    fld = s.getFieldRef().getField();
            }
            int f = domF.indexOf(fld);
            out.println("<datarace Oid=\"O" + o + "\" Fid=\"F" + f + "\" " +
                "TE1id=\"TE" + te1 + "\" "  + "TE2id=\"TE" + te2 + "\"/>");
        }
        relUltimateRace.close();
        relRaceEEH.close();
        out.println("</dataracelist>");
        out.close();

        ClassicProject.g().runTask("LI-dlog");
        ClassicProject.g().runTask("LE-dlog");
        ClassicProject.g().runTask("syncLH-dlog");
        final ProgramRel relLI = (ProgramRel) ClassicProject.g().getTrgt("LI");
        final ProgramRel relLE = (ProgramRel) ClassicProject.g().getTrgt("LE");
        final ProgramRel relSyncLH = (ProgramRel) ClassicProject.g().getTrgt("syncLH");
        relLI.load();
        relLE.load();
        relSyncLH.load();

        final Map<SootMethod, ShortestPathBuilder<SootMethod>> srcNodeToSPB =
            new HashMap<SootMethod, ShortestPathBuilder<SootMethod>>();

        final IPathVisitor<SootMethod> visitor = new IPathVisitor<SootMethod>() {
            public String visit(SootMethod srcM, SootMethod dstM) {
                Set<Unit> insts = thrSenCICG.getLabels(srcM, dstM);
                int mIdx = domM.indexOf(srcM);
                String lockStr = "";
                Unit inst = insts.iterator().next();
                int iIdx = domI.indexOf(inst);
                RelView view = relLI.getView();
                view.selectAndDelete(1, iIdx);
                Iterable<Object> locks = view.getAry1ValTuples();
                for (Object lock : locks) {
                    int lIdx = domL.indexOf(lock);
                    RelView view2 = relSyncLH.getView();
                    view2.selectAndDelete(0, lIdx);
                    Iterable<Object> ctxts = view2.getAry1ValTuples();
                    Set<Unit> pts = SetUtils.newSet(view2.size());
                    for (Object o : ctxts)
                        pts.add((Unit) o);
                    int oIdx = domO.getOrAdd(new CIObj(pts));
                    view2.free();
                    lockStr += "<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
                        mIdx + "\" Oid=\"O" + oIdx + "\"/>";
                }
                view.free();
                return lockStr + "<elem Iid=\"I" + iIdx + "\"/>";
            }
        };

        out = OutDirUtils.newPrintWriter("TElist.xml");
        out.println("<TElist>");
        for (Pair<SootMethod, Unit> te : domTE) {
            SootMethod srcM = te.val0;
            Unit heapInst = te.val1;
            int eIdx = domE.indexOf(heapInst);
            out.println("<TE id=\"TE" + domTE.indexOf(te) + "\" " +
                "Tid=\"A" + domA.indexOf(srcM)    + "\" " +
                "Eid=\"E" + eIdx + "\">");
            SootMethod dstM = SootUtilities.getMethod(heapInst);
            int mIdx = domM.indexOf(dstM);
            RelView view = relLE.getView();
            view.selectAndDelete(1, eIdx);
            Iterable<Object> locks = view.getAry1ValTuples();
            for (Object lock : locks) {
                int lIdx = domL.indexOf(lock);
                RelView view2 = relSyncLH.getView();
                view2.selectAndDelete(0, lIdx);
                Iterable<Object> objs = view2.getAry1ValTuples();
                Set<Unit> pts = SetUtils.newSet(view2.size());
                for (Object o : objs)
                    pts.add((Unit) o);
                int oIdx = domO.getOrAdd(new CIObj(pts));
                view2.free();
                out.println("<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
                    mIdx + "\" Oid=\"O" + oIdx + "\"/>");
            }
            view.free();
            ShortestPathBuilder<SootMethod> spb = srcNodeToSPB.get(srcM);
            if (spb == null) {
                spb = new ShortestPathBuilder<SootMethod>(thrSenCICG, srcM, visitor);
                srcNodeToSPB.put(srcM, spb);
            }
            String path = spb.getShortestPathTo(dstM);
            out.println("<path>");
            out.println(path);
            out.println("</path>");
            out.println("</TE>");
        }
        out.println("</TElist>");
        out.close();

        relLI.close();
        relLE.close();
        relSyncLH.close();

        domO.saveToXMLFile();
        domA.saveToXMLFile();
        domH.saveToXMLFile();
        domI.saveToXMLFile();
        domM.saveToXMLFile();
        domE.saveToXMLFile();
        domF.saveToXMLFile();
        domL.saveToXMLFile();

        OutDirUtils.copyResourceByName("web/style.css");
        OutDirUtils.copyResourceByName("petablox/analyses/method/Mlist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/method/M.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/lock/Llist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alloc/Hlist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alloc/H.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/invk/Ilist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/invk/I.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/heapacc/Elist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/heapacc/E.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/field/Flist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/field/F.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/thread/Alist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/thread/A.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/alias/Olist.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/alias/O.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/datarace/web/results.dtd");
        OutDirUtils.copyResourceByName("petablox/analyses/datarace/web/results.xml");
        OutDirUtils.copyResourceByName("petablox/analyses/datarace/web/group.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/datarace/web/paths.xsl");
        OutDirUtils.copyResourceByName("petablox/analyses/datarace/web/races.xsl");

        OutDirUtils.runSaxon("results.xml", "group.xsl");
        OutDirUtils.runSaxon("results.xml", "paths.xsl");
        OutDirUtils.runSaxon("results.xml", "races.xsl");

        Program.g().HTMLizeJavaSrcFiles();
    }
}
