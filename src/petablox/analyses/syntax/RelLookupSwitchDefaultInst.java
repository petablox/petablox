package petablox.analyses.syntax;

import java.util.Iterator;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.JLookupSwitchStmt;

import petablox.program.visitors.ILookupSwitchInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name = "LookupSwitchDefaultInst", sign = "P0,P1:P0_P1")
public class RelLookupSwitchDefaultInst extends ProgramRel implements ILookupSwitchInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JLookupSwitchStmt s) {
        add(s, s.getDefaultTarget());
    }
}
