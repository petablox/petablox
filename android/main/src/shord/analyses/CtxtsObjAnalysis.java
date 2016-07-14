package shord.analyses;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;

import gnu.trove.list.array.TIntArrayList;

import soot.Scene;
import soot.Unit;
import soot.Type;
import soot.RefType;
import soot.jimple.Stmt;
import soot.SootMethod;
import soot.util.NumberedSet;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;

import shord.program.Program;
import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.Config;
import shord.project.Messages;

import static shord.analyses.PAGBuilder.isQuasiStaticInvk;
import static shord.analyses.PAGBuilder.isQuasiStaticMeth;

import chord.util.Utils;
import chord.project.Chord;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;
import chord.bddbddb.Rel.RelView;

/**
 * Analysis for pre-computing abstract contexts.
 * @author Yu Feng (yufeng@cs.stanford.edu)
 * @author Saswat Anand
 */
@Chord(name = "ctxts-obj-java",
       consumes = { "MI", "MH", "ci_pt", "StatIM", "Stub", "ci_reachableM", "ci_IM"},
       produces = { "C", "CC", "CH", "CI", "CtxtInsMeth", "CM"},
       namesOfTypes = { "C" },
       types = { DomC.class }
)
public class CtxtsObjAnalysis extends JavaAnalysis 
{
    private static final Set<Ctxt> emptyCtxtSet = Collections.emptySet();
    private static final Set<SootMethod> emptyMethSet = Collections.emptySet();
    private static final Object[] emptyElems = new Object[0];

    // includes all methods in domain
    private Set<Ctxt>[] methToCtxts;
    
    private TIntArrayList[] methToRcvSites;  // ctxt kind is KOBJSEN
    private TIntArrayList[] methToClrSites;  // ctxt kind is KCfa, for static method
    
    private Map<SootMethod, ThisVarNode> methToThis = new HashMap<SootMethod, ThisVarNode>();

    private Set<SootMethod>[] methToClrMeths; // ctxt kind is CTXTCPY


    private Set<Ctxt> epsilonCtxtSet;

	public static int K = 4;

    private int[] ItoM;
    private int[] HtoM;
    private Unit[] ItoQ;
    private AllocNode[] HtoQ;

    private SootMethod mainMeth;

    private DomV domV;
    private DomM domM;
    private DomI domI;
    private DomH domH;
    private DomC domC;

    private ProgramRel relCC;
    private ProgramRel relCH;
    private ProgramRel relCI;
    private ProgramRel relIpt;
    private ProgramRel relIM;
    private ProgramRel relCtxtInsMeth;

	private NumberedSet stubs;

