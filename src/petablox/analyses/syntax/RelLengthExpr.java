package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.LengthExpr;

@Petablox(name = "LengthExpr", sign = "EXPR0,EXPR1:EXPR0_EXPR1")
public class RelLengthExpr extends ProgramRel implements IExprVisitor {

	@Override
	public void visit(Unit q) { }

	@Override
	public void visit(SootMethod m) { }

	@Override
	public void visit(SootClass c) { }

	@Override
	public void visit(Value e) {
		if (e instanceof LengthExpr) {
			LengthExpr le = (LengthExpr) e;
			add(e, le.getOp());
		}
	}

}
