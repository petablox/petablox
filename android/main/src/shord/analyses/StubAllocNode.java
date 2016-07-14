package shord.analyses;

import soot.Type;
import soot.SootMethod;

/*
 * @author Yu Feng
 * @author Saswat Anand
 */
public class StubAllocNode extends AllocNode
{
	private final SootMethod meth;

	public StubAllocNode(Type t, SootMethod meth)
	{
		super(t);
		this.meth = meth;
	}

	public String toString()
	{
		return "StubAlloc$" + type + "@"+meth.getSignature();
	}
}
