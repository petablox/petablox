package petablox.project.analyses.rhs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import petablox.util.soot.JEntryNopStmt;
import petablox.util.soot.SootUtilities;
import soot.SootMethod;
import soot.Unit;
import soot.toolkits.graph.Block;

/**
 * The backward trace iterator.
 * 
 * @author xin
 */
public class BackTraceIterator<PE extends IEdge, SE extends IEdge> implements Iterator<IWrappedPE<PE, SE>> {
    private IWrappedPE<PE, SE> currentWPE;                // current wrapped path edge
    private final Stack<IWrappedPE<PE, SE>> callStack;    // call stack for dealing with summary edges
    private Set<SootMethod> skipList;

    /**
     * Instantiate a {@link BackTraceIterator}.
     * 
     * @param wpe The wrapped path edge as the iterator's first element
     */
    public BackTraceIterator(final IWrappedPE<PE, SE> wpe) {
        currentWPE = wpe;
        callStack = new Stack<IWrappedPE<PE, SE>>();
        skipList = new HashSet<SootMethod>();
    }

    public void addMethodToSkipList(SootMethod m) {
        this.skipList.add(m);
    }
    
    @Override
    public boolean hasNext() {
        return (currentWPE != null);
    }

    @Override
    public IWrappedPE<PE, SE> next() {
        IWrappedPE<PE, SE> ret = currentWPE;
        Unit inst = currentWPE.getInst();
        if (inst instanceof JEntryNopStmt && !callStack.empty()) {
            currentWPE = callStack.pop();
            return ret;
        }

        IWrappedSE<PE, SE> wse = currentWPE.getWSE();
        IWrappedPE<PE, SE> wpe = currentWPE.getWPE();
        SootMethod m = SootUtilities.getMethod(((Unit)wse.getWPE().getInst()));
        if (wse != null && !skipList.contains(m)) {
            Unit q = (Unit) wpe.getInst();
            if (!(SootUtilities.isInvoke(q))) {
                throw new RuntimeException("Provence must be an invoke instruction!");
            }
            callStack.push(wpe);
            currentWPE = wse.getWPE();
        } else {
            currentWPE = wpe;
        }
        return ret;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation not supported!");
    }

    /**
     * Provides the current wrapped path edge.
     * 
     * @return  The current wrapped path edge.
     */
    public IWrappedPE<PE, SE> curr() {
        return currentWPE;
    }
}
