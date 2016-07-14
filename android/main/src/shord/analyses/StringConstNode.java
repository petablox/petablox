package shord.analyses;

import soot.RefType;
import soot.SootMethod;


/*
 * @author Yu Feng
 */
public class StringConstNode extends GlobalAllocNode
{
	public final String value;

	public StringConstNode(String value)
	{
		super(RefType.v("java.lang.String"));
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public String toString()
	{
		return "StringConst$" + value; 
	}
	
	public boolean equals(Object other)
	{
		if(!(other instanceof StringConstNode))
			return false;
		return value.equals(((StringConstNode) other).value);
	}
	
	public int hashCode()
	{
		return value.hashCode();
	}
}
