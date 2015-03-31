package chord.analyses.mustalias.tdbu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.analyses.alloc.DomH;
import chord.analyses.method.DomM;
import chord.analyses.typestate.AbstractState;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.Edge;
import chord.analyses.typestate.EdgeKind;
import chord.analyses.typestate.GlobalAccessPath;
import chord.analyses.typestate.Helper;
import chord.analyses.typestate.RegisterAccessPath;
import chord.program.Loc;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.project.analyses.tdbu.TopDownAnalysis;
import chord.util.ArraySet;
import chord.util.Timer;
import chord.util.tuple.object.Pair;

/**
 * The top-down must-alias analysis, modified from the typestate analysis System
 * properties: 1. File specifying type-state spec: chord.typestate.specfile
 * (default value: [chord.work.dir]/typestatespec.txt) 2. Max number of instance
 * fields in any access path in any must set tracked chord.typestate.maxdepth
 * (default value: 6) 3. Alloc sites to exclude from queries chord.check.exclude
 * (default value: JDK libraries)
 */

public class MustAliasTopDownAnalysis extends
		TopDownAnalysis<Edge, Edge, MustAliasBUEdge, MustAliasBUEdge> {

	private static boolean DEBUG = false;
	private CIPAAnalysis cipa;
	private ICICG cicg;
	private Set<Quad> trackedSites;
	private Set<Quad> checkIncludedI;
	private MyQuadVisitor qv = new MyQuadVisitor();
	private Set<Pair<Quad, Quad>> allQueries;
	private Set<Pair<Quad, Quad>> errQueries;
	// protected jq_Method threadStartMethod;
	public static int maxDepth;
	protected String cipaName, cicgName;
	private boolean isInit;
	private boolean jumpEmpty;
	private boolean buAllMethods;
	private Map<jq_Method,Edge> emptyEdgeMap;
	private final static boolean DEFAULT_MAY = true;
	private final static boolean DEFAULT_RET = false;
	private Quad qh = null;
	private Set<jq_Method> rmsFrRoots;

	private Timer timer;

	public MustAliasTopDownAnalysis(int tdLimit, boolean autoAdjustBU,
			boolean jumpEmpty, boolean buAllMethods, Set<Quad> trackedSites, Set<jq_Method> rmsFrRoots) {
		super(tdLimit, autoAdjustBU);
		this.jumpEmpty = jumpEmpty;
		this.buAllMethods = buAllMethods;
		this.trackedSites = trackedSites;
		this.emptyEdgeMap = new HashMap<jq_Method,Edge>();
		this.rmsFrRoots = rmsFrRoots;
	}

	@Override
	public void init() {
		// XXX: do not compute anything here which needs to be re-computed on
		// each call to run() below.

		if (isInit)
			return;
		isInit = true;

		maxDepth = Integer.getInteger("chord.mustalias.maxdepth", 2);
		assert (maxDepth >= 0);


		cicg = MustAliasHybridAnalysis.cicg;
		cipa = MustAliasHybridAnalysis.cipa;

		super.init();
		if(trackedSites.size()>0)
		qh = trackedSites.iterator().next();

		if (!buAllMethods) {
			ClassicProject.g().runTask("checkExcludedM-dlog");
			ProgramRel relCheckExcludedM = (ProgramRel) ClassicProject.g()
					.getTrgt("checkExcludedM");
			relCheckExcludedM.load();
			DomM domM = (DomM) ClassicProject.g().getTrgt("M");
			ClassicProject.g().runTask(domM);
			Set<jq_Method> libMethods = new HashSet<jq_Method>();
			for (jq_Method m : domM) {
				if (relCheckExcludedM.contains(m))
					libMethods.add(m);
			}
			super.setBUMethods(libMethods);
		}

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
		allQueries = this.getAllQueries();
		errQueries = new HashSet<Pair<Quad, Quad>>();
		timer = new Timer("hybrid-mustalias-java");
	}

	@Override
	public void run() {
		init();
		timer.init();
		runPass();
		timer.done();
		long inclusiveTime = timer.getInclusiveTime();
		System.out.println("TDBU Total running time: "
				+ Timer.getTimeStr(inclusiveTime));
		if (DEBUG)
			print();
		done();
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
		for (Quad q : trackedSites) {
			Edge edge = new Edge(null, null, EdgeKind.ALLOC, q);
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
		Quad h = pe.h;
		if(this.jumpEmpty&&pe.dstNode!=null)
			if(pe.dstNode.ms.isEmpty()){
				Edge entryEdge = this.emptyEdgeMap.get(m);
				if(entryEdge != null)
					return entryEdge;
			}
		AbstractState oldDst = pe.dstNode;
		assert (oldDst != null);
		ArraySet<AccessPath> newMS = AliasUtilities.handleParametersTD(
				oldDst.ms, q, m);

		AbstractState newSrc = new AbstractState(oldDst.may, oldDst.ts, newMS);
		AbstractState newDst = new AbstractState(oldDst.may, oldDst.ts, newMS);
		Edge newEdge = new Edge(newSrc, newDst, EdgeKind.FULL, h);
		if (DEBUG)
			System.out.println("LEAVE getInitPathEdge: " + newEdge);
		if(this.jumpEmpty&&pe.dstNode!=null)
			if(pe.dstNode.ms.isEmpty()){
				this.emptyEdgeMap.put(m, newEdge);
			}
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
	 * 
	 * Here's how the thing works:
	 * 1. Treat parameter passing as move statements
	 * 2. Apply the summary
	 * 3. Treat return as a move statement
	 * 4. Kill return register 
	 */
	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		if (DEBUG)
			System.out.println("ENTER getInvkPathEdge: q=" + q + " clrPE="
					+ clrPE + " m=" + m + " tgtSE=" + tgtSE);
		/*
		 * if (m == threadStartMethod) { if (tgtSE == Edge.NULL) return
		 * getCopy(clrPE); return null; }
		 */
		if (this.jumpEmpty&&tgtSE.type == EdgeKind.FULL) {
			if (tgtSE.srcNode.ms.isEmpty()) {
				if (clrPE.dstNode != null)
					if (clrPE.dstNode.ms.isEmpty())
						return getCopy(clrPE);
			}
		}
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

		 Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q)
		 .getRegister() : null;
		 if (tgtRetReg != null) {
			 newMS = AliasUtilities.killRegisterTD(newMS, tgtRetReg);
			 ArraySet<AccessPath> newMS1 = new ArraySet<AccessPath>();
			 for (AccessPath ap : newMS) {
				 if (ap instanceof RegisterAccessPath) {
					 RegisterAccessPath rap = (RegisterAccessPath) ap;
					 if (rap.isRet) {
						 newMS1.add(new RegisterAccessPath(tgtRetReg, rap.fields));
					 } else
						 newMS1.add(ap);
				 } else
					 newMS1.add(ap);
			 }
			 newMS = newMS1;
		 }
		 newMS = AliasUtilities.killRetRegisterTD(newMS);
		AbstractState newDst = new AbstractState(DEFAULT_MAY, tgtSE.dstNode.ts,
				newMS);
		EdgeKind newType = (clrPE.type == EdgeKind.NULL) ? EdgeKind.ALLOC
				: clrPE.type;
		Edge newEdge = new Edge(clrPE.srcNode, newDst, newType, tgtSE.h);
		if (DEBUG)
			System.out.println("LEAVE getInvkPathEdge: " + newEdge);
		return newEdge;
	}

	// // Refactored into a method to enable overloading later on
	// public boolean addFallThroughAccessPaths(Quad q, Edge clrPE, jq_Method m,
	// Edge tgtSE, ArraySet<AccessPath> newMS, ArraySet<AccessPath> clrMS) {
	// newMS.addAll(clrMS);
	// return(Helper.removeModifiableAccessPaths(methodToModFields.get(m),
	// newMS));
	// }

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
		if (pe.dstNode == null)
			return pe;
		ArraySet<AccessPath> newMS = AliasUtilities.killLocalRegisterTD(
				pe.dstNode.ms, m);
		AbstractState newDst = new AbstractState(pe.dstNode.may, pe.dstNode.ts,
				newMS);
		Edge newEdge = new Edge(pe.srcNode, newDst, pe.type, pe.h);
		return newEdge;
		// return getCopy(pe);
	}

	public Set<Pair<Quad, Quad>> getAllQueries() {
		if (this.allQueries == null) {
			allQueries = new HashSet<Pair<Quad, Quad>>();
			for (Quad q : checkIncludedI) {
				if (isInterestingSite(q.getOperator())) {
					Register v = Invoke.getParam(q, 0).getRegister();
					for (Quad h : cipa.pointsTo(v).pts) {
						if (trackedSites.contains(h)) {
							allQueries.add(new Pair<Quad, Quad>(q, h));
						}
					}
				}
			}
		}
		return allQueries;
	}

	public Set<Pair<Quad, Quad>> getErrQueries() {
		return errQueries;
	}

	public Set<Pair<Quad, Quad>> getProvedQueries() {
		Set<Pair<Quad, Quad>> ret = new HashSet<Pair<Quad, Quad>>(allQueries);
		ret.removeAll(errQueries);
		return ret;
	}

	private static boolean isInterestingSite(Operator o) {
		return o instanceof Invoke && !(o instanceof InvokeStatic);
	}

	@Override
	protected void processInvk(Loc loc, Edge pe) {
		checkQuery(loc, pe);
		super.processInvk(loc, pe);
	}

	@Override
	protected void processInstState(Loc val0, Edge val1) {
		checkQuery(val0, val1);
	}

	private void checkQuery(Loc loc, Edge edge) {
		if (edge.dstNode != null) {
			AbstractState dstState = edge.dstNode;
			Quad h = edge.h;
			Quad i = (Quad) loc.i;
			Pair<Quad, Quad> query = new Pair<Quad, Quad>(i, h);
			if (allQueries.contains(query)) {
				Register v = Invoke.getParam(i, 0).getRegister();
				if (Helper.getIndexInAP(dstState.ms, v) < 0) {
					errQueries.add(query);
				}
			}
		}
	}

	@Override
	protected Set<Edge> hack(Loc loc, Edge pe) {
		if (jumpEmpty && pe.dstNode != null && pe.dstNode.ms.isEmpty()) {
			ArraySet<Edge> ret = new ArraySet<Edge>();
			ret.add(getCopy(pe));
			return ret;
		}
		return null;
	}

	public class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		public AbstractState istate; // immutable, may be null
		public AbstractState ostate; // mutable, initially ostate == istate
		public Quad h; // immutable, non-null

		@Override
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}

		@Override
		public void visitMove(Quad q) {
			if (istate == null)
				return; // edge is ALLOC:<null, h, null>
			// edge is ALLOC:<null, h, AS> or FULL:<AS', h, AS>
			Register dstR = Move.getDest(q).getRegister();
			ArraySet<AccessPath> oldMS = istate.ms;
			ArraySet<AccessPath> newMS = Helper.removeReference(oldMS, dstR);
			if (Move.getSrc(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Move.getSrc(q))
						.getRegister();
				for (int i = -1; (i = Helper.getPrefixIndexInAP(oldMS, srcR, i)) >= 0;) {
					if (newMS == null)
						newMS = new ArraySet<AccessPath>(oldMS);
					newMS.add(new RegisterAccessPath(dstR, oldMS.get(i).fields));
				}
			}
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitPhi(Quad q) {
			throw new RuntimeException("Get rid of Phi in the SSA config!");
			// if (istate == null)
			// return; // edge is ALLOC:<null, h, null>
			// // edge is ALLOC:<null, h, AS> or FULL:<AS', h, AS>
			// Register dstR = Phi.getDest(q).getRegister();
			// ArraySet<AccessPath> oldMS = istate.ms;
			// ArraySet<AccessPath> newMS = Helper.removeReference(oldMS, dstR);
			// ParamListOperand ros = Phi.getSrcs(q);
			// int n = ros.length();
			// for (int i = 0; i < n; i++) {
			// RegisterOperand ro = ros.get(i);
			// if (ro == null)
			// continue;
			// Register srcR = ((RegisterOperand) ro).getRegister();
			// for (int j = -1; (j = Helper.getIndexInAP(oldMS, srcR, j)) >= 0;)
			// {
			// if (newMS == null)
			// newMS = new ArraySet<AccessPath>(oldMS);
			// newMS.add(new RegisterAccessPath(dstR, oldMS.get(j).fields));
			// }
			// }
			// if (newMS != null)
			// ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitNew(Quad q) {
			if (istate == null) {
				// edge is ALLOC:<null, h, null>; check if q == h
				if (h == q && trackedSites.contains(q)) {
					ArraySet<AccessPath> newMS = new ArraySet<AccessPath>(1);
					Register dstR = New.getDest(q).getRegister();
					newMS.add(new RegisterAccessPath(dstR));
					ostate = new AbstractState(true, null, newMS);
				}
			} else {
				// edge is ALLOC:<null, h, AS> or FULL:<AS', h, AS>
				Register dstR = New.getDest(q).getRegister();
				ArraySet<AccessPath> newMS = Helper.removeReference(istate.ms,
						dstR);
				if (newMS != null)
					ostate = new AbstractState(istate.may, istate.ts, newMS);
			}
		}

		@Override
		public void visitNewArray(Quad q) {
			if (istate == null)
				return;
			Register dstR = NewArray.getDest(q).getRegister();
			ArraySet<AccessPath> newMS = Helper
					.removeReference(istate.ms, dstR);
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitMultiNewArray(Quad q) {
			if (istate == null)
				return;
			Register dstR = MultiNewArray.getDest(q).getRegister();
			ArraySet<AccessPath> newMS = Helper
					.removeReference(istate.ms, dstR);
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitALoad(Quad q) {
			if (istate == null)
				return;
			Register dstR = ALoad.getDest(q).getRegister();
			ArraySet<AccessPath> newMS = Helper
					.removeReference(istate.ms, dstR);
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitGetstatic(Quad q) {
			if (istate == null)
				return;
			Register dstR = Getstatic.getDest(q).getRegister();
			jq_Field srcF = Getstatic.getField(q).getField();
			ArraySet<AccessPath> oldMS = istate.ms;
			ArraySet<AccessPath> newMS = Helper.removeReference(oldMS, dstR);
			for (int i = -1; (i = Helper.getPrefixIndexInAP(oldMS, srcF, i)) >= 0;) {
				if (newMS == null)
					newMS = new ArraySet<AccessPath>(oldMS);
				newMS.add(new RegisterAccessPath(dstR, oldMS.get(i).fields));
			}
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitPutstatic(Quad q) {
			if (istate == null)
				return;
			jq_Field dstF = Putstatic.getField(q).getField();
			ArraySet<AccessPath> oldMS = istate.ms;
			ArraySet<AccessPath> newMS = Helper.removeReference(oldMS, dstF);
			if (Putstatic.getSrc(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Putstatic.getSrc(q))
						.getRegister();
				for (int i = -1; (i = Helper.getPrefixIndexInAP(oldMS, srcR, i)) >= 0;) {
					if (newMS == null)
						newMS = new ArraySet<AccessPath>(oldMS);
					newMS.add(new GlobalAccessPath(dstF, oldMS.get(i).fields));
				}
			}
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		/*
		 * Outgoing APmust' is computed as follows: Step 1: Capture effect of
		 * v.f = null, by removing all e.f.y from APmust such that may-alias(e,
		 * v) Step 2: Capture effect of v.f = u as follows: Step 2.1: Whenever
		 * APmust contains u.y, add v.f.y Step 2.2: Remove any access paths
		 * added in Step 2.1 that exceed access-path-depth parameter.
		 * 
		 * Outgoing may-bit is computed as follows:(Checking if all aliased
		 * paths are present in the ms is redundant since Step 1 deletes all
		 * aliased paths from the ms) If incoming may-bit is true, then it
		 * remains true. But if it is false, then: Case 1: If nothing is removed
		 * in Step 1 and Step 2.2, then it remains false. Case 2: If something
		 * is removed in Step 2.2, then clearly it becomes true. Case 3: If
		 * nothing is removed in Step 2.2, and only v.f is removed in Step 1,
		 * then may-bit remains false. Case 4: If nothing is removed in Step
		 * 2.2, and something other than v.f is removed in Step 1, then may-bit
		 * becomes true. Case 5: If nothing is removed in Step 1, but there
		 * exists at least one 'e' such that v aliases with e, then may-bit
		 * becomes true.
		 */
		@Override
		public void visitPutfield(Quad q) {
			if (istate == null)
				return;
			if (!(Putfield.getBase(q) instanceof RegisterOperand))
				return;

			boolean deleteAlias = false;
			boolean deleteDepthExceed = false;
			boolean deleteSelf = false;

			Register dstR = ((RegisterOperand) Putfield.getBase(q))
					.getRegister();
			jq_Field dstF = Putfield.getField(q).getField();
			ArraySet<AccessPath> oldMS = istate.ms;
			ArraySet<AccessPath> newMS = null;
			for (AccessPath ap : oldMS) {
				if (Helper.mayPointsTo(ap, dstR, dstF, cipa)) {
					if (newMS == null)
						newMS = new ArraySet<AccessPath>(oldMS);
					newMS.remove(ap);
				}
			}

			if (newMS != null) {
				ArrayList<jq_Field> definedField = new ArrayList<jq_Field>();
				definedField.add(dstF);
				RegisterAccessPath definedAP = new RegisterAccessPath(dstR,
						definedField);
				if (newMS.size() != oldMS.size() - 1
						|| !oldMS.contains(definedAP))
					deleteAlias = true;
				if (oldMS.contains(definedAP))
					deleteSelf = true;
			}

			if (Putfield.getSrc(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Putfield.getSrc(q))
						.getRegister();
				for (int i = -1; (i = Helper.getPrefixIndexInAP(oldMS, srcR, i)) >= 0;) {
					AccessPath oldAP = oldMS.get(i);
					if (oldAP.fields.size() == maxDepth) {
						deleteDepthExceed = true;
						continue;
					}
					List<jq_Field> fields = new ArrayList<jq_Field>();
					fields.add(dstF);
					fields.addAll(oldAP.fields);
					if (newMS == null)
						newMS = new ArraySet<AccessPath>(oldMS);
					newMS.add(new RegisterAccessPath(dstR, fields));
				}
			}
			if (newMS != null) {
				// if (istate.may)
				ostate = new AbstractState(DEFAULT_MAY, istate.ts, newMS);
				// else {
				// // boolean may = Helper.isAliasMissing(newMS, dstR, dstF,
				// // cipa);
				// boolean may = deleteAlias || deleteDepthExceed;
				// if (!may && !deleteSelf)
				// may = Helper.doesAliasExist(dstR, cipa);
				// ostate = new AbstractState(may, istate.ts, newMS);
				// }
			}
		}

		@Override
		public void visitGetfield(Quad q) {
			if (istate == null)
				return;
			Register dstR = Getfield.getDest(q).getRegister();
			ArraySet<AccessPath> oldMS = istate.ms;
			ArraySet<AccessPath> newMS = Helper.removeReference(oldMS, dstR);
			if (Getfield.getBase(q) instanceof RegisterOperand) {
				Register srcR = ((RegisterOperand) Getfield.getBase(q))
						.getRegister();
				jq_Field srcF = Getfield.getField(q).getField();
				// when stmt is x=y.f, we add x.* if y.f.* is in the must set
				for (int i = -1; (i = Helper.getPrefixIndexInAP(oldMS, srcR,
						srcF, i)) >= 0;) {
					List<jq_Field> fields = new ArrayList<jq_Field>(
							oldMS.get(i).fields);
					fields.remove(0);
					if (newMS == null)
						newMS = new ArraySet<AccessPath>(oldMS);
					newMS.add(new RegisterAccessPath(dstR, fields));
				}
			}
			if (newMS != null)
				ostate = new AbstractState(istate.may, istate.ts, newMS);
		}

		@Override
		public void visitReturn(Quad q) {
			if (istate == null)
				return; // edge is ALLOC:<null, h, null>
			// edge is ALLOC:<null, h, AS> or FULL:<AS', h, AS>
			if (q.getOperator() instanceof THROW_A)
				return;
			if (Return.getSrc(q) instanceof RegisterOperand) {
				Register tgtR = ((RegisterOperand) (Return.getSrc(q)))
						.getRegister();
				ArraySet<AccessPath> newMS = new ArraySet<AccessPath>(istate.ms);
				for (AccessPath ap : newMS) {
					if (ap instanceof RegisterAccessPath) {
						RegisterAccessPath rap = (RegisterAccessPath) ap;
						if (rap.var.equals(tgtR))
							rap.isRet = true;
					}
				}
				ostate = new AbstractState(istate.ts, newMS, DEFAULT_RET,
						istate.may);
			}
		}
	}

}
