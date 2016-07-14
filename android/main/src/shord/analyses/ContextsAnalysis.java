package shord.analyses;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import gnu.trove.list.array.TIntArrayList;

import soot.Scene;
import soot.Unit;
import soot.SootMethod;
import soot.util.NumberedSet;

import shord.program.Program;
import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.Config;
import shord.project.Messages;

import chord.project.Chord;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;
import chord.bddbddb.Rel.RelView;

@Chord(name = "contexts-java",
	   consumes = { "ci_reachableM", "ci_IM", "I", "M", "H", "MH", "MI", "Stub" },
	   produces = { "C", "CC", "CI", "CM", "CH", "CtxtInsMeth" },
	   namesOfTypes = { "C" },
	   types = { DomC.class },
	   namesOfSigns = { "CC", "CI", "CM", "CH", "CtxtInsMeth" },
	   signs = { "C0,C1:C0xC1", "C0,I0:C0_I0", "C0,M0:C0_M0", "C0,H0:C0_H0", "M0:M0"}
	   )
public class ContextsAnalysis extends JavaAnalysis
{
    private static final Set<Ctxt> emptyCtxtSet = Collections.emptySet();
    private static final Set<SootMethod> emptyMethSet = Collections.emptySet();
    private static final Object[] emptyElems = new Object[0];

	public static int K = 2;

    private Set<Ctxt>[] methToCtxts;
    private TIntArrayList[] methToClrSites;

    private int[] ItoM;
    private Unit[] ItoQ;
    private int[] HtoM;
    private AllocNode[] HtoQ;

    private Set<Ctxt> epsilonCtxtSet;

    private DomM domM;
    private DomI domI;
    private DomC domC;

    private ProgramRel relIM;
	private ProgramRel relCtxtInsMeth;

	private NumberedSet stubs;

	public void run()
	{
		domI = (DomI) ClassicProject.g().getTrgt("I");
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domC = (DomC) ClassicProject.g().getTrgt("C");

		relCtxtInsMeth = (ProgramRel) ClassicProject.g().getTrgt("CtxtInsMeth");

        int numM = domM.size();
		int numI = domI.size();

        //int numA = domH.getLastI() + 1; 

        methToCtxts = new Set[numM];
        methToClrSites = new TIntArrayList[numM];

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

        DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		int numH = domH.size();
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

        Ctxt epsilon = domC.setCtxt(emptyElems);
        epsilonCtxtSet = new ArraySet<Ctxt>(1);
        epsilonCtxtSet.add(epsilon);

        relIM = (ProgramRel) ClassicProject.g().getTrgt("ci_IM");
		relIM.load();
		relCtxtInsMeth.zero();

		doAnalysis();

		relIM.close();
		relCtxtInsMeth.save();

		C();

		CC_CI();

		CH();
		
		CM();
	}

	private void CM()
	{
		ProgramRel relCM = (ProgramRel) ClassicProject.g().getTrgt("CM");
		relCM.zero();
        for (int mIdx = 0; mIdx < methToCtxts.length; mIdx++) {
            SootMethod meth = (SootMethod) domM.get(mIdx);
			Set<Ctxt> ctxts = methToCtxts[mIdx];
			if(ctxts == null)//either meth is unreachable or a reachable stub
				continue;
			for(Ctxt c : ctxts){
				relCM.add(c, meth);
				//System.out.println("meth: " + meth + " ctxt: "+c);
			}
		}
		relCM.save();
	}

	private void CH()
	{
		ProgramRel relCH = (ProgramRel) ClassicProject.g().getTrgt("CH");
		relCH.zero();

		for(int hIdx = 0; hIdx < HtoM.length; hIdx++){
			AllocNode alloc = HtoQ[hIdx];
			if(alloc instanceof GlobalAllocNode || alloc instanceof StubAllocNode){
				//we dont track GlobalAllocNode or StubAllocNode during k-cfa
				//so dont add to CH
				/*
				Object[] newElems = combine(K, alloc, emptyElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCH.add(newCtxt, alloc);
				*/
			} else {
				int mIdx = HtoM[hIdx];
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts == null)//either meth is unreachable or a reachable stub
					continue;			
				for (Ctxt oldCtxt : ctxts) {
					Object[] oldElems = oldCtxt.getElems();
					Object[] newElems = combine(K, alloc, oldElems);
					Ctxt newCtxt = domC.setCtxt(newElems);
					//relCC.add(oldCtxt, newCtxt);
					relCH.add(newCtxt, alloc);
				}
			}
        }

