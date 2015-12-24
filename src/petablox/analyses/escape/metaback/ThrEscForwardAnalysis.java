package petablox.analyses.escape.metaback;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import petablox.analyses.alias.ICICG;
import petablox.analyses.escape.ThrEscException;
import petablox.analyses.escape.hybrid.full.DstNode;
import petablox.analyses.escape.hybrid.full.Edge;
import petablox.analyses.escape.hybrid.full.FldObj;
import petablox.analyses.escape.hybrid.full.Obj;
import petablox.analyses.escape.hybrid.full.SrcNode;
import petablox.program.Loc;
import petablox.project.analyses.rhs.IWrappedPE;
import petablox.project.analyses.rhs.RHSAnalysis;
import petablox.project.analyses.rhs.TimeoutException;
import petablox.project.analyses.rhs.TraceOverflowException;
import petablox.util.ArraySet;
import petablox.util.Timer;
import petablox.util.soot.FGStmtSwitch;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;
import soot.*;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;

/**
 * The forward analysis of thread escape, modified from Mayur's
 * ThreadEscapeFullAnalysis
 * 
 * @author xin
 * 
 */
public class ThrEscForwardAnalysis extends RHSAnalysis<Edge, Edge> {
	private static final ArraySet<FldObj> emptyHeap = new ArraySet<FldObj>(0);
	private static final Obj[] emptyRetEnv = new Obj[] { Obj.EMTY };
	private static boolean useBOTH = false;
	private static boolean optimizeSumms = false;

	private Set<Unit> currHs;
	private Set<Unit> currLocEs;
	private Set<Unit> currEscEs = new HashSet<Unit>();
	private MyQuadVisitor qv = new MyQuadVisitor();
	private EscQuadVisitor eqv = new EscQuadVisitor();
	private IterThrEscAnalysis iterAnalysis;
	
	private boolean timeOut;

	public ThrEscForwardAnalysis(IterThrEscAnalysis iterAnalysis, Set<Unit> param, Set<Unit> queries) {
		this.iterAnalysis = iterAnalysis;
		this.currLocEs = new HashSet<Unit>(queries);
		this.currHs = param;
	}

	public void run() {
		init();
		timeOut = false;
		System.out.println("**************");
		System.out.println("currEs:");
		for (Unit q : currLocEs) {
			int x = iterAnalysis.domE().indexOf(q);
			System.out.println("\t" + SootUtilities.toVerboseStr(q) + " " + x);
		}
		System.out.println("currHs:");
		for (Unit q : currHs)
			System.out.println("\t" + SootUtilities.toVerboseStr(q));
		Timer timer = new Timer("thresc-shape-timer");
		timer.init();
		try {
			runPass();
		} catch (TimeoutException ex) {
			for (Unit q : currLocEs)
				currEscEs.add(q);
			currLocEs.clear();
			timeOut = true;
		} catch (TraceOverflowException ex){
			for (Unit q : currLocEs)
				currEscEs.add(q);
			currLocEs.clear();
			timeOut = true;
		}
		catch (ThrEscException ex) {
			// do nothing
		}
		for (Unit q : currLocEs)
			System.out.println("LOC: " + SootUtilities.toVerboseStr(q));
		for (Unit q : currEscEs)
			System.out.println("ESC: " + SootUtilities.toVerboseStr(q));
		timer.done();
		long inclusiveTime = timer.getInclusiveTime();
		System.out.println((timeOut?"TIMED OUT ":"")+"ForwardTime: "+inclusiveTime);
		System.out.println(Timer.getTimeStr(inclusiveTime));
		done();
	}

	public static void setUseBOTH(boolean useBOTH) {
		ThrEscForwardAnalysis.useBOTH = useBOTH;
	}

	public static void setOptimize(boolean opt) {
		optimizeSumms = opt;
	}

	@Override
	public ICICG getCallGraph() {
		return iterAnalysis.getCallGraph();
	}

	public boolean isTimeOut() {
		return timeOut;
	}
	
