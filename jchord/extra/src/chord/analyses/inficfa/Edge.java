package chord.analyses.inficfa;


import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.analyses.rhs.IEdge;
import chord.util.ArraySet;
import chord.util.Utils;

public class Edge<F> implements IEdge {
	final public AbstractState<F> srcNode;
	public AbstractState<F> dstNode;

	public Edge() {
		srcNode = new AbstractState<F>(new Env<Register, F>());
		dstNode = new AbstractState<F>(new Env<Register, F>());
	}

	public Edge(AbstractState<F> s, AbstractState<F> d) {
		assert (s != null && d != null);
		srcNode = s;
		dstNode = d;
	}

	/**
	 * Two path (or summary) edges for the same program point (or method) can be
	 * merged if the edges are of the same type, their source nodes match, and
	 * one's destination node subsumes the other's.
	 */
	@Override
	public int canMerge(IEdge e, boolean mustMerge) {
		assert (!mustMerge);  // not implemented yet
		Edge<F> that = (Edge<F>) e;

		return Utils.areEqual(this.srcNode, that.srcNode) ? 0 : -1;
	}

	@Override
	public boolean mergeWith(IEdge e) {    
		Edge<F> that = (Edge<F>) e;
		
		assert(that.dstNode != null && this.dstNode != null);
		
		/*if (that.dstNode == null) {
			return false;
		}
		if (this.dstNode == null) {
			this.dstNode = that.dstNode;
			return true;
		}*/

		Env<Register,F> newEnvLocal = new Env<Register,F>(this.dstNode.envLocal);
		newEnvLocal.insert(that.dstNode.envLocal);

		Set<F> newReturnVarEnv = new ArraySet<F>(this.dstNode.returnVarEnv);
		newReturnVarEnv.addAll(that.dstNode.returnVarEnv);

		AbstractState<F> newAbstractState = new AbstractState<F>(newEnvLocal, newReturnVarEnv);

		if(newAbstractState.equals(this.dstNode))
			return false;
		else{
			this.dstNode = newAbstractState;
			return true;
		}
	}

	@Override
	public String toString() {
		return "[SOURCE=[" + srcNode + "],DEST=[" + dstNode + "]]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof Edge) {
			Edge<F> that = (Edge<F>) obj;
			return Utils.areEqual(this.srcNode, that.srcNode) &&
					Utils.areEqual(this.dstNode, that.dstNode);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return ((srcNode != null) ? srcNode.hashCode() : 0) +
				((dstNode != null) ? dstNode.hashCode() : 0);
	}
}
