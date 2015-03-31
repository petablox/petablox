package chord.project.analyses.tdbu;

import java.util.Set;
import java.util.SortedSet;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.program.Loc;
import chord.project.analyses.rhs.IEdge;
import chord.util.tuple.object.Pair;

/**
 * This class represents the edge in the bottom-up analysis. Unlike top-down
 * analysis, summary edges and path edges are not treated differently. The edge
 * is supposed to be made of two parts: 1. a symbolic state part; 2. a
 * constraint part. Basically, BUEdge can be viewed as a summary of arbitrary
 * length of instructions. So BUEdge([alpha,S1;S2]) =
 * BUEdge([BUEdge([alpha,S1]),S2])
 * 
 * @author xin
 * 
 * @param <TDPE>
 */
public interface BUEdge<TDPE extends IEdge, TDSE extends IEdge> {

	/**
	 * If current BUEdge is a summary edge for a function, get the path edge
	 * after the call site using this BU edge. If the current BU Edge doesn't
	 * apply to the top-down path edge, return null.
	 * 
	 * @param q
	 *            the Invoke instruction at the call site
	 * @param clrPE
	 *            the path edge of top-down analysis before the call site
	 * @param loc
	 *            the call site location
	 * @param tgtM
	 *            the target method that the invoke instruction calls
	 * @return
	 */
	public TDPE applyInvoke(Quad q, TDPE clrPE, Loc loc, jq_Method tgtM);
	
	public TDPE applyInvokeWithoutRet(Quad q, TDPE clrPE, Loc loc, jq_Method tgtM);

	public TDPE apply(TDPE tdpe, Loc loc);

	/**
	 * If current BUEdge is a summary edge, get the bu edge set after the call
	 * site using this BU edge. If the current Bu edge doesn't apply to the
	 * given buEdge, return null.
	 * The return value is a set because of parameter passing and return values are like move statements
	 * @param q
	 * @param clrEdge
	 * @param loc
	 * @param tgtM
	 * @return
	 */
	public Set<BUEdge<TDPE, TDSE>> applyInvoke(Quad q,
			BUEdge<TDPE, TDSE> clrEdge, Loc loc, jq_Method tgtM,SortedSet<Constraint> trackedCases, int bulimit);

	public BUEdge<TDPE, TDSE> apply(BUEdge<TDPE, TDSE> clrEdge, Loc loc);

	public Set<BUEdge<TDPE, TDSE>> applyInvokeWithoutRet(Quad q,
			BUEdge<TDPE, TDSE> clrEdge, Loc loc, jq_Method tgtM,SortedSet<Constraint> trackedCases, int bulimit);
	
	/**
	 * Check whether the given tdse is contained in current bu edge
	 * 
	 * @param tdse
	 * @return
	 */
	public boolean satisfy(TDSE tdse);

	/**
	 * ret = 1, this contains other; ret = 0, equal; ret = -1, other contains this; ret = -2, incomparable
	 * @param other
	 * @return
	 */
	public int canMerge(BUEdge other);

	public boolean mergeWith(BUEdge other);

	public Constraint getConstraint();

	public BUEdge<TDPE, TDSE> changeConstraint(Constraint cons);

	/**
	 * Don't use the default jdk implementation!
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(Object other);
	
	public Set<Pair<Loc, BUEdge<TDPE,TDSE>>> getInstStates();
	
	public void setInstStates(Set<Pair<Loc, BUEdge<TDPE,TDSE>>> stateSet);
	
	public BUEdge<TDPE,TDSE> cloneWithoutInstStates();
	
	public int size();
	
	public BUEdge<TDPE,TDSE> checkValid(jq_Method m); 
}