	// m is either the main method or the thread root method
	private Edge getRootPathEdge(SootMethod m) {
		assert (m == iterAnalysis.getMainMethod() ||
				m == iterAnalysis.getThreadStartMethod() ||
				m.getName().toString().equals("<clinit>"));
		int n = iterAnalysis.methToNumVars(m);
		Obj[] env = new Obj[n];
		for (int i = 0; i < n; i++)
			env[i] = Obj.EMTY;
		if (m == iterAnalysis.getThreadStartMethod()) {
			// arg of start method of java.lang.Thread escapes
			env[0] = Obj.ONLY_ESC;
		}
		SrcNode srcNode = new SrcNode(env, emptyHeap);
		DstNode dstNode = new DstNode(env, emptyHeap, false, false);
		Edge pe = new Edge(srcNode, dstNode);
		return pe;
	}

	@Override
	public Set<Pair<Loc, Edge>> getInitPathEdges() {
		Set<SootMethod> roots = cicg.getRoots();
		Set<Pair<Loc, Edge>> initPEs = new ArraySet<Pair<Loc, Edge>>(roots.size());
		for (SootMethod m : roots) {
			Edge pe = getRootPathEdge(m);
			Block bb = SootUtilities.getCFG(m).getHeads().get(0);
			Loc loc = new Loc(bb, -1);
			Pair<Loc, Edge> pair = new Pair<Loc, Edge>(loc, pe);
			initPEs.add(pair);
		}
		return initPEs;
	}

	@Override
	public Edge getInitPathEdge(Unit q, SootMethod m2, Edge pe) {
		Edge pe2;
		if (m2 == iterAnalysis.getThreadStartMethod()) {
			// ignore pe
			pe2 = getRootPathEdge(m2);
		} else {
			DstNode dstNode = pe.dstNode;
			Obj[] dstEnv = dstNode.env;
			List<soot.Value> args = SootUtilities.getInvokeArgs(q);
			if(SootUtilities.isInstanceInvoke(q)){
				soot.Value thisV = SootUtilities.getInstanceInvkBase(q);
				args.add(0, thisV);
			}
			int numArgs = args.size();
			int numVars = iterAnalysis.methToNumVars(m2);
			Obj[] env = new Obj[numVars];
			int z = 0;
			boolean allEsc = optimizeSumms ? true : false;
			for (int i = 0; i < numArgs; i++) {
				soot.Value ao = args.get(i);
				if (ao.getType() instanceof RefLikeType) {
					int aIdx = getIdx(ao);
					Obj aPts = dstEnv[aIdx];
					if (aPts != Obj.EMTY && aPts != Obj.ONLY_ESC)
						allEsc = false;
					env[z++] = aPts;
				}
			}
			while (z < numVars)
				env[z++] = Obj.EMTY;
			ArraySet<FldObj> dstHeap2 = allEsc ? emptyHeap : dstNode.heap;
			SrcNode srcNode2 = new SrcNode(env, dstHeap2);
			DstNode dstNode2 = new DstNode(env, dstHeap2, false, false);
			pe2 = new Edge(srcNode2, dstNode2);
		}
		return pe2;
	}

	@Override
	public Edge getMiscPathEdge(Unit q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		qv.iDstNode = dstNode;
		qv.oDstNode = dstNode;
		q.apply(qv);
		DstNode dstNode2 = qv.oDstNode;
		return new Edge(pe.srcNode, dstNode2);
	}

	private Edge getForkPathEdge(Unit q, Edge pe) {
		DstNode dstNode = pe.dstNode;
		Obj[] iEnv = dstNode.env;
		List<soot.Value> args = SootUtilities.getInvokeArgs(q);
		if(SootUtilities.isInstanceInvoke(q)){
			soot.Value thisV = SootUtilities.getInstanceInvkBase(q);
			args.add(0, thisV);
		}
		soot.Value ao = args.get(0);
		int aIdx = getIdx(ao);
		Obj aPts = iEnv[aIdx];
		DstNode dstNode2;
		if (aPts == Obj.ONLY_ESC || aPts == Obj.EMTY)
			dstNode2 = dstNode;
		else
			dstNode2 = reset(dstNode);
		Edge pe2 = new Edge(pe.srcNode, dstNode2);
		return pe2;
	}

	@Override
	public Edge getSummaryEdge(SootMethod m, Edge pe) {
		Edge se;
		DstNode dstNode = pe.dstNode;
		if (dstNode.isRetn)
			se = new Edge(pe.srcNode, dstNode);
		else {
			dstNode = new DstNode(emptyRetEnv, dstNode.heap, dstNode.isKill, true);
			se = new Edge(pe.srcNode, dstNode);
		}
		return se;
	}

	@Override
	public Edge getPECopy(Edge pe) {
		return new Edge(pe.srcNode, pe.dstNode);
	}

