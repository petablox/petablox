package stamp.analyses.string;

import soot.Local;
import soot.jimple.Stmt;

public class Criterion extends Statement
{
	final Local local;
	
	public Criterion(Local local, Stmt stmt)
	{
		super(stmt);
		this.local = local;
	}
	
	public boolean equals(Object other)
	{
		if(!(other instanceof Criterion))
			return false;
		Criterion c = (Criterion) other;
		return local.equals(c.local) && stmt.equals(c.stmt);
	}
	
	public int hashCode()
	{
		return local.hashCode()+stmt.hashCode();
	}
}