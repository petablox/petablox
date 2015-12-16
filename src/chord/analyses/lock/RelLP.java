package chord.analyses.lock;

import gnu.trove.list.array.TIntArrayList;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;
import soot.Unit;
import soot.jimple.MonitorStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import chord.analyses.point.DomP;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (l,p) such that quad p is lexically enclosed in the
 * synchronized block or synchronized method that acquires the lock at point l.
 * <p>
 * A quad may be lexically enclosed in multiple synchronized blocks but in at most one
 * synchronized method (i.e. its containing method).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
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
        CFG cfg = SootUtilities.getCFG(m);
        Block entry = cfg.getHeads().get(0);
        TIntArrayList locks = new TIntArrayList();
        if (m.isSynchronized()) {
            int lIdx = domL.indexOf(entry.getHead());
            assert (lIdx >= 0);
            locks.add(lIdx);
        }
        process(entry, locks);
        visited.clear();
    }
    private void process(Block bb, TIntArrayList locks) {
        int k = locks.size();
        Iterator<Unit> uit = bb.iterator();
        while(uit.hasNext()){
        	Unit u = uit.next();
            if (u instanceof MonitorStmt) {
                if (u instanceof JEnterMonitorStmt) {
                    TIntArrayList locks2 = new TIntArrayList(k + 1);
                    for (int j = 0; j < k; j++)
                        locks2.add(locks.get(j));
                    int lIdx = domL.indexOf(u);
                    assert (lIdx >= 0);
                    locks2.add(lIdx);
                    locks = locks2;
                    k++;
                } else {
                    k--;
                    TIntArrayList locks2 = new TIntArrayList(k);
                    for (int j = 0; j < k; j++)
                        locks2.add(locks.get(j));
                    locks = locks2;
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
                process(bb2, locks);
            }
        }
    }
}
