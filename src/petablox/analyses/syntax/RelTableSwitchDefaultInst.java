package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.JTableSwitchStmt;

import petablox.program.visitors.ITableSwitchInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "TableSwitchDefaultInst", sign = "P0,P1:P0_P1")
public class RelTableSwitchDefaultInst extends ProgramRel implements ITableSwitchInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JTableSwitchStmt s) {
        add(s, s.getDefaultTarget());
    }
}
