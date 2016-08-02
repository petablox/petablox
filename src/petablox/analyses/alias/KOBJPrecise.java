package petablox.analyses.alias;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.iterator.TIntIterator;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

import petablox.bddbddb.Rel.RelView;
import petablox.analyses.alloc.DomH;
import petablox.analyses.alias.DomC;
import petablox.analyses.invk.DomI;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import petablox.project.analyses.ProgramDom;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.util.tuple.object.Pair;
import petablox.util.tuple.object.Trio;
import soot.Unit;

/**
 * Implementation of a more precise k-obj analysis.
 * 
 * This analysis requires the following relations:
 * 		OAG : Object Allocation Graph which is a directed graph with allocation sites as nodes
 * 		diverge : The set of nodes in the OAG which have 2 or more outgoing edges
 * 		converge : The set of nodes in the OAG which have 2 or more incoming edges
 * 
 * Recognized system properties:
 * 		petablox.kobj.k : the 'k' value to use for each object allocation site.  Default is 1.
 * 
 * This analysis outputs the following domains and relations:
 * 		C: domain containing all abstract contexts
 *   	CC: each (c,c2) such that c2 is all but the last element of context c
 *   	CH: each (c,h) such that object allocation site h is the last element of abstract context c
 *   	CI: each (c,i) such that call site i is the last element of abstract context c
 * 
 * @author Ravi Teja
 */

@Petablox(name = "kobj-precise",
	consumes = { "OAG","diverge","converge" },
	produces = {"C","CC","CH","CI"}
)
public class KOBJPrecise extends JavaAnalysis {
	private static DomH domH;
	private static DomC domC;
	private static DomI domI;
	
	private ProgramRel relOAG;
	private ProgramRel relReachablePath;
	private ProgramRel relDiverge;
	private ProgramRel relConverge;
	private ProgramRel relCC;
	private ProgramRel relCH;
	private ProgramRel relCI;
	
	private Unit root;
	private int k;
	
	private TIntHashSet  prevCtxts;
	private Pair globalP = new Pair(0,0);
	
	private static Map<Pair,TIntHashSet> prevRel;
	private static Unit prevOi, prevOt;

	private static ArrayList<Trio<Ctxt,Unit,Ctxt>> CHC = new ArrayList<Trio<Ctxt,Unit,Ctxt>>();

	private static HashMap<Pair,TIntHashSet> relOIT = new HashMap<Pair,TIntHashSet>();
	private static HashMap<Pair,TIntHashSet> relOITf = new HashMap<Pair,TIntHashSet>();
	private static HashMap<Pair,TIntHashSet> iterOIT = new HashMap<Pair,TIntHashSet>();
	private static HashMap<Pair,TIntHashSet> iterOITf = new HashMap<Pair,TIntHashSet>();
	private static HashMap<Pair,TIntHashSet> changedOIT = new HashMap<Pair,TIntHashSet>();
	private static HashMap<Pair,TIntHashSet> changedOITf = new HashMap<Pair,TIntHashSet>();
	
	public static Map<Unit,HashSet<Unit>> outNodesOAG = new HashMap<Unit,HashSet<Unit>>();
	public static Map<Integer,TIntHashSet> reachableOAG = new HashMap<Integer,TIntHashSet>();
	public static Map<Integer,TIntHashSet> divergeOAG = new HashMap<Integer,TIntHashSet>();
	public static TIntHashSet convergeOAG = new TIntHashSet();
	
	public void run(){
		domH = (DomH) ClassicProject.g().getTrgt("H");
		domC = (DomC) ClassicProject.g().getTrgt("C");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		root = (Unit)domH.get(0);
		k = Integer.getInteger("petablox.kobj.k", 1);
		loadRels();
		loadRelsToMaps();
		
		//Initialize
		handleInit();			
		iterOIT = (HashMap) relOIT.clone();
		iterOITf = (HashMap) relOITf.clone();
		
		//Propagate
		boolean changed = true;
		for(int count = 0; changed ; count++){
			System.out.println("Iteration  #" + count);
			System.out.println("\tTotal tuples: " + (relOIT.size() + relOITf.size()));
			System.out.println("\tTuples in this iteration: "+ (iterOIT.size() + iterOITf.size()));
			changed = false;
			if(handleDiv(count))
				changed = true;
			if(handleCycles(count))
				changed = true;
			refresh();
		}
		
		//Compute CC and CH
		computeContextRels();
	}
	
