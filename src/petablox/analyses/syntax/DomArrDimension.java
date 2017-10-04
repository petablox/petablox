package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NewMultiArrayExpr;

@Petablox(name="ArrDimension")
public class DomArrDimension extends ProgramDom<Integer> implements IExprVisitor {
    public static int MAXZ = Integer.getInteger("petablox.domK.size", 32);

    @Override
    public void fill() {
        for (int i = 0; i < MAXZ; i++)
            getOrAdd(new Integer(i));  
    }

    @Override
    public void visit(Unit q) {	}

    @Override
    public void visit(SootMethod m) {	}

    @Override
    public void visit(SootClass c) {	}

    @Override
    public void visit(Value e) {
        if (e instanceof NewMultiArrayExpr) {
            MAXZ = ((NewMultiArrayExpr) e).getSizes().size();
            fill();
        }
    }
}
