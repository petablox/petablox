package shord.analyses;

import soot.SootMethod;

public class ParamVarNode extends VarNode
{
	public final SootMethod method;
	public final int index;

	public ParamVarNode(SootMethod m, int i)
	{
		this.method = m;
		this.index = i;
	}

	public String toString()
	{
		return "param$"+index+"@"+method;
	}
}