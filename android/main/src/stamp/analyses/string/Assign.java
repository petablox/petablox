package stamp.analyses.string;

import soot.Local;
import soot.Immediate;
import soot.jimple.Stmt;

public class Assign extends Statement
{
	final Immediate right;
	final Local left;

	public Assign(Immediate right, Local left, Stmt stmt)
	{
		super(stmt);
		this.left = left;
		this.right = right;
	}	
	
	public boolean equals(Object other)
	{
		if(!(other instanceof Assign))
			return false;
		Assign as = (Assign) other;
		return right.equals(as.right) && left.equals(as.left) && stmt.equals(as.stmt);
	}
	
	public int hashCode()
	{
		return right.hashCode()+left.hashCode()+stmt.hashCode();
	}
}