	/*
	 * Perform after each iteration to assign the changed pairs in previous iteration 
	 * as the new pairs to check for in the next iteration.
	 */
	private void refresh(){
		iterOIT.clear();
		iterOITf.clear();
		iterOIT = changedOIT;
		iterOITf = changedOITf;
		changedOIT = new HashMap<Pair,TIntHashSet>();
		changedOITf = new HashMap<Pair,TIntHashSet>();
	}
	
	/*
	 * Load the necessary relations
	 */
	protected void loadRels(){
		relCC = (ProgramRel)ClassicProject.g().getTrgt("CC");
		relCH = (ProgramRel)ClassicProject.g().getTrgt("CH");
		relCI = (ProgramRel)ClassicProject.g().getTrgt("CI");
		relOAG = (ProgramRel)ClassicProject.g().getTrgt("OAG");
		relReachablePath = (ProgramRel)ClassicProject.g().getTrgt("reachablePath");
		relDiverge = (ProgramRel)ClassicProject.g().getTrgt("diverge");
		relConverge = (ProgramRel)ClassicProject.g().getTrgt("converge");
		
		relOAG.load();
		relReachablePath.load();
		relDiverge.load();
		relConverge.load();
		relCI.load();
	}
	
	/*
	 * Load relations to HashMaps for faster processing
	 */
	private void loadRelsToMaps(){
		RelView view;
		
		// HashMap outNodes
		view = relOAG.getView();
		Iterator<petablox.util.tuple.object.Pair<Unit,Unit>> tuples = view.<Unit,Unit>getAry2ValTuples().iterator();
		while(tuples.hasNext()){
			petablox.util.tuple.object.Pair<Unit,Unit> edge = tuples.next();
			Unit oj = edge.val0;
			Unit oi = edge.val1;
			Set<Unit> outNodes = outNodesOAG.get(oj);
			if(outNodes == null)
				outNodes = new HashSet<Unit>();
			outNodes.add(oi);
			outNodesOAG.put(oj,(HashSet)outNodes);
		}
		
		// HashMap reachableOAG
		view = relReachablePath.getView();
		tuples = view.<Unit,Unit>getAry2ValTuples().iterator();
		while(tuples.hasNext()){
			petablox.util.tuple.object.Pair<Unit,Unit> edge = tuples.next();
			int oi = domH.indexOf(edge.val0);
			int ot = domH.indexOf(edge.val1);
			TIntHashSet reachableNodes = reachableOAG.get(oi);
			if(reachableNodes == null)
				reachableNodes = new TIntHashSet();
			reachableNodes.add(ot);
			reachableOAG.put(oi,reachableNodes);
		}

		// HashMap divergeOAG
		view = relDiverge.getView();
		tuples = view.<Unit,Unit>getAry2ValTuples().iterator();
		while(tuples.hasNext()){
			petablox.util.tuple.object.Pair<Unit,Unit> tuple = tuples.next();
			int oj = domH.indexOf(tuple.val0);
			int ot = domH.indexOf(tuple.val1);
			TIntHashSet endNodes = divergeOAG.get(oj);
			if(endNodes == null)
				endNodes = new TIntHashSet();
			endNodes.add(ot);
			divergeOAG.put(oj,endNodes);
		}
		
		// HashMap convergeOAG
		view = relConverge.getView();
		Iterator iter = view.<Unit>getAry1ValTuples().iterator();
		while(iter.hasNext()){
			Unit node = (Unit) iter.next();
			int idx = domH.indexOf(node);
			convergeOAG.add(idx);
		}	
	}
		