        relCH.save();
	}

	private void CC_CI()
	{
        ProgramRel relCC = (ProgramRel) ClassicProject.g().getTrgt("CC");
        ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCC.zero();
        relCI.zero();

		for(int iIdx = 0; iIdx < ItoM.length; iIdx++){
			int mIdx = ItoM[iIdx];
			Unit invk = ItoQ[iIdx];
            Set<Ctxt> ctxts = methToCtxts[mIdx];
			if(ctxts == null)//either meth is unreachable or a reachable stub
				continue;			
            for (Ctxt oldCtxt : ctxts) {
                Object[] oldElems = oldCtxt.getElems();
                Object[] newElems = combine(K, invk, oldElems);
                Ctxt newCtxt = domC.setCtxt(newElems);
				relCC.add(oldCtxt, newCtxt);
                relCI.add(newCtxt, invk);
			}
		}
        relCI.save();

		for(int hIdx = 0; hIdx < HtoM.length; hIdx++){
			AllocNode alloc = HtoQ[hIdx];
			if(alloc instanceof GlobalAllocNode || alloc instanceof StubAllocNode){
				//we dont track GlobalAllocNode or StubAllocNode during k-cfa
				//so dont add to CC
			} else {
				assert alloc instanceof SiteAllocNode;
				int mIdx = HtoM[hIdx];
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts == null)//either meth is unreachable or a reachable stub
					continue;			
				for (Ctxt oldCtxt : ctxts) {
					Object[] oldElems = oldCtxt.getElems();
					Object[] newElems = combine(K, alloc, oldElems);
					Ctxt newCtxt = domC.setCtxt(newElems);
					relCC.add(oldCtxt, newCtxt);
				}
			}
		}
		relCC.save();
	}

	private void C()
	{
		for(int iIdx = 0; iIdx < ItoM.length; iIdx++){
			int mIdx = ItoM[iIdx];
			Unit invk = ItoQ[iIdx];
            Set<Ctxt> ctxts = methToCtxts[mIdx];
			if(ctxts == null)//either meth is unreachable or a reachable stub
				continue;			
            for (Ctxt oldCtxt : ctxts) {
                Object[] oldElems = oldCtxt.getElems();
                Object[] newElems = combine(K, invk, oldElems);
                domC.setCtxt(newElems);
            }
        }

		for(int hIdx = 0; hIdx < HtoM.length; hIdx++){
			AllocNode alloc = HtoQ[hIdx];
			if(alloc instanceof GlobalAllocNode){
				Object[] newElems = combine(K, alloc, emptyElems);
				domC.setCtxt(newElems);
			} else {
				int mIdx = HtoM[hIdx];				
				Set<Ctxt> ctxts = methToCtxts[mIdx];
				if(ctxts == null)//either meth is unreachable or a reachable stub
					continue;			
				for (Ctxt oldCtxt : ctxts) {
					Object[] oldElems = oldCtxt.getElems();
					Object[] newElems = combine(K, alloc, oldElems);
					domC.setCtxt(newElems);
				}
			}
        }
		domC.save();		
	}

	private void doAnalysis()
	{
        SootMethod mainMeth = Program.g().getMainMethod();
        Set<SootMethod> roots = new HashSet<SootMethod>();
        Map<SootMethod, Set<SootMethod>> methToPredsMap = new HashMap<SootMethod, Set<SootMethod>>();
		
		stubs = stubMethods();
		//Iterator mIt = Program.g().scene().getReachableMethods().listener();
		//while(mIt.hasNext()){

		final ProgramRel relReachableM = (ProgramRel) ClassicProject.g().getTrgt("ci_reachableM");		
		relReachableM.load();
		Iterable<SootMethod> mIt = relReachableM.getAry1ValTuples();
		for(SootMethod meth : mIt){
			//SootMethod meth = (SootMethod) mIt.next();
			if(stubs.contains(meth)){
				//System.out.println("reachstub "+meth);
				continue;
			}
			int mIdx = domM.indexOf(meth);
			boolean treatCI = treatCI(meth);
			if (meth == mainMeth || meth.getName().equals("<clinit>") || treatCI){
                roots.add(meth);
                methToPredsMap.put(meth, emptyMethSet);
                methToCtxts[mIdx] = epsilonCtxtSet;
				if(treatCI)
					relCtxtInsMeth.add(meth);
			} else {
				Set<SootMethod> predMeths = new HashSet<SootMethod>();
				TIntArrayList clrSites = new TIntArrayList();
				for (Unit invk : getCallers(meth)) {
					//System.out.println("callsite "+invk + ","+invk.getMethod());
					int iIdx = domI.indexOf(invk);
					predMeths.add(domM.get(ItoM[iIdx])); // Which method can point to this method...?
					clrSites.add(iIdx); // sites that can call me
				}
				//System.out.println("callee "+meth+" "+mIdx+" "+clrSites.size());
				methToClrSites[mIdx] = clrSites;
				methToPredsMap.put(meth, predMeths);
				methToCtxts[mIdx] = emptyCtxtSet;
			}
		}
		relReachableM.close();

		process(roots, methToPredsMap);
	}

	// Compute all the contexts that each method can be called in
    private void process(Set<SootMethod> roots, Map<SootMethod, Set<SootMethod>> methToPredsMap)
	{
        IGraph<SootMethod> graph = new MutableGraph<SootMethod>(roots, methToPredsMap, null);
        List<Set<SootMethod>> sccList = graph.getTopSortedSCCs();
        int n = sccList.size();
        if (Config.v().verbose >= 2)
            System.out.println("numSCCs: " + n);
        for (int i = 0; i < n; i++) { // For each SCC...
            Set<SootMethod> scc = sccList.get(i);
            //if (Config.v().verbose >= 2)
            //    System.out.println("Processing SCC #" + i + " of size: " + scc.size());
            if (scc.size() == 1) { // Singleton
                SootMethod cle = scc.iterator().next();
				//System.out.println("contexting " +  cle);
                if (roots.contains(cle))
                    continue;
                if (!graph.hasEdge(cle, cle)) {
                    int cleIdx = domM.indexOf(cle);
                    methToCtxts[cleIdx] = getNewCtxts(cleIdx);
					//System.out.println("cle "+cle+" "+cleIdx);
					//for (Ctxt ctxt : methToCtxts[cleIdx]) {
					//	System.out.println("cctt "+ ctxt);
					//}
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
					//System.out.println("contexting " +  cle);
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

	private Iterable<Unit> getCallers(SootMethod meth)
	{
        RelView view = relIM.getView();
        view.selectAndDelete(1, meth);
        return view.getAry1ValTuples();
    }

    private Object[] combine(int k, Object inst, Object[] elems)
	{
        int oldLen = elems.length;
        int newLen = Math.min(k - 1, oldLen) + 1;
        Object[] newElems = new Object[newLen];
        if (newLen > 0) newElems[0] = inst;
        if (newLen > 1)
            System.arraycopy(elems, 0, newElems, 1, newLen - 1);
        return newElems;
    }

	// Update contexts for this method (callee)
	private Set<Ctxt> getNewCtxts(int cleIdx)
	{
        final Set<Ctxt> newCtxts = new HashSet<Ctxt>();
		TIntArrayList invks = methToClrSites[cleIdx]; // which call sites point to me
		int n = invks.size();
		//System.out.println("cleIdx: "+ cleIdx + " n: " +n);
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
		if(sig.equals("<android.app.Activity: android.view.View findViewById(int)>"))
			return false;
		return true;
	}
}