    public void run() 
	{
        domV = (DomV) ClassicProject.g().getTrgt("V");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domC = (DomC) ClassicProject.g().getTrgt("C");

        relCC = (ProgramRel) ClassicProject.g().getTrgt("CC");
        relCH = (ProgramRel) ClassicProject.g().getTrgt("CH");
        relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
        relIpt = (ProgramRel) ClassicProject.g().getTrgt("ci_pt");
        relIM = (ProgramRel) ClassicProject.g().getTrgt("ci_IM");
		relCtxtInsMeth = (ProgramRel) ClassicProject.g().getTrgt("CtxtInsMeth");

        mainMeth = Program.g().getMainMethod();

        int numV = domV.size();
        int numM = domM.size();
        int numH = domH.size();
        int numI = domI.size();

        for(VarNode vnode:domV){
            if(vnode instanceof ThisVarNode) {
                ThisVarNode thisVar = (ThisVarNode) vnode;
                methToThis.put(thisVar.method, thisVar); 
            }
        }

        ItoM = new int[numI];
        ItoQ = new Unit[numI];
        final ProgramRel relMI = (ProgramRel) ClassicProject.g().getTrgt("MI");		
        relMI.load();
        Iterable<Pair<SootMethod,Unit>> res = relMI.getAry2ValTuples();
        for(Pair<SootMethod,Unit> pair : res) {
            SootMethod meth = pair.val0;
            Unit invk = pair.val1;
            int mIdx = domM.indexOf(meth);
            int iIdx = domI.indexOf(invk);
            ItoM[iIdx] = mIdx;
            ItoQ[iIdx] = invk;
        }
        relMI.close();


        HtoQ = new AllocNode[numH];
		for (int hIdx = 0; hIdx < numH; hIdx++) {
			HtoQ[hIdx] = domH.get(hIdx);
		}

        HtoM = new int[numH];
        final ProgramRel relMH = (ProgramRel) ClassicProject.g().getTrgt("MH");		
        relMH.load();
        Iterable<Pair<SootMethod,AllocNode>> res1 = relMH.getAry2ValTuples();
        for(Pair<SootMethod,AllocNode> pair : res1) {
            SootMethod meth = pair.val0;
            AllocNode alloc = pair.val1;
            int mIdx = domM.indexOf(meth);
            int hIdx = domH.indexOf(alloc);
            HtoM[hIdx] = mIdx;
        }
        relMH.close();
        relIpt.load();
        relIM.load();

        Ctxt epsilon = domC.setCtxt(emptyElems);
        epsilonCtxtSet = new ArraySet<Ctxt>(1);
        epsilonCtxtSet.add(epsilon);

        methToCtxts = new Set[numM];

        methToRcvSites = new TIntArrayList[numM];
        methToClrSites = new TIntArrayList[numM];
        methToClrMeths = new Set[numM];

		relCtxtInsMeth.zero();

        // Do the heavy crunching
        doAnalysis();

        relIpt.close();
        relIM.close();
		relCtxtInsMeth.save();

        // Populate domC
        for(int iIdx = 0; iIdx < ItoM.length; iIdx++){
            int mIdx = ItoM[iIdx];
            Unit invk = ItoQ[iIdx];
			
			if(!isQuasiStaticInvk(invk))
				continue;
			
            Set<Ctxt> ctxts = methToCtxts[mIdx];
			if(ctxts != null){ //only if mIdx method is reachable in stamp's ci analysis
				for (Ctxt oldCtxt : ctxts) {
					Object[] oldElems = oldCtxt.getElems();
					Object[] newElems = combine(K, invk, oldElems);
					domC.setCtxt(newElems);
				}
			}
		}
        for (int hIdx = 0; hIdx < numH; hIdx++) {
			AllocNode alloc = HtoQ[hIdx];
            if(alloc instanceof GlobalAllocNode/* || alloc instanceof StubAllocNode*/) {
				Object[] newElems = combine(K, alloc, emptyElems);
				domC.setCtxt(newElems);
			} else {
				int mIdx = HtoM[hIdx];
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts != null){
					assert(ctxts != null) : alloc.toString();
					for (Ctxt oldCtxt : ctxts) {
						Object[] oldElems = oldCtxt.getElems();
						Object[] newElems = combine(K, alloc, oldElems);
						domC.setCtxt(newElems);
					}
				}
			}
        }
        domC.save();

        int numC = domC.size();

        relCC.zero();
        relCI.zero();

