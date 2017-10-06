package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceFieldRef;

@Petablox(name="InstanceFieldExpr", sign="EXPR0,EXPR1,F0:EXPR0_EXPR1_F0")
public class RelInstanceFieldExpr extends ProgramRel implements IExprVisitor {
    @Override
    public void visit(Unit q) {	}

    @Override
    public void visit(SootMethod m) {	}

    @Override
    public void visit(SootClass c) {	}

    @Override
    public void visit(Value e) {
        if (e instanceof InstanceFieldRef) {
            InstanceFieldRef sfe = (InstanceFieldRef) e;
            add(e, sfe.getBase(), sfe.getField());
        }
    }
}