	/*
	 * Initialize relOIT and relOITf by considering all nodes which have an incoming edge from the root node
	 */
	private void handleInit(){
		Ctxt epsilon = domC.setCtxt(new Unit[0]);
		RelView view = relOAG.getView();
		view.selectAndDelete(0,root);
		Iterator<Unit> ois = view.<Unit>getAry1ValTuples().iterator();
		while(ois.hasNext()){
			Unit oi = ois.next();
			view = relReachablePath.getView();
			view.selectAndDelete(0,oi);
			Iterator<Unit> ots = view.<Unit>getAry1ValTuples().iterator();
			while(ots.hasNext()){
				Unit ot = ots.next();
				if(relDiverge.contains(root,ot))
					add(relOIT,epsilon,oi,ot);
				else
					add(relOITf,epsilon,oi,ot);
				if(oi == ot)
					CHC.add(new Trio(epsilon,oi,combine(k,oi,epsilon)));
			}
		}
	}	
	
	/*
	 * Propagate OIT and OITf along paths in the Object Allocation Graph (OAG)
	 */
	private boolean handleDiv(int iter){
		boolean changed = false;
		//Iterate through all tuples where the contained node is NOT representative of a path in the OAG
		for (Pair p : iterOITf.keySet()) {
		    Unit oj = (Unit)domH.get(p.val0);
		    Unit ot = (Unit)domH.get(p.val1);
		    Set<Unit> oiSet = outNodesOAG.get(oj);
		    if(oiSet == null)
		    	continue;
		    Iterator<Unit> ois = oiSet.iterator();
			while(ois.hasNext()){			//for each oi such that oj -> oi
				Unit oi = ois.next();
				TIntHashSet reachableSet = reachableOAG.get(domH.indexOf(oi));
				if(reachableSet!=null && reachableSet.contains(domH.indexOf(ot)) && oj != ot){	//check that ot is reachable from oi and oj!=ot
					TIntHashSet ctxtSet = iterOITf.get(getPair(oj,ot));
					if(ctxtSet == null)
						continue;
					int[] ctxts = ctxtSet.toArray();
					for(int i = 0; i < ctxts.length; i++){
						Ctxt ctxt = (Ctxt)domC.get((Integer)ctxts[i]);
						Ctxt ctxtNew;
						TIntHashSet divergeSet = divergeOAG.get(domH.indexOf(oj));
						if(divergeSet!=null && divergeSet.contains(domH.indexOf(ot))){
							ctxtNew = ctxt;
							if(add(relOIT,ctxtNew, oi, ot)){
								changed = true;
								add(changedOIT,ctxtNew, oi, ot);
							}
						} else { 
							ctxtNew = ctxt;
							if(add(relOITf,ctxtNew, oi, ot)){
								changed = true;
								add(changedOITf,ctxtNew, oi, ot);
							}
						}
						if(oi == ot)
							CHC.add(new Trio(combine(k,oj,ctxt), oi,combine(k,oi,ctxtNew)));	
					}
				}
			}
		}
		//Iterate through all tuples where the contained node is representative of a path in the OAG
		for (Pair p : iterOIT.keySet()) {
		    Unit oj = (Unit)domH.get(p.val0);
		    Unit ot = (Unit)domH.get(p.val1);
		    Set<Unit> oiSet = outNodesOAG.get(oj);
		    if(oiSet == null)
		    	continue;
		    Iterator<Unit> ois = oiSet.iterator();
			while(ois.hasNext()){
				Unit oi = ois.next();
				TIntHashSet reachableSet = reachableOAG.get(domH.indexOf(oi));
				if(reachableSet!=null && reachableSet.contains(domH.indexOf(ot)) && oj != ot){
					TIntHashSet ctxtSet = iterOIT.get(getPair(oj,ot));
					if(ctxtSet == null)
						continue;
					int[] ctxts = ctxtSet.toArray();
					for(int i = 0; i < ctxts.length; i++){
						Ctxt ctxt = (Ctxt)domC.get((Integer)ctxts[i]);
						boolean repNew;
						Ctxt ctxtNew;
						if(convergeOAG.contains(domH.indexOf(oi))){
							TIntHashSet divergeSet = divergeOAG.get(domH.indexOf(oj));
							repNew = divergeSet != null && divergeSet.contains(domH.indexOf(ot));
							ctxtNew = combine(k, oj, ctxt); 
							if(repNew){
								if(add(relOIT,ctxtNew, oi, ot)){
									changed = true;
									add(changedOIT,ctxtNew, oi, ot);
								}
							}
							else{
								if(add(relOITf,ctxtNew, oi, ot)){
									changed = true;
									add(changedOITf,ctxtNew, oi, ot);
								}
							}
						} else { 
							ctxtNew = ctxt;
							if(add(relOIT,ctxtNew, oi, ot)){
								changed = true;
								add(changedOIT,ctxtNew, oi, ot);
							}
						}
						if(oi == ot)
							CHC.add(new Trio(combine(k,oj,ctxt), oi,combine(k,oi,ctxtNew)));
					}
				}
			}
		}
		return changed;
	}
	