	@Override
	public Edge getSECopy(Edge se) {
		return getPECopy(se);
	}

	@Override
	public Edge getInvkPathEdge(Unit q, Edge clrPE, SootMethod m, Edge tgtSE) {
		if (m == iterAnalysis.getThreadStartMethod()) {
			// ignore tgtSE
			return getForkPathEdge(q, clrPE);
		}
		DstNode clrDstNode = clrPE.dstNode;
		SrcNode tgtSrcNode = tgtSE.srcNode;
		Obj[] clrDstEnv = clrDstNode.env;
		Obj[] tgtSrcEnv = tgtSrcNode.env;
		List<soot.Value> args = SootUtilities.getInvokeArgs(q);
		if(SootUtilities.isInstanceInvoke(q)){
			soot.Value thisV = SootUtilities.getInstanceInvkBase(q);
			args.add(0, thisV);
		}
		int numArgs = args.size();
		boolean allEsc = optimizeSumms ? true : false;
		for (int i = 0, fIdx = 0; i < numArgs; i++) {
			soot.Value ao = args.get(i);
			if (ao.getType() instanceof RefLikeType) {
				int aIdx = getIdx(ao);
				Obj aPts = clrDstEnv[aIdx];
				Obj fPts = tgtSrcEnv[fIdx];
				if (aPts != fPts)
					return null;
				if (aPts != Obj.EMTY && aPts != Obj.ONLY_ESC)
					allEsc = false;
				fIdx++;
			}
		}
		ArraySet<FldObj> clrDstHeap = clrDstNode.heap;
		ArraySet<FldObj> tgtSrcHeap = tgtSrcNode.heap;
		DstNode tgtRetNode = tgtSE.dstNode;
		ArraySet<FldObj> tgtRetHeap = tgtRetNode.heap;
		ArraySet<FldObj> clrDstHeap2;
		if (allEsc) {
			assert (tgtSrcHeap == emptyHeap);
			clrDstHeap2 = new ArraySet<FldObj>(clrDstHeap);
			clrDstHeap2.addAll(tgtRetHeap);
		} else {
			if (!clrDstHeap.equals(tgtSrcHeap))
				return null;
			clrDstHeap2 = tgtRetHeap;
		}
		int n = clrDstEnv.length;
		Obj[] clrDstEnv2 = new Obj[n];
		soot.Value ro = null;
		if(q instanceof JAssignStmt){
			ro = ((JAssignStmt)q).leftBox.getValue();
		}
		int rIdx = -1;
		if (ro != null && ro.getType() instanceof RefLikeType) {
			rIdx = getIdx(ro);
			clrDstEnv2[rIdx] = tgtRetNode.env[0];
		}
		boolean isKill = tgtRetNode.isKill;
		if (allEsc || !isKill) {
			for (int i = 0; i < n; i++) {
				if (i != rIdx) {
					clrDstEnv2[i] = clrDstEnv[i];
				}
			}
		} else {
			for (int i = 0; i < n; i++) {
				if (i != rIdx) {
					if (clrDstEnv[i] == Obj.EMTY)
						clrDstEnv2[i] = Obj.EMTY;
					else
						clrDstEnv2[i] = Obj.ONLY_ESC;
				}
			}
		}
		DstNode clrDstNode2 = new DstNode(clrDstEnv2, clrDstHeap2, isKill
				|| clrDstNode.isKill, false);
		return new Edge(clrPE.srcNode, clrDstNode2);
	}

	public IWrappedPE<Edge, Edge> getEscEdge(Unit q) {
		eqv.escEdge = null;
		q.apply(eqv);
		return eqv.escEdge;
	}

	public Set<Unit> getLocs() {
		return currLocEs;
	}
	
	public Set<Unit> getEscs() {
		return currEscEs;
	}