        for(int iIdx = 0; iIdx < ItoM.length; iIdx++){
            int mIdx = ItoM[iIdx];
            Unit invk = ItoQ[iIdx];

			if(isQuasiStaticInvk(invk)){
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts != null){
					for (Ctxt oldCtxt : ctxts) {
						Object[] oldElems = oldCtxt.getElems();
						Object[] newElems = combine(K, invk, oldElems);
						Ctxt newCtxt = domC.setCtxt(newElems);
						relCC.add(oldCtxt, newCtxt);
						relCI.add(newCtxt, invk);
					}
				}
			}
        }
		
        relCI.save();
		
        assert (domC.size() == numC);
        ////CH
        relCH.zero();
		
        for (int hIdx = 0; hIdx < numH; hIdx++) {
			AllocNode alloc = HtoQ[hIdx];
			
            if(alloc instanceof GlobalAllocNode) {
				Object[] newElems = combine(K, alloc, emptyElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCH.add(newCtxt, alloc);
			} else {
				int mIdx = HtoM[hIdx];
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts != null){
					for (Ctxt oldCtxt : ctxts) {
						Object[] oldElems = oldCtxt.getElems();
						Object[] newElems = combine(K, alloc, oldElems);
						Ctxt newCtxt = domC.setCtxt(newElems);
						relCC.add(oldCtxt, newCtxt);
						relCH.add(newCtxt, alloc);
					}
				}
			}
        }
        relCH.save();

        assert (domC.size() == numC);
		
        relCC.save();
		
		CM();
	}

	private void CM()
	{
		ProgramRel relCM = (ProgramRel) ClassicProject.g().getTrgt("CM");
		relCM.zero();
        for (int mIdx = 0; mIdx < methToCtxts.length; mIdx++) {
            SootMethod meth = (SootMethod) domM.get(mIdx);
			Set<Ctxt> ctxts = methToCtxts[mIdx];
			if(ctxts == null){
				//either meth is unreachable or a reachable stub
				continue;
			}
			for(Ctxt c : ctxts){
				relCM.add(c, meth);
				//System.out.println("meth: " + meth + " ctxt: "+c);
			}
		}
		relCM.save();
	}

    private void doAnalysis() {
        SootMethod mainMeth = Program.g().getMainMethod();
        Set<SootMethod> roots = new HashSet<SootMethod>();
        Map<SootMethod, Set<SootMethod>> methToPredsMap = new HashMap<SootMethod, Set<SootMethod>>();
		stubs = stubMethods();
		ProgramRel relReachableM = (ProgramRel) ClassicProject.g().getTrgt("ci_reachableM");
		relReachableM.load();
		Iterable<SootMethod> reachableMethods = relReachableM.getAry1ValTuples();
		Iterator mIt = reachableMethods.iterator();
		while(mIt.hasNext()){
			SootMethod meth = (SootMethod) mIt.next();
			System.out.println("Reach: "+meth);
            int mIdx = domM.indexOf(meth);
			if (meth == mainMeth || meth.getName().equals("<clinit>") || treatCI(meth)) {
                roots.add(meth);
                methToPredsMap.put(meth, emptyMethSet);
                methToCtxts[mIdx] = epsilonCtxtSet;
				relCtxtInsMeth.add(meth);
			} else {
                Set<SootMethod> predMeths = new HashSet<SootMethod>();
                if(isQuasiStaticMeth(meth)) {
                    //do the copyctxt for static method.
                    /*Iterable<Object> ivks = getStatIvk(meth);
                    for (Object ivk : ivks) {
                        int iIdx = domI.indexOf(ivk);
                        int mm = ItoM[iIdx];
                        predMeths.add(domM.get(mm));
                    }
                    methToPredsMap.put(meth, predMeths);
                    methToCtxts[mIdx] = emptyCtxtSet;
                    methToClrMeths[mIdx] = predxbMeths;*/

                    ///use callsite.
                    TIntArrayList clrSites = new TIntArrayList();
                    for (Unit invk : getStatIvk(meth)) {
                        int iIdx = domI.indexOf(invk);
                        predMeths.add(domM.get(ItoM[iIdx])); // Which method can point to this method...?
                        clrSites.add(iIdx); // sitexbcicgxbs that can call me
                    }
                    methToClrSites[mIdx] = clrSites;
                    methToPredsMap.put(meth, predMeths);
                    methToCtxts[mIdx] = emptyCtxtSet;
                } else {
                    TIntArrayList rcvSites = new TIntArrayList();
                    ThisVarNode thisVar = methToThis.get(meth);
					assert thisVar != null : meth.getSignature();
                    Iterable<Object> pts = getPointsTo(thisVar);
					Set<Ctxt> newCtxts = null;
                    for (Object alloc : pts) {
                        int hIdx = domH.indexOf(alloc);
                        rcvSites.add(hIdx);
						if(!(alloc instanceof GlobalAllocNode))
							predMeths.add(domM.get(HtoM[hIdx]));
						else{
							if(newCtxts == null)
								newCtxts = new HashSet<Ctxt>();
							Object[] newElems = combine(K, alloc, emptyElems);
							Ctxt newCtxt = domC.setCtxt(newElems);
							newCtxts.add(newCtxt);
						}
                    }
                    methToRcvSites[mIdx] = rcvSites;
                    methToPredsMap.put(meth, predMeths);
                    methToCtxts[mIdx] = newCtxts == null ? emptyCtxtSet : newCtxts;
                }
            }
        }
        process(roots, methToPredsMap);
		relReachableM.close();
    }

    // Compute all the contexts that each method can be called in
    private void process(Set<SootMethod> roots, Map<SootMethod, Set<SootMethod>> methToPredsMap) {
        IGraph<SootMethod> graph = new MutableGraph<SootMethod>(roots, methToPredsMap, null);
        List<Set<SootMethod>> sccList = graph.getTopSortedSCCs();
        int n = sccList.size();
        if (Config.v().verbose >= 2)
            System.out.println("numSCCs: " + n);
        for (int i = 0; i < n; i++) { // For each SCC...
            Set<SootMethod> scc = sccList.get(i);
            if (Config.v().verbose >= 2)
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
                if (Config.v().verbose >= 2)
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

    private Iterable<Object> getPointsTo(VarNode var) 
	{
        RelView view = relIpt.getView();
        view.selectAndDelete(0, var);
        return view.getAry1ValTuples();
    }

    private Iterable<Unit> getStatIvk(SootMethod method) 
	{
        RelView view = relIM.getView();
        view.selectAndDelete(1, method);
        return view.getAry1ValTuples();
    }

    private Object[] combine(int k, Object inst, Object[] elems) {
        int oldLen = elems.length;
		Object[] newElems;
		if(oldLen > 0 && elems[0] instanceof StubAllocNode){
			//dont push, replace the stuballocnode with inst
			newElems = new Object[oldLen];
			newElems[0] = inst;
			System.arraycopy(elems, 1, newElems, 1, oldLen-1);
		} else {
			int newLen = Math.min(k - 1, oldLen) + 1;
			newElems = new Object[newLen];
			if (newLen > 0) newElems[0] = inst;
			if (newLen > 1)
				System.arraycopy(elems, 0, newElems, 1, newLen - 1);
		}
		return newElems;
    }

    private Set<Ctxt> getNewCtxts(int cleIdx) { // Update contexts for this method (callee)
        final Set<Ctxt> newCtxts = new HashSet<Ctxt>();
        SootMethod meth = (SootMethod) domM.get(cleIdx);

        if(isQuasiStaticMeth(meth)){//static?copy all the ctxts from its callers.
            /*Set<SootMethod> clrs = methToClrMeths[cleIdx];
            for (SootMethod clr : clrs) {
                int clrIdx = domM.indexOf(clr);
                Set<Ctxt> clrCtxts = methToCtxts[clrIdx];
                newCtxts.addAll(clrCtxts);
            }*/
            //instead of copy, we push the invk to the new ctxt.
            TIntArrayList invks = methToClrSites[cleIdx]; // which call sites point to me
            int n = invks.size();
            for (int i = 0; i < n; i++) {
                int iIdx = invks.get(i);
                Unit invk = ItoQ[iIdx];
                int clrIdx = ItoM[iIdx];
                Set<Ctxt> clrCtxts = methToCtxts[clrIdx]; // method of caller
                for (Ctxt oldCtxt : clrCtxts) {
                    Object[] oldElems = oldCtxt.getElems();
                    Object[] newElems = combine(K, invk, oldElems); // Append
                    Ctxt newCtxt = domC.setCtxt(newElems);
                    newCtxts.add(newCtxt);
                    //System.out.println("newCtxt: "+newCtxt);
                }
            }

        }else{
            TIntArrayList rcvs = methToRcvSites[cleIdx];
            int n = rcvs.size();
            for (int i = 0; i < n; i++) {
                int hIdx = rcvs.get(i);
                Object rcv = HtoQ[hIdx];
				if(rcv instanceof GlobalAllocNode){
					Object[] newElems = combine(K, rcv, emptyElems);
					Ctxt newCtxt = domC.setCtxt(newElems);
					newCtxts.add(newCtxt);
				} /*else if(rcv instanceof StubAllocNode) {
					int clrIdx = HtoM[hIdx];
					Set<Ctxt> rcvCtxts = methToCtxts[clrIdx];
					newCtxts.addAll(rcvCtxts);
					}*/
				else {
					int clrIdx = HtoM[hIdx];
					Set<Ctxt> rcvCtxts = methToCtxts[clrIdx];
					for (Ctxt oldCtxt : rcvCtxts) {
						Object[] oldElems = oldCtxt.getElems();
						Object[] newElems = combine(K, rcv, oldElems);
						Ctxt newCtxt = domC.setCtxt(newElems);
						newCtxts.add(newCtxt);
					}
				}
            }
        }
        return newCtxts;
    }

	private NumberedSet stubMethods()
	{
		NumberedSet stubMethods = new NumberedSet(Scene.v().getMethodNumberer());
		final ProgramRel relStub = (ProgramRel) ClassicProject.g().getTrgt("Stub");		
		relStub.load();
        RelView view = relStub.getView();
        Iterable<SootMethod> it = view.getAry1ValTuples();
		for(SootMethod m : it)
			stubMethods.add(m);		
		relStub.close();
		return stubMethods;
    }

	private boolean treatCI(SootMethod meth)
	{
		if(!stubs.contains(meth))
			return false;
		String sig = meth.getSignature();
		return true;
	}
}
