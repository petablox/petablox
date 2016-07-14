package shord.analyses;

import soot.SootMethod;
import soot.jimple.CastExpr;

public class CastVarNode extends VarNode
{
	public final SootMethod method;
	public final CastExpr castExpr;

	public CastVarNode(SootMethod method, CastExpr castExpr) {
		this.method = method;
		this.castExpr = castExpr;
	}

	public String toString() {
		return castExpr + "@" + method;
	}
}