	class EscQuadVisitor extends FGStmtSwitch{
		public IWrappedPE<Edge, Edge> escEdge = null;
		@Override
		public void caseArrayStoreStmt(ArrayRef dest, soot.Value src ) { findEscEdge(this.statement, dest.getBase()); }
		@Override
		public void caseArrayLoadStmt(Local dest, ArrayRef src ) {
			findEscEdge(this.statement, src.getBase());
		}
		@Override
		public void caseLoadStmt(Local dest, InstanceFieldRef src) {
			findEscEdge(statement, src.getBase());
		}
		@Override
		public void caseStoreStmt(InstanceFieldRef dest, soot.Value src) {
			findEscEdge(statement, dest.getBase());
		}
		private void findEscEdge(Unit q, Value bx) {
			if (!(bx instanceof Local))
				throw new RuntimeException("Register operand expected!");
			Local bo = (Local)bx;
			int bIdx = getIdx(bo);
			Set<Edge> peSet = pathEdges.get(q);
			for (Edge pe : peSet) {
				Obj pts = pe.dstNode.env[bIdx];
				if (pts == Obj.ONLY_ESC || pts == Obj.BOTH) {
					Pair<Object, Edge> pair = new Pair<Object, Edge>(q, pe);
					escEdge = wpeMap.get(pair);
					assert (escEdge != null);
					return;
				}
			}
		}
	}

	class MyQuadVisitor extends FGStmtSwitch{
		DstNode iDstNode;
		DstNode oDstNode;

		@Override
		public void caseReturnStmt(Local ro) {
			Obj[] oEnv = null;
			if (ro.getType() instanceof RefLikeType) {
				int rIdx = getIdx(ro);
				Obj rPts = iDstNode.env[rIdx];
				oEnv = new Obj[] { rPts };
			}
			if (oEnv == null)
				oEnv = emptyRetEnv;
			oDstNode = new DstNode(oEnv, iDstNode.heap, iDstNode.isKill, true);
		}

        /*@Override
        //public void visitCheckCast(Quad q) {
            visitMove(q);
        }*/

		@Override
		public void caseCopyStmt(Local lo, Value ro) {
			Type t = lo.getType();
			if (!(t instanceof RefLikeType))
				return;
			Obj[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Obj olPts;
			if (ro instanceof Local) {
				int rIdx = getIdx((Local)ro);
				olPts = iEnv[rIdx];
			} else
				olPts = Obj.EMTY;
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap, iDstNode.isKill, false);
		}

		@Override
		public void casePhiStmt(Local lo, PhiExpr src) {
			Type t = lo.getType();
			if (t == null || ! (t instanceof RefLikeType))
				return;
			Obj[] iEnv = iDstNode.env;
			List<Value> ros = src.getValues();
			int n = ros.size();
			Obj olPts = Obj.EMTY;
			for (int i = 0; i < n; i++) {
				Value ro = ros.get(i);
				if (ro != null && ro instanceof Local) {
					int rIdx = getIdx((Local)ro);
					Obj rPts = iEnv[rIdx];
					olPts = getObj(olPts, rPts);
					if (!useBOTH && olPts == Obj.BOTH) {
						oDstNode = reset(iDstNode);
						return;
					}
				}
			}
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap, iDstNode.isKill, false);
		}

