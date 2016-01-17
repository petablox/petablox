package petablox.analyses.lock;

import gnu.trove.list.array.TIntArrayList;
import petablox.analyses.point.DomP;
import petablox.program.visitors.IMethodVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.ICFG;
import petablox.util.soot.SootUtilities;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.Unit;
import soot.jimple.MonitorStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import petablox.analyses.lock.DomL;

/**
 * Relation containing each tuple (l,p) such that quad p is lexically enclosed in the
 * synchronized block or synchronized method that acquires the lock at point l.
 * <p>
 * A quad may be lexically enclosed in multiple synchronized blocks but in at most one
 * synchronized method (i.e. its containing method).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "LP",
    sign = "L0,P0:L0_P0"
)
public class RelLP extends ProgramRel implements IMethodVisitor {
    private Set<Block> visited = new HashSet<Block>();
    private DomP domP;
    private DomL domL;

    public void init() {
        domL = (DomL) doms[0];
        domP = (DomP) doms[1];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        ICFG cfg = SootUtilities.getCFG(m);
        Block entry = cfg.getHeads().get(0);
        TIntArrayList locks = new TIntArrayList();
        int ndx = 0;
        if (m.isSynchronized()) {
            int lIdx = domL.indexOf(entry.getHead());
            assert (lIdx >= 0);
            locks.add(lIdx);
            ndx++;
        }
        process(entry, locks, ndx);
        visited.clear();
    }
    private void process(Block bb, TIntArrayList locks, int ndx) {
        int k = ndx;
        Iterator<Unit> uit = bb.iterator();
        while(uit.hasNext()){
        	Unit u = uit.next();
            if (u instanceof MonitorStmt) {
                if (u instanceof JEnterMonitorStmt) {
                    int lIdx = domL.indexOf(u);
                    assert (lIdx >= 0);
                    locks.add(lIdx);
                    k++;
                } else {
                    k--;
                }
            } else if (k > 0) {
                int pIdx = domP.indexOf(u);
                assert (pIdx >= 0);
                add(locks.get(k - 1), pIdx);
            }
        }
        for (Object o : bb.getSuccs()) {
            Block bb2 = (Block) o;
            if (!visited.contains(bb2)) {
                visited.add(bb2);
                process(bb2, locks, k);
            }
        }
    }
}
