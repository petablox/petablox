package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NewMultiArrayExpr;

@Petablox(name = "NewMultiArrExpr", sign = "EXPR0,T0,ArrDimension0:EXPR0_T0xArrDimension0")
public class RelNewMutliArrExpr extends ProgramRel implements IExprVisitor {
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
            add(e, e.getType(), nmae.getSizeCount());
        }
    }
}
