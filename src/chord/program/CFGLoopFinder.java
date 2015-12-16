package chord.program;

import java.util.Stack;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import chord.util.tuple.object.Pair;
import soot.toolkits.graph.Block;
import chord.util.ArraySet;
import chord.util.soot.CFG;

/**
 * Inference of all loops in a CFG.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CFGLoopFinder {
    public static final boolean DEBUG = false;
    private Set<Block> visitedBef;
    private Set<Block> visitedAft;
    private Set<Pair<Block, Block>> backEdges;
    private Map<Block, Set<Block>> headToBody;
    private Map<Block, Set<Block>> headToExits;
    /**
     * Computes all loops in a given CFG.
     * It builds two maps:
     * <ul>
     * <li>headToBody:  from each loop header to the set of all basic blocks in that loop's body.</li>
     * <li>headToExits: from each loop header to the set of all basic blocks in that loop's body that have an immediate successor outside that loop's body.</li>
     * </ul>
     * 
     * @param cfg A CFG.
     */
    public void visit(CFG cfg) {
        // build back edges
        visitedBef = new ArraySet<Block>();
        visitedAft = new ArraySet<Block>();
        backEdges = new ArraySet<Pair<Block, Block>>();
        visit(cfg.getHeads().get(0));
        // build headToBody
        headToBody = new HashMap<Block, Set<Block>>();
        for (Pair<Block, Block> edge : backEdges) {
            Block tail = edge.val0;
            Block head = edge.val1;
            assert (!(head.getPreds().size()!=0));
            assert (!(head.getSuccs().size()!=0));
            // tail->head is a back edge
            Set<Block> body = headToBody.get(head);
            if (body == null) {
                body = new ArraySet<Block>();
                headToBody.put(head, body);
                body.add(head);
            }
            Stack<Block> working = new Stack<Block>();
            working.push(tail);
            while (!working.isEmpty()) {
                Block curr = working.pop();
                if (body.add(curr)) {
                    for (Object o : curr.getPreds()) {
                        Block pred = (Block) o;
                        working.push(pred);
                    }
                }
            }
        }
        // build headToExits
        headToExits = new HashMap<Block, Set<Block>>();
        for (Block head : headToBody.keySet()) {
            Set<Block> exits = new ArraySet<Block>();
            headToExits.put(head, exits);
            Set<Block> body = headToBody.get(head);
            for (Block curr : body) {
                for (Object o : curr.getSuccs()) {
                    Block succ = (Block) o;
                    if (!body.contains(succ)) {
                        assert (!(succ.getPreds().size()!=0));
                        assert (!(succ.getSuccs().size()!=0));
                        exits.add(succ);
                        break;
                    }
                }
            }
        }
        if (DEBUG) {
            System.out.println(cfg.toString());
            Set<Block> heads = getLoopHeads();
            for (Block head : heads) {
                System.out.println(head);
                System.out.println("BODY:");
                for (Block b : getLoopBody(head))
                    System.out.println("\t" + b);
                System.out.println("TAILS:");
                for (Block b : getLoopExits(head))
                    System.out.println("\t" + b);
            }
        }
    }
    /**
     * Provides the set of all loop header basic blocks in this CFG.
     * 
     * @return The set of all loop header basic blocks in this CFG.
     */
    public Set<Block> getLoopHeads() {
        return headToBody.keySet();
    }
    /**
     * Provides the set of all basic blocks in the body of the loop specified by the given loop header.
     * 
     * @param head A loop header.
     * 
     * @return The set of all basic blocks in the body of the loop specified by the given loop header.
     */
    public Set<Block> getLoopBody(Block head) {
        return headToBody.get(head);
    }
    /**
     * Provides the set of all basic blocks in the body of the loop specified by the given loop header
     * that have an immediate successor outside that loop's body.
     * 
     * @param head A loop header.
     * 
     * @return The set of all basic blocks in the body of the loop specified by the given loop header
     * that have an immediate successor outside that loop's body.
     */
    public Set<Block> getLoopExits(Block head) {
        return headToExits.get(head);
    }
    /**
     * Provides a map from each loop header in this CFG to the set of all basic blocks in that loop's body
     * that have an immediate successor outside that loop's body.
     * 
     * @return A map from each loop header in this CFG to the set of all basic blocks in that loop's body 
     * that have an immediate successor outside that loop's body.
     */
    public Map<Block, Set<Block>> getHeadToExitsMap() {
        return headToExits;
    }
    /**
     * Provides a map from each loop header in this CFG to the set of all basic blocks in that loop's body.
     * 
     * @return A map from each loop header in this CFG to the set of all basic blocks in that loop's body.
     */
    public Map<Block, Set<Block>> getHeadToBodyMap() {
        return headToBody;
    }
    /**
     * Provides the set of all back edges in this CFG.
     *
     * @return The set of all back edges in this CFG.
     */
    public Set<Pair<Block, Block>> getBackEdges() {
        return backEdges;
    }
    private void visit(Block curr) {
        visitedBef.add(curr);
        for (Object o : curr.getSuccs()) {
            Block succ = (Block) o;
            if (visitedBef.contains(succ)) {
                if (!visitedAft.contains(succ)) {
                    Pair<Block, Block> edge =
                        new Pair<Block, Block>(curr, succ);
                    backEdges.add(edge);
                }
            } else
                visit(succ);
        }
        visitedAft.add(curr);
    }
}
