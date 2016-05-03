package petablox.android.analyses;

import soot.Local;
import soot.SootMethod;

public class LocalVarNode extends VarNode
{
	public final Local local;
	public final SootMethod meth;

	public LocalVarNode(Local l, SootMethod m)
	{
		this.local = l;
		this.meth = m;
	}
	
	public String toString()
	{
		return local.getName()+"!"+local.getType()+"@"+meth;
	}
}