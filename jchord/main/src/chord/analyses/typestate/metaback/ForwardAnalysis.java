package chord.analyses.typestate.metaback;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.ICICG;
import chord.analyses.var.DomV;
import chord.program.Loc;
import chord.project.analyses.rhs.IWrappedPE;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.project.analyses.rhs.TimeoutException;
import chord.util.ArraySet;
import chord.util.Timer;
import chord.util.tuple.object.Pair;

/**
 * System properties: 1. File specifying type-state spec:
 * chord.typestate.specfile (default value: [chord.work.dir]/typestatespec.txt)
 * 2. Max number of instance fields in any access path in any must set tracked
 * chord.typestate.maxdepth (default value: 6) 3. Alloc sites to exclude from
 * queries chord.check.exclude (default value: JDK libraries)
 */

public class ForwardAnalysis extends RHSAnalysis<Edge, Edge> {
	private static boolean DEBUG = false;
	private ICICG cicg;
	private CIPAAnalysis cipa;
	private DomV domV;
	private Set<Quad> trackedSites;
	private Set<Pair<Quad,Quad>> queries;
	private Set<Pair<Quad,Quad>> errQueries;
	private MyQuadVisitor qv = new MyQuadVisitor();
	private ArraySet<Register> trackedAPs;
	private boolean isTimeOut = false;
	// protected jq_Method threadStartMethod;
	protected String cipaName, cicgName;
	private boolean isInit;
	private IterTypeStateAnalysis iterAnalysis;

	public ForwardAnalysis(IterTypeStateAnalysis iterAnalysis){
		this.iterAnalysis = iterAnalysis;
	}
	
	public void setQueries(Set<Pair<Quad,Quad>> queries){
		this.queries = new HashSet<Pair<Quad,Quad>>(queries);
		errQueries = new HashSet<Pair<Quad,Quad>>();
		trackedSites = new HashSet<Quad>();
		for(Pair<Quad,Quad> pair:queries)
			trackedSites.add(pair.val1);
	}
	
	public void setQueries(List<TSQuery> queryList){
		this.queries = new HashSet<Pair<Quad,Quad>>();
		for(TSQuery tsq : queryList)
			this.queries.add(new Pair<Quad,Quad>(tsq.getI(),tsq.getH()));
		errQueries = new HashSet<Pair<Quad,Quad>>();
		trackedSites = new HashSet<Quad>();
		for(Pair<Quad,Quad> pair:this.queries)
			trackedSites.add(pair.val1);
	}
	
	public void setCIPA(CIPAAnalysis cipa){
		this.cipa = cipa;
	}
	
	public void setCICG(ICICG cicg) {
		this.cicg = cicg;
	}

	public void setDomV(DomV domV) {
		this.domV = domV;
	}

	public void setTrackedAPs(ArraySet<Register> taps) {
		this.trackedAPs = taps;
	}
	
	public void setTrackedAPs(Set<Integer> taps){
		this.trackedAPs = new ArraySet<Register>();
		for(Integer i:taps)
			trackedAPs.add(domV.get(i));
	}

	@Override
	public void init() {
		// XXX: do not compute anything here which needs to be re-computed on
		// each call to run() below.

		if (isInit)
			return;
		isInit = true;

		super.init();
	}