		@Override
		public void caseArrayLoadStmt(Local lo, ArrayRef src) {
			if (currLocEs.contains(statement))
				check(statement, src.getBase());
			if (!(src.getType() instanceof RefLikeType))
				return;
			Obj[] iEnv = iDstNode.env;
			ArraySet<FldObj> iHeap = iDstNode.heap;
			Value bo = src.getBase();
			assert (bo instanceof Local);
			int bIdx = getIdx((Local)bo);
			Obj bPts = iEnv[bIdx];
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Obj olPts = getPtsFromHeap(bPts, null, iHeap);
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iHeap, iDstNode.isKill, false);
		}

		@Override
		public void caseLoadStmt(Local lo, InstanceFieldRef src) {
			if (currLocEs.contains(statement))
				check(statement, src.getBase());
			SootField f = src.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			Obj[] iEnv = iDstNode.env;
			ArraySet<FldObj> iHeap = iDstNode.heap;
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Value bx = src.getBase();
			Obj olPts;
			if (bx instanceof Local) {
				Local bo = (Local) bx;
				int bIdx = getIdx(bo);
				Obj bPts = iEnv[bIdx];
				olPts = getPtsFromHeap(bPts, f, iHeap);
			} else
				olPts = Obj.EMTY;
			if (olPts == ilPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iHeap, iDstNode.isKill, false);
		}

		@Override
		public void caseArrayStoreStmt(ArrayRef dest, Value src) {
			if (currLocEs.contains(statement))
				check(statement, dest.getBase());
			if (!(dest.getType() instanceof RefLikeType))
				return;
			if (!(src instanceof Local))
				return;
			Local bo = (Local) dest.getBase();
			Local ro = (Local) src;
			Obj[] iEnv = iDstNode.env;
			int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
			if (rPts == Obj.EMTY)
				return;
			int bIdx = getIdx(bo);
			Obj bPts = iEnv[bIdx];
			if (bPts == Obj.EMTY)
				return;
			processWrite(bPts, rPts, null);
		}

		@Override
		public void caseStoreStmt(InstanceFieldRef dest, Value src) {
			if (currLocEs.contains(statement))
				check(statement, dest.getBase());
			SootField f = dest.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			if (!(src instanceof Local))
				return;
			Value bx = dest.getBase();
			if (!(bx instanceof Local))
				return;
			Obj[] iEnv = iDstNode.env;
			Local ro = (Local) src;
			int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
			if (rPts == Obj.EMTY)
				return;
			Local bo = (Local) bx;
			int bIdx = getIdx(bo);
			Obj bPts = iEnv[bIdx];
			if (bPts == Obj.EMTY)
				return;
			processWrite(bPts, rPts, f);
		}

		private void processWrite(Obj bPts, Obj rPts, SootField f) {
			if ((bPts == Obj.ONLY_ESC || bPts == Obj.BOTH)
					&& (rPts == Obj.ONLY_LOC || rPts == Obj.BOTH)) {
				oDstNode = reset(iDstNode);
				return;
			}
			if (bPts == Obj.ONLY_LOC || bPts == Obj.BOTH) {
				ArraySet<FldObj> iHeap = iDstNode.heap;
				int n = iHeap.size();
				ArraySet<FldObj> oHeap = null;
				for (int i = 0; i < n; i++) {
					FldObj fo = iHeap.get(i);
					if (fo.f == f) {
						boolean isLoc = fo.isLoc;
						boolean isEsc = fo.isEsc;
						Obj pts1 = getObj(isLoc, isEsc);
						Obj pts2 = getObj(pts1, rPts);
						boolean isLoc2 = isLoc(pts2);
						boolean isEsc2 = isEsc(pts2);
						if (isLoc == isLoc2 && isEsc == isEsc2)
							return;
						if (!useBOTH && pts2 == Obj.BOTH) {
							oDstNode = reset(iDstNode);
							return;
						}
						oHeap = new ArraySet<FldObj>(n);
						for (int j = 0; j < i; j++)
							oHeap.add(iHeap.get(j));
						FldObj fo2 = new FldObj(f, isLoc2, isEsc2);
						oHeap.add(fo2);
						for (int j = i + 1; j < n; j++)
							oHeap.add(iHeap.get(j));
						break;
					}
				}
				if (oHeap == null) {
					oHeap = new ArraySet<FldObj>(iHeap);
					boolean isLoc = isLoc(rPts);
					boolean isEsc = isEsc(rPts);
					FldObj fo = new FldObj(f, isLoc, isEsc);
					oHeap.add(fo);
				}
				oDstNode = new DstNode(iDstNode.env, oHeap, iDstNode.isKill, false);
			}
		}

		@Override
		public void caseGlobalStoreStmt(StaticFieldRef dest, Value rx) {
			SootField f = dest.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			if (!(rx instanceof Local))
				return;
			Obj[] iEnv = iDstNode.env;
			Local ro = (Local) rx;
			int rIdx = getIdx(ro);
			Obj rPts = iEnv[rIdx];
			if (rPts == Obj.ONLY_ESC || rPts == Obj.EMTY)
				return;
			oDstNode = reset(iDstNode);
		}

		@Override
		public void caseGlobalLoadStmt(Local lo, StaticFieldRef src) {
			SootField f = src.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			Obj[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			if (iEnv[lIdx] == Obj.ONLY_ESC)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = Obj.ONLY_ESC;
			oDstNode = new DstNode(oEnv, iDstNode.heap, iDstNode.isKill, false);
		}

		@Override
		public void caseNewStmt(Local vo, NewExpr e) {
			processAlloc(statement, vo);
		}

		@Override
		public void caseNewArrayStmt(Local vo, NewArrayExpr e) {
			processAlloc(statement, vo);
		}

		@Override
		public void caseNewMultiArrayStmt(Local vo, NewMultiArrayExpr e) {
			processAlloc(statement, vo);
		}

		private void processAlloc(Unit q, Local lo) {
			Obj[] iEnv = iDstNode.env;
			int lIdx = getIdx(lo);
			Obj ilPts = iEnv[lIdx];
			Obj olPts;
			if (!currHs.contains(q)) {
				olPts = Obj.ONLY_ESC;
			} else {
				olPts = Obj.ONLY_LOC;
			}
			if (ilPts == olPts)
				return;
			Obj[] oEnv = copy(iEnv);
			oEnv[lIdx] = olPts;
			oDstNode = new DstNode(oEnv, iDstNode.heap, iDstNode.isKill, false);
		}

		private void check(Unit q, Value bx) {
			if (!(bx instanceof Local))
				return;
			Local bo = (Local) bx;
			int bIdx = getIdx(bo);
			Obj pts = iDstNode.env[bIdx];
			if (pts == Obj.ONLY_ESC || pts == Obj.BOTH) {
				currLocEs.remove(q);
				currEscEs.add(q);
				if (currLocEs.size() == 0)
					throw new ThrEscException();
			}
		}
	}

	private static Obj getPtsFromHeap(Obj bPts, SootField f, ArraySet<FldObj> heap) {
		if (bPts == Obj.EMTY || bPts == Obj.ONLY_ESC)
			return Obj.ONLY_ESC;  // in newest version of forward transfer functions, N.f = E
		Obj pts = null;
		int n = heap.size();
		for (int i = 0; i < n; i++) {
			FldObj fo = heap.get(i);
			if (fo.f == f) {
				pts = getObj(fo.isLoc, fo.isEsc);
				break;
			}
		}
		if (pts == null) {
			return (bPts == Obj.ONLY_LOC) ? Obj.EMTY : Obj.ONLY_ESC;
		} else {
			return (bPts == Obj.ONLY_LOC) ? pts : getObj(pts, Obj.ONLY_ESC);
		}
	}

	private static DstNode reset(DstNode in) {
		Obj[] iEnv = in.env;
		int n = iEnv.length;
		boolean change = false;
		for (int i = 0; i < n; i++) {
			Obj pts = iEnv[i];
			if (pts != Obj.ONLY_ESC && pts != Obj.EMTY) {
				change = true;
				break;
			}
		}
		Obj[] oEnv;
		if (change) {
			oEnv = new Obj[n];
			for (int i = 0; i < n; i++) {
				Obj pts = iEnv[i];
				oEnv[i] = (pts == Obj.EMTY) ? Obj.EMTY : Obj.ONLY_ESC;
			}
		} else {
			if (in.isKill && in.heap.isEmpty())
				return in;
			oEnv = iEnv;
		}
		return new DstNode(oEnv, emptyHeap, true, false);
	}

	/*****************************************************************
	 * Frequently used functions
	 *****************************************************************/

	private int getIdx(soot.Value ro) {
		return iterAnalysis.getLocalIdx(ro);
	}

	private static Obj[] copy(Obj[] a) {
		int n = a.length;
		Obj[] b = new Obj[n];
		for (int i = 0; i < n; i++)
			b[i] = a[i];
		return b;
	}

	private static Obj getObj(boolean isLoc, boolean isEsc) {
		if (isLoc)
			return isEsc ? Obj.BOTH : Obj.ONLY_LOC;
		else
			return isEsc ? Obj.ONLY_ESC : Obj.EMTY;
	}

	private static Obj getObj(Obj o1, Obj o2) {
		if (o1 == o2)
			return o1;
		if (o1 == Obj.EMTY)
			return o2;
		if (o2 == Obj.EMTY)
			return o1;
		return Obj.BOTH;
	}

	private static boolean isLoc(Obj pts) {
		return pts == Obj.ONLY_LOC || pts == Obj.BOTH;
	}

	private static boolean isEsc(Obj pts) {
		return pts == Obj.ONLY_ESC || pts == Obj.BOTH;
	}

	/*****************************************************************
	 * Printing functions
	 *****************************************************************/

	public static String toString(Obj[] env) {
		String s = null;
		for (Obj o : env) {
			String x = toString(o);
			s = (s == null) ? x : (s + "," + x);
		}
		if (s == null)
			return "[]";
		return "[" + s + "]";
	}

	public static String toString(Obj o) {
		switch (o) {
		case EMTY:
			return "N";
		case ONLY_LOC:
			return "L";
		case ONLY_ESC:
			return "E";
		case BOTH:
			return "*";
		}
		assert (false);
		return null;
	}

}