	/*
	 * Propagate OIT and OITf along cycles in the Object Allocation Graph.
	 */
	private boolean handleCycles(int iter){
		boolean changed = false;
		//Iterate through all tuples where the contained node is representative of a path in the OAG
		for (Pair p : iterOIT.keySet()) {
		    Unit oj = (Unit)domH.get(p.val0);
		    Unit ot = (Unit)domH.get(p.val1);
		    Set<Unit> oiSet = outNodesOAG.get(oj);
		    if(oiSet == null)
		    	continue;
		    Iterator<Unit> ois = oiSet.iterator();
			while(ois.hasNext()){
				Unit oi = ois.next();
				TIntHashSet reachableSet = reachableOAG.get(domH.indexOf(oi));
				if(reachableSet != null && reachableSet.contains(domH.indexOf(ot)) && oj == ot){		//oj = ot since ot is in a cycle
					TIntHashSet ctxtSet = iterOIT.get(getPair(oj,ot));
					if(ctxtSet == null)
						continue;
					int[] ctxts = ctxtSet.toArray();
					for(int i = 0; i < ctxts.length; i++){
						Ctxt ctxt = (Ctxt)domC.get((Integer)ctxts[i]);
						Ctxt ctxtNew;
						if(convergeOAG.contains(domH.indexOf(oi))){
							ctxtNew = combine(k,oj,ctxt);
							if(add(relOIT,ctxtNew, oi, ot)){
								changed = true;
								add(changedOIT,ctxtNew, oi, ot);
							}
						} else{
							ctxtNew = ctxt;
							if(add(relOIT,ctxtNew, oi, ot)){
								changed = true;
								add(changedOIT,ctxtNew, oi, ot);
							}
						}
						if(oi == ot)
							CHC.add(new Trio(combine(k,oj,ctxt), oi,combine(k,oi,ctxtNew)));	
					}
				}
			}
		}	
		//Iterate through all tuples where the contained node is NOT representative of a path in the OAG
		for (Pair p : iterOITf.keySet()) {
		    Unit oj = (Unit)domH.get(p.val0);
		    Unit ot = (Unit)domH.get(p.val1);
		    Set<Unit> oiSet = outNodesOAG.get(oj);
		    if(oiSet == null)
		    	continue;
		    Iterator<Unit> ois = oiSet.iterator();
			while(ois.hasNext()){
				Unit oi = ois.next();
				TIntHashSet reachableSet = reachableOAG.get(domH.indexOf(oi));
				if(reachableSet != null && reachableSet.contains(domH.indexOf(ot)) && oj == ot){	
					TIntHashSet ctxtSet = iterOITf.get(getPair(oj,ot));
					if(ctxtSet == null)
						continue;
					int[] ctxts = ctxtSet.toArray();
					for(int i = 0; i < ctxts.length; i++){
						Ctxt ctxt = (Ctxt)(domC.get((Integer)ctxts[i]));						
						Ctxt ctxtNew = ctxt;
						if(add(relOIT,ctxtNew, oi, ot)){
							changed = true;
							add(changedOIT,ctxtNew, oi, ot);
						}
						if(oi == ot)
							CHC.add(new Trio(combine(k,oj,ctxt), oi,combine(k,oi,ctxtNew)));
					}
				}
			}
		}
		return changed;
	}
	
