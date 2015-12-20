package petablox.analyses.alias;

import java.util.Iterator;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;
import petablox.analyses.method.DomM;
import petablox.bddbddb.Rel.RelView;
import petablox.project.analyses.ProgramRel;
import petablox.util.ArraySet;
import petablox.util.SetUtils;
import petablox.util.graph.AbstractGraph;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;


/**
 * Implementation of a context-insensitive call graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CICG extends AbstractGraph<SootMethod> implements ICICG {
    private DomM domM;
    private ProgramRel relRootM;
    private ProgramRel relReachableM;
    private ProgramRel relIM;
    private ProgramRel relMM;
    public CICG(DomM domM, ProgramRel relRootM, ProgramRel relReachableM,
            ProgramRel relIM, ProgramRel relMM) {
        this.domM = domM;
        this.relRootM = relRootM;
        this.relReachableM = relReachableM;
        this.relIM = relIM;
        this.relMM = relMM;
    }
    public Set<Unit> getCallers(SootMethod meth) {
        if (!relIM.isOpen())
            relIM.load();
        RelView view = relIM.getView();
        view.selectAndDelete(1, meth);
        Iterable<Unit> res = view.getAry1ValTuples();
        Set<Unit> invks = SetUtils.newSet(view.size());
        for (Unit invk : res)
            invks.add(invk);
        return invks;
    }
    public ArraySet<Unit> getCallersOrdered(SootMethod meth) {
        if (!relIM.isOpen())
            relIM.load();
        RelView view = relIM.getView();
        view.selectAndDelete(1, meth);
        Iterable<Unit> res = view.getAry1ValTuples();
        ArraySet<Unit> invks = new ArraySet<Unit>(view.size());
        for (Unit invk : res)
            invks.add(invk);
        return invks;
    }
    public Set<SootMethod> getTargets(Unit invk) {
        if (!relIM.isOpen())
            relIM.load();
        RelView view = relIM.getView();
        view.selectAndDelete(0, invk);
        Iterable<SootMethod> res = view.getAry1ValTuples();
        Set<SootMethod> meths = SetUtils.newSet(view.size());
        for (SootMethod meth : res)
            meths.add(meth);
        return meths;
    }
    public ArraySet<SootMethod> getTargetsOrdered(Unit invk) {
        if (!relIM.isOpen())
            relIM.load();
        RelView view = relIM.getView();
        view.selectAndDelete(0, invk);
        Iterable<SootMethod> res = view.getAry1ValTuples();
        ArraySet<SootMethod> meths = new ArraySet<SootMethod>(view.size());
        for (SootMethod meth : res)
            meths.add(meth);
        return meths;
    }
    public int numRoots() {
        if (!relRootM.isOpen())
            relRootM.load();
        return relRootM.size();
    }
    public int numNodes() {
        if (!relReachableM.isOpen())
            relReachableM.load();
        return relReachableM.size();
    }
    public int numPreds(SootMethod node) {
        throw new UnsupportedOperationException();
    }
    public int numSuccs(SootMethod node) {
        throw new UnsupportedOperationException();
    }
    public Set<SootMethod> getRoots() {
        if (!relRootM.isOpen())
            relRootM.load();
        Iterable<SootMethod> res = relRootM.getAry1ValTuples();
        return SetUtils.iterableToSet(res, relRootM.size());
    }
    public Set<SootMethod> getRootsOrdered() {
    	if (!relRootM.isOpen())
            relRootM.load();
        Iterable<SootMethod> res = relRootM.getAry1ValTuples();
        Set<SootMethod> set = new ArraySet<SootMethod>();
        for (SootMethod e : res) set.add(e);
        return set;
    }
    public Set<SootMethod> getNodes() {
        if (!relReachableM.isOpen())
            relReachableM.load();
        Iterable<SootMethod> res = relReachableM.getAry1ValTuples();
        return SetUtils.iterableToSet(res, relReachableM.size());
    }
    public ArraySet<SootMethod> getNodesOrdered() {
        if (!relReachableM.isOpen())
            relReachableM.load();
        Iterable<SootMethod> res = relReachableM.getAry1ValTuples();
        ArraySet<SootMethod> as = new ArraySet<SootMethod>(relReachableM.size());
        for (SootMethod m  : res) as.add(m);
        return as;
    }
    public Set<SootMethod> getPreds(SootMethod meth) {
        if (!relMM.isOpen())
            relMM.load();
        RelView view = relMM.getView();
        view.selectAndDelete(1, meth);
        Iterable<SootMethod> res = view.getAry1ValTuples();
        return SetUtils.iterableToSet(res, view.size());
    }
    public Set<SootMethod> getSuccs(SootMethod meth) {
        if (!relMM.isOpen())
            relMM.load();
        RelView view = relMM.getView();
        view.selectAndDelete(0, meth);
        Iterable<SootMethod> res = view.getAry1ValTuples();
        return SetUtils.iterableToSet(res, view.size());
    }
    public Set<Unit> getLabels(SootMethod srcMeth, SootMethod dstMeth) {
        Set<Unit> invks = new ArraySet<Unit>();
        CFG cfg = SootUtilities.getCFG(srcMeth);
        for (Block bb : cfg.reversePostOrder()) {
        	Iterator<Unit> uit = bb.iterator();
            while (uit.hasNext()) {
            	Unit q = uit.next();
            	if (SootUtilities.isInvoke(q) && calls(q, dstMeth)) {
            		invks.add(q);
            	}
            }
        }
        return invks;
    }
    public boolean calls(Unit invk, SootMethod meth) {
        if (!relIM.isOpen())
            relIM.load();
        return relIM.contains(invk, meth);
    }
    public boolean hasRoot(SootMethod meth) {
        return domM.indexOf(meth) == 0;
    }
    public boolean hasNode(SootMethod meth) {
        if (!relReachableM.isOpen())
            relReachableM.load();
        return relReachableM.contains(meth);
    }
    public boolean hasEdge(SootMethod meth1, SootMethod meth2) {
        if (!relMM.isOpen())
            relMM.load();
        return relMM.contains(meth1, meth2);
    }
    /**
     * Frees relations used by this call graph if they are in memory.
     * <p>
     * This method must be called after clients are done exercising
     * the interface of this call graph.
     */
    public void free() {
        if (relRootM.isOpen())
            relRootM.close();
        if (relReachableM.isOpen())
            relReachableM.close();
        if (relIM.isOpen())
            relIM.close();
        if (relMM.isOpen())
            relMM.close();
    }
}

