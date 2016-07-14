package stamp.analyses.string;

import soot.jimple.Stmt;

public abstract class Statement
{
	final protected Stmt stmt;

	protected Statement(Stmt stmt)
	{
		this.stmt = stmt;
	}
}