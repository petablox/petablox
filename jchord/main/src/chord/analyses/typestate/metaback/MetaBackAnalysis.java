package chord.analyses.typestate.metaback;

import java.util.Map;
import java.util.Set;
import java.util.Stack;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.escape.metaback.Alarm;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.project.analyses.metaback.dnf.Clause;
import chord.project.analyses.metaback.dnf.ClauseSizeCMP;
import chord.project.analyses.metaback.dnf.DNF;
import chord.project.analyses.metaback.dnf.Domain;
import chord.project.analyses.metaback.dnf.Variable;
import chord.project.analyses.rhs.BackTraceIterator;
import chord.project.analyses.rhs.IWrappedPE;
import chord.project.analyses.rhs.TimeoutException;
import chord.util.Timer;

/**
 * The meta-backward analysis of thread escape analysis
 * 
 * @author xin
 * 
 */
public class MetaBackAnalysis {
	private BackTraceIterator<Edge, Edge> backIter;
	private DNF errSuf; // The sufficient condition of error
	private DNF nc; // The necessary condition for proof
	private int retIdx; // Used to handle return instructions
	private IterTypeStateAnalysis iterAnalysis;
	private IWrappedPE<Edge, Edge> pre; // Previous edge when going backward
	private BackQuadVisitor qv; // The backward visitor, containing backward
								// transfer functions.
	private Stack<IWrappedPE<Edge, Edge>> callStack;
	private Set<Integer> trackedAps; // The parameter for forward analysis
	private DNF preSuf; // previous error sufficient condition, used for
						// debugging
	private IWrappedPE<Edge, Edge> queryWPE;
	private DomV domV;
	private DomH domH;
	private DomI domI;
	private Quad h;
	private CIPAAnalysis cipa;

	private static boolean DEBUG;
	private static boolean optimizeSumms;
	private static int errSufSize; // Num of disjuncts to keep in error
									// sufficient condition
	private static int timeout;
	private static boolean dnegation = true;
	private static boolean prune = true;
	private Alarm alarm;
	private Timer timer;

	public MetaBackAnalysis(IterTypeStateAnalysis iterAnalysis, DNF errSuf,
			BackTraceIterator<Edge, Edge> backIter, Set<Integer> trackedAps) {
		this.backIter = backIter;
		this.errSuf = errSuf;
		this.iterAnalysis = iterAnalysis;
		this.pre = null;
		this.qv = new BackQuadVisitor();
		this.callStack = new Stack<IWrappedPE<Edge, Edge>>();
		this.trackedAps = trackedAps;
		this.queryWPE = backIter.next();
		this.domH = iterAnalysis.domH();
		this.domV = iterAnalysis.domV();
		this.domI = iterAnalysis.domI();
		this.cipa = iterAnalysis.getCIPA();
	}

	private void checkTimeout() {
		if (timeout > 0 && alarm.isTimedOut()) {
			System.out.println("TIMED OUT");
			alarm.cancel();
			printRunTime(true);
			throw new TimeoutException();
		}
	}

	private void printRunTime(boolean isTimeOut) {
		timer.done();
		long inclusiveTime = timer.getInclusiveTime();
		int iIndex = iterAnalysis.domI().indexOf(queryWPE.getInst());
		int hIndex = iterAnalysis.domH().indexOf(queryWPE.getPE().h);
		System.out.println((isTimeOut ? "TIMED OUT " : "") + "BackwardTime: "
				+ iIndex + ", " + hIndex + " " + inclusiveTime);
		System.out.println(Timer.getTimeStr(inclusiveTime));
	}