	@Override
	public void run() {
		init();
		Timer timer = new Timer("forward-timer");
		timer.init();
		try{
		System.out.println("Run forward analysis. Queries: ");
		for(Pair<Quad,Quad> pair:queries)
			System.out.println(pair);
		System.out.println("Tracked Variables: ");
		for(Register r:trackedAPs){
			System.out.println(r);
		}
		runPass();
		}catch(AllQueryAnsweredException e){
			
		}
		catch(TimeoutException ex){
			isTimeOut = true;
			System.out.println("TIMED OUT");
		}
		timer.done();
		System.out.println(timer.getInclusiveTimeStr());
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
				|| (pe.type == EdgeKind.ALLOC && pe.dstNode == null)) {
			if (DEBUG)
				System.out.println("LEAVE getInitPathEdge: " + Edge.NULL);
			return Edge.NULL;
		}
		AbstractState oldDst = pe.dstNode;
		assert (oldDst != null);
		AbstractState newSrc = getInitState(pe.h,pe.dstNode,q,m);
		AbstractState newDst = new AbstractState(newSrc.isError, newSrc.ms);
		Edge newEdge = new Edge(newSrc, newDst, EdgeKind.FULL, pe.h);
		if (DEBUG)
			System.out.println("LEAVE getInitPathEdge: " + newEdge);
		return newEdge;
	}

	private AbstractState getInitState(Quad h,AbstractState abs,Quad invoke,jq_Method m){
		ArraySet<Register> oldMS = abs.ms;
		ArraySet<Register> newMS = new ArraySet<Register>();
		ParamListOperand args = Invoke.getParamList(invoke);
		RegisterFactory rf = m.getCFG().getRegisterFactory();
		boolean isError = abs.isError;
		if(iterAnalysis.isInterestingMethod(m,h,invoke)){
			Register thisR = Invoke.getParam(invoke, 0).getRegister();
			if(Helper.mayPointsTo(thisR, h, cipa)&&!oldMS.contains(thisR))
				isError = true;
		}
		
		for (int i = 0; i < args.length(); i++) {
			Register actualReg = args.get(i).getRegister();
			Register formalReg = rf.get(i);
			if(oldMS.contains(actualReg))
				newMS = addAccessPath(newMS,formalReg);
		}

		AbstractState newAbs = new AbstractState(isError, newMS);
		return newAbs;
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

	@Override
	protected void processInvk(Loc loc, Edge pe) {
		if (pe.dstNode != null) {
			Quad h = pe.h;
			Quad i = (Quad) loc.i;
			Pair<Quad, Quad> query = new Pair<Quad, Quad>(i, h);
			if(pe.dstNode!=null){
				if (queries.contains(query)&&pe.dstNode.isError) {
					queries.remove(query);
					errQueries.add(query);
					if(queries.isEmpty())
						throw new AllQueryAnsweredException();
				}
			}
		}
		super.processInvk(loc, pe);
	}

	public Set<Pair<Quad,Quad>> getErrQueries(){
		return errQueries;
	}
	
	public Set<Pair<Quad,Quad>> getProvenQueries(){
		return queries;
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
				}else{
					Quad h = tgtSE.h;
					if(!Helper.mayStoredInField(h, cipa)){
						RegisterOperand ro = Invoke.getDest(q);
						if(ro == null)
							return null;
						Register r = ro.getRegister();
						if(!r.getType().isReferenceType())
							return null;
						if(!Helper.mayPointsTo(r, h, cipa))
							return null;
					}
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
				if (clrPE.dstNode == null || clrPE.h != tgtSE.h
						) {
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
				if (clrPE.h != tgtSE.h ) {
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

		ArraySet<Register> newMS = null;

		if (clrPE.type == EdgeKind.ALLOC || clrPE.type == EdgeKind.FULL) {
			// Compare must sets; they should be equal in order to apply summary
			// Build this must set tmpMS in two steps
			AbstractState tmpAbs = this.getInitState(clrPE.h, clrPE.dstNode, q, m);
			if (!tgtSE.srcNode.equals(tmpAbs)) {
				if (DEBUG)
					System.out
							.println("LEAVE getInvkPathEdge: null (must sets or type state don't match)");
				return null;
			}
		}
		
		if(clrPE.dstNode!=null&&clrPE.dstNode.ms != null)
		newMS = new ArraySet<Register>(clrPE.dstNode.ms);
		else
			newMS = new ArraySet<Register>();

		Register tgtRetReg = (Invoke.getDest(q) != null) ? Invoke.getDest(q)
				.getRegister() : null;
		if(tgtRetReg!=null){
			if(tgtSE.dstNode.canReturn)
				newMS = addAccessPath(newMS,tgtRetReg);
			else
				newMS.remove(tgtRetReg);
		}
		AbstractState newDst = new AbstractState(tgtSE.dstNode.isError, newMS);
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

	public IWrappedPE<Edge, Edge> getErrWPE(Quad qi, Quad qh){
		Set<Edge> pes = pathEdges.get(qi);
		for(Edge e: pes){
			if(e.h == qh){
				if(e.dstNode!=null&&e.dstNode.isError){
					Pair<Inst,Edge> key = new Pair<Inst,Edge>(qi,e);
					IWrappedPE<Edge, Edge> wpe = this.wpeMap.get(key);
					return wpe;
				}
			}
		}
		throw new RuntimeException("Cannot find the error wpe requested. Check if there's something wrong.");
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

	private ArraySet<Register> handleMove(ArraySet<Register> ms, Register from, Register to){
		ArraySet<Register> ret = new ArraySet<Register>(ms);
		ret.remove(to);
		if(from!=null&&ms.contains(from))
			ret = addAccessPath(ms,to);
		return ret;
	}
	
	private ArraySet<Register> addAccessPath(ArraySet<Register> ms, Register r){
		if(trackedAPs.contains(r)&&!ms.contains(r)){
			ArraySet<Register> ret = new ArraySet<Register>(ms);
			ret.add(r);
			return ret;
		}
		return ms;
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
			ArraySet<Register> oldMS = istate.ms;
			Register srcR = null;
			if (Move.getSrc(q) instanceof RegisterOperand) {
				srcR = ((RegisterOperand) Move.getSrc(q)).getRegister();
			}
			ArraySet<Register> newMS = handleMove(oldMS, srcR, dstR);
			if (newMS != null)
				ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
		}

		@Override
		public void visitPhi(Quad q) {
			throw new RuntimeException("Get rid of Phi in the SSA config!");
		}

		@Override
		public void visitNew(Quad q) {
			if (istate == null) {
				// edge is ALLOC:<null, h, null>; check if q == h
				if (h == q && trackedSites.contains(q)) {
					ArraySet<Register> newMS = new ArraySet<Register>();
					Register dstR = New.getDest(q).getRegister();
					newMS = addAccessPath(newMS,dstR);
					ostate = new AbstractState(false,newMS);
				}
			} else {
				// edge is ALLOC:<null, h, AS> or FULL:<AS', h, AS>
				Register dstR = New.getDest(q).getRegister();
				ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
				ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
			}
		}

		@Override
		public void visitNewArray(Quad q) {
			if (istate == null)
				return;
			Register dstR = NewArray.getDest(q).getRegister();
			ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
			ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
		}

		@Override
		public void visitMultiNewArray(Quad q) {
			if (istate == null)
				return;
			Register dstR = MultiNewArray.getDest(q).getRegister();
			ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
			ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
		}

		@Override
		public void visitALoad(Quad q) {
			if (istate == null)
				return;
			Register dstR = ALoad.getDest(q).getRegister();
			ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
			ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
		}

		@Override
		public void visitGetstatic(Quad q) {
			if (istate == null)
				return;
			jq_Field srcF = Getstatic.getField(q).getField();
			if (srcF.getType().isReferenceType()) {
				Register dstR = Getstatic.getDest(q).getRegister();
				ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
				ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
			}
		}

		@Override
		public void visitPutstatic(Quad q) {
		}

		@Override
		public void visitPutfield(Quad q) {
			return;
		}

		@Override
		public void visitGetfield(Quad q) {
			if (istate == null)
				return;
			Register dstR = Getfield.getDest(q).getRegister();
			if (dstR.getType().isReferenceType()) {
				ArraySet<Register> newMS = handleMove(istate.ms,null,dstR);
				ostate = new AbstractState(istate.isError, newMS, istate.canReturn);
			}
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
				if(istate.ms.contains(tgtR))
				ostate = new AbstractState(istate.isError, istate.ms, true);
			}
		}
	}

	public boolean isTimeOut() {
		return isTimeOut;
	}
}
