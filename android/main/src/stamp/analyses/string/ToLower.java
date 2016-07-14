package stamp.analyses.string;

import soot.Local;
import soot.Immediate;
import soot.jimple.Stmt;

public class ToLower extends Assign
{
	public ToLower(Immediate right, Local left, Stmt stmt)
	{
		super(right, left, stmt);
	}	
	
	public boolean equals(Object other)
	{
		if(!(other instanceof ToLower))
			return false;
		ToLower as = (ToLower) other;
		return right.equals(as.right) && left.equals(as.left) && stmt.equals(as.stmt);
	}
}