	public DNF run() throws TimeoutException {
		if (optimizeSumms == true)
			throw new RuntimeException(
					"Currently the metaback analysis doesn't support optimized summaries");
		timer = new Timer("meta-back-timer");
		timer.init();
		if (timeout > 0) {
			alarm = new Alarm(timeout);
		}
		System.out.println("**************");
		System.out.println(queryWPE.getInst().toVerboseStr());
		System.out.println(queryWPE.getPE().h.toVerboseStr());
		checkInvoke(queryWPE);
		while (!notParamChangable(errSuf)&&!isFixed(errSuf) && backIter.hasNext()) {
			checkTimeout();
			IWrappedPE<Edge, Edge> wpe = backIter.next();
			if(h==null){
				h = wpe.getPE().h;
			}
			Inst inst = wpe.getInst();
			if (inst instanceof BasicBlock) {
				BasicBlock bb = (BasicBlock) inst;
				if (bb.isEntry() || bb.isExit()) {
					pre = wpe;
					if (DEBUG) {
						System.out.println(wpe);
					}
					if (bb.isEntry()) {
					} else {
						errSuf = increaseContext(errSuf); // Adjust the context
															// level of each
															// variable
					}
					continue;
				} else
					assert (bb.size() == 0);
			}
			preSuf = errSuf;
			errSuf = backTransfer(errSuf, wpe.getInst());
			if (DEBUG) {
				System.out.println(wpe.getInst());
				System.out.println("After trans: " + errSuf);
			}
			// An optimization, if errSuf remains unchanged, no need to do
			// double negation
			if (dnegation && !errSuf.equals(preSuf)
					&& errSuf.size() > errSufSize)
				errSuf = negate(negate(errSuf));
			if (DEBUG) {
				System.out.println("After double negation: " + errSuf);
			}
			Clause fwdState = encodePathEdge(wpe, errSuf);
			if (DEBUG) {
				System.out.println("Forward state: " + fwdState);
				System.out.println(wpe.getPE());
			}
			if (prune)
				errSuf = errSuf.prune(errSufSize, fwdState); // Drop clauses to
																// prevent
																// blowing up
			if (DEBUG)
				System.out.println("After prune:" + errSuf);
			pre = wpe;
			checkInvoke(wpe);
		}
		if(notParamChangable(errSuf)){
			errSuf = DNF.getTrue(errSuf.getCMP());
		}
		if (errSuf.isFalse()) {
			dump();
			throw new RuntimeException("Something wrong with meta back!");
			// nc = DNF.getTrue();
		} else if (errSuf.isTrue()){
			System.out.println(pre.getInst());
			nc = DNF.getFalse(new ClauseSizeCMP());
		}
		else {
			errSuf = chopNonParameter(errSuf);
			nc = negate(errSuf);
		}
		System.out.println("NC: " + nc.toString());
		printRunTime(false);
		if (timeout > 0)
			alarm.cancel();
		return nc;
	}

	private boolean notParamChangable(DNF dnf){
		if (dnf.isTrue() || dnf.isFalse())
			return true;
		for (Clause c : dnf.getClauses()) {
			for (Map.Entry<Variable, Domain> e : c.getLiterals().entrySet()) {
				if (e.getKey() instanceof TSPVariable)
						return false;
				if (e.getKey() instanceof TSVVariable)
					return false;
				if(e.getKey() instanceof TSVariable)
					return false;
			}
		}
		return true;
	}
	
