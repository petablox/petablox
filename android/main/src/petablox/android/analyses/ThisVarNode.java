package petablox.android.analyses;

import soot.SootMethod;

public class ThisVarNode extends VarNode
{
	public final SootMethod method;

	public ThisVarNode(SootMethod m)
	{
		this.method = m;
	}

	public String toString()
	{
		return "this@"+method;
	}
}