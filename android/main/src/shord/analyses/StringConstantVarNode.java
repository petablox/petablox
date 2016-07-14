package shord.analyses;

import soot.SootMethod;

public class StringConstantVarNode extends VarNode
{
	public final SootMethod method;
	public final String sc;

	public StringConstantVarNode(SootMethod method, String sc) {
		this.method = method;
		this.sc = sc;
	}

	public String toString() {
		return (sc == null ? "global" : sc) + "@" + method;
	}
}
