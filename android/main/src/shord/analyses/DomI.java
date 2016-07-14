package shord.analyses;

import soot.Unit;
import shord.project.analyses.ProgramDom;
import shord.program.Program;

/**
 * Domain of method invocation stmts
 * 
 * @author Saswat Anand
 */
public class DomI extends ProgramDom<Unit> {
    @Override
    public String toUniqueString(Unit u) {
		return Program.unitToString(u);
    }
}
