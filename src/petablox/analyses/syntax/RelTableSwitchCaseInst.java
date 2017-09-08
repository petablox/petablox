package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JTableSwitchStmt;

import petablox.program.visitors.ITableSwitchInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "TableSwitchCaseInst", sign = "P0,P1:P0_P1")
public class RelTableSwitchCaseInst extends ProgramRel implements ITableSwitchInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JTableSwitchStmt s) {
        Value key = s.getKey();
        for (Unit target : s.getTargets())
            add(s, key, target);
    }
}
