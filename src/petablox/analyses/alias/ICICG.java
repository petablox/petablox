package petablox.analyses.alias;

import java.util.Set;

import petablox.util.ArraySet;
import petablox.util.graph.ILabeledGraph;
import soot.SootMethod;
import soot.Unit;

/**
 * Specification of a context-insensitive call graph.
 * 
 * @author Mayur Naik <mhn@cs.stanford.edu>
 */
public interface ICICG extends ILabeledGraph<SootMethod, Unit> {
    /**
     * Provides the set of all methods that may be called by a given call site.
     * 
     * @param invk A call site.
     * 
     * @return The set of all methods that may be called by call site <tt>invk</tt>.
     */
    public Set<SootMethod> getTargets(Unit invk);
    /**
     * Provides an ordered set of all methods that may be called by a given call site.
     * 
     * @param invk A call site.
     * 
     * @return The ordered set of all methods that may be called by call site <tt>invk</tt>.
     */
    public ArraySet<SootMethod> getTargetsOrdered(Unit invk);
    /**
     * Provides the set of all call sites that may call a given method.
     * 
     * @param meth A method.
     * 
     * @return The set of all call sites that may call method <tt>meth</tt>.
     */
    public Set<Unit> getCallers(SootMethod meth);
    /**
     * Provides an ordered set of all call sites that may call a given method.
     * 
     * @param meth A method.
     * 
     * @return The ordered set of all call sites that may call method <tt>meth</tt>.
     */
    public ArraySet<Unit> getCallersOrdered(SootMethod meth);
    /**
     * Determines whether a given call site may call a given method.
     * 
     * @param invk A call site.
     * @param meth A method.
     * 
     * @return true iff call site <tt>invk</tt> may call method <tt>meth</tt>.
     */
    public boolean calls(Unit invk, SootMethod meth);
    /**
     * Provides an ordered set of all nodes in the CICG.
     * 
     * @return The ordered set of all nodes in the CICG.
     */
    public ArraySet<SootMethod> getNodesOrdered();
    
    /**
     * Provides an ordered set of all root nodes of this graph.
     *
     * @return All root nodes of this graph.
     */
    public Set<SootMethod> getRootsOrdered();
}
