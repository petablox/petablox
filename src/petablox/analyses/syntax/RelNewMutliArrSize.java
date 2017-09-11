package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NewMultiArrayExpr;

// TODO: Make sure this sign is correct
@Petablox(name = "NewMultiArrSize", sign = "EXPR0,D0,EXPR1:EXPR0_D0xEXPR1")
public class RelNewMutliArrSize extends ProgramRel implements IExprVisitor {

	@Override
	public void visit(Unit q) {	}

	@Override
	public void visit(SootMethod m) { }

	@Override
	public void visit(SootClass c) {	}

	@Override
	public void visit(Value e) { 
		if (e instanceof NewMultiArrayExpr) {
			NewMultiArrayExpr nmae = (NewMultiArrayExpr) e;
			for (int i = 0; i < nmae.getSizeCount(); i++) {
				add(nmae, i, nmae.getSize(i));
			}
		}
	}

}
