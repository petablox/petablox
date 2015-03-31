package chord.analyses.mustalias.tdbu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.typestate.AbstractState;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.Edge;
import chord.analyses.typestate.EdgeKind;
import chord.program.Loc;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

public class PrePassAnalysis extends RHSAnalysis<Edge, Edge> {
	private static boolean DEBUG = false;
	private CIPAAnalysis cipa;
	private ICICG cicg;
	private Set<Quad> trackedSites;
	private Set<Quad> checkIncludedI;
	private MyQuadVisitor qv = new MyQuadVisitor();
	private Set<jq_Method> rmsFrRoots;

	protected String cipaName, cicgName;
	private boolean isInit;
	private final static boolean DEFAULT_MAY = true;
	private final static boolean DEFAULT_RET = false;

	public PrePassAnalysis(Set<Quad> trackedSites,Set<jq_Method> rmsFrRoots) {
		this.trackedSites = trackedSites;
		this.rmsFrRoots = rmsFrRoots;
	}

	@Override
	public void init() {
		// XXX: do not compute anything here which needs to be re-computed on
		// each call to run() below.

		if (isInit)
			return;
		isInit = true;
		cicg = MustAliasHybridAnalysis.cicg;
		cipa = MustAliasHybridAnalysis.cipa;
		super.init();

		{
			checkIncludedI = new HashSet<Quad>();

			ClassicProject.g().runTask("checkIncludedI-dlog");
			ProgramRel relI = (ProgramRel) ClassicProject.g().getTrgt(
					"checkIncludedI");
			relI.load();
			Iterable<Quad> tuples = relI.getAry1ValTuples();
			for (Quad q : tuples)
				checkIncludedI.add(q);
			relI.close();
		}
	}

	@Override
	public void run() {
		init();
		runPass();
		if (DEBUG)
			print();
		done();
	}

	public Set<jq_Method> getNoFullSummsMethods(){
		Set<jq_Method> allMs = cicg.getNodes();
		Set<jq_Method> ret = new HashSet<jq_Method>();
		out:for(jq_Method m:allMs){
			Set<Edge> ses = summEdges.get(m);
			if(ses == null||ses.size() == 0){
				ret.add(m);
				continue;
			}
			for(Edge se:ses)
				if(se.type == EdgeKind.FULL)
					continue out;
			ret.add(m);
		}
		return ret;
	}
	
	@Override
	public ICICG getCallGraph() {
		return cicg;
	}

	/*
	 * For each reachable method 'm' adds the following path edges: 1. <null,
	 * null, null> 2. for each tracked alloc site 'h' in the body of 'm': <null,
	 * h, null>
	 */
	@Override
	public Set<Pair<Loc, Edge>> getInitPathEdges() {
		Set<Pair<Loc, Edge>> initPEs = new ArraySet<Pair<Loc, Edge>>();
		Map<jq_Method, Loc> methToEntry = new HashMap<jq_Method, Loc>();
		for (jq_Method m : cicg.getNodes()) {
			if(!rmsFrRoots.contains(m))
				continue;
			BasicBlock bb = m.getCFG().entry();
			Loc loc = new Loc(bb, -1);
			methToEntry.put(m, loc);
			Pair<Loc, Edge> pair = new Pair<Loc, Edge>(loc, Edge.NULL);
			if (DEBUG)
				System.out.println("getInitPathEdges: Added " + pair);
			initPEs.add(pair);
		}
		Quad dh = null;
		if(trackedSites.size() > 0)
		dh = trackedSites.iterator().next();
		for (Quad q : trackedSites) {
			Edge edge = new Edge(null, null, EdgeKind.ALLOC, dh);
			jq_Method m = q.getMethod();
			if(!rmsFrRoots.contains(m))
				continue;
			Loc loc = methToEntry.get(m);
			if (loc == null) {
				// ignore allocs in methods unreachable from 0cfa call graph
				continue;
			}
			Pair<Loc, Edge> pair = new Pair<Loc, Edge>(loc, edge);
			if (DEBUG)
				System.out.println("getInitPathEdges: Added " + pair);
			initPEs.add(pair);
		}
		if (DEBUG) {
			System.out.println("===== ENTER ALL QUERIES");
			for (Pair<Loc, Edge> pair : initPEs) {
				System.out.println(pair);
			}
			System.out.println("===== LEAVE ALL QUERIES");
		}
		return initPEs;
	}

