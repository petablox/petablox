package petablox.analyses.escape.metaback;

import java.util.*;

import petablox.analyses.escape.hybrid.full.DstNode;
import petablox.analyses.escape.hybrid.full.Edge;
import petablox.analyses.escape.hybrid.full.FldObj;
import petablox.project.analyses.metaback.dnf.Clause;
import petablox.project.analyses.metaback.dnf.ClauseSizeCMP;
import petablox.project.analyses.metaback.dnf.DNF;
import petablox.project.analyses.metaback.dnf.Domain;
import petablox.project.analyses.metaback.dnf.Variable;
import petablox.project.analyses.parallelizer.ParallelAnalysis;
import petablox.project.analyses.rhs.BackTraceIterator;
import petablox.project.analyses.rhs.IWrappedPE;
import petablox.project.analyses.rhs.TimeoutException;
import petablox.util.Timer;
import petablox.util.soot.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;

/**
 * The meta-backward analysis of thread escape analysis
 * 
 * @author xin
 * 
 */
public class MetaBackAnalysis {
	private BackTraceIterator<Edge, Edge> backIter;
	private DNF errSuf;  // The sufficient condition of error
	private DNF nc;  // The necessary condition for proof
	private int retIdx;  // Used to handle return instructions
	private IterThrEscAnalysis iterAnalysis;
	private IWrappedPE<Edge, Edge> pre;  // Previous edge when going backward
	private BackQuadVisitor qv;  // The backward visitor, containing backward transfer functions.
	private Stack<IWrappedPE<Edge, Edge>> callStack;
	private Set<Integer> Ls;  // The parameter for forward analysis
	private DNF preSuf;  // previous error sufficient condition, used for debugging
	private IWrappedPE<Edge, Edge> queryWPE;
	
	private static boolean DEBUG;
	private static boolean optimizeSumms;
	private static int errSufSize;  // Num of disjuncts to keep in error sufficient condition
	private static int timeout;
	private static boolean dnegation=true;
	private static boolean prune=true;
	private Alarm alarm;
	private Timer timer;
	
	public MetaBackAnalysis(IterThrEscAnalysis iterAnalysis, DNF errSuf,
			BackTraceIterator<Edge, Edge> backIter, 
			Set<Integer> Ls) {
		this.backIter = backIter;
		this.errSuf = errSuf;
		this.iterAnalysis = iterAnalysis;
		this.pre = null;
		this.qv = new BackQuadVisitor();
		this.callStack = new Stack<IWrappedPE<Edge, Edge>>();
		this.Ls = Ls;
		this.queryWPE = backIter.next();
	}

	private void checkTimeout(){
		if (timeout > 0 && alarm.isTimedOut()) {
            System.out.println("TIMED OUT");
            alarm.cancel();
            printRunTime(true);
            throw new TimeoutException();
        }
	}
	
	private void printRunTime(boolean isTimeOut){
        timer.done();
        long inclusiveTime = timer.getInclusiveTime();
        int queryIndex = iterAnalysis.domE().indexOf(queryWPE.getInst());
        System.out.println((isTimeOut?"TIMED OUT ":"")+"BackwardTime: "+queryIndex+" "+inclusiveTime);
		System.out.println(Timer.getTimeStr(inclusiveTime));
	}
	
	public DNF run() throws TimeoutException{
		if (optimizeSumms == true)
			throw new RuntimeException("Currently the metaback analysis doesn't support optimized summaries");
		timer = new Timer("meta-back-timer");
		timer.init();
		if (timeout > 0) {
            alarm = new Alarm(timeout);
        }
		System.out.println("**************");
		System.out.println(SootUtilities.toVerboseStrInst(queryWPE.getInst()));
		checkInvoke(queryWPE);
		while (!isFixed(errSuf) && backIter.hasNext()) {
			checkTimeout();
			IWrappedPE<Edge, Edge> wpe = backIter.next();
			Object inst = wpe.getInst();
			if (inst instanceof Block) {
				Block bb = (Block) inst;
				if (bb.getHead() instanceof JEntryNopStmt ||
						bb.getHead() instanceof JExitNopStmt) {
					pre = wpe;
					if (DEBUG) {
						System.out.println(wpe);
					}
					if (bb.getHead() instanceof JEntryNopStmt) {
						if(checkThreadStart()) // For threadStart, it's a special root method
							break;
					} else {
						errSuf = increaseContext(errSuf);  // Adjust the context level of each variable
					}
					continue;
				} else
					assert (bb.getHead() == null);
			}
			preSuf = errSuf;
			errSuf = backTransfer(errSuf, wpe.getInst());
			if (DEBUG) {
				System.out.println(wpe.getInst());
				System.out.println("After trans: " + errSuf);
			}
			//An optimization, if errSuf remains unchanged, no need to do double negation
			if(dnegation&&!errSuf.equals(preSuf)&&errSuf.size()>errSufSize)
			errSuf = negate(negate(errSuf));
			if(DEBUG){
				System.out.println("After double negation: "+errSuf);
			}
			Clause fwdState = encodePathEdge(wpe, errSuf);
			if (DEBUG) {
				System.out.println("Forward state: " + fwdState + " " + wpe.getPE().dstNode.isKill);
				System.out.println(wpe.getPE());
				System.out.println(iterAnalysis.methToFstVar(SootUtilities.getMethodInst(wpe.getInst())));
			}
			if(prune)
			errSuf = errSuf.prune(errSufSize, fwdState);  // Drop clauses to prevent blowing up
			if (DEBUG)
				System.out.println("After prune:" + errSuf);
			pre = wpe;
			checkInvoke(wpe);
		}
		if (errSuf.isFalse()) {
			dump();
			throw new RuntimeException("Something wrong with meta back!");
			// nc = DNF.getTrue();
		} else if (errSuf.isTrue())
			nc = DNF.getFalse(new LNumCMP());
		else {
			errSuf = chopNonParameter(errSuf);
			// create a true DNF, since we're going to do intersection
//			nc = new DNF(new LNumCMP(), true);
//			for (Clause c : errSuf.getClauses()) {
//				// create a false DNF, since we're going to do join
//				DNF cDNF = new DNF(new LNumCMP(), false);
//				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
//						.entrySet()) {
//					Value v;
//					if (Value.L().equals(entry.getValue()))
//						v = Value.E();
//					else
//						v = Value.L();
//					DNF hDNF = new DNF(new LNumCMP(), entry.getKey(), v);
//					cDNF = cDNF.join(hDNF);
//				}
//				nc = nc.intersect(cDNF);
//			}
			nc= negate(errSuf);
		}
		System.out.println("NC: " + nc.toString());
		printRunTime(false);
		if (timeout > 0)
	         alarm.cancel();
		return nc;
	}

