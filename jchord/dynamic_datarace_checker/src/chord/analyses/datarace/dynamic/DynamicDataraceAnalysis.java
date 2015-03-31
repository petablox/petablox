/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace.dynamic;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import chord.bddbddb.Rel.IntPairIterable;
import chord.instr.InstrScheme;
import chord.analyses.lock.DomL;
import chord.analyses.lock.DomR;
import chord.analyses.heapacc.DomE;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.runtime.BasicEventHandler;
import chord.util.IntArraySet;
import chord.util.tuple.integer.IntPair;

/**
 * Dynamic datarace analysis.
 * Options:
 * 1. chord.check.combined = [true|false] default=false
 *    - do various checks denoted by chord.dynrace.check simultaneously
 * 1. chord.dynrace.check = [(a|e|t|l)*]
 *    - a = do aliasing check
 *    - e = do escaping check
 *    - t = do may-happen-in-parallel check (only thread start/join events)
 *    - l = do common guarded lock check
 * 2. chord.dynrace.thr = [concrete|abstract] default=concrete
 *    - treat threads concretely or abstractly when filtering false races
 * 3. chord.dynrace.alias = [concrete|weak_concrete|abstract]
 *    default=weak_concrete
 *    - kind of aliasing information to use for aliasing check
 * 4. chord.dynrace.esc = [concrete|weak_concrete] default=weak_concrete
 *    - kind of thread-escape information to use for escaping check
 * 5. chord.dynrace.join
 *    - model effect of thread join events
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="dynamic-datarace-java",
	consumes = { "startingRacePairs" },
	produces = { "aliasingRacePairs", "parallelRacePairs", "unguardedRacePairs",
		"combinedRacePairs", "escE" }
)
public class DynamicDataraceAnalysis extends DynamicAnalysis {
	private boolean verbose = false;
	private final IntArraySet emptySet = new IntArraySet(0);

	// accessedE == true iff heap-accessing statement with index e in domain E
	// is reached at least once during the execution.
	private boolean[] accessedE;

	// Map from heap-accessing statement with index e in domain E to the info
	// needed to determine if it is involved in a race.
	private RaceInfo[] raceInfoE;

    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToAllocSite;

	// Map from each thread to its parent thread.
	// An entry is added to this map on each thread start event.
	// Entries are never removed from this map.
	private TIntIntHashMap threadToParent;

	// Map from each thread to all its descendant threads (i.e., threads
	// spawned directly or transitively by it).
	// This map is computed entirely using threadToParent, after the
	// instrumented program terminates.
	// Entries are never removed from this map.
	private TIntObjectHashMap<IntArraySet> threadToDescendants;

	// Map from each thread t1 to the set containing each thread t2 that is
	// (directly) started by t1 but hasn't yet been joined.
	// A mapping from t1 to t2 is added to this map whenever t1 starts t2, and
	// the mapping from t1 to t2 is removed whenever t1 joins t2 (i.e., t1
	// waits for t2 to finish).
	// This map is used to populate MHP information: whenever thread t1
	// accesses heap-accessing statement with index e in domain E, each thread
	// t2 to which t1 is mapped in this map is added to the appropriate
	// element i in raceInfo[e][i].ts
	private TIntObjectHashMap<IntArraySet> threadToLiveChildren;
	
	// Set of pairs of threads (t1,t2) such that t2 may happen in parallel
	// with all actions of t1.
	// This set is computed after the instrumented program terminates.
	// It uses threadToDescendants:
	// (t1,t2) in halfParallelThreads if t2 in ancestors(t1)
	private Set<IntPair> halfParallelThreads;
	
	// Set of pairs of threads (t1,t2) such that t2 may happen in parallel
	// with all actions of t1, and vice versa.
	// The invariant t1 <= t2 holds for each pair of threads in this set.
	// This set is computed in two phases: partly during the execution of
	// the instrumented program and partly after it terminates.
	// In the first phase, (t1,t2) is added to fullParallelThreads whenever
	// t1 spawns t2.
	// In the second phase, it uses threadToDescendants:
	// (t1,t2) is added to fullParallelThreads if either
	// (t3,t2) in fullParallelThreads and t1 in descendants(t3) or
	// (t1,t3) is fullParallelThreads and t2 in descendants(t3).
	private Set<IntPair> fullParallelThreads;

	// Map from each object to a list containing each non-null instance field
	// of reference type along with its value.
	private TIntObjectHashMap<List<FldObj>> objToFldObjs;

	// Set of currently escaping concrete/abstract objects.
	private TIntHashSet escObjs;

	private TIntObjectHashMap<IntArraySet> threadToLocks;

	////// analysis configuration options //////

	private boolean checkCombined;
	private boolean checkA, checkE, checkT, checkL;
	private ThrKind thrKind;
	private AliasingCheckKind aliasingCheckKind;
	private EscapingCheckKind escapingCheckKind;
	private boolean modelJoin;
	private boolean needAbsObjs;

	private InstrScheme instrScheme;
	private DomE domE;
	private DomM domM;
	private DomI domI;
	private DomL domL;
	private DomR domR;

	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null)
			return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMainMethodEvent(true);
        instrScheme.setBefNewEvent(true, true, true);
        instrScheme.setNewArrayEvent(true, true, true);
		instrScheme.setThreadStartEvent(true, true, true);
		instrScheme.setThreadJoinEvent(true, true, true);
		instrScheme.setAcquireLockEvent(true, true, true);
		instrScheme.setReleaseLockEvent(true, true, true);
		instrScheme.setGetstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setGetstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setPutstaticPrimitiveEvent(true, true, true, true);
		instrScheme.setPutstaticReferenceEvent(true, true, true, true, true);
		instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setGetfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
		instrScheme.setPutfieldReferenceEvent(true, true, true, true, true);
		instrScheme.setAloadPrimitiveEvent(true, true, true, true);
		instrScheme.setAloadReferenceEvent(true, true, true, true, true);
		instrScheme.setAstorePrimitiveEvent(true, true, true, true);
		instrScheme.setAstoreReferenceEvent(true, true, true, true, true);
		return instrScheme;
	}

	@Override
	public void initAllPasses() { }

	private void processOptions() {
		checkCombined = System.getProperty("chord.dynrace.combined", "false").equals("true");
		String checkStr = System.getProperty("chord.dynrace.check", "aetl");
		for (char c : checkStr.toCharArray()) {
			if (c == 'a')
				checkA = true;
			else if (c == 'e')
				checkE = true;
			else if (c == 't')
				checkT = true;
			else if (c == 'l')
				checkL = true;
			else {
				Messages.fatal("Invalid value for chord.dynrace.check: " + checkStr);
			}
		}

		String thrKindStr = System.getProperty("chord.dynrace.thr", "concrete");
		if (thrKindStr.equals("concrete"))
			thrKind = ThrKind.CONCRETE;
		else if (thrKindStr.equals("abstract"))
			thrKind = ThrKind.ABSTRACT;
		else {
			Messages.fatal("Invalid value for chord.dynrace.thr: " + thrKindStr);
		}

		String aliasingCheckKindStr = System.getProperty("chord.dynrace.alias", "concrete");
		if (aliasingCheckKindStr.equals("weak_concrete"))
			aliasingCheckKind = AliasingCheckKind.WEAK_CONCRETE;
		else if (aliasingCheckKindStr.equals("concrete"))
			aliasingCheckKind = AliasingCheckKind.CONCRETE;
		else if (aliasingCheckKindStr.equals("abstract"))
			aliasingCheckKind = AliasingCheckKind.ABSTRACT;
		else {
			Messages.fatal("Invalid value for chord.dynrace.alias: " + aliasingCheckKindStr);
		}

		String escapingCheckKindStr = System.getProperty("chord.dynrace.esc", "weak_concrete");
		if (escapingCheckKindStr.equals("weak_concrete"))
			escapingCheckKind = EscapingCheckKind.WEAK_CONCRETE;
		else if (escapingCheckKindStr.equals("concrete"))
			escapingCheckKind = EscapingCheckKind.CONCRETE;
		else {
			Messages.fatal("Invalid value for chord.dynrace.esc: " + escapingCheckKindStr);
		}

		modelJoin = System.getProperty("chord.dynrace.join", "true").equals("true");
	}

	@Override
	public void initPass() {
		emptySet.setReadOnly();

		processOptions();

		modelJoin = checkT && modelJoin;

		needAbsObjs = (aliasingCheckKind == AliasingCheckKind.ABSTRACT) || (thrKind == ThrKind.ABSTRACT);

		domM = (DomM) ClassicProject.g().getTrgt("M");
		domE = (DomE) ClassicProject.g().getTrgt("E");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domL = (DomL) ClassicProject.g().getTrgt("L");
		domR = (DomR) ClassicProject.g().getTrgt("R");
		ClassicProject.g().runTask("M");
		ClassicProject.g().runTask("E");
		ClassicProject.g().runTask("I");
		ClassicProject.g().runTask("L");
		ClassicProject.g().runTask("R");
		int numE = domE.size();

		accessedE = new boolean[numE];
		raceInfoE = new RaceInfo[numE];
		for (int e = 0; e < numE; e++)
			raceInfoE[e] = new RaceInfo();
		
		if (needAbsObjs)
			objToAllocSite = new TIntIntHashMap();

		if (checkE) {
			escObjs = new TIntHashSet();
			objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
		}

		if (checkT) {
			threadToParent = new TIntIntHashMap();
			threadToDescendants = new TIntObjectHashMap<IntArraySet>();
			threadToLiveChildren = new TIntObjectHashMap<IntArraySet>();
			halfParallelThreads = new HashSet<IntPair>();
			fullParallelThreads = new HashSet<IntPair>();
		}

		if (checkL)
			threadToLocks = new TIntObjectHashMap<IntArraySet>();
	}

	private static interface IComparator {
		public boolean check(RaceElem re1, RaceElem re2);
	}
	
	private boolean existsCheck(int e1, int e2, IComparator comp) {
		List<RaceElem> l1 = raceInfoE[e1].raceElems;
		if (l1 == null) return false;
		List<RaceElem> l2 = raceInfoE[e2].raceElems;
		if (l2 == null) return false;
		int n1 = l1.size();
		int n2 = l2.size();
		for (int i = 0; i < n1; i++) {
			RaceElem re1 = l1.get(i);
			for (int j = 0; j < n2; j++) {
				RaceElem re2 = l2.get(j);
				if (comp.check(re1, re2))
					return true;
			}
		}
		return false;
	}

	private boolean forallCheck(int e1, int e2, IComparator comp) {
		List<RaceElem> l1 = raceInfoE[e1].raceElems;
		if (l1 == null) return false;
		List<RaceElem> l2 = raceInfoE[e2].raceElems;
		if (l2 == null) return false;
		int n1 = l1.size();
		int n2 = l2.size();
		for (int i = 0; i < n1; i++) {
			RaceElem re1 = l1.get(i);
			for (int j = 0; j < n2; j++) {
				RaceElem re2 = l2.get(j);
				if (!comp.check(re1, re2))
					return false;
			}
		}
		return true;
	}
	
	@Override
	public void donePass() {
		if (checkT) {
			// build threadToDescendants using threadToParent;
			// also simultaneously compute halfParallelThreads
			for (int c : threadToParent.keys()) {
				int p = threadToParent.get(c);
				while (p != 0) {
					IntArraySet s = threadToDescendants.get(p);
					if (s == null) {
						s = new IntArraySet();
						threadToDescendants.put(p, s);
					}
					s.add(c);
					halfParallelThreads.add(new IntPair(c, p));
					p = threadToParent.get(p);
				}
			}
			if (verbose) {
				for (int t : threadToDescendants.keys()) {
					System.out.print("DESCENDANTS of " + t + ": ");
					IntArraySet d = threadToDescendants.get(t);
					if (d != null)
						System.out.print(toStr(d));
					System.out.println();
				}
			}
			// do second phase of computing fullParallelThreads using
			// threadToDescendants
			Set<IntPair> s = new HashSet<IntPair>();
			for (IntPair tt : fullParallelThreads) {
				int t1 = tt.idx0;
				int t2 = tt.idx1;
				IntArraySet d1 = threadToDescendants.get(t1);
				if (d1 == null) continue;
				IntArraySet d2 = threadToDescendants.get(t2);
				if (d2 == null) continue;
				int n1 = d1.size();
				int n2 = d2.size();
				for (int i = 0; i < n1; i++) {
					int t3 = d1.get(i);
					for (int j = 0; j < n2; j++) {
						int t4 = d2.get(j);
						IntPair tt2 = (t3 < t4) ?
							new IntPair(t3, t4) : new IntPair(t4, t3);
						s.add(tt2);
					}
				}
			}
			fullParallelThreads.addAll(s);
		}

		ProgramRel relEscE = (ProgramRel) ClassicProject.g().getTrgt("escE");
		if (!checkE || checkCombined)
			relEscE.one();
		else {
			relEscE.zero();
			int numE = domE.size();
			for (int e = 0; e < numE; e++) {
				List<RaceElem> l = raceInfoE[e].raceElems;
				if (l == null) continue;
				assert (accessedE[e] == true);
				int n = l.size();
				for (int i = 0; i < n; i++) {
					RaceElem re = l.get(i);
					if (re.isEsc) {
						relEscE.add(e);
						break;
					}
				}
			}
		}

		ProgramRel relAliasingRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("aliasingRacePairs");
		if (!checkA || checkCombined)
			relAliasingRacePairs.one();
		else
			relAliasingRacePairs.zero();

		ProgramRel relParallelRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("parallelRacePairs");
		if (!checkT || checkCombined)
			relParallelRacePairs.one();
		else
			relParallelRacePairs.zero();

		// unguardedRacePairs contains those pairs in startingRacePairs that
		// are not guarded by a common lock
		// Formally: (e1,e2) is in guardedRacePairs iff for each o such that
		// e1->o and e2->o: there exists an o' such that a lock is held on o'
		// by each thread while it accesses o at e1 or e2
		ProgramRel relUnguardedRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("unguardedRacePairs");
		if (!checkL || checkCombined)
			relUnguardedRacePairs.one();
		else
			relUnguardedRacePairs.zero();

        // combinedRacePairs contains those pairs in startingRacePairs that
        // *simultaneously* satisfy aliasing, parallel, and unguarded checks
        ProgramRel relCombinedRacePairs =
            (ProgramRel) ClassicProject.g().getTrgt("combinedRacePairs");
        if (!checkCombined)
            relCombinedRacePairs.one();
        else
            relCombinedRacePairs.zero();

		ProgramRel relStartingRacePairs =
			(ProgramRel) ClassicProject.g().getTrgt("startingRacePairs");
		relStartingRacePairs.load();
		IntPairIterable startingRacePairs = relStartingRacePairs.getAry2IntTuples();
		int numReachableRaces = 0;
		for (IntPair p : startingRacePairs) {
			int e1 = p.idx0;
			int e2 = p.idx1;
			if (accessedE[e1] && accessedE[e2]) {
				numReachableRaces++;
                if (checkCombined) {
                    if (existsCheck(e1, e2, combinedComparator))
                        relCombinedRacePairs.add(e1, e2);
                } else {
					// no need to do thresc check; it has already been done if
					// checkE was true
					if (checkA && existsCheck(e1, e2, aliasComparator)) {
						relAliasingRacePairs.add(e1, e2);
						// System.out.println("ALIASING: " + eStr(e1) + "," + eStr(e2)); 
					}
					if (checkT && existsCheck(e1, e2, mhpComparator)) {
						// System.out.println("PARALLEL: " + eStr(e1) + "," + eStr(e2)); 
						relParallelRacePairs.add(e1, e2);
					}
					if (checkL && existsCheck(e1, e2, locksComparator)) {
						// System.out.println("UNGUARDED: " + eStr(e1) + "," + eStr(e2)); 
						relUnguardedRacePairs.add(e1, e2);
					}
				}
			}
       	}
		System.out.println("number of reachable races: " + numReachableRaces);
		relEscE.save();
		relAliasingRacePairs.save();
		relParallelRacePairs.save();
		relUnguardedRacePairs.save();
		relCombinedRacePairs.save();
	}

    private final IComparator combinedComparator = new IComparator() {
        public boolean check(RaceElem re1, RaceElem re2) {
			if (checkE && (!re1.isEsc || !re2.isEsc))
				return false;
			if (checkA && re1.o != re2.o)
				return false;
			if (checkT && !mhpComparator.check(re1, re2))
				return false;
			if (checkL && (re1.o != re2.o || re1.ls.overlaps(re2.ls)))
				return false;
			return true;
        }
    };

	private final IComparator aliasComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			return re1.o == re2.o;
		}
	};

	private final IComparator mhpComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			int t1 = re1.t;
			int t2 = re2.t;
			if (thrKind == ThrKind.CONCRETE && t1 == t2)
				return false;
			if (isFullParallel(t1, t2)) {
				return true;
			}
			if (!isLiveParallel(t1, re2.ts)) {
				// when t2 executes anything (including e2),
				// is t1 running in parallel?
				tmpPair.idx0 = t2;
				tmpPair.idx1 = t1;
				if (!containsThreadPair(halfParallelThreads, tmpPair))
					return false;
			}
			if (!isLiveParallel(t2, re1.ts)) {
				// when t1 executes anything (including e1),
				// is t2 running in parallel?
				tmpPair.idx0 = t1;
				tmpPair.idx1 = t2;
				if (!containsThreadPair(halfParallelThreads, tmpPair))
					return false;
			}
			return true;
		}
	};

	private final IComparator locksComparator = new IComparator() {
		public boolean check(RaceElem re1, RaceElem re2) {
			return !(re1.o != re2.o || re1.ls.overlaps(re2.ls));
		}
	};

	private boolean containsThreadPair(Set<IntPair> s, IntPair p) {
		if (thrKind == ThrKind.CONCRETE)
			return s.contains(p);
		else {
			int a1 = objToAllocSite.get(p.idx0);
			int a2 = objToAllocSite.get(p.idx1);
			for (IntPair p2 : s) {
				int a3 = objToAllocSite.get(p2.idx0);
				if (a1 != a3) continue;
				int a4 = objToAllocSite.get(p2.idx1);
				if (a2 == a4)
					return true;
			}
			return false;
		}
	}

	private IntPair tmpPair = new IntPair(0, 0);

	// Note: t1 and t2 can be in any order
	private boolean isFullParallel(int t1, int t2) {
		if (t1 < t2) {
			tmpPair.idx0 = t1;
			tmpPair.idx1 = t2;
		} else {
			tmpPair.idx0 = t2;
			tmpPair.idx1 = t1;
		}
		return fullParallelThreads.contains(tmpPair);
	}

	// Can t run in parallel while another thread (say t2) executes a
	// statement when set of t2's "live children threads" is s?
	private boolean isLiveParallel(int t, IntArraySet s) {
		int n = s.size();
		if (thrKind == ThrKind.CONCRETE) {
			for (int i = 0; i < n; i++) {
				int t2 = s.get(i);
				if (t2 == t)
					return true;
				IntArraySet s2 = threadToDescendants.get(t2);
				if (s2 != null && s2.contains(t))
					return true;
			}
			return false;
		} else {
			int a = objToAllocSite.get(t);
			for (int i = 0; i < n; i++) {
				int t2 = s.get(i);
				if (objToAllocSite.get(t2) == a)
					return true;
				IntArraySet s2 = threadToDescendants.get(t2);
				if (s2 != null) {
					int n2 = s2.size();
					for (int j = 0; j < n2; j++) {
						int t3 = s2.get(j);
						if (objToAllocSite.get(t3) == a)
							return true;
					}
				}
			}
			return false;
		}
	}

	private String iStr(int i) { return (i < 0) ? "-1" : domI.get(i).toJavaLocStr(); }
	private String eStr(int e) { return (e < 0) ? "-1" : domE.get(e).toJavaLocStr(); }
	private String lStr(int l) { return (l < 0) ? "-1" : domL.get(l).toJavaLocStr(); }
	private String rStr(int r) { return (r < 0) ? "-1" : domR.get(r).toJavaLocStr(); }

	@Override
	public void doneAllPasses() { }

	@Override
	public void processEnterMainMethod(int t) {
		if (verbose) System.out.println("MAIN: " + t);
	}

	@Override
    public void processBefNew(int h, int t, int o) {
		processNewOrNewArray(h, t, o);
	}

	@Override
	public void processAftNew(int h, int t, int o) {
		// do nothing
	}

	@Override
    public void processNewArray(int h, int t, int o) {
		processNewOrNewArray(h, t, o);
	}

	private void processNewOrNewArray(int h, int t, int o) {
		if (verbose) System.out.println("NEW: " + h + " " + t + " " + o);
        if (o == 0) return;
		if (needAbsObjs) {
			if (h >= 0)
				objToAllocSite.put(o, h);
			else
				objToAllocSite.remove(o);
		}
		if (checkE) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
		}
	}

	@Override
	public void processThreadStart(int p, int t, int o) {
		if (verbose) System.out.println("START: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (checkT) {
			int t2 = threadToParent.get(o);
			assert (t2 == 0);
			threadToParent.put(o, t);
			IntArraySet s = threadToLiveChildren.get(t);
			if (s == null)
				s = new IntArraySet(1);
			else {
				int n = s.size();
				for (int i = 0; i < n; i++) {
					int o2 = s.get(i);
					assert (o2 != o);
					IntPair tt = (o2 < o) ? new IntPair(o2, o) : new IntPair(o, o2);
					System.out.println("ADDING1 to fullParallel: " + tt);
					fullParallelThreads.add(tt);
				}
				s = new IntArraySet(s);
			}
			s.add(o);
			threadToLiveChildren.put(t, s);
		}
		if (checkE)
			markAndPropEsc(o);
	}

	@Override
	public void processThreadJoin(int p, int t, int o) {
		if (verbose) System.out.println("JOIN: " + iStr(p) + " " + t + " " + o);
		assert (o > 0);
		if (modelJoin) {
			IntArraySet s = threadToLiveChildren.get(t);
			if (s != null && s.contains(o)) {
				int n = s.size();
				IntArraySet s2 = new IntArraySet(n - 1);
				for (int i = 0; i < n; i++) {
					int o2 = s.get(i);
					if (o2 != o)
						s2.add(o2);
				}
				threadToLiveChildren.put(t, s2);
			}
		}
	}

	@Override
	public void processAcquireLock(int p, int t, int l) {
		if (verbose) System.out.println("ACQLOCK: " + lStr(p) + " t=" + t);
		if (checkL) {
			IntArraySet locks = threadToLocks.get(t);
			if (locks == null)
				locks = new IntArraySet(1);
			else {
				int n = locks.size();
				IntArraySet newLocks = new IntArraySet(n + 1);
				for (int i = 0; i < n; i++) {
					int l2 = locks.get(i);
					newLocks.addForcibly(l2);
				}
				locks = newLocks;
			}
			locks.addForcibly(l);
			threadToLocks.put(t, locks);
		}
	}

	@Override
	public void processReleaseLock(int p, int t, int l) {
		if (verbose) System.out.println("RELLOCK: " + rStr(p) + " t=" + t);
		if (checkL) {
			IntArraySet locks = threadToLocks.get(t);
			if (locks == null) {
				System.out.println("WARNING: no locks on stack"); 
			} else {
				int i = locks.size() - 1;
				while (true) {
					int l2 = locks.get(i);
					if (l2 == l)
						break;
					System.out.println("WARNING: popping off unmatched lock: " + l2);
					if (i == 0) break;
					--i;
				}
				IntArraySet newLocks;
				if (i == 0)
					newLocks = emptySet;
				else {
					newLocks = new IntArraySet(i);
					for (int j = 0; j < i; j++) {
						int l3 = locks.get(j);
						newLocks.addForcibly(l3);
					}
				}
				threadToLocks.put(t, newLocks);
			}
		}
	}

	@Override
	public void processGetstaticPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("GETSTATIC: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, -1, -1);
	}

	@Override
	public void processGetstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETSTATIC: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, -1, -1);
	}

	@Override
	public void processPutstaticPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, -1, -1);
	}

	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTSTATIC: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, -1, -1);
		if (checkE) {
	  		if (o != 0)
				markAndPropEsc(o);
		}
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("GETFIELD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, -1);
	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (verbose) System.out.println("PUTFIELD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, -1);
		processHeapWr(e, t, b, f, o);
	}

	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ALOAD: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, i);
	}

	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (verbose) System.out.println("ASTORE: " + eStr(e) + " t=" + t);
		processHeapRd(e, t, b, i);
		processHeapWr(e, t, b, i, o);
	}

	private void processHeapRd(int e, int t, int b, int i) {
		if (e < 0) return;
		accessedE[e] = true;
		if (b == 0) return;
		RaceElem re = new RaceElem();
		if (checkT) {
			re.t = t;
			IntArraySet ts = threadToLiveChildren.get(t);
			if (ts == null)
				ts = emptySet;
			re.ts = ts;
		} else {
			re.t = 0;
			re.ts = emptySet;
		}
		if (checkL) {
			IntArraySet locks = threadToLocks.get(t);
			re.ls = (locks != null) ? locks : emptySet;
		} else
			re.ls = emptySet;
		switch (aliasingCheckKind) {
		case WEAK_CONCRETE:
			// ignore array element index for WEAK_CONCRETE
			// i.e., do not distinguish between different elements of same array
			re.o = b;
			break;
		case CONCRETE:
			re.o = (i == -1) ? b : BasicEventHandler.getPrimitiveId(b, i);
			break;
		case ABSTRACT:
			re.o = objToAllocSite.get(b);
			break;
		default:
			Messages.fatal("Unknown aliasing check kind: " + aliasingCheckKind);
		}
		if (checkE) {
			if (b == -1 || escObjs.contains(b))
				re.isEsc = true;
		}
		raceInfoE[e].addRaceElem(re);
	}

	private void processHeapWr(int e, int t, int b, int f, int r) {
		if (checkE) {
			if (e < 0 || b == 0 || f < 0)
				return;
			List<FldObj> l = objToFldObjs.get(b);
			if (r == 0) {
				// this is a strong update; so remove field f if it is there
				if (l != null) {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = l.get(i);
						if (fo.f == f) {
							l.remove(i);
							break;
						}
					}
				}
				return;
			}
			boolean added = false;
			if (l == null) {
				l = new ArrayList<FldObj>();
				objToFldObjs.put(b, l);
			} else {
				for (FldObj fo : l) {
					if (fo.f == f) {
						fo.o = r;
						added = true;
						break;
					}
				}
			}
			if (!added)
				l.add(new FldObj(f, r));
			if (escObjs.contains(b))
				markAndPropEsc(r);
		}
	}

	private void markAndPropEsc(int o) {
		if (escObjs.add(o)) {
			List<FldObj> l = objToFldObjs.get(o);
			if (l != null) {
				for (FldObj fo : l)
					markAndPropEsc(fo.o);
			}
		}
	}

	private static String toStr(IntArraySet s) {
		String str = "";
		int n = s.size();
		for (int i = 0; i < n; i++)
			str += s.get(i) + ","; 
		return str;
	}
}

class RaceElem {
	int t;	// ID of accessing thread
	long o;	// ID of object accessed:
			// 0 if static field access, o if instance field access,
			// o+i if array element access
	boolean isEsc;		// whether or not accessed object thread-escapes
	IntArraySet ls;		// locks held by this thread during this access
	IntArraySet ts;		// threads that may run in parallel when this
						// thread executes this access
	public int hashCode() {
		return (int) o;
	}
	public boolean equals(Object o) {
		if (o instanceof RaceElem)
			return equals((RaceElem) o);
		return false;
	}
	public boolean equals(RaceElem that) {
		return t == that.t && o == that.o && isEsc == that.isEsc &&
			ts.equals(that.ts) && ls.equals(that.ls);
	}
	public String toString() {
		String str = "t=" + t + ", o=" + o + ", isEsc=" + isEsc + ", ls=";
		if (ls == null)
			str += "null,";
		else {
			int n = ls.size();
			for (int i = 0; i < n; i++)
				str += ls.get(i) + ","; 
		}
		str += " ts=";
		if (ts == null)
			str += "null";
		else {
			int n = ts.size();
			for (int i = 0; i < n; i++)
				str += ts.get(i) + ","; 
		}
		return str;
	}
}

class RaceInfo {
	List<RaceElem> raceElems;  // allocated lazily
	public void addRaceElem(RaceElem re) {
		if (raceElems == null) {
			raceElems = new ArrayList<RaceElem>();
			raceElems.add(re);
		} else if (!raceElems.contains(re))
			raceElems.add(re);
	}
}