	/*
	 * If incoming path edge 'pe' is of the form <null, null, null> or <null, h,
	 * null>, or if the target method is threadSart, then do nothing: return
	 * null edge.
	 * 
	 * If incoming path edge 'pe' is of the form <null, h, AS> or <AS', h, AS>
	 * then create and return new path edge in callee of the form <AS1, h, AS2>
	 * where AS1 and AS2 are as follows: type-state of AS1 = type-state of AS
	 * type-state of AS2 = type-state of AS if this method is non-interesting,
	 * and the appropriate transitioned state otherwise. must-set of AS1 =
	 * must-set of AS2 = subset of must-set of AS consisting of two kinds of
	 * access paths: those of the form v.* where v is an actual argument (now
	 * replaced by the corresponding formal argument), and those of the form g.*
	 * where g is a static field.
	 */
	@Override
	public Edge getInitPathEdge(Quad q, jq_Method m, Edge pe) {
		if (DEBUG)
			System.out.println("ENTER getInitPathEdge: q=" + q + " m=" + m
					+ " pe=" + pe);
		if (pe == Edge.NULL
				|| (pe.type == EdgeKind.ALLOC && pe.dstNode == null)) /*
																	 * || m ==
																	 * threadStartMethod
																	 * )
																	 */{
			if (DEBUG)
				System.out.println("LEAVE getInitPathEdge: " + Edge.NULL);
			return Edge.NULL;
		}
		// if(pe.dstNode!=null)
		// if(pe.dstNode.ms.isEmpty()){
		// Set<Edge> peSet = pathEdges.get(m.getCFG().entry());
		// if(peSet!=null)
		// for(Edge pe1:peSet)
		// if(pe1.srcNode!=null&&pe1.srcNode.ms.isEmpty())
		// return pe1;
		// }
		AbstractState oldDst = pe.dstNode;
		assert (oldDst != null);
		ArraySet<AccessPath> newMS = AliasUtilities.handleParametersTD(
				oldDst.ms, q, m);

		AbstractState newSrc = new AbstractState(oldDst.may, oldDst.ts, newMS);
		AbstractState newDst = new AbstractState(oldDst.may, oldDst.ts, newMS);
		Edge newEdge = new Edge(newSrc, newDst, EdgeKind.FULL, pe.h);
		if (DEBUG)
			System.out.println("LEAVE getInitPathEdge: " + newEdge);
		return newEdge;
	}

	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		if (DEBUG)
			System.out.println("ENTER getMiscPathEdge: q=" + q + " pe=" + pe);
		if (pe == Edge.NULL)
			return pe;
		qv.istate = pe.dstNode;
		qv.ostate = pe.dstNode;
		qv.h = pe.h;
		// may modify only qv.ostate
		q.accept(qv);
		// XXX: DO NOT REUSE incoming PE (merge does strong updates)
		Edge newEdge = new Edge(pe.srcNode, qv.ostate, pe.type, pe.h);
		if (DEBUG)
			System.out.println("LEAVE getMiscPathEdge: ret=" + newEdge);
		return newEdge;
	}
	
	/**
	 * If target method is threadStart, then only matching summary edge tgtSE is
	 * the null edge, in which case (a copy of) the incoming path edge clrPE is
	 * returned.
	 */
	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		if (DEBUG)
			System.out.println("ENTER getInvkPathEdge: q=" + q + " clrPE="
					+ clrPE + " m=" + m + " tgtSE=" + tgtSE);
		switch (clrPE.type) {
		case NULL:
			switch (tgtSE.type) {
			case NULL:
				if (DEBUG)
					System.out.println(Edge.NULL);
				if (DEBUG)
					System.out.println("LEAVE getInvkPathEdge: " + Edge.NULL);
				return Edge.NULL;
			case FULL:
				if (DEBUG)
					System.out.println("LEAVE getInvkPathEdge: null");
				return null;
			case ALLOC:
				if (tgtSE.dstNode == null) {
					if (DEBUG)
						System.out.println("LEAVE getInvkPathEdge: null");
					return null;
				}
			}
			break;
		case ALLOC:
			switch (tgtSE.type) {
			case NULL:
				if (clrPE.dstNode == null) {
					if (DEBUG)
						System.out
								.println("LEAVE getInvkPathEdge: incoming clrPE");
					return getCopy(clrPE);
				}
				if (DEBUG)
					System.out.println("LEAVE getInvkPathEdge: null");
				return null;
			case FULL:
				if (clrPE.dstNode == null || clrPE.h != tgtSE.h) {
					if (DEBUG)
						System.out.println("LEAVE getInvkPathEdge: null");
					return null;
				}
				// postpone check for equality of clrPE.dstNode.ms and
				// tgtSE.srcNode.ms
				break;
			case ALLOC:
				if (DEBUG)
					System.out.println("LEAVE getInvkPathEdge: null");
				return null;
			}
			break;
		case FULL:
			switch (tgtSE.type) {
			case FULL:
				if (clrPE.h != tgtSE.h) {
					if (DEBUG)
						System.out.println("LEAVE getInvkPathEdge: null");
					return null;
				}
				// postpone check for equality of clrPE.dstNode.ms and
				// tgtSE.srcNode.ms
				break;
			default:
				if (DEBUG)
					System.out.println("LEAVE getInvkPathEdge: null");
				return null;
			}
		}

		// At this point, we have one of the following three cases:
		// clrPE tgtSE condition
		// ============================================================
		// FULL:<AS1,h,AS2> FULL:<AS3,h,AS4> AS2.ts==AS3.ts (need ms equality
		// check below)
		// ALLOC:<null,h,AS1> FULL:<AS2,h,AS3> AS1.ts==AS2.ts (need ms equality
		// check below)
		// NULL:<null,null,null> ALLOC:<null,h,AS> None (need to generate
		// suitable ms)

		ArraySet<AccessPath> srcMS = new ArraySet<AccessPath>();
		ParamListOperand args = Invoke.getParamList(q);
		RegisterFactory rf = m.getCFG().getRegisterFactory();

		if (clrPE.type == EdgeKind.ALLOC || clrPE.type == EdgeKind.FULL) {
			srcMS = AliasUtilities.handleParametersTD(clrPE.dstNode.ms, q, m);
			if (!tgtSE.srcNode.ms.equals(srcMS)) {
				if (DEBUG)
					System.out
							.println("LEAVE getInvkPathEdge: null (must sets don't match)");
				return null;
			}
		}

		ArraySet<AccessPath> newMS = new ArraySet<AccessPath>(tgtSE.dstNode.ms);

		AbstractState newDst = new AbstractState(DEFAULT_MAY, tgtSE.dstNode.ts,
				newMS);
		EdgeKind newType = (clrPE.type == EdgeKind.NULL) ? EdgeKind.ALLOC
				: clrPE.type;
		Edge newEdge = new Edge(clrPE.srcNode, newDst, newType, tgtSE.h);
		if (DEBUG)
			System.out.println("LEAVE getInvkPathEdge: " + newEdge);
		return newEdge;
	}

	@Override
	public Edge getPECopy(Edge pe) {
		return getCopy(pe);
	}

	@Override
	public Edge getSECopy(Edge se) {
		return getCopy(se);
	}

	private Edge getCopy(Edge pe) {
		if (DEBUG)
			System.out.println("Called Copy with: " + pe);
		return (pe == Edge.NULL) ? pe : new Edge(pe.srcNode, pe.dstNode,
				pe.type, pe.h);
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		if (DEBUG)
			System.out.println("\nCalled getSummaryEdge: m=" + m + " pe=" + pe);
		return getCopy(pe);
	}


	public class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		public AbstractState istate; // immutable, may be null
		public AbstractState ostate; // mutable, initially ostate == istate
		public Quad h; // immutable, non-null

		@Override
		public void visitPhi(Quad q) {
			throw new RuntimeException("Get rid of Phi in the SSA config!");
		}

		@Override
		public void visitNew(Quad q) {
			if (istate == null) {
				// edge is ALLOC:<null, h, null>; check if q == h
				if (trackedSites.contains(q)) {
					ArraySet<AccessPath> newMS = new ArraySet<AccessPath>();
					ostate = new AbstractState(true, null, newMS);
				}
			}
		}
	}
}
