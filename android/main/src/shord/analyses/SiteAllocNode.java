package shord.analyses;

import shord.program.Program;
import shord.project.analyses.ProgramDom;
import soot.jimple.AssignStmt;
import soot.Unit;

/*
 * @author Yu Feng
 * @author Saswat Anand
 */
public class SiteAllocNode extends AllocNode
{
	private final Unit unit;

	public SiteAllocNode(Unit u)
	{
		super(((AssignStmt) u).getRightOp().getType());
		this.unit = u;
	}

	public Unit getUnit()
	{
		return unit;
	}

	public String toString()
	{
		return "SiteAlloc$" + Program.unitToString(unit);
	}	
}