package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NewArrayExpr;

@Petablox(name="NewArrExpr", sign="EXPR0,T0,EXPR1:EXPR0_T0xEXPR1")
public class RelNewArrExpr extends ProgramRel implements IExprVisitor {

	@Override
	public void visit(Unit q) {	}

	@Override
	public void visit(SootMethod m) {	}

	@Override
	public void visit(SootClass c) {	}

	@Override
	public void visit(Value e) {
		if (e instanceof NewArrayExpr) {
			NewArrayExpr nae = (NewArrayExpr) e;
			add(e, e.getType(), nae.getSize());
		}
	}

}
