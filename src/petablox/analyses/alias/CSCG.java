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
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;

/**
 * Implementation of a context-sensitive call graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CSCG extends AbstractGraph<Pair<Ctxt, SootMethod>> implements ICSCG {
    protected DomM domM;
    protected ProgramRel relRootCM;
    protected ProgramRel relReachableCM;
    protected ProgramRel relCICM;
    protected ProgramRel relCMCM;
    public CSCG(DomM domM, ProgramRel relRootCM, ProgramRel relReachableCM,
            ProgramRel relCICM, ProgramRel relCMCM) {
        this.domM = domM;
        this.relRootCM = relRootCM;
        this.relReachableCM = relReachableCM;
        this.relCICM = relCICM;
        this.relCMCM = relCMCM;
    }
    public Set<Pair<Ctxt, SootMethod>> getNodes() {
        if (!relReachableCM.isOpen())
            relReachableCM.load();
        Iterable<Pair<Ctxt, SootMethod>> res = relReachableCM.getAry2ValTuples();
        return SetUtils.iterableToSet(res, relReachableCM.size());
    }
    public Set<Pair<Ctxt, SootMethod>> getRoots() {
        if (!relRootCM.isOpen())
            relRootCM.load();
        Iterable<Pair<Ctxt, SootMethod>> res = relRootCM.getAry2ValTuples();
        return SetUtils.iterableToSet(res, relRootCM.size());
    }
    public Set<Pair<Ctxt, SootMethod>> getPreds(Pair<Ctxt, SootMethod> cm) {
        if (!relCMCM.isOpen())
            relCMCM.load();
        RelView view = relCMCM.getView();
        view.selectAndDelete(2, cm.val0);
        view.selectAndDelete(3, cm.val1);
        Iterable<Pair<Ctxt, SootMethod>> res = view.getAry2ValTuples();
        return SetUtils.iterableToSet(res, view.size());
    }
    public Set<Pair<Ctxt, SootMethod>> getSuccs(Pair<Ctxt, SootMethod> cm) {
        if (!relCMCM.isOpen())
            relCMCM.load();
        RelView view = relCMCM.getView();
        view.selectAndDelete(0, cm.val0);
        view.selectAndDelete(1, cm.val1);
        Iterable<Pair<Ctxt, SootMethod>> res = view.getAry2ValTuples();
        return SetUtils.iterableToSet(res, view.size());
    }
    public boolean hasNode(Pair<Ctxt, SootMethod> node) {
        if (!relReachableCM.isOpen())
            relReachableCM.load();
        return relReachableCM.contains(node.val0, node.val1);
    }
    public boolean hasRoot(Pair<Ctxt, SootMethod> node) {
        if (!relRootCM.isOpen())
            relRootCM.load();
        if (relRootCM.contains(node.val0, node.val1))
            return true;
        return false;
    }
    public int numSuccs(Pair<Ctxt, SootMethod> node) {
        if (!relCMCM.isOpen())
            relCMCM.load();
        RelView view = relCMCM.getView();
        view.selectAndDelete(0, node.val0);
        view.selectAndDelete(1, node.val1);
        return view.size();
    }
    public Set<Ctxt> getContexts(SootMethod SootMethod) {
        if (!relReachableCM.isOpen())
            relReachableCM.load();
        RelView view = relReachableCM.getView();
        view.selectAndDelete(1, SootMethod);
        Iterable<Ctxt> res = view.getAry1ValTuples();
        Set<Ctxt> ctxts = SetUtils.newSet(view.size());
        for (Ctxt ctxt : res)
            ctxts.add(ctxt);
        return ctxts;
    }
    public Set<Pair<Ctxt, Unit>> getCallers(Ctxt ctxt, SootMethod meth) {
        if (!relCICM.isOpen())
            relCICM.load();
        RelView view = relCICM.getView();
        view.selectAndDelete(2, ctxt);
        view.selectAndDelete(3, meth);
        Iterable<Pair<Ctxt, Unit>> res = view.getAry2ValTuples();
        Set<Pair<Ctxt, Unit>> CIs = SetUtils.newSet(view.size());
        for (Pair<Ctxt, Unit> ci : res)
            CIs.add(ci);
        return CIs;
    }
    public Set<Pair<Ctxt, SootMethod>> getTargets(Ctxt ctxt, Unit invk) {
        if (!relCICM.isOpen())
            relCICM.load();
        RelView view = relCICM.getView();
        view.selectAndDelete(0, ctxt);
        view.selectAndDelete(1, invk);
        Iterable<Pair<Ctxt, SootMethod>> res = view.getAry2ValTuples();
        Set<Pair<Ctxt, SootMethod>> CMs = SetUtils.newSet(view.size());
        for (Pair<Ctxt, SootMethod> cm : res)
            CMs.add(cm);
        return CMs;
    }
    public Set<Unit> getLabels(Pair<Ctxt, SootMethod> origNode, Pair<Ctxt, SootMethod> destNode) {
        SootMethod meth1 = origNode.val1;
        Set<Unit> invks = new ArraySet<Unit>();
        ICFG cfg = SootUtilities.getCFG(meth1);
        Ctxt ctxt1 = origNode.val0;
        SootMethod meth2 = destNode.val1;
        Ctxt ctxt2 = destNode.val0;
        for (Block bb : cfg.reversePostOrder()) {
        	Iterator<Unit> uit = bb.iterator();
            while (uit.hasNext()) {
            	Unit q = uit.next();
            	if (SootUtilities.isInvoke(q) && calls(ctxt1, q, ctxt2, meth2)) {
            		invks.add(q);
            	}
            }
        }
        return invks;
    }
    public boolean hasEdge(Pair<Ctxt, SootMethod> node1, Pair<Ctxt, SootMethod> node2) {
        if (!relCMCM.isOpen())
            relCMCM.load();
        return relCMCM.contains(node1.val0, node1.val1, node2.val0, node2.val1);
    }
    public int numRoots() {
        if (!relRootCM.isOpen())
            relRootCM.load();
        return relRootCM.size();
    }
    public int numNodes() {
        if (!relReachableCM.isOpen())
            relReachableCM.load();
        return relReachableCM.size();
    }
    public int numPreds(Pair<Ctxt, SootMethod> node) {
        if (!relCMCM.isOpen())
            relCMCM.load();
        RelView view = relCMCM.getView();
        view.selectAndDelete(2, node.val0);
        view.selectAndDelete(3, node.val1);
        return view.size();
    }
    public boolean calls(Ctxt origCtxt, Unit origInvk, Ctxt destCtxt, SootMethod destMeth) {
        if (!relCICM.isOpen())
            relCICM.load();
        return relCICM.contains(origCtxt, origInvk, destCtxt, destMeth);
    }
    /**
     * Frees relations used by this call graph if they are in memory.
     * <p>
     * This method must be called after clients are done exercising
     * the interface of this call graph.
     */
    public void free() {
        if (relRootCM.isOpen())
            relRootCM.close();
        if (relReachableCM.isOpen())
            relReachableCM.close();
        if (relCICM.isOpen())
            relCICM.close();
        if (relCMCM.isOpen())
            relCMCM.close();
    }
}
