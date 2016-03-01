package petablox.analyses.provenance.kcfa;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import petablox.analyses.alias.Ctxt;
import petablox.analyses.alias.DomC;
import petablox.analyses.alloc.DomH;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.analyses.var.DomV;
import petablox.bddbddb.Rel.RelView;
import petablox.program.Program;
import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.util.ArraySet;
import petablox.util.graph.IGraph;
import petablox.util.graph.MutableGraph;
import petablox.util.soot.SootUtilities;
import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewMultiArrayExpr;

/**
 * Analysis for pre-computing abstract contexts.
 * <p>
 * The goal of this analysis is to translate client-specified inputs concerning the desired kind of context sensitivity
 * into relations that are subsequently consumed by context-sensitive points-to and call-graph analyses.
 * <p>
 * This analysis allows:
 * <ul>
 *   <li>each method to be analyzed using a different kind of context sensitivity, namely, one of context insensitivity,
 *       k-CFA, k-object-sensitivity, and copy-context-sensitivity;</li>
 *   <li>each local variable to be analyzed context sensitively or insensitively; and</li>
 *   <li>a different 'k' value to be used for each object allocation site and method call site.</li>
 * </ul>
 * Recognized system properties:
 * <ul>
 *   <li>petablox.inst.ctxt.kind: the kind of context sensitivity to use for each instance method (and all its locals).
 *       One of 'ci' (context insensitive), 'cs' (k-CFA), or 'co' (k-object-sensitive).  Default is 'ci'.</li>
 *   <li>petablox.stat.ctxt.kind: the kind of context sensitivity to use for each static method (and all its locals).
 *       One of 'ci' (context insensitive), 'cs' (k-CFA), or 'co' (copy-context-sensitive).  Default is 'ci'.</li>
 *   <li>petablox.ctxt.kind: the kind of context sensitivity to use for each method (and all its locals).
 *       One of 'ci', 'cs', or 'co'.  Serves as shorthand for properties petablox.inst.ctxt.kind and petablox.stat.ctxt.kind.</li>
 *   <li>petablox.kobj.k and petablox.kcfa.k: the 'k' value to use for each object allocation site and each method call site, 
 *       respectively.  Default is 0.</li>
 * </ul>
 * <p>
 * This analysis outputs the following domains and relations:
 * <ul>
 *   <li>C: domain containing all abstract contexts</li>
 *   <li>CC: each (c,c2) such that c2 is all but the last element of context c</li>
 *   <li>CH: each (c,h) such that object allocation site h is the last element of abstract context c</li>
 *   <li>CI: each (c,i) such that call site i is the last element of abstract context c</li>
 *   <li>CVC: each (c,v,o) such that local v might point to object o in context c of its declaring method</li>
 *   <li>CFC: each (o1,f,o2) such that instance field f of object o1 might point to object o2</li>
 *   <li>FC: each (f,o) such that static field f may point to object o</li>
 *   <li>CICM: each (c,i,c2,m) if invocation i in context c can reach method m (in context c2)</li>
 *   <li>rootCM: each (c,m) such that method m is an entry method in context c</li>
 *   <li>reachableCM: each (c,m) such that method m can be called in context c</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "pro-ctxts-java",
       consumes = { "IM", "VH" },
       produces = { "C", "CC", "CH", "CI", "epsilonM", "kcfaSenM", "kobjSenM", "ctxtCpyM" },
       namesOfTypes = { "C" },
       types = { DomC.class }
)
public class CtxtsAnalysis extends JavaAnalysis {
    private static final Set<Ctxt> emptyCtxtSet = Collections.emptySet();
    private static final Set<SootMethod> emptyMethSet = Collections.emptySet();
    private static final Unit[] emptyElems = new Unit[0];

     // includes all methods in domain
    private Set<Ctxt>[] methToCtxts;
    
    private TIntArrayList[] methToClrSites;  // ctxt kind is KCFASEN
    private TIntArrayList[] methToRcvSites;  // ctxt kind is KOBJSEN
    private Set<SootMethod>[] methToClrMeths; // ctxt kind is CTXTCPY
    
    private Set<Ctxt> epsilonCtxtSet;

    public static final int CTXTINS = 0;  // abbr ci; must be 0
    public static final int KOBJSEN = 1;  // abbr co
    public static final int KCFASEN = 2;  // abbr cs
    public static final int CTXTCPY = 3;  // abbr cc

    private int[] ItoM;
    private int[] HtoM;
    private Unit[] ItoQ;
    private Unit[] HtoQ;

    private SootMethod mainMeth;
    private int[] methKind;       // indexed by domM
    private int[] kobjValue;      // indexed by domH
    private int[] kcfaValue;      // indexed by domI

    private int instCtxtKind;
    private int statCtxtKind;

    private DomV domV;
    private DomM domM;
    private DomI domI;
    private DomH domH;
    private DomC domC;

    private ProgramRel relIM;
    private ProgramRel relVH;

    private ProgramRel relCC;
    private ProgramRel relCH;
    private ProgramRel relCI;
    
    private ProgramRel relEpsilonM;
    private ProgramRel relKcfaSenM;
    private ProgramRel relKobjSenM;
    private ProgramRel relCtxtCpyM;

    public void run() {
        domV = (DomV) ClassicProject.g().getTrgt("V");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domC = (DomC) ClassicProject.g().getTrgt("C");

        relIM = (ProgramRel) ClassicProject.g().getTrgt("IM");
        relVH = (ProgramRel) ClassicProject.g().getTrgt("VH");
        
        relCC = (ProgramRel) ClassicProject.g().getTrgt("CC");
        relCH = (ProgramRel) ClassicProject.g().getTrgt("CH");
        relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
        relEpsilonM = (ProgramRel) ClassicProject.g().getTrgt("epsilonM");
        relKcfaSenM = (ProgramRel) ClassicProject.g().getTrgt("kcfaSenM");
        relKobjSenM = (ProgramRel) ClassicProject.g().getTrgt("kobjSenM");
        relCtxtCpyM = (ProgramRel) ClassicProject.g().getTrgt("ctxtCpyM");

        mainMeth = Program.g().getMainMethod();

        String ctxtKindStr = System.getProperty("petablox.ctxt.kind", "ci");
        Config.check(ctxtKindStr, new String[] { "ci", "cs", "co" }, "petablox.ctxt.kind");
        String instCtxtKindStr = System.getProperty("petablox.inst.ctxt.kind", ctxtKindStr);
        Config.check(instCtxtKindStr, new String[] { "ci", "cs", "co" }, "petablox.inst.ctxt.kind");
        String statCtxtKindStr = System.getProperty("petablox.stat.ctxt.kind", ctxtKindStr);
        Config.check(statCtxtKindStr, new String[] { "ci", "cs", "co" }, "petablox.stat.ctxt.kind");
        if (instCtxtKindStr.equals("ci")) {
            instCtxtKind = CTXTINS;
        } else if (instCtxtKindStr.equals("cs")) {
            instCtxtKind = KCFASEN;
        } else
            instCtxtKind = KOBJSEN;
        if (statCtxtKindStr.equals("ci")) {
            statCtxtKind = CTXTINS;
        } else if (statCtxtKindStr.equals("cs")) {
            statCtxtKind = KCFASEN;
        } else
            statCtxtKind = CTXTCPY;

        int kobjK = Integer.getInteger("petablox.kobj.k", 1);
        assert (kobjK > 0);
        int kcfaK = Integer.getInteger("petablox.kcfa.k", 0);
        // assert (kobjK <= kcfaK+1)

        int numV = domV.size();
        int numM = domM.size();
        int numA = domH.getLastI() + 1;
        int numI = domI.size();

		{
			// set k values to use for sites: build arrays kobjValue and kcfaValue

			kobjValue = new int[numA];
			HtoM = new int[numA]; // Which method is h located in?
			HtoQ = new Unit[numA];
			for (int i = 1; i < numA; i++) {
				kobjValue[i] = kobjK;
				Unit site = (Unit) domH.get(i);
				SootMethod m = SootUtilities.getMethod(site);
				HtoM[i] = domM.indexOf(m);
				HtoQ[i] = site;
			}
			kcfaValue = new int[numI];
			ItoM = new int[numI]; // Which method is i located in?
			ItoQ = new Unit[numI];
			for (int i = 0; i < numI; i++) {
				kcfaValue[i] = kcfaK;
				Unit invk = domI.get(i);
				SootMethod m = SootUtilities.getMethod(invk);
				ItoM[i] = domM.indexOf(m);
				ItoQ[i] = invk;
			}

        	String kvals = System.getProperty("petablox.kvals", null);
			if (kvals != null) {
				String[] list = kvals.split(",");
				for (String elem : list) {
                    // elem must have format: H32 2 or I3 5
                    String[] tokens = elem.split(" ");         
                    assert (tokens.length == 2);
                    int idx = Integer.parseInt(tokens[0].substring(1));
                    int val = Integer.parseInt(tokens[1]);
                    switch (tokens[0].charAt(0)) {
                    case 'H': kobjValue[idx] = val; break;
                    case 'I': kcfaValue[idx] = val; break;
                    default: assert false;
                    }
                }
			}
		}

		{
			// populate arrays methKind

        	// set the kind of context sensitivity to use for various methods
			methKind = new int[numM];
			for (int mIdx = 0; mIdx < numM; mIdx++) {
				SootMethod m = domM.get(mIdx);
				int kind;
				if (m == mainMeth || m.getName().contains("<clinit>") || !m.isConcrete())
					kind = CTXTINS;
				else
					kind = m.isStatic() ? statCtxtKind : instCtxtKind;
				methKind[mIdx] = kind;
			}
			// override default kind of context-sensitivity if defined in a file
			String ctxts = System.getProperty("petablox.ctxts", null);
			if (ctxts != null) {
				String[] list = ctxts.split(",");
				for (String elem : list) {
					// elem must have format: M23 [0|1|2|3]
					String[] tokens = elem.split(" ");
                    assert (tokens.length == 2);
                    int idx = Integer.parseInt(tokens[0].substring(1));
                    int val = Integer.parseInt(tokens[1]);
					methKind[idx] = val;
				}
			}

		}

        validate();

        relIM.load();
        relVH.load();

        Ctxt epsilon = domC.setCtxt(emptyElems);
        epsilonCtxtSet = new ArraySet<Ctxt>(1);
        epsilonCtxtSet.add(epsilon);

        methToCtxts = new Set[numM];

        methToClrSites = new TIntArrayList[numM];
        methToRcvSites = new TIntArrayList[numM];
        methToClrMeths = new Set[numM];

        // Do the heavy crunching
        doAnalysis();

        relIM.close();
        relVH.close();

        // Populate domC
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit invk = (Unit) domI.get(iIdx);
            SootMethod meth = SootUtilities.getMethod(invk);
            int mIdx = domM.indexOf(meth);
            Set<Ctxt> ctxts = methToCtxts[mIdx];
            int k = kcfaValue[iIdx];
            for (Ctxt oldCtxt : ctxts) {
                Unit[] oldElems = oldCtxt.getElems();
                Unit[] newElems = combine(k, invk, oldElems);
                domC.setCtxt(newElems);
            }
        }
        for (int hIdx = 1; hIdx < numA; hIdx++) {
            Unit inst = (Unit) domH.get(hIdx);
            SootMethod meth = SootUtilities.getMethod(inst);
            int mIdx = domM.indexOf(meth);
            Set<Ctxt> ctxts = methToCtxts[mIdx];
            int k = kobjValue[hIdx];
            for (Ctxt oldCtxt : ctxts) {
                Unit[] oldElems = oldCtxt.getElems();
                Unit[] newElems = combine(k, inst, oldElems);
                domC.setCtxt(newElems);
            }
        }
        domC.save();

        int numC = domC.size();

        relCC.zero();
        relCI.zero();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit invk = (Unit) domI.get(iIdx);
            SootMethod meth = SootUtilities.getMethod(invk);
            Set<Ctxt> ctxts = methToCtxts[domM.indexOf(meth)];
            int k = kcfaValue[iIdx];
            for (Ctxt oldCtxt : ctxts) {
                Unit[] oldElems = oldCtxt.getElems();
                Unit[] newElems = combine(k, invk, oldElems);
                Ctxt newCtxt = domC.setCtxt(newElems);
                relCC.add(oldCtxt, newCtxt);
                relCI.add(newCtxt, invk);
            }
        }
        relCI.save();

        assert (domC.size() == numC);

        relCH.zero();
        for (int hIdx = 1; hIdx < numA; hIdx++) {
            Unit inst = (Unit) domH.get(hIdx);
            SootMethod meth = SootUtilities.getMethod(inst);
            int mIdx = domM.indexOf(meth);
            Set<Ctxt> ctxts = methToCtxts[mIdx];
            int k = kobjValue[hIdx];
            for (Ctxt oldCtxt : ctxts) {
                Unit[] oldElems = oldCtxt.getElems();
                Unit[] newElems = combine(k, inst, oldElems);
                Ctxt newCtxt = domC.setCtxt(newElems);
                relCC.add(oldCtxt, newCtxt);
                relCH.add(newCtxt, inst);
            }
        }
        relCH.save();

        assert (domC.size() == numC);

        relCC.save();

        relEpsilonM.zero();
        relKcfaSenM.zero();
        relKobjSenM.zero();
        relCtxtCpyM.zero();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            int kind = methKind[mIdx];
            switch (kind) {
            case CTXTINS:
                relEpsilonM.add(mIdx);
                break;
            case KOBJSEN:
                relKobjSenM.add(mIdx);
                break;
            case KCFASEN:
                relKcfaSenM.add(mIdx);
                break;
            case CTXTCPY:
                relCtxtCpyM.add(mIdx);
                break;
            default:
                assert false;
            }
        }
        relEpsilonM.save();
        relKcfaSenM.save();
        relKobjSenM.save();
        relCtxtCpyM.save();

    }

    private void validate() {
        // check that the main jq_Method and each class initializer method and each method without a body
		// is not asked to be analyzed context sensitively.
        int numM = domM.size();
        for (int m = 0; m < numM; m++) {
            int kind = methKind[m];
            if (kind != CTXTINS) {
                SootMethod meth = domM.get(m);
                assert (meth != mainMeth);
                assert (!(meth.getName().contains("<clinit>")));
                if (kind == KOBJSEN) {
                    assert (!meth.isStatic());
                } else if (kind == CTXTCPY) {
                    assert (meth.isStatic());
                }
            }
        }
    }

    private void doAnalysis() {
        Set<SootMethod> roots = new HashSet<SootMethod>();
        Map<SootMethod, Set<SootMethod>> methToPredsMap = new HashMap<SootMethod, Set<SootMethod>>();
        for (int mIdx = 0; mIdx < domM.size(); mIdx++) { // For each method...
            SootMethod meth = domM.get(mIdx);
            int kind = methKind[mIdx];
            switch (kind) {
            case CTXTINS:
            {
                roots.add(meth);
                methToPredsMap.put(meth, emptyMethSet);
                methToCtxts[mIdx] = epsilonCtxtSet;
                break;
            }
            case KCFASEN:
            {
                Set<SootMethod> predMeths = new HashSet<SootMethod>();
                TIntArrayList clrSites = new TIntArrayList();
                for (Unit invk : getCallers(meth)) {
                    predMeths.add(SootUtilities.getMethod(invk)); // Which method can point to this method...?
                    int iIdx = domI.indexOf(invk);
                    clrSites.add(iIdx); // sites that can call me
                }
                methToClrSites[mIdx] = clrSites;
                methToPredsMap.put(meth, predMeths);
                methToCtxts[mIdx] = emptyCtxtSet;
                break;
            }
            case KOBJSEN:
            {
                Set<SootMethod> predMeths = new HashSet<SootMethod>();
                TIntArrayList rcvSites = new TIntArrayList();
                Local thisVar = meth.getActiveBody().getThisLocal();
                Iterable<Unit> pts = getPointsTo(thisVar);
                for (Unit inst : pts) {
                    predMeths.add(SootUtilities.getMethod(inst));
                    int hIdx = domH.indexOf(inst);
                    rcvSites.add(hIdx);
                }
                methToRcvSites[mIdx] = rcvSites;
                methToPredsMap.put(meth, predMeths);
                methToCtxts[mIdx] = emptyCtxtSet;
                break;
            }
            case CTXTCPY:
            {
                Set<SootMethod> predMeths = new HashSet<SootMethod>();
                for (Unit invk : getCallers(meth)) {
                    predMeths.add(SootUtilities.getMethod(invk));
                }
                methToClrMeths[mIdx] = predMeths;
                methToPredsMap.put(meth, predMeths);
                methToCtxts[mIdx] = emptyCtxtSet;
                break;
            }
            default:
                assert false;
            }
        }
        process(roots, methToPredsMap);
    }

    // Compute all the contexts that each method can be called in
    private void process(Set<SootMethod> roots, Map<SootMethod, Set<SootMethod>> methToPredsMap) {
        IGraph<SootMethod> graph = new MutableGraph<SootMethod>(roots, methToPredsMap, null);
        List<Set<SootMethod>> sccList = graph.getTopSortedSCCs();
        int n = sccList.size();
        if (Config.verbose >= 2)
            System.out.println("numSCCs: " + n);
        for (int i = 0; i < n; i++) { // For each SCC...
            Set<SootMethod> scc = sccList.get(i);
            if (Config.verbose >= 2)
                System.out.println("Processing SCC #" + i + " of size: " + scc.size());
            if (scc.size() == 1) { // Singleton
                SootMethod cle = scc.iterator().next();
                if (roots.contains(cle))
                    continue;
                if (!graph.hasEdge(cle, cle)) {
                    int cleIdx = domM.indexOf(cle);
                    methToCtxts[cleIdx] = getNewCtxts(cleIdx);
                    continue;
                }
            }
            for (SootMethod cle : scc) {
                assert (!roots.contains(cle));
            }
            boolean changed = true;
            for (int count = 0; changed; count++) { // Iterate...
                if (Config.verbose >= 2)
                    System.out.println("\tIteration  #" + count);
                changed = false;
                for (SootMethod cle : scc) { // For each node (method) in SCC
                    int mIdx = domM.indexOf(cle);
                    Set<Ctxt> newCtxts = getNewCtxts(mIdx);
                    if (!changed) {
                        Set<Ctxt> oldCtxts = methToCtxts[mIdx];
                        if (newCtxts.size() > oldCtxts.size())
                            changed = true;
                        else {
                            for (Ctxt ctxt : newCtxts) {
                                if (!oldCtxts.contains(ctxt)) {
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                    methToCtxts[mIdx] = newCtxts;
                }
            }
        }
    }

    private Iterable<Unit> getPointsTo(Local var) {
        RelView view = relVH.getView();
        view.selectAndDelete(0, var);
        return view.getAry1ValTuples();
    }

    private Iterable<Unit> getCallers(SootMethod meth) {
        RelView view = relIM.getView();
        view.selectAndDelete(1, meth);
        return view.getAry1ValTuples();
    }

    private Unit[] combine(int k, Unit inst, Unit[] elems) {
        int oldLen = elems.length;
        int newLen = Math.min(k - 1, oldLen) + 1;
        Unit[] newElems = new Unit[newLen];
        if (newLen > 0) newElems[0] = inst;
        if (newLen > 1)
            System.arraycopy(elems, 0, newElems, 1, newLen - 1);
        return newElems;
    }

    private Set<Ctxt> getNewCtxts(int cleIdx) { // Update contexts for this method (callee)
        final Set<Ctxt> newCtxts = new HashSet<Ctxt>();
        int kind = methKind[cleIdx];
        switch (kind) {
        case KCFASEN:
        {
            TIntArrayList invks = methToClrSites[cleIdx]; // which call sites point to me
            int n = invks.size();
            for (int i = 0; i < n; i++) {
                int iIdx = invks.get(i);
                Unit invk = ItoQ[iIdx];
                int k = kcfaValue[iIdx];
                int clrIdx = ItoM[iIdx];
                Set<Ctxt> clrCtxts = methToCtxts[clrIdx]; // method of caller
                for (Ctxt oldCtxt : clrCtxts) {
                    Unit[] oldElems = oldCtxt.getElems();
                    Unit[] newElems = combine(k, invk, oldElems); // Append
                    Ctxt newCtxt = domC.setCtxt(newElems);
                    newCtxts.add(newCtxt);
                }
            }
            break;
        }
        case KOBJSEN:
        {
            TIntArrayList rcvs = methToRcvSites[cleIdx];
            int n = rcvs.size();
            for (int i = 0; i < n; i++) {
                int hIdx = rcvs.get(i);
                Unit rcv = HtoQ[hIdx];
                int k = kobjValue[hIdx];
                int clrIdx = HtoM[hIdx];
                Set<Ctxt> rcvCtxts = methToCtxts[clrIdx];
                for (Ctxt oldCtxt : rcvCtxts) {
                    Unit[] oldElems = oldCtxt.getElems();
                    Unit[] newElems = combine(k, rcv, oldElems);
                    Ctxt newCtxt = domC.setCtxt(newElems);
                    newCtxts.add(newCtxt);
                }
            }
            break;
        }
        case CTXTCPY:
        {
            Set<SootMethod> clrs = methToClrMeths[cleIdx];
            for (SootMethod clr : clrs) {
                int clrIdx = domM.indexOf(clr);
                Set<Ctxt> clrCtxts = methToCtxts[clrIdx];
                newCtxts.addAll(clrCtxts);
            }
            break;
        }
        default:
            assert false;
        }
        return newCtxts;
    }

    public static String getCspaKind() {
        String ctxtKindStr = System.getProperty("petablox.ctxt.kind", "ci");
        String instCtxtKindStr = System.getProperty("petablox.inst.ctxt.kind", ctxtKindStr);
        String statCtxtKindStr = System.getProperty("petablox.stat.ctxt.kind", ctxtKindStr);
        int instCtxtKind, statCtxtKind;
        if (instCtxtKindStr.equals("ci")) {
            instCtxtKind = CtxtsAnalysis.CTXTINS;
        } else if (instCtxtKindStr.equals("cs")) {
            instCtxtKind = CtxtsAnalysis.KCFASEN;
        } else if (instCtxtKindStr.equals("co")) {
            instCtxtKind = CtxtsAnalysis.KOBJSEN;
        } else
            throw new RuntimeException();
        if (statCtxtKindStr.equals("ci")) {
            statCtxtKind = CtxtsAnalysis.CTXTINS;
        } else if (statCtxtKindStr.equals("cs")) {
            statCtxtKind = CtxtsAnalysis.KCFASEN;
        } else if (statCtxtKindStr.equals("co")) {
            statCtxtKind = CtxtsAnalysis.CTXTCPY;
        } else
            throw new RuntimeException();
        String cspaKind;
        if (instCtxtKind == CtxtsAnalysis.CTXTINS && statCtxtKind == CtxtsAnalysis.CTXTINS)
            cspaKind = "cspa-0cfa-dlog";
        else if (instCtxtKind == CtxtsAnalysis.KOBJSEN && statCtxtKind == CtxtsAnalysis.CTXTCPY)
            cspaKind = "cspa-kobj-dlog";
        else if (instCtxtKind == CtxtsAnalysis.KCFASEN && statCtxtKind == CtxtsAnalysis.KCFASEN)
            cspaKind = "cspa-kcfa-dlog";
        else
            cspaKind = "cspa-hybrid-dlog";
        return cspaKind;
    }

    Type h2t(Unit h) {
    	if(h instanceof JAssignStmt){
    		JAssignStmt j = (JAssignStmt)h;
    		if(SootUtilities.isNewStmt(j)){
        		NewExpr ne = (NewExpr)j.rightBox.getValue();
        		return ne.getType();
        	}else if(SootUtilities.isNewArrayStmt(j)){
        		NewArrayExpr nae = (NewArrayExpr)j.rightBox.getValue();
        		return nae.getType();
        	}else if(SootUtilities.isNewMultiArrayStmt(j)){
        		JNewMultiArrayExpr jnmae = (JNewMultiArrayExpr)j.rightBox.getValue();
        		return jnmae.getType();
        	}
    	}
    	return null;
    }
    String hstr(Unit h) {
        String path = new File(SootUtilities.toJavaLocStr(h)).getName();
        Type t = h2t(h);
        return path+"("+(t == null ? "?" : t.toString())+")";
    }
    String istr(Unit i) {
        String path = new File(SootUtilities.toJavaLocStr(i)).getName();
        if(SootUtilities.isInvoke(i)){
        	InvokeExpr ie = SootUtilities.getInvokeExpr(i);
        	SootMethod m = ie.getMethod();
        	return path+"("+m.getName()+")";
        }
        return "";
    }
    String jstr(Unit j) { return isAlloc(j) ? hstr(j) : istr(j); }
    String estr(Unit e) {
        String path = new File(SootUtilities.toJavaLocStr(e)).getName();
        String op = e.getClass().getName();
        return path+"("+op+")";
    }
    String cstr(Ctxt c) {
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        for (int i = 0; i < c.length(); i++) {
            if (i > 0) buf.append(" | ");
            Unit q = c.get(i);
            buf.append(isAlloc(q) ? hstr(q) : istr(q));
        }
        buf.append('}');
        return buf.toString();
    }
    String fstr(SootField f) { return f.getDeclaringClass()+"."+f.getName(); }
    String vstr(Local v) { return v+"@"+mstr(domV.getMethod(v)); }
    String mstr(SootMethod m) { return m.getDeclaringClass().getShortName()+"."+m.getName(); }
    boolean isAlloc(Unit q) { return domH.indexOf(q) != -1; }
}
