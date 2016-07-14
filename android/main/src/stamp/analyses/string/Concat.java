package stamp.analyses.string;

import soot.Local;
import soot.Immediate;
import soot.jimple.Stmt;

public class Concat extends Statement
{
	final Immediate right1;
	final Immediate right2;
	final Local left;

	public Concat(Immediate right1, Immediate right2, Local left, Stmt stmt)
	{
		super(stmt);
		this.left = left;
		this.right1 = right1;
		this.right2 = right2;
	}

	public boolean equals(Object other)
	{
		if(!(other instanceof Concat))
			return false;
		Concat c = (Concat) other;
		return right1.equals(c.right1) && right2.equals(c.right2) && left.equals(c.left) && stmt.equals(c.stmt);
	}
	
	public int hashCode()
	{
		return right1.hashCode()+right2.hashCode()+left.hashCode()+stmt.hashCode();
	}
}