	/*
	 * Final step - Compute CC, CH and CI
	 */
	private void computeContextRels(){
		//Save invoke quads from CI which is computed by the analysis ctxts-java
		ArrayList<Unit> invokeQuads = new ArrayList<Unit>();
		Iterator<petablox.util.tuple.object.Pair<Ctxt,Unit>> ciIt = relCI.<Ctxt,Unit>getAry2ValTuples().iterator();
		while(ciIt.hasNext())
			invokeQuads.add(ciIt.next().val1);
		
		domC.save();		//All the relations can now be computed since domC is saved..
		relCC.zero();
		relCH.zero();
		Iterator<Trio<Ctxt,Unit,Ctxt>> chcIt =  CHC.iterator();
		while(chcIt.hasNext()){
			Trio<Ctxt,Unit,Ctxt> chc = chcIt.next();
			Ctxt ctxtOld = chc.val0;
			Unit oi = chc.val1;
			Ctxt ctxtNew = chc.val2;
			relCC.add(ctxtOld,ctxtNew);
			relCH.add(ctxtNew,oi);
		}
		relCC.save();
		relCH.save();
		
		//Write back the invoke quads to CI - this rewrite is required since dom C is modified
		Ctxt epsilon = domC.setCtxt(new Unit[0]);
		relCI.zero();
		for(Unit q : invokeQuads)
			relCI.add(epsilon,q);
		relCI.save();
	}		
	
	/*
	 * Combine inst with ctxt; k limits the size of ctxt
	 */
	private Ctxt combine(int k, Unit inst, Ctxt ctxt) {
		Unit[] elems = ctxt.getElems();
        int oldLen = elems.length;
        int newLen = Math.min(k - 1, oldLen) + 1;
        Unit[] newElems = new Unit[newLen];
        if (newLen > 0) newElems[0] = inst;
        if (newLen > 1)
            System.arraycopy(elems, 0, newElems, 1, newLen - 1);
        Ctxt ctxtNew = new Ctxt(newElems);
        domC.setCtxt(newElems);
        return ctxtNew;
    }
	
	/*
	 * Add the tuple (ctxtNew, oi, ot) to relation rel
	 */
	private boolean add(Map<Pair,TIntHashSet> rel, Ctxt ctxtNew, Unit oi, Unit ot){
		boolean changed;
		Pair p = new Pair(domH.indexOf(oi),domH.indexOf(ot));
		TIntHashSet ctxts; 
		if(rel == prevRel && prevOi==oi && prevOt==ot)
			ctxts = prevCtxts;
		else
			ctxts = rel.get(p);
		if(ctxts == null)
			ctxts = new TIntHashSet();
		changed = ctxts.add(domC.indexOf(ctxtNew));
		if(changed)
			rel.put(p,ctxts);
		prevRel = rel;
		prevOi = oi;
		prevOt = ot;
		prevCtxts = ctxts;
		return changed;
	}
	
	/*
	 * Print any of the HashMaps ending with OIT(f)
	 */
	public void printRel(Map<Pair,TIntHashSet> rel, String name){
		System.out.println("Relation :"+name);
		for(Pair p : rel.keySet()){
			System.out.println("Pair: "+domH.toFIString(domH.get(p.val0)) + " and "+ domH.toFIString(domH.get(p.val1)));
			System.out.println("Contexts:");
			TIntIterator it = rel.get(p).iterator();
			while(it.hasNext()){
				Ctxt c = domC.get(it.next());
				System.out.println(c);
			}
		}
	}
	
	/*
	 * Helper class : Pair of integers
	 */
	private class Pair{
		public int val0;
		public int val1;
		
		Pair(int a, int b){
			this.val0 = a;
			this.val1 = b;
		}
		
		@Override
		public boolean equals(Object o){
			Pair p = (Pair)o;
			if(p==null)
				return false;
			return this.val0 == p.val0 && this.val1 == p.val1;
		}
		
		@Override
	    public int hashCode(){
			return (val0 == 0 ? 0 : ((Integer) val0).hashCode()) + (val1 == 0 ? 0 : ((Integer) val1).hashCode());
	    }
	}
	
	/*
	 * Returns a Pair which helps in looking up hashmaps without allocating memory during each lookup
	 */
	private Pair getPair(Unit a, Unit b){
		globalP.val0 = domH.indexOf(a);
		globalP.val1 = domH.indexOf(b);
		return globalP;
	}
		
}
