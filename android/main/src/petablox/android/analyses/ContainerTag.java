package petablox.android.analyses;

import soot.SootMethod;
import soot.tagkit.Tag;

public class ContainerTag implements Tag
{
	public final SootMethod method;

	public ContainerTag(SootMethod meth)
	{
		this.method = meth;
	}

	public String getName()
	{
		return "ContainerTag";
	}
	
	public byte[] getValue()
	{
		throw new RuntimeException("unimplemented");
	}
}