	private DNF negate(DNF dnf) {
		if (dnf.isTrue())
			return DNF.getFalse(dnf.getCMP());
		if (dnf.isFalse())
			return DNF.getTrue(dnf.getCMP());
		DNF nDNF = new DNF(dnf.getCMP(), true);
		for (Clause c : dnf.getClauses()) {
			// create a false DNF, since we're going to do join
			DNF cDNF = new DNF(dnf.getCMP(), false);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				checkTimeout();
				TSBoolDomain v;
				if (TSBoolDomain.T().equals(entry.getValue()))
					v = TSBoolDomain.F();
				else
					v = TSBoolDomain.T();
				DNF hDNF = new DNF(dnf.getCMP(), entry.getKey(), v);
				cDNF = cDNF.join(hDNF);
			}
			nDNF = nDNF.intersect(cDNF);
		}
		return nDNF;
	}

	private void dump() {
		System.out
				.println("=====================dump out current state======================");
		System.out.println(preSuf.toString());
		System.out.println(pre.getInst());
		System.out.println(pre);
		System.out.println("====dump out the stack====");
		for (int j = callStack.size() - 1; j >= 0; j--) {
			IWrappedPE<Edge, Edge> wpe = callStack.get(j);
			System.out.println(wpe);
		}
		throw new RuntimeException();
	}

	private DNF increaseContext(DNF dnf) {
		DNF nDNF = new DNF(dnf.getCMP(), false);
		for (Clause c : dnf.getClauses()) {
			Clause tnc = new Clause(true);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof TSVVariable) {
					TSVVariable tv = (TSVVariable) entry.getKey();
					tnc.addLiteral(tv.getIncreased(), entry.getValue());
				} else
					tnc.addLiteral(entry.getKey(), entry.getValue());
			}
			nDNF.addClause(tnc);
		}
		return nDNF;
	}

	private DNF decreaseContext(DNF dnf) {
		DNF nDNF = new DNF(dnf.getCMP(), false);
		for (Clause c : dnf.getClauses()) {
			Clause tnc = new Clause(true);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof TSVVariable) {
					TSVVariable tv = (TSVVariable) entry.getKey();
					tnc.addLiteral(tv.getDecreased(), entry.getValue());
				} else
					tnc.addLiteral(entry.getKey(), entry.getValue());
			}
			nDNF.addClause(tnc);
		}
		return nDNF;

	}

	private DNF killDeadConstraints(DNF dnf) {
		DNF nDNF = new DNF(errSuf.getCMP(), false);
		for (Clause c : dnf.getClauses()) {
			Clause tnc = new Clause(true);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof TSVVariable) {
					TSVVariable vv = (TSVVariable) entry.getKey();
					if (vv.getContext() < 0) {
						if (!TSBoolDomain.F().equals(entry.getValue()))
							throw new RuntimeException("Only track notInMS.");
					} else
						tnc.addLiteral(entry.getKey(), entry.getValue());
				} else
					tnc.addLiteral(entry.getKey(), entry.getValue());
			}
			nDNF.addClause(tnc);
		}
		return nDNF;
	}

	private DNF chopNonParameter(DNF dnf) {
		if (dnf.isFalse() || dnf.isTrue())
			return dnf;
		DNF ret = new DNF(dnf.getCMP(), false);
		OUT: for (Clause c : dnf.getClauses()) {
			Clause rc = new Clause();
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof TSEVariable) {
					if (entry.getValue() == TSBoolDomain.T())
						continue OUT;
					else
						continue;
				}
				if (entry.getKey() instanceof TSVariable)
					continue OUT;
				if (entry.getValue() == TSBoolDomain.T())
					throw new RuntimeException(
							"Only notInMS and notParam are tracked.");
				if (entry.getKey() instanceof TSPVariable)
					rc.addLiteral(entry.getKey(), entry.getValue());
			}
			ret.addClause(rc);
		}
		return ret;
	}

	/**
	 * Check whether current instruction is an instruction after a method invoke
	 * 
	 * @param wpe
	 */
	private void checkInvoke(IWrappedPE<Edge, Edge> wpe) {
		if (wpe.getWSE() != null) { // if wse!=null, wpe is a path edge
			// immediately after a method call
			IWrappedPE<Edge, Edge> cwpe = wpe.getWPE();
			Inst inst = cwpe.getInst();
			if (inst instanceof BasicBlock) { // an empty basic block
				assert ((BasicBlock) inst).size() == 0;
				return;
			}
			Quad q = (Quad) inst;
			RegisterOperand ro = Invoke.getDest(q);
			retIdx = -1;
			if (ro != null && ro.getType().isReferenceType()) {
				retIdx = domV.indexOf(ro.getRegister());
			}
			callStack.push(cwpe);
		}
	}

	/**
	 * Get the weakest precondition of es over inst
	 * 
	 * @param es
	 * @param inst
	 * @return
	 */
	private DNF backTransfer(DNF es, Inst inst) {
		if (inst instanceof BasicBlock) {
			BasicBlock bb = (BasicBlock) inst;
			// bb might be entry, exit, or empty basic block
			assert (bb.size() == 0);
			return es;
		}
		qv.h = h;
		qv.iDNF = es;
		qv.oDNF = es;
		Quad q = (Quad) inst;
		q.accept(qv);
		return qv.oDNF;
	}

	// dnf is fixed when it does not contain constraint over Full edges
	public boolean isFixed(DNF dnf) {
		if (dnf.isTrue() || dnf.isFalse())
			return true;
		for (Clause c : dnf.getClauses()) {
			for (Map.Entry<Variable, Domain> e : c.getLiterals().entrySet()) {
				if (e.getKey() instanceof TSEVariable)
					if (e.getValue() == TSBoolDomain.T())
						return false;
			}
		}
		return true;
	}

	// Here we encode the forward state lazily, only encode the part relative to
	// the backward DNF
	private Clause encodePathEdge(IWrappedPE<Edge, Edge> wpe, DNF dnf) {
		Clause ret = new Clause(true);
		AbstractState dNode = wpe.getPE().dstNode;
		for (Clause c : dnf.getClauses())
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				Variable v = entry.getKey();
				if (v instanceof TSVVariable && dNode!=null) {
					TSVVariable vv = (TSVVariable) v;
					int cl = vv.getContext();
					if (0 == cl) {
						ret.addLiteral(vv,
								TSBoolDomain.objToValue(dNode.ms, vv));
					} else {
						int stackSize = callStack.size();
						IWrappedPE<Edge, Edge> uWPE = callStack.get(stackSize
								- cl);
						AbstractState uNode = uWPE.getPE().dstNode;
						TSBoolDomain tv;
						if(uNode==null)
							tv = TSBoolDomain.F();
						else
						    tv = TSBoolDomain.objToValue(uNode.ms,vv);
						ret.addLiteral(vv, tv);
					}
				}
				if(v instanceof TSPVariable){
					TSPVariable pv = (TSPVariable)v;
					if(trackedAps.contains(pv.getIdx()))
						ret.addLiteral(pv, TSBoolDomain.T());
					else
						ret.addLiteral(pv, TSBoolDomain.F());
				}
				if(v instanceof TSVariable&&dNode!=null){
					ret.addLiteral(v, TSBoolDomain.get(!dNode.isError));
				}
				if(v instanceof TSEVariable){
					ret.addLiteral(v, TSBoolDomain.get(dNode!=null));
				}
			}
		if (ret.isTrue()) {
			return new Clause(false);
		}
		if (ret.isFalse())
			throw new RuntimeException("How could it be?");
		return ret;
	}

	public static boolean isDEBUG() {
		return DEBUG;
	}

	public static void setDEBUG(boolean dEBUG) {
		DEBUG = dEBUG;
	}

	public static boolean isOptimizeSumms() {
		return optimizeSumms;
	}

	public static void setOptimizeSumms(boolean optimizeSumms) {
		MetaBackAnalysis.optimizeSumms = optimizeSumms;
	}

	public static int getTimeout() {
		return timeout;
	}

	public static void setTimeout(int timeout) {
		MetaBackAnalysis.timeout = timeout;
	}

	public static int getErrSufSize() {
		return errSufSize;
	}

	public static void setErrSufSize(int size) {
		errSufSize = size;
	}

	public static void setDNegation(boolean dn) {
		dnegation = dn;
	}

	public static void setPrune(boolean p) {
		prune = p;
	}

	class BackQuadVisitor extends QuadVisitor.EmptyVisitor {
		DNF iDNF;
		DNF oDNF;
		Quad h;

		/**
		 * Invoke is like a group of moves before the method call. Also, we need
		 * to change the typestate here
		 */
		@Override
		public void visitInvoke(Quad obj) {
			oDNF = iDNF;
			if (iterAnalysis.isSkippedMethod(obj)) {
				return;
			}
			ParamListOperand args = Invoke.getParamList(obj);
			if (!callStack.empty()) {
				IWrappedPE<Edge, Edge> wpe = callStack.pop();
				if (!wpe.getInst().equals(obj))
					throw new RuntimeException("Unmatch invoke!"
							+ wpe.getInst() + " " + obj);
			}
			int numArgs = args.length();
			jq_Method m = pre.getInst().getMethod();
			RegisterFactory rf = m.getCFG().getRegisterFactory();
			// HashSet<Integer> liveVs = new HashSet<Integer>();
			oDNF = decreaseContext(oDNF);
			for (int i = 0; i < numArgs; i++) {
				RegisterOperand ro = args.get(i);
				if (ro.getType().isReferenceType()) {
					int fromIdx = domV.indexOf(ro.getRegister());
					Register r = rf.get(i);
					int toIdx = domV.indexOf(r);
					oDNF = processMove(fromIdx, toIdx, oDNF, 1);
					// liveVs.add(fromIdx);
				}
			}
			if (iterAnalysis.isInterestingMethod(m,h,obj)) {
				Register r0 = Invoke.getParam(obj, 0).getRegister();;
				if (iterAnalysis.mayPointTo(r0, h))
					oDNF = processStateChange(oDNF, r0);
			}
			oDNF = killDeadConstraints(oDNF);
		}

		/**
		 * Return is like a move statement after the method call
		 */
		@Override
		public void visitReturn(Quad q) {
			if (retIdx == -1)
				return;
			int fromIdx = -1;
			if (!(q.getOperator() instanceof THROW_A)) {
				Operand rx = Return.getSrc(q);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					if (ro.getType().isReferenceType()) {
						fromIdx = domV.indexOf(ro.getRegister());
					}
				}
			}
			oDNF = processMove(fromIdx, retIdx, iDNF, -1);
		}

		@Override
		public void visitCheckCast(Quad q) {
			visitMove(q);
		}

		@Override
		public void visitMove(Quad q) {
			RegisterOperand lo = Move.getDest(q);
			jq_Type t = lo.getType();
			if (!t.isReferenceType())
				return;
			int toIdx = domV.indexOf(lo.getRegister());
			int fromIdx = -1;
			Operand rx = Move.getSrc(q);
			if (rx instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) rx;
				fromIdx = domV.indexOf(ro.getRegister());
			}
			oDNF = processMove(fromIdx, toIdx, iDNF, 0);
		}

		private DNF processStateChange(DNF dnf, Register r0) {
			DNF ret = new DNF(new ClauseSizeCMP(), false);
			for (Clause c : dnf.getClauses()) {
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
						.entrySet()) {
					checkTimeout();
					if (Helper.mayPointsTo(r0, h, cipa)
							&& entry.getKey() instanceof TSVariable) {
						if (!entry.getValue().equals(TSBoolDomain.F()))
							throw new RuntimeException(
									"Only error state should be tracked in the backward analysis.");
						DNF dnf11 = new DNF(new ClauseSizeCMP(),
								entry.getKey(), entry.getValue());
						DNF dnf12 = new DNF(new ClauseSizeCMP(),
								new TSVVariable(r0, domV), TSBoolDomain.F());
						DNF dnf1 = dnf11.join(dnf12);
						cDNF = cDNF.intersect(dnf1);
					} else {
						DNF dnf1 = new DNF(new ClauseSizeCMP(), entry.getKey(),
								entry.getValue());
						cDNF = cDNF.intersect(dnf1);
					}
				}
				ret = ret.join(cDNF);
			}
			return ret;
		}

		/**
		 * A common helper method for move like statement. fromIdx is set to -1
		 * if the src operand is N. <br>
		 * 
		 * @param fromIdx
		 * @param toIdx
		 * @param ftContextDif
		 *            , use to handle method parameters and return value.
		 *            ftContextDif = from.context-to.context <br>
		 *            Normal Move:0<br>
		 *            args: 1 <br>
		 *            return: -1
		 */
		private DNF processMove(int fromIdx, int toIdx, DNF dnf,
				int ftContextDif) {
			DNF ret = new DNF(new ClauseSizeCMP(), false);
			TSVVariable rv = new TSVVariable(toIdx, iterAnalysis.domV());
			if (ftContextDif == 1)
				rv = rv.getDecreased();
			if (ftContextDif == -1)
				rv = rv.getIncreased();
			OUT: for (Clause c : dnf.getClauses()) {
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);// Construct a
																// true dnf
				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
						.entrySet()) {
					if (rv.equals(entry.getKey())) {
						if (fromIdx == -1) {
							if (entry.getValue().equals(TSBoolDomain.F())) // tracking
								// notInMS(v)
								continue;
							else {
								// This clause is evaluated to be false, since
								// after tv = null, tv cannot be in the ms
								continue OUT;
							}
						} else {
							DNF dnf11 = new DNF(new ClauseSizeCMP(),
									new TSVVariable(fromIdx, domV),
									entry.getValue());
							DNF dnf12 = new DNF(new ClauseSizeCMP(),
									new TSPVariable(toIdx, domV),
									entry.getValue());
							DNF dnf1 = dnf11.join(dnf12);
							cDNF = cDNF.intersect(dnf1);
						}
					} else {
						DNF dnf1 = new DNF(new ClauseSizeCMP(),
								entry.getKey(),
								entry.getValue()); // unaffected
						// clauses
						cDNF = cDNF.intersect(dnf1);
					}
				}
				ret = ret.join(cDNF);
			}

			return ret;
		}

		@Override
		public void visitPhi(Quad q) {
			System.out.println(System.getProperty("chord.ssa.kind"));
			throw new RuntimeException("PHI is not supported!");
		}

		@Override
		public void visitALoad(Quad q) {
			oDNF = iDNF;
			Operator op = q.getOperator();
			if (!((ALoad) op).getType().isReferenceType())
				return;
			RegisterOperand dest = ALoad.getDest(q);
			int toIdx = domV.indexOf(dest.getRegister());
			oDNF = processMove(-1, toIdx, iDNF, 0);
			if(oDNF.isTrue())
				System.out.println("M513Because of fields.");
		}

		@Override
		public void visitGetfield(Quad q) {
			oDNF = iDNF;
			jq_Field f = q.getField();
			if (!f.getType().isReferenceType())
				return;
			RegisterOperand dest = Getfield.getDest(q);
			int toIdx = domV.indexOf(dest.getRegister());
			oDNF = processMove(-1, toIdx, iDNF, 0);
			if(oDNF.isTrue())
				System.out.println("M513Because of fields.");
		}

		@Override
		public void visitAStore(Quad q) {
			oDNF = iDNF;
		}

		@Override
		public void visitPutfield(Quad q) {
			oDNF = iDNF;
		}

		/**
		 * Pay extra attention to this backward transfer function, since
		 * disjuncts are produced
		 */
		@Override
		public void visitPutstatic(Quad q) {
			oDNF = iDNF;
		}

		@Override
		public void visitGetstatic(Quad q) {
			oDNF = iDNF;
			jq_Field f = Getstatic.getField(q).getField();
			if (!f.getType().isReferenceType())
				return;
			RegisterOperand ro = Getstatic.getDest(q);
			int toIdx = domV.indexOf(ro.getRegister());
			oDNF = processMove(-1, toIdx, iDNF, 0);
			if(oDNF.isTrue())
				System.out.println("M512Because of global.");
		}

		@Override
		public void visitNew(Quad q) {
			RegisterOperand ro = New.getDest(q);
			int toIdx = domV.indexOf(ro.getRegister());
			if (q.equals(h))
				oDNF = processAlloc(iDNF, q, toIdx);
			else
				oDNF = processMove(-1, toIdx, iDNF, 0);
		}

		@Override
		public void visitNewArray(Quad q) {
			RegisterOperand ro = NewArray.getDest(q);
			int toIdx = domV.indexOf(ro.getRegister());
			oDNF = processMove(-1, toIdx, iDNF, 0);
		}

		@Override
		public void visitMultiNewArray(Quad q) {
			RegisterOperand ro = New.getDest(q);
			int toIdx = domV.indexOf(ro.getRegister());
			oDNF = processMove(-1, toIdx, iDNF, 0);
		}

		/**
		 * A common method to handle allocation statements
		 * 
		 * @param q
		 * @param toIdx
		 */
		private DNF processAlloc(DNF dnf, Quad q, int toIdx) {
			DNF ret = new DNF(new ClauseSizeCMP(), false);
			TSVVariable rv = new TSVVariable(toIdx, iterAnalysis.domV());
			OUT: for (Clause c : iDNF.getClauses()) {
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
						.entrySet()) {
					if (cDNF.isFalse())
						continue OUT;
					if (entry.getKey() instanceof TSEVariable) {
						if (entry.getValue() == TSBoolDomain.F()) {
							DNF dnf1 = new DNF(new ClauseSizeCMP(),
									entry.getKey(), entry.getValue());
							cDNF = cDNF.intersect(dnf1);
						} else {
							DNF dnf11 = new DNF(new ClauseSizeCMP(),
									entry.getKey(), TSBoolDomain.T());
							DNF dnf12 = new DNF(new ClauseSizeCMP(),
									entry.getKey(), TSBoolDomain.F());
							cDNF = cDNF.intersect(dnf11.join(dnf12));
						}
						continue;
					}
					if (entry.getKey() instanceof TSVariable) {
						DNF dnf11 = new DNF(new ClauseSizeCMP(),
								entry.getKey(), entry.getValue());
						DNF dnf12 = new DNF(new ClauseSizeCMP(),
								TSEVariable.getSingleton(), TSBoolDomain.T());
						DNF dnf1 = dnf11.intersect(dnf12);
						if (entry.getValue() == TSBoolDomain.T()) {
							DNF dnf2 = new DNF(new ClauseSizeCMP(),
									TSEVariable.getSingleton(),
									TSBoolDomain.F());
							dnf1 = dnf1.join(dnf2);
						}
						cDNF = cDNF.intersect(dnf1);
						continue;
					}
					if (entry.getKey() instanceof TSVVariable) {
						if (entry.getValue() == TSBoolDomain.T())
							throw new RuntimeException(
									"Only track notInMS and notParam!");
						if (entry.getKey().equals(rv)) {
							DNF dnf1 = new DNF(new ClauseSizeCMP(),
									TSEVariable.getSingleton(),
									TSBoolDomain.T());
							DNF dnf21 = new DNF(new ClauseSizeCMP(),
									TSEVariable.getSingleton(),
									TSBoolDomain.F());
							DNF dnf22 = new DNF(new ClauseSizeCMP(),
									new TSPVariable(toIdx, domV),
									TSBoolDomain.F());
							DNF dnf2 = dnf21.intersect(dnf22);
							cDNF = cDNF.intersect(dnf1.join(dnf2));
						} else {
							DNF dnf1 = new DNF(new ClauseSizeCMP(),
									TSEVariable.getSingleton(),
									TSBoolDomain.F());
							DNF dnf21 = new DNF(new ClauseSizeCMP(),
									TSEVariable.getSingleton(),
									TSBoolDomain.T());
							DNF dnf22 = new DNF(new ClauseSizeCMP(),
									new TSPVariable(toIdx, domV),
									TSBoolDomain.F());
							DNF dnf2 = dnf21.intersect(dnf22);
							cDNF = cDNF.intersect(dnf1.join(dnf2));
						}
						continue;
					}
					cDNF = cDNF.intersect(new DNF(new ClauseSizeCMP(), entry
							.getKey(), entry.getValue()));
				}
				ret = ret.join(cDNF);
			}
			if(ret.isTrue())
				System.out.println("M1024Because of 0-CFA?");
			return ret;
		}
	}
}
