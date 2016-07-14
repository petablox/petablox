package stamp.analyses.string;

import soot.Local;
import soot.jimple.Stmt;

public class Havoc extends Statement
{
	final Local local;

	public Havoc(Local local, Stmt stmt)
	{
		super(stmt);
		this.local = local;
	}

	public boolean equals(Object other)
	{
		if(!(other instanceof Havoc))
			return false;
		Havoc o = (Havoc) other;
		return local.equals(o.local) && stmt.equals(o.stmt);
	}
	
	public int hashCode()
	{
		return local.hashCode()+stmt.hashCode();
	}
}