package shord.analyses;

import soot.Type;

/*
 * @author Yu Feng
 * @author Saswat Anand
 */
public abstract class AllocNode
{
	protected final Type type;

	public AllocNode(Type type)
	{
		this.type = type;
	}

	public Type getType()
	{
		return type;
	}	
}
