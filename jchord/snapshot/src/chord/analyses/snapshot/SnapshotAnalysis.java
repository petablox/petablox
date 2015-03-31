/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import joeq.Class.jq_Reference;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Inst;
import chord.instr.InstrScheme;
import chord.program.Program;
import chord.project.Config;
import chord.project.analyses.DynamicAnalysis;
import chord.util.Execution;
import chord.util.Utils;

import chord.util.StatFig;
import chord.project.ClassicProject;
import chord.analyses.heapacc.DomE;
import chord.analyses.field.DomF;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.lock.DomL;
import chord.analyses.method.DomM;

/**
 * Evaluate the precision and complexity of various heap abstractions.
 * Maintains a graph over the concrete heap.
 * Abstractions are functions that operate on snapshots of the concrete heap.
 * Client will override this class and provide queries on the abstractions.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public abstract class SnapshotAnalysis extends DynamicAnalysis implements AbstractionListener, AbstractionInitializer {
	public abstract String propertyName();

	static final int ARRAY_FIELD = 0;

	InstrScheme instrScheme;
	protected DomE domE;
	protected DomF domF;
	protected DomH domH;
	protected DomI domI;
	protected DomL domL;
	protected DomM domM;

	// Execution management/logging
	Execution X;

	// Parameters of the analysis (updates and abstraction)
	int verbose;
	boolean useStrongUpdates;
	Abstraction abstraction;
	int kCFA; // Number of call sites keep in k-CFA
	int recencyOrder; // Recency order (number of objects to keep distinct)
	int randSize; // Number of abstract values for random abstraction
	GraphMonitor graphMonitor;
	int maxFieldAccessesToPrint;
	PrintWriter fieldAccessOut;
	boolean queryOnlyAtSnapshot; // For efficiency (but incorrect)
	boolean includeAllQueries; // Include queries based on all objects (if false, look at scope.check.exclude)
	int maxCommands;
	int maxCallDepth;
	boolean collapseArrayIndices;

	// We have a graph over abstract values (determined by updateAbstraction); each node impliciting representing a set of objects
	State state = new State();
	TIntObjectHashMap<ThreadInfo> threadInfos = new TIntObjectHashMap<ThreadInfo>(); // thread t -> ThreadInfo
	Set<jq_Reference> excludedClasses = new HashSet<jq_Reference>();
	boolean[] statementIsExcluded, fieldIsExcluded, lockIsExcluded; // For answering queries (precomputed)

	HashMap<Query, QueryResult> queryResults = new HashMap<Query, QueryResult>();
	int numQueryHits;
	int numFieldAccesses;
	int numCommands;

	public boolean importantLog() { // See if we should print out an important thing (cap it though)
		if (verbose < 1) return false;
		if (numCommands >= maxCommands) return false;
		numCommands++;
		return true;
	}

	public boolean require_a2o() { return false; } // By default, we don't need a map from abstract to concrete
	public boolean requireGraph() { return false; } // By default, we don't construct the graph (FUTURE: abstraction could require a graph)

	private boolean anyRequireGraph() { return requireGraph() || abstraction.requireGraph(); }

	public Abstraction parseAbstraction(String abstractionType) {
		if (abstractionType.equals("none")) return new NoneAbstraction();
		if (abstractionType.equals("random")) return new RandomAbstraction(randSize);
		if (abstractionType.equals("alloc")) return new AllocAbstraction(kCFA);
		if (abstractionType.equals("recency")) return new RecencyAbstraction(new AllocAbstraction(kCFA), recencyOrder);
		if (abstractionType.equals("reach")) return new ReachableFromAbstraction(new AllocAbstraction(kCFA));
		if (abstractionType.equals("point")) return new PointedToByAbstraction(new AllocAbstraction(kCFA));
		if (abstractionType.equals("pointed-to")) return new PointedToByAllocAbstraction();
		if (abstractionType.equals("alloc-reachability")) return new ReachableFromAllocAbstraction();
		if (abstractionType.equals("alloc-x-field-reachability")) return new ReachableFromAllocPlusFieldsAbstraction();
		throw new RuntimeException("Unknown abstraction: "+abstractionType+" (possibilities: none|alloc|recency|reachability)");
	}

	public void run() {
		domE = (DomE)ClassicProject.g().getTrgt("E"); ClassicProject.g().runTask(domE);
		domF = (DomF)ClassicProject.g().getTrgt("F"); ClassicProject.g().runTask(domF);
		domH = (DomH)ClassicProject.g().getTrgt("H"); ClassicProject.g().runTask(domH);
		domI = (DomI)ClassicProject.g().getTrgt("I"); ClassicProject.g().runTask(domI);
		domL = (DomL)ClassicProject.g().getTrgt("L"); ClassicProject.g().runTask(domL);
		domM = (DomM)ClassicProject.g().getTrgt("M"); ClassicProject.g().runTask(domM);

		X = new Execution("partition");
		X.addSaveFiles("queries.out", "graph", "fieldAccessed", "snapshot-abstractions");
		boolean success = false;
		try {
			// Parse options
			verbose = X.getIntArg("verbose", 0);

			kCFA = X.getIntArg("kCFA", 0);
			recencyOrder = X.getIntArg("recencyOrder", 1);
			randSize = X.getIntArg("randSize", 1);

			includeAllQueries = X.getBooleanArg("includeAllQueries", false);
			collapseArrayIndices = X.getBooleanArg("collapseArrayIndices", false);

			useStrongUpdates = X.getBooleanArg("useStrongUpdates", true);
			abstraction = parseAbstraction(X.getStringArg("abstraction", ""));

			// For debugging
			maxCommands = X.getIntArg("graph.maxCommands", 100000);
			if (X.getBooleanArg("outputGraph", false))
				graphMonitor = new SerializingGraphMonitor(X.path("graph"), maxCommands);
			maxFieldAccessesToPrint = X.getIntArg("maxFieldAccessesToPrint", 0);
			if (maxFieldAccessesToPrint > 0)
				fieldAccessOut = new PrintWriter(X.path("fieldAccesses"));

			// Save options
			HashMap<Object,Object> options = new LinkedHashMap<Object,Object>();
			options.put("program", System.getProperty("chord.work.dir"));
			options.put("property", propertyName());
			options.put("verbose", verbose);
			options.put("useStrongUpdates", useStrongUpdates);
			options.put("abstraction", abstraction);
			options.put("exclude", X.getStringArg("exclude", ""));
			options.put("includeAllQueries", includeAllQueries);
			options.put("collapseArrayIndices", collapseArrayIndices);
			X.writeMap("options.map", options);

			super.run();

			X.finish(null);
		} catch (Throwable t) {
			X.finish(t);
		}
	}

	public void computedExcludedClasses() {
		String[] checkExcludedPrefixes = Utils.toArray(Config.checkExcludeStr);
		Program program = Program.g();
		for (jq_Reference r : program.getClasses()) {
			String rName = r.getName();
			for (String prefix : checkExcludedPrefixes) {
				if (rName.startsWith(prefix))
					excludedClasses.add(r);
			}
		}
	}

	private boolean computeStatementIsExcluded(int e) {
		if (includeAllQueries) return false;
		Quad q = domE.get(e);
		jq_Class c = q.getMethod().getDeclaringClass();
		return excludedClasses.contains(c);
	}

	private boolean computeFieldIsExcluded(int f) {
		if (includeAllQueries) return false;
		jq_Class c = ((jq_Field)domF.get(f)).getDeclaringClass();
		return excludedClasses.contains(c);
	}

	private boolean computeLockIsExcluded(int l) {
		if (includeAllQueries) return false;
		Inst inst = domL.get(l);
		jq_Class c = inst.getMethod().getDeclaringClass();
		return excludedClasses.contains(c);
	}

	public boolean statementIsExcluded(int e) { return statementIsExcluded[e]; }
	public boolean fieldIsExcluded(int f) { return fieldIsExcluded[f]; }
	public boolean lockIsExcluded(int l) { return lockIsExcluded[l]; }

	public boolean isIgnore(int o) {
		// Allow o = 0 (null objects)
		boolean good = o == 0 || (o > 0 && state.o2h.containsKey(o));
		return !good;
	}

	public ThreadInfo threadInfo(int t) {
		if (t == -1) return null;
		ThreadInfo info = threadInfos.get(t);
		if (info == null)
			threadInfos.put(t, info = new ThreadInfo());
		return info;
	}

	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();

        instrScheme.setBefNewEvent(true, true, true); // h, t, o
		instrScheme.setNewArrayEvent(true, true, true); // h, t, o

		instrScheme.setPutstaticReferenceEvent(true, true, true, true, true); // e, t, b, f, o

		instrScheme.setGetfieldPrimitiveEvent(true, true, true, true); // e, t, b, f
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, true); // e, t, b, f
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, true); // e, t, b, f, o
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, true); // e, t, b, f, o

		instrScheme.setAloadPrimitiveEvent(true, true, true, true); // e, t, b, i
		instrScheme.setAstorePrimitiveEvent(true, true, true, true); // e, t, b, i
		instrScheme.setAloadReferenceEvent(true, true, true, true, true); // e, t, b, i, o
		instrScheme.setAstoreReferenceEvent(true, true, true, true, true); // e, t, b, i, o

		instrScheme.setThreadStartEvent(true, true, true); // i, t, o

		instrScheme.setAcquireLockEvent(true, true, true); // l, t, o

		instrScheme.setBefMethodCallEvent(true, true, true); // i, t, o
		instrScheme.setAftMethodCallEvent(true, true, true); // i, t, o

		return instrScheme;
	}

	public void answerQuery(Query query, boolean isTrue) {
		QueryResult result = queryResult(query);
		result.add(isTrue);
		numQueryHits++;
		if (importantLog())
			X.logs("QUERY %s: result = %s", query, result);
	}

	private QueryResult queryResult(Query q) {
		QueryResult qr = queryResults.get(q);
		if (qr == null)
			queryResults.put(q, qr = new QueryResult());
		return qr;
	}

	public void outputQueries() {
		PrintWriter out = Utils.openOut(X.path("queries.out"));
		StatFig fig = new StatFig();
		for (Query q : queryResults.keySet()) {
			QueryResult qr = queryResults.get(q);
			out.println(String.format("%s | %s %s", q, qr.numTrue, qr.numFalse));
			fig.add(qr.numTrue + qr.numFalse);
		}
		out.close();
		X.output.put("query.numHits", fig.mean());
		X.logs("  # hits per query: %s (%s total hits)", fig, fig.n);
	}

	public void initAllPasses() {
		int E = domE.size();
		int H = domH.size();
		int F = domF.size();
		int L = domL.size();

		// Compute excluded stuff
		computedExcludedClasses();
		statementIsExcluded = new boolean[E];
		fieldIsExcluded = new boolean[F];
		lockIsExcluded = new boolean[L];
		for (int e = 0; e < E; e++)
			statementIsExcluded[e] = computeStatementIsExcluded(e);
		for (int f = 1; f < F; f++)
			fieldIsExcluded[f] = computeFieldIsExcluded(f);
		for (int l = 0; l < L; l++)
			lockIsExcluded[l] = computeLockIsExcluded(l);

		X.logs("initAllPasses: |E| = %s, |H| = %s, |F| = %s, |L| = %s, excluding %s classes", E, H, F, L, excludedClasses.size());
		abstraction.init(this);
	}
  
	public void initAbstraction(Abstraction abstraction) {
		abstraction.X = X;
		abstraction.state = state;
		abstraction.listener = this;
		abstraction.require_a2o = require_a2o();
	}

	public void doneAllPasses() {
		// Evaluate on queries (real metric)
		int numTrue = 0;
		int numSelected = 0;
		for (QueryResult qr : queryResults.values()) {
			if (qr.isTrue()) numTrue++;
			numSelected++;
		}

		X.logs("  %d total queries; %d/%d = %.2f queries proposed to have property %s",
			queryResults.size(), numTrue, numSelected, 1.0*numTrue/numSelected, propertyName());
		X.output.put("query.totalNumHits", numQueryHits);
		X.output.put("query.numTrue", numTrue);
		X.output.put("query.numSelected", numSelected);
		X.output.put("query.numTotal", queryResults.size());
		X.output.put("query.fracTrue", 1.0*numTrue/numSelected);
		outputQueries();

		X.output.put("finalObjects.numTotal", state.o2h.size());
		X.output.put("numFieldAccesses", numFieldAccesses);
		X.output.put("maxCallDepth", maxCallDepth);
		X.output.put("numThreads", threadInfos.size());

		// Print out information about abstractions
		Set<Object> abstractValues = abstraction.getAbstractValues();
		int complexity = abstractValues.size(); // Complexity of this abstraction (number of abstract values)
		if (false) {
			PrintWriter out = Utils.openOut(X.path("snapshot-abstractions"));
			for (Object a : abstractValues) out.println(a);
			out.close();
		}
		X.logs("Abstract complexity: %d values", complexity);
		X.output.put("complexity", complexity);

		if (graphMonitor != null) graphMonitor.finish();
		if (fieldAccessOut != null) fieldAccessOut.close();
	}

	//////////////////////////////
	// Override these graph construction handlers (remember to call super though)
  
	public abstract void abstractionChanged(int o, Object a); // Override if necessary

	public void nodeCreated(int t, int o) {
		assert (o > 0);

		if (anyRequireGraph())
			state.o2edges.put(o, new ArrayList<Edge>());

		ThreadInfo info = threadInfo(t);
		abstraction.nodeCreated(info, o);
		if (importantLog()) {
			int h = state.o2h.get(o);
			assert (h > 0);
			X.logs("ADDNODE t=%s o=%s @ h=%s:%s | a=%s", tstr(t), ostr(o), h, hstr(h), abstraction.getValue(o));
		}
		if (graphMonitor != null) graphMonitor.addNode(o, null);
	}

	public void edgeCreated(int t, int b, int f, int o) {
		assert (b > 0 && f >= 0 && o >= 0);
		if (!anyRequireGraph()) return;

		// Strong update: remove existing field pointer
		List<Edge> edges = state.o2edges.get(b);
		if (useStrongUpdates) {
			for (int i = 0; i < edges.size(); i++) {
				if (edges.get(i).f == f) {
					int old_o = edges.get(i).o;
					abstraction.edgeDeleted(b, f, old_o);
					if (graphMonitor != null) graphMonitor.deleteEdge(b, old_o, ""+f);
					if (importantLog())
						X.logs("DELEDGE b=%s f=%s old_o=%s", ostr(b), fstr(f), ostr(old_o));
					edges.remove(i);
					break;
				}
			}
		}

		if (o != 0) {
			edges.add(new Edge(f, o));
			abstraction.edgeCreated(b, f, o);
			if (graphMonitor != null) graphMonitor.addEdge(b, o, ""+f);
			if (importantLog())
				X.logs("ADDEDGE b=%s f=%s o=%s", ostr(b), fstr(f), ostr(o));
		}
	}

	// Typically, this function is the source of queries
	TIntIntHashMap e_numHits = new TIntIntHashMap(); // e -> number of times it was hit
	int curr_e;
	public void fieldAccessed(int e, int t, int b, int f, int o) {
		// Note: o == 0 is possible
		// Note: o == -1 is possible for primitive fields
		assert (e >= 0 && b > 0 && f >= 0);
		this.curr_e = e;
		numFieldAccesses++;
		if (fieldAccessOut != null) {
			if (e_numHits.adjustOrPutValue(e, 1, 1) <= maxFieldAccessesToPrint) {
				fieldAccessOut.println(String.format("%s | %s | %s | %s %s | %s %s",
					numFieldAccesses, estr(e), t, ostr(b), astr(abstraction.getValue(b)),
					ostr(o), astr(abstraction.getValue(o))));
			}
		}
	}

	//////////////////////////////
	// Pretty-printing

	public String fstr(int f) { // field
		if (f >= ARRAY_FIELD) return "["+(f-ARRAY_FIELD)+"]";
		return f < 0 ? "-" : domF.get(f).toString();
	}
	public String hstr(int h) { return h < 0 ? "-" : domH.toUniqueString(h); } // heap allocation site
	public String estr(int e) {
		if (e < 0) return "-";
		Quad quad = (Quad)domE.get(e);
		return quad.toJavaLocStr()+" "+quad.toString();
	}
	public String mstr(int m) { return m < 0 ? "-" : domM.toUniqueString(m); } // method
	public String istr(int i) { return i < 0 ? "-" : domI.toUniqueString(i); } // call site
	public String ostr(int o) { return o < 0 ? "-" : (o == 0 ? "null" : "O"+o); } // concrete object
	public String tstr(int t) { return t < 0 ? "-" : "T"+t; } // thread
	public String lstr(int l) { return l < 0 ? "-" : domL.toUniqueString(l); } // lock

	public String astr(Object a) {
		if (abstraction instanceof NoneAbstraction) return a.toString();
		if (a == null) return "(null)";
		if (a instanceof Integer) return hstr((Integer)a); // HACK: assume it's an allocation site
		return a.toString();
	}

	////////////////////////////////////////////////////////////
	// Handlers

	@Override
	public void processBefNew(int h, int t, int o) {
		processNew(h, t, o);
	}

	@Override
	public void processNewArray(int h, int t, int o) {
		processNew(h, t, o);
	}

	private void processNew(int h, int t, int o) {
		if (h < 0 || o <= 0) return;
		state.o2h.put(o, h);
		nodeCreated(t, o);
		if (verbose >= 5)
			X.logs("EVENT new: h=%s, t=%s, o=%s", hstr(h), tstr(t), ostr(o));
	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (e < 0 || f < 0) return;
		if (isIgnore(o)) return; // Note that b corresponds to the static class
		if (verbose >= 5)
			X.logs("EVENT putStaticReference: e=%s, t=%s, b=%s, f=%s, o=%s",
				estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
		onProcessPutstaticReference(e, t, b, f, o);
	}

	public void onProcessPutstaticReference(int e, int t, int b, int f, int o) { }

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (e < 0 || f < 0) return;
		if (b == 0 || isIgnore(b)) return;
		fieldAccessed(e, t, b, f, -1);
		onProcessGetfieldPrimitive(e, t, b, f);
	}

	public void onProcessGetfieldPrimitive(int e, int t, int b, int f) { }

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) { // ... = b.f, where b.f = o
		if (e < 0 || f < 0) return;
		if (b == 0 || isIgnore(b) || isIgnore(o)) return;
		if (verbose >= 5)
			X.logs("EVENT getFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s",
				estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
		fieldAccessed(e, t, b, f, o);
		onProcessGetfieldReference(e, t, b, f, o);
	}

	public void onProcessGetfieldReference(int e, int t, int b, int f, int o) { }

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (e < 0 || f < 0) return;
		if (b == 0 || isIgnore(b)) return;
		fieldAccessed(e, t, b, f, -1);
		onProcessPutfieldPrimitive(e, t, b, f);
	}

	public void onProcessPutfieldPrimitive(int e, int t, int b, int f) { }

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) { // b.f = o
		if (e < 0 || f < 0) return;
		if (b == 0 || isIgnore(b) || isIgnore(o)) return;
		if (verbose >= 5)
			X.logs("EVENT putFieldReference: e=%s, t=%s, b=%s, f=%s, o=%s",
				estr(e), tstr(t), ostr(b), fstr(f), ostr(o));
		fieldAccessed(e, t, b, f, o);
		edgeCreated(t, b, f, o);
		onProcessPutfieldReference(e, t, b, f, o);
	}

	public void onProcessPutfieldReference(int e, int t, int b, int f, int o) { }

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		if (e < 0 || i < 0) return;
		if (b == 0 || isIgnore(b)) return;
		fieldAccessed(e, t, b, collapseArrayIndices ? ARRAY_FIELD : ARRAY_FIELD+i, -1);
	}

	@Override
	public void processBefMethodCall(int i, int t, int o) {
		if (verbose >= 5) X.logs("EVENT methodCallBefore: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
		ThreadInfo info = threadInfo(t);
		info.callSites.push(i);
		maxCallDepth = Math.max(maxCallDepth, info.callSites.size());
		//info.callAllocs.push(state.o2h.get(o));
	}

	@Override
	public void processAftMethodCall(int i, int t, int o) {
		ThreadInfo info = threadInfo(t);
		if (verbose >= 5) X.logs("EVENT methodCallAfter: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));

		// NOTE: we might not get every method after event,
		// so we might have to pop several things off the stack.
		boolean ok = false;
		while (info.callSites.size() > 0) {
			int ii = info.callSites.pop();
			if (i == ii) { ok = true; break; }
		}
		if (!ok)
			X.errors("Could not pop i=%s, leaving the stack empty", istr(i));
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		if (e < 0 || i < 0) return;
		if (b == 0 || isIgnore(b) || isIgnore(o)) return;
		if (verbose >= 5)
			X.logs("EVENT loadReference: e=%s, t=%s, b=%s, i=%s, o=%s",
				estr(e), tstr(t), ostr(b), i, ostr(o));
		fieldAccessed(e, t, b, collapseArrayIndices ? ARRAY_FIELD : ARRAY_FIELD+i, o);
	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (e < 0 || i < 0) return;
		if (b == 0 || isIgnore(b)) return;
		fieldAccessed(e, t, b, collapseArrayIndices ? ARRAY_FIELD : ARRAY_FIELD+i, -1);
	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (e < 0 || i < 0) return;
		if (b == 0 || isIgnore(b) || isIgnore(o)) return;
		if (verbose >= 5)
			X.logs("EVENT storeReference: e=%s, t=%s, b=%s, i=%s, o=%s",
				estr(e), tstr(t), ostr(b), i, ostr(o));
		fieldAccessed(e, t, b, collapseArrayIndices ? ARRAY_FIELD : ARRAY_FIELD+i, o);
		edgeCreated(t, b, collapseArrayIndices ? ARRAY_FIELD : ARRAY_FIELD+i, o);
	}

	@Override
	public void processThreadStart(int i, int t, int o) {
		assert (o > 0);
		if (isIgnore(o)) return;
		if (verbose >= 4)
			X.logs("EVENT threadStart: i=%s, t=%s, o=%s", istr(i), tstr(t), ostr(o));
		onProcessThreadStart(i, t, o);
	}

	public void onProcessThreadStart(int i, int t, int o) { }

	@Override
	public void processAcquireLock(int l, int t, int o) {
		assert (o > 0);
		if (isIgnore(o)) return;
		onProcessAcquireLock(l, t, o);
	}

	public void onProcessAcquireLock(int l, int t, int o) { }

	// Query for thread escape: is the object pointed to by the relvant variable thread-escaping at program point e?
	class ProgramPointQuery extends Query {
		public ProgramPointQuery(int e) { this.e = e; }
		public int e; // Program point
		@Override
		public boolean equals(Object _that) {
			if (_that instanceof ProgramPointQuery) {
				ProgramPointQuery that = (ProgramPointQuery)_that;
				return this.e == that.e;
			}
			return false;
		}
		@Override
		public int hashCode() { return e; }
		@Override
		public String toString() { return estr(e); }
	}
}

////////////////////////////////////////////////////////////

// Pointer via field f to object o
class Edge {
	public Edge(int f, int o) {
		this.f = f;
		this.o = o;
	}
	public int f;
	public int o;
}

abstract class Query {
}

class QueryResult {
	public int numTrue = 0;
	public int numFalse = 0;

	public boolean isTrue() { return numTrue > 0; } // Existential property
	public void add(boolean b) {
		if (b) numTrue++;
		else numFalse++;
	}

	@Override
	public String toString() { return numTrue+"|"+numFalse; }
}

@SuppressWarnings("unchecked")
class ThreadInfo {
	public Stack<Integer> callSites = new Stack(); // Elements are call sites i (for kCFA)
}

class State {
	TIntIntHashMap o2h = new TIntIntHashMap(); // object o -> heap allocation site h
	TIntObjectHashMap<List<Edge>> o2edges = new TIntObjectHashMap<List<Edge>>(); // object o -> list of outgoing edges from o
}
