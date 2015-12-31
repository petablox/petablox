package petablox.project.analyses.rhs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gnu.trove.map.hash.TObjectIntHashMap;
import petablox.util.soot.*;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import petablox.util.tuple.object.Pair;
import petablox.program.Loc;
import petablox.analyses.alias.ICICG;
import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.project.ClassicProject;
import petablox.project.analyses.JavaAnalysis;
import petablox.util.ArraySet;
import petablox.util.Alarm;
import petablox.project.Messages;

/**
 * Implementation of the Reps-Horwitz-Sagiv algorithm for context-sensitive dataflow analysis.
 *
 * Relevant system properties:
 * - chord.rhs.merge = [lossy|pjoin|naive] (default = lossy)
 * - chord.rhs.order = [bfs|dfs] (default = bfs) 
 * - chord.rhs.trace = [none|any|shortest] (default = none)
 * - chord.rhs.timeout = N milliseconds (default N = 0, no timeouts)
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class RHSAnalysis<PE extends IEdge, SE extends IEdge> extends JavaAnalysis {
    protected static boolean DEBUG = false;
    protected static final String CHORD_RHS_MERGE_PROPERTY = "chord.rhs.merge";
    protected static final String CHORD_RHS_ORDER_PROPERTY = "chord.rhs.order";
    protected static final String CHORD_RHS_TRACE_PROPERTY = "chord.rhs.trace";
    protected static final String CHORD_RHS_TIMEOUT_PROPERTY = "chord.rhs.timeout";

    protected List<Pair<Loc, PE>> workList = new ArrayList<Pair<Loc, PE>>();
    protected Map<Unit, Set<PE>> pathEdges = new HashMap<Unit, Set<PE>>();
    protected Map<SootMethod, Set<SE>> summEdges = new HashMap<SootMethod, Set<SE>>();
    protected DomI domI;
    protected DomM domM;
    protected ICICG cicg;
    protected TObjectIntHashMap<Unit> quadToRPOid;
    protected Map<Unit, Loc> invkQuadToLoc;
    protected Map<SootMethod, Set<Unit>> callersMap = new HashMap<SootMethod, Set<Unit>>();
    protected Map<Unit, Set<SootMethod>> targetsMap = new HashMap<Unit, Set<SootMethod>>();

    protected Map<Pair<Unit, PE>, WrappedPE<PE, SE>> wpeMap = new HashMap<Pair<Unit, PE>, WrappedPE<PE, SE>>();
    protected Map<Pair<SootMethod, SE>, WrappedSE<PE, SE>> wseMap = new HashMap<Pair<SootMethod, SE>, WrappedSE<PE, SE>>();

    protected boolean isInit, isDone;

    protected MergeKind mergeKind;
    protected OrderKind orderKind;
    protected TraceKind traceKind;

    private int timeout;
    private Alarm alarm;

    protected boolean mustMerge;
    protected boolean mayMerge;

    /*********************************************************************************
     * Methods that clients must define.
     *********************************************************************************/

    // get the initial set of path edges
    public abstract Set<Pair<Loc, PE>> getInitPathEdges();

    // get the path edge(s) in callee for target method m called from call site q
    // with caller path edge pe
    public abstract PE getInitPathEdge(Unit q, SootMethod m, PE pe);

    // get outgoing path edge(s) from q, given incoming path edge pe into q.
    // q is guaranteed to not be an invoke statement, return statement, entry
    // basic block, or exit basic block.
    // the set returned can be reused by client.
    public abstract PE getMiscPathEdge(Unit q, PE pe);
 
    // q is an invoke statement and m is the target method.
    // get path edge to successor of q given path edge into q and summary edge of a
    // target method m of the call site q.
    // returns null if the path edge into q is not compatible with the summary edge.
    public abstract PE getInvkPathEdge(Unit q, PE clrPE, SootMethod m, SE tgtSE);

    public abstract PE getPECopy(PE pe);

    public abstract SE getSECopy(SE pe);

    // m is a method and pe is a path edge from entry to exit of m
    // (in case of forward analysis) or vice versa (in case of backward analysis)
    // that must be lifted to a summary edge.
    public abstract SE getSummaryEdge(SootMethod m, PE pe);

    /**
     * Provides the call graph to be used by the analysis.
     *
     * @return  The call graph to be used by the analysis.
     */
    public abstract ICICG getCallGraph();

    /*********************************************************************************
     * Methods that clients might want to override but is not mandatory; alternatively,
     * their default behavior can be changed by setting relevant chord.rhs.* property.
     *********************************************************************************/

    /**
     * Determines how this analysis must merge PEs at each program point that
     * have the same source state but different target states and, likewise,
     * SEs of each method that have the same source state but different target
     * states.
     */
    public void setMergeKind() {
        String s = System.getProperty(CHORD_RHS_MERGE_PROPERTY, "lossy");
        if (s.equals("lossy"))
            mergeKind = MergeKind.LOSSY;
        else if (s.equals("pjoin"))
            mergeKind = MergeKind.PJOIN;
        else if (s.equals("naive"))
            mergeKind = MergeKind.NAIVE;
        else
            throw new RuntimeException("Bad value for property " + CHORD_RHS_MERGE_PROPERTY + ": " + s);
    }

    public void setOrderKind() {
        String s = System.getProperty(CHORD_RHS_ORDER_PROPERTY, "bfs");
        if (s.equals("bfs"))
            orderKind = OrderKind.BFS;
        else if (s.equals("dfs"))
            orderKind = OrderKind.DFS;
        else
            throw new RuntimeException("Bad value for property " + CHORD_RHS_ORDER_PROPERTY + ": " + s);
    }

    public void setTraceKind() {
        String s = System.getProperty(CHORD_RHS_TRACE_PROPERTY, "none");
        if (s.equals("none"))
            traceKind = TraceKind.NONE;
        else if (s.equals("any"))
            traceKind = TraceKind.ANY;
        else if (s.equals("shortest"))
            traceKind = TraceKind.SHORTEST;
        else
            throw new RuntimeException("Bad value for property " + CHORD_RHS_TRACE_PROPERTY + ": " + s);
    }

    public void setTimeout() {
        timeout = Integer.getInteger("chord.rhs.timeout", 0);
    }

    /*********************************************************************************
     * Methods that client may call/override.  Example usage:
     * init();
     * while (*) {
     *   runPass();
     *   // done building path/summary edges; clients can now call:
     *   // getPEs(i), getSEs(m), getAllPEs(), getAllSEs(),
     *   // getBackTracIterator(pe), print()
     * }
     * done();
     *********************************************************************************/

    public void init() {
        if (isInit) return;
        isInit = true;

        // start configuring the analysis
        setMergeKind();
        setOrderKind();
        setTraceKind();
        mayMerge  = (mergeKind != MergeKind.NAIVE);
        mustMerge = (mergeKind == MergeKind.LOSSY);
        if (mustMerge && !mayMerge) {
            Messages.fatal("Cannot create RHS analysis '" + getName() + "' with mustMerge but without mayMerge.");
        }
        if (mustMerge && traceKind != TraceKind.NONE) {
            Messages.fatal("Cannot create RHS analysis '" + getName() + "' with mustMerge and trace generation.");
        }
        setTimeout();
        // done configuring the analysis

        if (timeout > 0) {
            alarm = new Alarm(timeout);
            alarm.initAllPasses();
        }
        domI = (DomI) ClassicProject.g().getTrgt("I");
        ClassicProject.g().runTask(domI);
        domM = (DomM) ClassicProject.g().getTrgt("M");
        ClassicProject.g().runTask(domM);
        cicg = getCallGraph();
        quadToRPOid = new TObjectIntHashMap<Unit>();
        invkQuadToLoc = new HashMap<Unit, Loc>();
        for (SootMethod m : cicg.getNodes()) {
            if (m.isAbstract()) continue;
            CFG cfg = SootUtilities.getCFG(m);
            quadToRPOid.put(cfg.getHeads().get(0).getHead(), 0);
            int rpoId = 1;
            for (Block bb : cfg.reversePostOrder()) {
            	int i = 0;
            	Iterator<Unit> itr = bb.iterator();
                while(itr.hasNext()) {
                    Unit q = itr.next();
                    quadToRPOid.put(q, rpoId++);
                    if (SootUtilities.isInvoke(q)) {
                        Loc loc = new Loc(q, i);
                        invkQuadToLoc.put(q, loc);
                    }
                    i++;
                }
            }
            quadToRPOid.put(cfg.getTails().get(0).getHead(), rpoId);
        }
    }

    public void done() {
        if (isDone) return;
        isDone = true;
        if (timeout > 0)
            alarm.doneAllPasses();
    }

    /**
     * Run an instance of the analysis afresh.
     * Clients may call this method multiple times from their {@link #run()} method.
     */
    public void runPass() throws TimeoutException {
        if (timeout > 0)
            alarm.initNewPass();
        // clear these sets since client may call this method multiple times
        workList.clear();
        summEdges.clear();
        pathEdges.clear();
        wpeMap.clear();
        wseMap.clear();
        Set<Pair<Loc, PE>> initPEs = getInitPathEdges();
        for (Pair<Loc, PE> pair : initPEs) {
            Loc loc = pair.val0;
            PE pe = pair.val1;
            addPathEdge(loc, pe, null, null, null, null);
        }
        propagate();
    }

    // TODO: might have to change the argument type to PE
    public BackTraceIterator<PE,SE> getBackTraceIterator(IWrappedPE<PE, SE> wpe) {
        if (traceKind == TraceKind.NONE) {
            throw new RuntimeException("trace generation not enabled");
        }
        return new BackTraceIterator<PE,SE>(wpe);
    }

    public Set<PE> getPEs(Unit i) {
        return pathEdges.get(i);
    }

    public Map<Unit, Set<PE>> getAllPEs() {
        return pathEdges;
    }

    public Set<SE> getSEs(SootMethod m) {
        return summEdges.get(m);
    }

    public Map<SootMethod, Set<SE>> getAllSEs() {
        return summEdges;
    }

    public void print() {
        for (SootMethod m : summEdges.keySet()) {
            System.out.println("SE of " + m);
            Set<SE> seSet = summEdges.get(m);
            if (seSet != null) {
                for (SE se : seSet)
                    System.out.println("\tSE " + se);
            }
        }
        for (Unit i : pathEdges.keySet()) {
            System.out.println("PE of " + (Unit)i);
            Set<PE> peSet = pathEdges.get(i);
            if (peSet != null) {
                for (PE pe : peSet)
                    System.out.println("\tPE " + pe);
            }
        }
    }
        
    /*********************************************************************************
     * Internal methods.
     *********************************************************************************/

    protected Set<Unit> getCallers(SootMethod m) {
        Set<Unit> callers = callersMap.get(m);
        if (callers == null) {
            callers = cicg.getCallers(m);
            callersMap.put(m, callers);
        }
        return callers;
    }

    protected final Set<SootMethod> getTargets(Unit i) {
        Set<SootMethod> targets = targetsMap.get(i);
        if (targets == null) {
            targets = cicg.getTargets(i);
            targetsMap.put(i, targets);
        }
        return targets;
    }
    
    protected boolean skipMethod(Unit q, SootMethod m, PE predPe, PE pe){
    	return false;
    }
    
    protected boolean jumpToMethodEnd(Unit q, SootMethod m, PE predPe, PE pe){
    	return false;
    }
    
    /**
     * Propagate analysis results until fixpoint is reached.
     */
    private void propagate() throws TimeoutException {
        while (!workList.isEmpty()) {
            if (timeout > 0 && alarm.passTimedOut()) {
                System.out.println("TIMED OUT");
                throw new TimeoutException();
            }
            if (DEBUG) {
                System.out.println("WORKLIST:");
                for (Pair<Loc, PE> pair : workList) {
                    Loc loc = pair.val0;
                    int id = quadToRPOid.get(loc.i);
                    System.out.println("\t" + pair + " " + id);
                }
            }
            int last = workList.size() - 1;
            Pair<Loc, PE> pair = workList.remove(last);
            Loc loc = pair.val0;
            PE pe = pair.val1;
            Unit i = loc.i;
            if (DEBUG) System.out.println("Processing loc: " + loc + " PE: " + pe);
            if (i instanceof JEntryExitNopStmt) {
                // i is either method entry basic block, method exit basic block, or an empty basic block
                if (i instanceof JEntryNopStmt) {
                    processEntry(i, pe);
                } else if (i instanceof JExitNopStmt) {
                    processExit(i, pe);
                }
                // TODO: Verify that empty basic block case is irrelevant
                /*else {
					final PE pe2 = mayMerge ? getPECopy(pe) : pe;
                    propagatePEtoPE(loc, pe2, pe, null, null);
				}*/
            } else {
                Unit q = i;
                // invoke or misc quad
                if (SootUtilities.isInvoke(q)) {
                    processInvk(loc, pe);
                } else {
                    PE pe2 = getMiscPathEdge(q, pe);
                    propagatePEtoPE(loc, pe2, pe, null, null);
                }
            }
        }
    }

    protected void processInvk(final Loc loc, final PE pe) {
        final Unit q = (Unit) loc.i;
        final Set<SootMethod> targets = getTargets(q);
        if (targets.isEmpty()) {
            final PE pe2 = mayMerge ? getPECopy(pe) : pe;
            propagatePEtoPE(loc, pe2, pe, null, null);
        } else {
            for (SootMethod m2 : targets) {
                if (DEBUG) System.out.println("\tTarget: " + m2);
                final PE pe2 = getInitPathEdge(q, m2, pe);
                
                if (skipMethod(q, m2, pe, pe2)) {
                	final PE pe3 = mayMerge ? getPECopy(pe) : pe;
                    propagatePEtoPE(loc, pe3, pe, null, null);
                } else if(jumpToMethodEnd(q, m2, pe, pe2)){
                	Unit bb2 = SootUtilities.getCFG(m2).getHeads().get(0).getHead();
                    Loc loc2 = new Loc(bb2, -1);
                    int initWorkListSize = workList.size();
                    addPathEdge(loc2, pe2, q, pe, null, null);
                    int finalWorkListSize = workList.size();
                    if(initWorkListSize != finalWorkListSize){
                    	//This operation is safe since any entry added to the worklist will necessarily be at the last
                    	//position irrespective of BFS or DFS updates
                    	Pair<Loc,PE> entryP = workList.remove(finalWorkListSize - 1); 
                    	Unit bb3 = SootUtilities.getCFG(m2).getTails().get(0).getHead();
                        Loc loc3 = new Loc(bb3, -1);
                    	PE pe3 = mayMerge ? getPECopy(entryP.val1) : entryP.val1;
                        addPathEdge(loc3, pe3, bb2, entryP.val1, null, null);
                    }                    
                } else {
                	Unit bb2 = SootUtilities.getCFG(m2).getHeads().get(0).getHead();
                    Loc loc2 = new Loc(bb2, -1);
                    addPathEdge(loc2, pe2, q, pe, null, null);
                }
                final Set<SE> seSet = summEdges.get(m2);
                if (seSet == null) {
                    if (DEBUG) System.out.println("\tSE set empty");
                    continue;
                }
                for (SE se : seSet) {
                    if (DEBUG) System.out.println("\tTesting SE: " + se);
                    if (propagateSEtoPE(pe, loc, m2, se)) {
                        if (DEBUG) System.out.println("\tMatched");
                        if (mustMerge) {
                            // this was only SE; stop looking for more
                            break;
                        }
                    } else {
                        if (DEBUG) System.out.println("\tDid not match");
                    }
                }
            }
        }
    }

    private void processEntry(Unit b, PE pe) {
        Block bb = SootUtilities.getBlock(b);
        for (Block bb2 : bb.getSuccs()) {
            Unit i2; int q2Idx;
            if (bb2.getHead() instanceof JEntryExitNopStmt) {
                i2 = bb2.getHead();
                q2Idx = -1;
            } else {
                i2 = bb2.getHead();
                q2Idx = 0;
            }
            Loc loc2 = new Loc(i2, q2Idx);
            PE pe2 = mayMerge ? getPECopy(pe) : pe;
            addPathEdge(loc2, pe2, b, pe, null, null);
        }
    }

    protected void processExit(Unit b, PE pe) {
    	Unit head = b;
        SootMethod m = SootUtilities.getMethod(head);
        SE se = getSummaryEdge(m, pe);
        Set<SE> seSet = summEdges.get(m);
        if (DEBUG) System.out.println("\tChecking if " + m + " has SE: " + se);
        SE seToAdd = se;
        if (seSet == null) {
            seSet = new HashSet<SE>();
            summEdges.put(m, seSet);
            seSet.add(se);
            if (DEBUG) System.out.println("\tNo, adding it as first SE");
        } else if (mayMerge) {
            boolean matched = false;
            for (SE se2 : seSet) {
                int result = se2.canMerge(se, mustMerge);
                if (result >= 0) {
                    if (DEBUG) System.out.println("\tNo, but matches SE: " + se2);
                    boolean changed = se2.mergeWith(se);
                    if (DEBUG) System.out.println("\tNew SE after merge: " + se2);
                    if (!changed) {
                        if (DEBUG) System.out.println("\tExisting SE did not change");
                        if (traceKind != TraceKind.NONE && result == 0)
                            updateWSE(m, se2, b, pe);
                        return;
                    }
                    if (DEBUG) System.out.println("\tExisting SE changed");
                    // se2 is already in summEdges(m), so no need to add it
                    seToAdd = se2;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (DEBUG) System.out.println("\tNo, adding");
                seSet.add(se);
            }
        } else if (!seSet.add(se)) {
            if (DEBUG) System.out.println("\tYes, not adding");
            if (traceKind != TraceKind.NONE)
                updateWSE(m, se, b, pe);
            return;
        }
        if (traceKind != TraceKind.NONE) {
            recordWSE(m, seToAdd, b, pe);
        }
        for (Unit q2 : getCallers(m)) {
            if (DEBUG) System.out.println("\tCaller: " + q2 + " in " + SootUtilities.getMethod(q2));
            Set<PE> peSet = pathEdges.get(q2);
            if (peSet == null)
                continue;
            // make a copy as propagateSEtoPE might add a path edge to this set itself;
            // in this case we could get a ConcurrentModification exception if we don't
            // make a copy.
            List<PE> peList = new ArrayList<PE>(peSet);
            Loc loc2 = invkQuadToLoc.get(q2);
            for (PE pe2 : peList) {
                if (DEBUG) System.out.println("\tTesting PE: " + pe2);
                boolean match = propagateSEtoPE(pe2, loc2, m, seToAdd);
                if (match) {
                    if (DEBUG) System.out.println("\tMatched");
                } else {
                    if (DEBUG) System.out.println("\tDid not match");
                }
            }
        }
    }

    // Add 'pe' as an incoming PE into loc.
    // 'predPE' and 'predSE' are treated as the provenance of 'pe', where 'predPE' is incoming PE into 'pred'.
    // 'predPE' is null iff 'predI' is null.
    // 'predSE' is null iff 'predM' is null.
    // 'loc' may be anything: entry basic block, exit basic block, invk quad, or misc quad.
    protected void addPathEdge(Loc loc, PE pe, Unit predI, PE predPE, SootMethod predM, SE predSE) {
        if (DEBUG) System.out.println("\tChecking if " + loc + " has PE: " + pe);
        Unit i = loc.i;
        Set<PE> peSet = pathEdges.get(i);
        PE peToAdd = pe;
        if (peSet == null) {
            peSet = new HashSet<PE>();
            pathEdges.put(i, peSet);
            peSet.add(pe);
            if (DEBUG) System.out.println("\tNo, adding it as first PE");
        } else if (mayMerge) {
            boolean matched = false;
            for (PE pe2 : peSet) {
                int result = pe2.canMerge(pe, mustMerge);
                if (result >= 0) {
                    if (DEBUG) System.out.println("\tNo, but matches PE: " + pe2);
                    boolean changed = pe2.mergeWith(pe);
                    if (DEBUG) System.out.println("\tNew PE after merge: " + pe2); 
                    if (!changed) {
                        if (DEBUG) System.out.println("\tExisting PE did not change");
                        if (traceKind != TraceKind.NONE && result == 0)
                            updateWPE(i, pe2, predI, predPE, predM, predSE);
                        return;
                    }
                    if (DEBUG) System.out.println("\tExisting PE changed");
                    // pe2 is already in pathEdges(i) so no need to add it; but it may or may not be in workList
                    for (int j = workList.size() - 1; j >= 0; j--) {
                        Pair<Loc, PE> pair = workList.get(j);
                        PE pe3 = pair.val1;
                        if (pe3 == pe2) {
                            assert (loc.equals(pair.val0));
                            if (traceKind != TraceKind.NONE) {
                                recordWPE(i, pe2, predI, predPE, predM, predSE);
                            }
                            return;
                        }
                    }
                    peToAdd = pe2;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (DEBUG) System.out.println("\tNo, adding");
                peSet.add(pe);
            }
        } else if (!peSet.add(pe)) {
            if (DEBUG) System.out.println("\tYes, not adding");
            if (traceKind != TraceKind.NONE)
                updateWPE(i, pe, predI, predPE, predM, predSE);
            return;
        }
        assert (peToAdd != null);
        if (traceKind != TraceKind.NONE) {
            recordWPE(i, peToAdd, predI, predPE, predM, predSE);
        }
        Pair<Loc, PE> pair = new Pair<Loc, PE>(loc, peToAdd);
        int j = workList.size() - 1;
        if (orderKind == OrderKind.BFS) {
            SootMethod m = SootUtilities.getMethod(loc.i);
            int rpoId = quadToRPOid.get(i);
            for (; j >= 0; j--) {
                Loc loc2 = workList.get(j).val0;
                Unit i2 = loc2.i;
                if (SootUtilities.getMethod(i2) != m) break;
                int rpoId2 = quadToRPOid.get(i2);
                if (rpoId2 > rpoId)
                    break;
            }
        }
        if (DEBUG) System.out.println("\tAlso adding to worklist at " + (j + 1));
        workList.add(j + 1, pair);
    }

    // Adds 'pe' as an incoming PE into each immediate successor of 'loc'.
    // 'predPE' and 'predSE' are treated as the provenance of 'pe', where 'predPE' is incoming PE into 'loc'.
    // 'predPE' is guaranteed to be non-null but 'predSE' may be null.
    protected void propagatePEtoPE(Loc loc, PE pe, PE predPE, SootMethod predM, SE predSE) {
        int qIdx = loc.qIdx;
        Unit i = loc.i;
        Block bb = CFG.getBasicBlock(i);
        Iterator<Unit> uit=bb.iterator();
        int curIdx=-1;
        while(uit.hasNext()){
        	uit.next();
        	curIdx=curIdx+1;
        	if (qIdx==curIdx && uit.hasNext()) {
        		int q2Idx = qIdx + 1;
                Unit q2 = uit.next(); 
                Loc loc2 = new Loc(q2, q2Idx);                           
                addPathEdge(loc2, pe, i, predPE, predM, predSE);
                return;
            }
        }
        boolean isFirst = true;
        for (Block bb2 : bb.getSuccs()) {
            Unit i2; int q2Idx;
            if(bb2.getHead() instanceof JEntryExitNopStmt){
            	i2 = bb2.getHead();
            	q2Idx = -1;
            }else{
            	i2 = bb2.getHead();
            	q2Idx = 0;
            }
            Loc loc2 = new Loc(i2, q2Idx);
            PE pe2;
            if (!mayMerge)
                pe2 = pe;
            else {
                if (isFirst) {
                    pe2 = pe;
                    isFirst = false;
                } else
                    pe2 = getPECopy(pe);
            }
            addPathEdge(loc2, pe2, i, predPE, predM, predSE);
        }
    }

    protected boolean propagateSEtoPE(PE clrPE, Loc loc, SootMethod tgtM, SE tgtSE) {
        Unit q = (Unit) loc.i;
        PE pe2 = getInvkPathEdge(q, clrPE, tgtM, tgtSE);
        if (pe2 == null)
            return false;
        propagatePEtoPE(loc, pe2, clrPE, tgtM, tgtSE);
        return true;
    }

    /*********************************************************************************
     * Provenance record/update methods.
     *********************************************************************************/

    /**
     * Potentially updates provenance of given SE.
     * Pre-condition: traceKind != NONE.
     * (m, seToAdd): the SE whose provenance will be potentially updated.
     * (bb, predPE): the new provenance of the SE.
     * Replace the old provenance with the new provenance if traceKind is SHORTEST
     * and new provenance has shorter length than old provenance.
     */
    protected void updateWSE(SootMethod m, SE seToAdd, Unit bb, PE predPE) {
        assert (seToAdd != null && predPE != null);
        Pair<SootMethod, SE> p = new Pair<SootMethod, SE>(m, seToAdd);
        WrappedSE<PE, SE> wse = wseMap.get(p);
        assert (wse != null);
        if (traceKind == TraceKind.SHORTEST) {
            int oldLen = wse.getLen();
            Pair<Unit, PE> predP = new Pair<Unit, PE>(bb, predPE);
            WrappedPE<PE, SE> newWPE = wpeMap.get(predP);
            assert (newWPE != null);
            int newLen = 1 + newWPE.getLen();
            if (newLen < oldLen)
                wse.update(newWPE, newLen);
        }
    }

    /**
     * Potentially updates provenance of given PE.
     * Pre-condition: traceKind != NONE.
     * (i, peToAdd): the PE whose provenance will be potentially updated.
     * (predI, predPE, predM, predSE): the new provenance of the PE.
     * Replace the old provenance with the new provenance if traceKind is SHORTEST
     * and new provenance has shorter length than old provenance.
     */
    private void updateWPE(Unit i, PE peToAdd, Unit predI, PE predPE,SootMethod predM, SE predSE) {
        assert (peToAdd != null);
        // predPE and/or predSE may be null
        Pair<Unit, PE> p = new Pair<Unit, PE>(i, peToAdd);
        WrappedPE<PE, SE> wpe = wpeMap.get(p);
        assert (wpe != null);
        if (traceKind == TraceKind.SHORTEST) {
            int oldLen = wpe.getLen();
            if (predPE == null) {
                assert (oldLen == 0);
                // cannot reduce below 0, so return
                return;
            }
            int newLen;
            Pair<Unit, PE> predP = new Pair<Unit, PE>(predI, predPE);
            WrappedPE<PE, SE> newWPE = wpeMap.get(predP);
            assert (newWPE != null);
            if (i instanceof JEntryNopStmt)
                newLen = 0;
            else
                newLen = 1 + newWPE.getLen();
            WrappedSE<PE, SE> newWSE;
            if (predSE != null) {
                assert (predM != null);
                Pair<SootMethod, SE> predN = new Pair<SootMethod, SE>(predM, predSE);
                newWSE = wseMap.get(predN);
                assert (newWSE != null);
                newLen += newWSE.getLen();
            } else {
                assert (predM == null);
                newWSE = null;
            }
            if(newLen < 0)
                throw new TraceOverflowException();
            if (newLen < oldLen)
                wpe.update(newWPE, newWSE, newLen);
        }
    }

    /**
     * Initializes provenance of given SE.
     * Pre-condition: traceKind != NONE.
     * (m, seToAdd): the SE whose provenance will be initialized.
     * (bb, predPE): the provenance of the SE.
     */
    protected void recordWSE(SootMethod m, SE seToAdd, Unit bb, PE predPE) {
        assert (seToAdd != null && predPE != null);
        assert (!wseMap.containsKey(seToAdd));
        WrappedPE<PE, SE> wpe = wpeMap.get(new Pair<Unit, PE>(bb, predPE));
        assert (wpe != null);
        SE seCopy = getSECopy(seToAdd);
        int len = 1 + wpe.getLen();
        WrappedSE<PE, SE> wse = new WrappedSE<PE, SE>(seCopy, wpe, len);
        wseMap.put(new Pair<SootMethod, SE>(m, seCopy), wse);
    }

    /**
     * Initializes provenance of given PE.
     * Pre-condition: traceKind != NONE.
     * (i, peToAdd): the PE whose provenance will be initialized.
     * (predI, predPE, predM, predSE): the provenance of the PE.
     */
    private void recordWPE(Unit i, PE peToAdd, Unit predI, PE predPE, SootMethod predM, SE predSE) {
        assert (peToAdd != null);
        // predPE and/or predSE may be null
        assert (!wpeMap.containsKey(peToAdd));
        PE peCopy = getPECopy(peToAdd);
        WrappedPE<PE, SE> predWPE;
        int len;
        if (predPE == null) {
            assert (predI == null && predM == null && predSE == null);
            predWPE = null;
            len = 0;
        } else {
            assert (predI != null);
            predWPE = wpeMap.get(new Pair<Unit, PE>(predI, predPE));
            assert (predWPE != null);
            if (i instanceof JEntryNopStmt)
                len = 0;
            else
                len = 1 + predWPE.getLen();
        }
        WrappedSE<PE, SE> predWSE;
        if (predSE == null) {
            assert (predM == null);
            predWSE = null;
        } else {
            assert (predM != null);
            predWSE = wseMap.get(new Pair<SootMethod, SE>(predM, predSE));
            assert (predWSE != null);
            len += predWSE.getLen();
        }
        if(len < 0){
            System.out.println("Trace length overflowed.");
            throw new TraceOverflowException();
            }
        WrappedPE<PE, SE> wpe = new WrappedPE<PE, SE>(i, peCopy, predWPE, predWSE, len);
        wpeMap.put(new Pair<Unit, PE>(i, peCopy), wpe);
    }
}

