package shord.analyses;

import soot.SootMethod;

public class RetVarNode extends VarNode
{
	public final SootMethod method;

	public RetVarNode(SootMethod m)
	{
		this.method = m;
	}

	public String toString()
	{
		return "return@"+method;
	}
}