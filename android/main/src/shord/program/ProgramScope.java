package shord.program;

import soot.SootMethod;

public abstract class ProgramScope
{
	protected Program prog;
	
	public ProgramScope(Program prog)
	{
		this.prog = prog;
	}

	public abstract boolean exclude(SootMethod method);
	
	public abstract boolean ignoreStub();

}