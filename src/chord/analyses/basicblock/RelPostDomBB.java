package chord.analyses.basicblock;

import chord.util.ArraySet;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import soot.SootMethod;
import soot.SootClass;
import soot.toolkits.graph.Block;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each pair of basic blocks (b1,b2)
 * such that b1 is immediate postdominator of b2.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "postDomBB",
    sign = "B0,B1:B0xB1"
)
public class RelPostDomBB extends ProgramRel implements IMethodVisitor {
    private final Map<Block, Set<Block>> pdomMap =
        new HashMap<Block, Set<Block>>();
    public void visit(SootClass c) { }
    public void visit(SootMethod m) {
        if (m.isAbstract())
            return;
        pdomMap.clear();
        CFG cfg = SootUtilities.getCFG(m);
        Block exit = cfg.getTails().get(0);
        Set<Block> exitSet = new ArraySet<Block>(1);
        exitSet.add(exit);
        pdomMap.put(exit, exitSet);
        List<Block> rpo = cfg.reversePostOrder();
        int n = rpo.size();
        Set<Block> initSet = new ArraySet<Block>(n);
        for (int i = 0; i < n; i++) {
            Block bb = rpo.get(i);
            initSet.add(bb);
        }
        for (int i = 0; i < n; i++) {
            Block bb = rpo.get(i);
            if (bb != exit)
                pdomMap.put(bb, initSet);
        }
        boolean changed;
        while (true) {
            changed = false;
            for (int i = n - 1; i >= 0; i--) {
                Block bb = rpo.get(i);
                if (bb == exit)
                    continue;
                Set<Block> oldPdom = pdomMap.get(bb);
                Set<Block> newPdom = null;
                java.util.List<Block> succs = bb.getSuccs();
                int k = succs.size();
                if (k >= 1) {
                    Set<Block> fst = pdomMap.get(succs.get(0));
                    newPdom = new ArraySet<Block>(fst);
                    for (int j = 1; j < k; j++) {
                        Set<Block> nxt = pdomMap.get(succs.get(j));
                        newPdom.retainAll(nxt);
                    }
                } else
                    newPdom = new ArraySet<Block>(1);
                newPdom.add(bb);
                if (!oldPdom.equals(newPdom)) {
                    changed = true;
                    pdomMap.put(bb, newPdom);
                }
            }
            if (!changed)
                break;
        }
        for (Block bb : pdomMap.keySet()) {
            // System.out.print("postdominators of " + bb + ":");
            for (Block bb2 : pdomMap.get(bb)) {
                // System.out.print(" " + bb2);
                add(bb2, bb);
            }
            // System.out.println();
        }
    }
}