	private DNF negate(DNF dnf){
		if(dnf.isTrue())
			return DNF.getFalse(dnf.getCMP());
		if(dnf.isFalse())
			return DNF.getTrue(dnf.getCMP());
		DNF nDNF = new DNF(dnf.getCMP(), true);
		for (Clause c : dnf.getClauses()) {
			// create a false DNF, since we're going to do join
			DNF cDNF = new DNF(dnf.getCMP(), false);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				checkTimeout();
				if (entry.getKey() instanceof EscHVariable) {
					petablox.analyses.escape.metaback.Value v;
					if (petablox.analyses.escape.metaback.Value.L().equals(entry.getValue()))
						v = petablox.analyses.escape.metaback.Value.E();
					else
						v = petablox.analyses.escape.metaback.Value.L();
					DNF hDNF = new DNF(dnf.getCMP(), entry.getKey(), v);
					cDNF = cDNF.join(hDNF);
				} else {
					Domain d = entry.getValue();
					for (Domain nd : d.space()) {
						if (nd.equals(d))
							continue;
						DNF nhDNF = new DNF(dnf.getCMP(), entry.getKey(), nd);
						cDNF = cDNF.join(nhDNF);
					}
				}
			}
			nDNF = nDNF.intersect(cDNF);
		}
		return nDNF;
	}
	
	private void dump() {
		System.out.println("=====================dump out current state======================");
		System.out.println(preSuf.toString());
		System.out.println(pre.getInst());
		System.out.println(pre);
		System.out.println("====dump out the stack====");
		System.out.println(iterAnalysis.methToFstVar(SootUtilities.getMethodInst(pre.getInst())));
		for (int j = callStack.size() - 1; j >= 0; j--) {
			IWrappedPE<Edge, Edge> wpe = callStack.get(j);
			System.out.println(wpe);
			System.out.println(iterAnalysis.methToFstVar(SootUtilities.getMethodInst(pre.getInst())));
		}
		throw new RuntimeException();
	}

	private DNF increaseContext(DNF dnf) {
		DNF nDNF = new DNF(dnf.getCMP(), false);
		for (Clause c : dnf.getClauses()) {
			Clause tnc = new Clause(true);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof EscVVariable) {
					EscVVariable tv = (EscVVariable) entry.getKey();
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
				if (entry.getKey() instanceof EscVVariable) {
					EscVVariable tv = (EscVVariable) entry.getKey();
					tnc.addLiteral(tv.getDecreased(), entry.getValue());
				} else
					tnc.addLiteral(entry.getKey(), entry.getValue());
			}
			nDNF.addClause(tnc);
		}
		return nDNF;

	}

	private boolean checkThreadStart() {
		if (SootUtilities.getMethodInst(pre.getInst()) == iterAnalysis.getThreadStartMethod()) {
			System.out.println("Reach the entry block of Thread.start().");
			int thisIdx = iterAnalysis.methToFstVar(iterAnalysis.getThreadStartMethod());
			EscVVariable ev = new EscVVariable(thisIdx, iterAnalysis.domV());
			DNF nDNF = new DNF(errSuf.getCMP(),false);
			OUT: for (Clause c : errSuf.getClauses()) {
				Clause tnv = new Clause(true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
					if (ev.equals(entry.getKey())) {//Thread.this object
						if (!Value.E().equals(entry.getValue()))
							continue OUT;
					} else if (!Value.N().equals(entry.getValue())){
						if(!(entry.getKey() instanceof EscHVariable))
						continue OUT;
						tnv.addLiteral(entry.getKey(), entry.getValue());
					}
				}
				nDNF.addClause(tnv);
			}
			errSuf = nDNF;
			return true;
		}
		return false;
	}

	private DNF killDeadConstraints(DNF dnf) {
		DNF nDNF = new DNF(errSuf.getCMP(), false);
		OUT: for (Clause c : dnf.getClauses()) {
			Clause tnc = new Clause(true);
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				if (entry.getKey() instanceof EscVVariable) {
					EscVVariable vv = (EscVVariable) entry.getKey();
					if (vv.getContext() < 0) {
						if (!Value.N().equals(entry.getValue()))
							continue OUT;
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
				if (Value.N().equals(entry.getValue()))
					continue;
				//If we allow to track all superfluous cases, there would be trouble
				if(!(entry.getKey() instanceof EscHVariable))
					continue OUT;
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
		if (wpe.getWSE() != null) {  // if wse!=null, wpe is a path edge
			// immediately after a method call
			IWrappedPE<Edge, Edge> cwpe = wpe.getWPE();
			Object inst = cwpe.getInst();
			if (inst instanceof Block) { // an empty basic block
				assert ((Block) inst).getHead() == null;
				return;
			}
			Unit q = (Unit) inst;
			if (iterAnalysis.isThreadStart(q))
				return;
			soot.Value ro = null;
			if(q instanceof JAssignStmt){
				ro = ((JAssignStmt)q).leftBox.getValue();
			}
			retIdx = -1;
			if (ro != null && ro.getType() instanceof RefLikeType) {
				retIdx = iterAnalysis.getDomVIdx((Local)ro);
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
	private DNF backTransfer(DNF es, Object inst) {
		if (inst instanceof Block) {
			Block bb = (Block) inst;
			// bb might be entry, exit, or empty basic block
			assert bb.getHead() == null || bb.getHead() instanceof JEntryExitNopStmt;
			return es;
		}
		qv.iDNF = es;
		qv.oDNF = es;
		Unit q = (Unit) inst;
		q.apply(qv);
		return qv.oDNF;
	}

	// dnf is fixed when it only contains clauses over allocation sites
	public boolean isFixed(DNF dnf) {
		if (dnf.isTrue() || dnf.isFalse())
			return true;
		for (Clause c : dnf.getClauses()) {
			for (Map.Entry<Variable, Domain> e : c.getLiterals().entrySet()) {
				if (!(e.getKey() instanceof EscHVariable))
					return false;
			}
		}
		return true;
	}

	// Here we encode the forward state lazily, only encode the part relative to
	// the backward DNF
	private Clause encodePathEdge(IWrappedPE<Edge, Edge> wpe, DNF dnf) {
		Clause ret = new Clause(true);
		DstNode dNode = wpe.getPE().dstNode;
		Set<Integer> relF = new HashSet<Integer>();
		Set<Integer> relH = new HashSet<Integer>();
		for (Clause c : dnf.getClauses())
			for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
				Variable v = entry.getKey();
				if (v instanceof EscVVariable) {
					EscVVariable vv = (EscVVariable) v;
					int cl = vv.getContext();
					if (0 == cl) {
						int firstVar = iterAnalysis.methToFstVar(SootUtilities.getMethodInst(wpe.getInst()));
						ret.addLiteral(vv, Value.objToValue(dNode.env[vv.getIdx() - firstVar]));
					} else {
						int stackSize = callStack.size();
						IWrappedPE<Edge, Edge> uWPE = callStack.get(stackSize - cl);
						boolean isKill = dNode.isKill;
						for (int i = 1; i <= cl-1; i++)
							isKill |= callStack.get(stackSize - i).getPE().dstNode.isKill;
						DstNode uNode = uWPE.getPE().dstNode;
						int firstVar = iterAnalysis.methToFstVar(SootUtilities.getMethodInst(wpe.getInst()));
						Value tv = Value.objToValue(uNode.env[vv.getIdx() - firstVar]);
						if (isKill && Value.L().equals(tv))
							ret.addLiteral(vv, Value.E());
						else
							ret.addLiteral(vv, tv);
					}
				}
				if (v instanceof EscFVariable) {
					EscFVariable fv = (EscFVariable) v;
					relF.add(fv.getIdx());
				}
				if (v instanceof EscHVariable) {
					EscHVariable hv = (EscHVariable) v;
					relH.add(hv.getIdx());
				}
			}
		for (int h : relH) {
			if (Ls.contains(h))
				ret.addLiteral(new EscHVariable(h, iterAnalysis.domH()), Value.L());
			else
				ret.addLiteral(new EscHVariable(h, iterAnalysis.domH()), Value.E());
		}
		Set<Integer> doneF = new HashSet<Integer>();
		for (int i = 0; i < dNode.heap.size(); i++) {
			FldObj f = dNode.heap.get(i);
			int fIdx = iterAnalysis.getDomFIdx(f.f);
			if (!relF.contains(fIdx))
				continue;
			ret.addLiteral(new EscFVariable(fIdx, iterAnalysis.domF()),
					Value.fldToValue(f));
			doneF.add(fIdx);
		}
		relF.removeAll(doneF);
		for (int i : relF) {
			ret.addLiteral(new EscFVariable(i, iterAnalysis.domF()), Value.N());
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

	public static void setDNegation(boolean dn){
		dnegation = dn;
	}
	
	public static void setPrune(boolean p){
		prune = p;
	}
	
	class BackQuadVisitor extends FGStmtSwitch {
		DNF iDNF;
		DNF oDNF;

		/**
		 * Invoke is like a group of moves before the method call
		 */
		@Override
		public void caseInvokeStmt( Local dest, InvokeExpr e) {
			oDNF = iDNF;
			List<soot.Value> args = SootUtilities.getInvokeArgs(statement);
			if(SootUtilities.isInstanceInvoke(statement)){
				soot.Value thisV = SootUtilities.getInstanceInvkBase(statement);
				args.add(0, thisV);
			}
			if (iterAnalysis.isThreadStart(statement)) {// special handling for
				// Thread.Start()
				soot.Value ro = args.get(0);
				int fromIdx = iterAnalysis.getDomVIdx((Local)ro);
				processEscape(fromIdx, oDNF);
				return;
			}
			if (iterAnalysis.isSkippedMethod(statement)) {
				return;
			}
			if (!callStack.empty()) {
				IWrappedPE<Edge, Edge> wpe = callStack.pop();
				if (!wpe.getInst().equals(statement))
					throw new RuntimeException("Unmatch invoke!" + wpe.getInst() + " " + statement);
			}
			int numArgs = args.size();
			//RegisterFactory rf = pre.getInst().getMethod().getCFG().getRegisterFactory();
			Local[] rf =
			SootUtilities.getMethArgLocals(SootUtilities.getMethodInst(pre.getInst()));
			// HashSet<Integer> liveVs = new HashSet<Integer>();
			oDNF = decreaseContext(oDNF);
			for (int i = 0; i < rf.length; i++) {
				soot.Value ro = args.get(i);
				if (ro.getType() instanceof RefLikeType) {
					int fromIdx = iterAnalysis.getDomVIdx((Local)ro);
					Local r = rf[i];
					int toIdx = iterAnalysis.getDomVIdx(r);
					oDNF = processMove(fromIdx, toIdx, oDNF, 1);
					// liveVs.add(fromIdx);
				}
			}
			oDNF = killDeadConstraints(oDNF);
		}

		/**
		 * Return is like a move statement after the method call
		 */
		@Override
		public void caseReturnStmt( Local val ) {
			if (retIdx == -1)
				return;
			int fromIdx = -1;
            if (val.getType() instanceof RefLikeType) {
                fromIdx = iterAnalysis.getDomVIdx(val);
            }


			oDNF = processMove(fromIdx, retIdx, iDNF, -1);
		}

		//@Override
		/*public void visitCheckCast(Quad q) {
			visitMove(q);
		}*/

		@Override
		public void caseCopyStmt( Local dest, soot.Value src ) {
			Type t = dest.getType();
			if (!(t instanceof RefLikeType))
				return;
			int toIdx = iterAnalysis.getDomVIdx(dest);
			int fromIdx = -1;
			if (src instanceof Local) {
				Local ro = (Local) src;
				fromIdx = iterAnalysis.getDomVIdx(ro);
			}
			oDNF = processMove(fromIdx, toIdx, iDNF, 0);
		}

		/**
		 * A common helper method for move like statement. fromIdx is set to -1
		 * if the src operand is N. <br>
		 * 
		 * @param fromIdx
		 * @param toIdx
		 * @param ftContextDif
		 *          , use to handle method parameters and return value.
		 *			ftContextDif = from.context-to.context <br>
		 *            Normal Move:0<br>
		 *            args: 1 <br>
		 *            return: -1
		 */
		private DNF processMove(int fromIdx, int toIdx, DNF dnf, int ftContextDif) {
			DNF ret = new DNF(new ClauseSizeCMP(), false);
			EscVVariable rv = new EscVVariable(toIdx, iterAnalysis.domV());
			if (ftContextDif == 1)
				rv = rv.getDecreased();
			if (ftContextDif == -1)
				rv = rv.getIncreased();
			OUT: for (Clause c : dnf.getClauses()) {
				Clause nc = new Clause(true);// Construct a true Clause
				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
						.entrySet()) {
					if (nc.isFalse())
						continue OUT;
					if (rv.equals(entry.getKey())) {
						if (fromIdx == -1) {
							if (entry.getValue().equals(Value.N()))  // tracking v=N
								continue;
							else {
								// This clause is evaluated to be false, since we're tracking v!=N
								continue OUT;
							}
						} else
							nc.addLiteral(new EscVVariable(fromIdx,
									iterAnalysis.domV()), entry.getValue());
					} else
						nc.addLiteral(entry.getKey(), entry.getValue());  // unaffected clauses
				}
				ret.addClause(nc);
			}

			return ret;
		}

		@Override
		public void casePhiStmt( Local dest, PhiExpr src ) {
			System.out.println(System.getProperty("chord.ssa.kind"));
			throw new RuntimeException("PHI is not supported!");
		}

		@Override
		public void caseArrayLoadStmt( Local dest, ArrayRef src ) {
			if (!(src.getType() instanceof RefLikeType))
				return;
			int toIdx = iterAnalysis.getDomVIdx(dest);
			soot.Value bo = src.getBase();
			int fromIdx = iterAnalysis.getDomVIdx((Local)bo);
			processRead(null, fromIdx, toIdx);
		}

		@Override
		public void caseLoadStmt( Local dest, InstanceFieldRef src ) {
			SootField f = src.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			int toIdx = iterAnalysis.getDomVIdx(dest);
			soot.Value bo = src.getBase();
			int fromIdx = iterAnalysis.getDomVIdx((Local)bo);
			processRead(f, fromIdx, toIdx);
		}

		/**
		 * A common method to process field write
		 * 
		 * @param f
		 *            the field, null when f represents an element in an array
		 * @param fromIdx
		 * @param toIdx
		 */
		private void processRead(SootField f, int fromIdx, int toIdx) {
			EscVVariable tv = new EscVVariable(toIdx, iterAnalysis.domV());
			EscVVariable frv = new EscVVariable(fromIdx, iterAnalysis.domV());
			EscFVariable fiev = new EscFVariable(f, iterAnalysis.domF());
			// Construct a false DNF, since we're going to do join next
			oDNF = new DNF(new ClauseSizeCMP(), false);
			for (Clause c : iDNF.getClauses()) {
				// Construct a true DNF, since we're going to do intersection
				// next
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals()
						.entrySet()) {
					checkTimeout();
					if (tv.equals(entry.getKey())) {
						if (Value.E().equals(entry.getValue())) {
							DNF dnf1 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF dnf2 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF dnf31 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF dnf32 = new DNF(new ClauseSizeCMP(), fiev, Value.E());
							DNF dnf3 = dnf31.intersect(dnf32);
							DNF dnf0 = dnf1.join(dnf2);
							dnf0 = dnf0.join(dnf3);
							cDNF = cDNF.intersect(dnf0);
						} else {
							DNF dnf1 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF dnf2 = new DNF(new ClauseSizeCMP(), fiev, entry.getValue());
							DNF dnf = dnf1.intersect(dnf2);
							cDNF = cDNF.intersect(dnf);
						}
					} else {
						DNF dnf = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
						cDNF = cDNF.intersect(dnf);
					}
				}
				oDNF = oDNF.join(cDNF);
			}
		}

		@Override
		public void caseArrayStoreStmt( ArrayRef dest, soot.Value src ) {
			if (!(dest.getType() instanceof RefLikeType))
				return;
			if (!(src instanceof Local))
				return;
			Local ro = (Local) src;
			int fromIdx = iterAnalysis.getDomVIdx(ro);
			soot.Value bo = dest.getBase();
			int toIdx = iterAnalysis.getDomVIdx((Local)bo);
			processWrite(null, fromIdx, toIdx);
		}

		@Override
		public void caseStoreStmt( InstanceFieldRef dest, soot.Value src ) {
			SootField f = dest.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			if (!(src instanceof Local))
				return;
			soot.Value bx = dest.getBase();
			if (!(bx instanceof Local))
				return;
			Local rb = (Local) bx;
			int fromIdx = iterAnalysis.getDomVIdx((Local)src);
			int toIdx = iterAnalysis.getDomVIdx(rb);
			processWrite(f, fromIdx, toIdx);
		}

		/**
		 * A common method to handle field write. The most complex backward
		 * transfer function. Pay extra extra attention here!!!!
		 * 
		 * @param f
		 * @param fromIdx
		 *            the index of the source variable in DomV
		 * @param toIdx
		 */
		private void processWrite(SootField f, int fromIdx, int toIdx) {
			oDNF = new DNF(new ClauseSizeCMP(), false);
			EscVVariable frv = new EscVVariable(fromIdx, iterAnalysis.domV());
			EscVVariable tv = new EscVVariable(toIdx, iterAnalysis.domV());
			EscFVariable fiv = new EscFVariable(f, iterAnalysis.domF());
			for (Clause c : iDNF.getClauses()) {
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
					checkTimeout();
					Value v = (Value) entry.getValue();
					if (entry.getKey() instanceof EscHVariable) {
						DNF hDNF = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
						cDNF = cDNF.intersect(hDNF);
					}
					if (entry.getKey() instanceof EscVVariable) {
						if (v.equals(Value.N())) {
							DNF vDNF = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							cDNF = cDNF.intersect(vDNF);
						}
						if (v.equals(Value.L())) {
							DNF vDNF1 = new DNF(new ClauseSizeCMP(), tv, Value.N());// toV->N
							DNF vDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.E());// toV->E
							DNF vDNF221 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF222 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF vDNF22 = vDNF221.join(vDNF222);
							DNF vDNF2 = vDNF21.intersect(vDNF22);
							DNF vDNF = vDNF1.join(vDNF2);
							DNF vDNF31 = new DNF(new ClauseSizeCMP(), tv, Value.L());// toV->L
							DNF vDNF321 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF vDNF322 = new DNF(new ClauseSizeCMP(), fiv, Value.N());
							DNF vDNF32 = vDNF321.join(vDNF322);
							DNF vDNF3231 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF vDNF3232 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
							DNF vDNF323 = vDNF3231.intersect(vDNF3232);
							vDNF32 = vDNF32.join(vDNF323);
							DNF vDNF3241 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF3242 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
							DNF vDNF324 = vDNF3241.intersect(vDNF3242);
							vDNF32 = vDNF32.join(vDNF324);
							DNF vDNF3 = vDNF31.intersect(vDNF32);
							vDNF = vDNF.join(vDNF3);
							DNF vDNF4 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							vDNF = vDNF.intersect(vDNF4);
							cDNF = cDNF.intersect(vDNF);
						}
						if (v.equals(Value.E())) {
							DNF vDNF1 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());// v->E
							DNF vDNF21 = new DNF(new ClauseSizeCMP(), entry.getKey(), Value.L());// v->L
							DNF vDNF2211 = new DNF(new ClauseSizeCMP(), tv, Value.E());
							DNF vDNF2212 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF vDNF221 = vDNF2211.intersect(vDNF2212);  // frV->L, tV->E
							DNF vDNF2221 = new DNF(new ClauseSizeCMP(), tv, Value.L());
							DNF vDNF2222 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF2223 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
							DNF vDNF222 = vDNF2221.intersect(vDNF2222).intersect(vDNF2223);  // tv->L, f->L, frv->E
							DNF vDNF2231 = new DNF(new ClauseSizeCMP(), tv, Value.L());
							DNF vDNF2232 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF vDNF2233 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
							DNF vDNF223 = vDNF2231.intersect(vDNF2232).intersect(vDNF2233);  // tv->L, f->E, frv->L
							DNF vDNF22 = vDNF221.join(vDNF222).join(vDNF223);
							DNF vDNF2 = vDNF21.intersect(vDNF22);
							DNF vDNF = vDNF1.join(vDNF2);
							cDNF = cDNF.intersect(vDNF);
						}
					}
					if (entry.getKey() instanceof EscFVariable) {
						if (!(fiv.equals(entry.getKey()))) {// f != \delta
							if (entry.getValue().equals(Value.N())) {
								DNF fDNF1 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());// entry.f->N
								DNF fDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.E());// tv->E
								DNF fDNF22 = new DNF(new ClauseSizeCMP(), frv, Value.L());// fv->L
								DNF fDNF2 = fDNF21.intersect(fDNF22);
								DNF fDNF31 = new DNF(new ClauseSizeCMP(), tv, Value.L());// tv->L
								DNF fDNF3211 = new DNF(new ClauseSizeCMP(), fiv, Value.L());// f->L
								DNF fDNF3212 = new DNF(new ClauseSizeCMP(), frv, Value.E());// fv->E
								DNF fDNF321 = fDNF3211.intersect(fDNF3212);
								DNF fDNF3221 = new DNF(new ClauseSizeCMP(), fiv, Value.E());// f->E
								DNF fDNF3222 = new DNF(new ClauseSizeCMP(), frv, Value.L());// fv->L
								DNF fDNF322 = fDNF3221.intersect(fDNF3222);
								DNF fDNF32 = fDNF321.join(fDNF322);
								DNF fDNF3 = fDNF31.intersect(fDNF32);
								DNF fDNF = fDNF1.join(fDNF2).join(fDNF3);
								cDNF = cDNF.intersect(fDNF);
							} else {
								DNF fDNF1 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
								DNF fDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.N());// tv->N
								DNF fDNF221 = new DNF(new ClauseSizeCMP(), tv, Value.E());// tv->E
								DNF fDNF2221 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF2222 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF222 = fDNF2221.join(fDNF2222);
								DNF fDNF22 = fDNF221.intersect(fDNF222);
								DNF fDNF231 = new DNF(new ClauseSizeCMP(), tv, Value.L());// tv->L
								DNF fDNF2321 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF2322 = new DNF(new ClauseSizeCMP(), fiv, Value.N());
								DNF fDNF23231 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
								DNF fDNF23232 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF2323 = fDNF23231.intersect(fDNF23232);
								DNF fDNF23241 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
								DNF fDNF23242 = new DNF(new ClauseSizeCMP(), frv, Value.L());
								DNF fDNF2324 = fDNF23241.intersect(fDNF23242);
								DNF fDNF232 = fDNF2321.join(fDNF2322)
										.join(fDNF2323).join(fDNF2324);
								DNF fDNF23 = fDNF231.intersect(fDNF232);
								DNF fDNF2 = fDNF21.join(fDNF22).join(fDNF23);
								DNF fDNF = fDNF1.intersect(fDNF2);
								cDNF = cDNF.intersect(fDNF);
							}
						} else {// f = \delta
							if (Value.N().equals(entry.getValue())) {
								DNF fDNF11 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
								DNF fDNF121 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF122 = new DNF(new ClauseSizeCMP(), tv, Value.N());
								DNF fDNF1231 = new DNF(new ClauseSizeCMP(), tv, Value.L());
								DNF fDNF1232 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF123 = fDNF1231.intersect(fDNF1232);
								DNF fDNF12 = fDNF121.join(fDNF122).join(fDNF123);
								DNF fDNF1 = fDNF11.intersect(fDNF12);
								DNF fDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF22 = new DNF(new ClauseSizeCMP(), frv, Value.L());
								DNF fDNF2 = fDNF21.intersect(fDNF22);
								DNF fDNF31 = new DNF(new ClauseSizeCMP(), tv, Value.L());
								DNF fDNF32 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
								DNF fDNF33 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF3 = fDNF31.intersect(fDNF32).intersect( fDNF33);
								DNF fDNF41 = new DNF(new ClauseSizeCMP(), tv, Value.L());
								DNF fDNF42 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
								DNF fDNF43 = new DNF(new ClauseSizeCMP(), frv, Value.L());
								DNF fDNF4 = fDNF41.intersect(fDNF42).intersect(fDNF43);
								DNF fDNF = fDNF1.join(fDNF2).join(fDNF3).join(fDNF4);
								cDNF = cDNF.intersect(fDNF);
							}
							if (Value.L().equals(entry.getValue())) {
								DNF fDNF11 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
								DNF fDNF121 = new DNF(new ClauseSizeCMP(), tv, Value.N());
								DNF fDNF1221 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF1222 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF122 = fDNF1221.intersect(fDNF1222);
								DNF fDNF1231 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF1232 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF123 = fDNF1231.intersect(fDNF1232);
								DNF fDNF12 = fDNF121.join(fDNF122).join(fDNF123);
								DNF fDNF1 = fDNF11.intersect(fDNF12);
								DNF fDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.L());
								DNF fDNF2211 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
								DNF fDNF2212 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF221 = fDNF2211.intersect(fDNF2212);
								DNF fDNF2221 = new DNF(new ClauseSizeCMP(), fiv, Value.N());
								DNF fDNF2222 = new DNF(new ClauseSizeCMP(), frv, Value.L());
								DNF fDNF222 = fDNF2221.intersect(fDNF2222);
								DNF fDNF2231 = new DNF(new ClauseSizeCMP(), fiv, Value.L());
								DNF fDNF2232 = new DNF(new ClauseSizeCMP(), frv, Value.L());
								DNF fDNF223 = fDNF2231.intersect(fDNF2232);
								DNF fDNF22 = fDNF221.join(fDNF222).join(fDNF223);
								DNF fDNF2 = fDNF21.intersect(fDNF22);
								DNF fDNF = fDNF1.join(fDNF2);
								cDNF = cDNF.intersect(fDNF);
							}
							if (Value.E().equals(entry.getValue())) {
								DNF fDNF11 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
								DNF fDNF121 = new DNF(new ClauseSizeCMP(), tv, Value.N());
								DNF fDNF1221 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF1222 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF122 = fDNF1221.intersect(fDNF1222);
								DNF fDNF1231 = new DNF(new ClauseSizeCMP(), tv, Value.E());
								DNF fDNF1232 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF123 = fDNF1231.intersect(fDNF1232);
								DNF fDNF12 = fDNF121.join(fDNF122).join(fDNF123);
								DNF fDNF1 = fDNF11.intersect(fDNF12);
								DNF fDNF21 = new DNF(new ClauseSizeCMP(), tv, Value.L());
								DNF fDNF2211 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
								DNF fDNF2212 = new DNF(new ClauseSizeCMP(), frv, Value.N());
								DNF fDNF221 = fDNF2211.intersect(fDNF2212);
								DNF fDNF2221 = new DNF(new ClauseSizeCMP(), fiv, Value.N());
								DNF fDNF2222 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF222 = fDNF2221.intersect(fDNF2222);
								DNF fDNF2231 = new DNF(new ClauseSizeCMP(), fiv, Value.E());
								DNF fDNF2232 = new DNF(new ClauseSizeCMP(), frv, Value.E());
								DNF fDNF223 = fDNF2231.intersect(fDNF2232);
								DNF fDNF22 = fDNF221.join(fDNF222).join(fDNF223);
								DNF fDNF2 = fDNF21.intersect(fDNF22);
								DNF fDNF = fDNF1.join(fDNF2);
								cDNF = cDNF.intersect(fDNF);
							}
						}
					}
				}
				oDNF = oDNF.join(cDNF);
			}
		}

		/**
		 * Pay extra attention to this backward transfer function, since
		 * disjuncts are produced
		 */
		@Override
		public void caseGlobalStoreStmt(StaticFieldRef dest, soot.Value src ) {
			oDNF = iDNF;
			SootField f = dest.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			if (!(src instanceof Local))
				return;// Assigning NULL to a static field has no effect on the
						// DNF
			Local ro = (Local) src;
			int fromIdx = iterAnalysis.getDomVIdx(ro);
			processEscape(fromIdx, oDNF);
		}

		private void processEscape(int fromIdx, DNF iDNF1) {
			EscVVariable frv = new EscVVariable(fromIdx, iterAnalysis.domV());
			oDNF = new DNF(new ClauseSizeCMP(), false);
			OUT: for (Clause c : iDNF1.getClauses()) {
				// Since we're going to do intersections for the following DNFs,
				// here we create a true DNF
				DNF cDNF = new DNF(new ClauseSizeCMP(), true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
					checkTimeout();
					if (entry.getKey() instanceof EscHVariable) { // For allocation sites
						DNF hDNF = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
						cDNF = cDNF.intersect(hDNF);
					}
					if (entry.getKey() instanceof EscVVariable) { // For variables
						if (Value.E().equals(entry.getValue())) {
							DNF vDNF11 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF vDNF12 = new DNF(new ClauseSizeCMP(), entry.getKey(), Value.L());
							DNF vDNF1 = vDNF11.intersect(vDNF12);
							DNF vDNF2 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							DNF vDNF = vDNF1.join(vDNF2);
							cDNF = cDNF.intersect(vDNF);
						}
						if (Value.L().equals(entry.getValue())) {
							DNF vDNF11 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF12 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF vDNF1 = vDNF11.join(vDNF12);
							DNF vDNF2 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							DNF vDNF = vDNF1.intersect(vDNF2);
							cDNF = cDNF.intersect(vDNF);
						}
						if (Value.N().equals(entry.getValue())) {
							DNF vDNF = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							cDNF = cDNF.intersect(vDNF);
						}
					}
					if (entry.getKey() instanceof EscFVariable) { // For fields
						if (Value.N().equals(entry.getValue())) {
							DNF vDNF1 = new DNF(new ClauseSizeCMP(), frv, Value.L());
							DNF vDNF211 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF212 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF vDNF21 = vDNF211.join(vDNF212);
							DNF vDNF22 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							DNF vDNF2 = vDNF21.intersect(vDNF22);
							DNF vDNF = vDNF1.join(vDNF2);
							cDNF = cDNF.intersect(vDNF);
						} else {
							DNF vDNF1 = new DNF(new ClauseSizeCMP(), entry.getKey(), entry.getValue());
							DNF vDNF21 = new DNF(new ClauseSizeCMP(), frv, Value.E());
							DNF vDNF22 = new DNF(new ClauseSizeCMP(), frv, Value.N());
							DNF vDNF2 = vDNF21.join(vDNF22);
							DNF vDNF = vDNF1.intersect(vDNF2);
							cDNF = cDNF.intersect(vDNF);
						}
					}
				}
				oDNF = oDNF.join(cDNF);
			}
		}

		@Override
		public void caseGlobalLoadStmt( Local dest, StaticFieldRef src ){
			oDNF = iDNF;
			SootField f = src.getField();
			if (!(f.getType() instanceof RefLikeType))
				return;
			int toIdx = iterAnalysis.getDomVIdx(dest);
			oDNF = new DNF(new ClauseSizeCMP(), false);
			EscVVariable rv = new EscVVariable(toIdx, iterAnalysis.domV());
			OUT: for (Clause c : iDNF.getClauses()) {
				Clause nc = new Clause(true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
					if (rv.equals(entry.getKey())) {
						if (entry.getValue().equals(Value.E()))
							continue;
						else
							continue OUT;
					} else
						nc.addLiteral(entry.getKey(), entry.getValue());
				}
				oDNF.addClause(nc);
			}
		}

		@Override
		public void caseNewStmt( Local dest, NewExpr e )  {
			int toIdx = iterAnalysis.getDomVIdx(dest);
			processAlloc(statement, toIdx);
		}

		@Override
		public void caseNewArrayStmt( Local dest, NewArrayExpr e ) {
			int toIdx = iterAnalysis.getDomVIdx(dest);
			processAlloc(statement, toIdx);
		}

		@Override
		public void caseNewMultiArrayStmt( Local dest, NewMultiArrayExpr e)  {
			int toIdx = iterAnalysis.getDomVIdx(dest);
			processAlloc(statement, toIdx);
		}

		/**
		 * A common method to handle allocation statements
		 * 
		 * @param q
		 * @param toIdx
		 */
		private void processAlloc(Unit q, int toIdx) {
			oDNF = new DNF(new ClauseSizeCMP(), false);
			EscVVariable rv = new EscVVariable(toIdx, iterAnalysis.domV());
			OUT: for (Clause c : iDNF.getClauses()) {
				Clause nc = new Clause(true);
				for (Map.Entry<Variable, Domain> entry : c.getLiterals().entrySet()) {
					if (nc.isFalse())
						continue OUT;
					if (rv.equals(entry.getKey())) {
						if (entry.getValue().equals(Value.N()))
							continue OUT; // No way to make an allocation site point to NULL
						nc.addLiteral(new EscHVariable(q, iterAnalysis.domH()), entry.getValue());
					} else
						nc.addLiteral(entry.getKey(), entry.getValue());
				}
				oDNF.addClause(nc);
			}
		}
	}
}

