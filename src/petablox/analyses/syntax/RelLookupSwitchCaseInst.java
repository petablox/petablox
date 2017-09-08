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

@Petablox(name = "LookupSwitchCaseInst", sign = "P0,EXPR0,IntConst0,P1:P0_EXPR0xIntConst0xP1")
public class RelLookupSwitchCaseInst extends ProgramRel implements ILookupSwitchInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(JLookupSwitchStmt s) {
        Value key = s.getKey();
        Iterator<IntConstant> it_value = s.getLookupValues().iterator();
        Iterator<Unit> it_target = s.getTargets().iterator();
        for (;it_value.hasNext() && it_target.hasNext();)
            add(s, key, it_value.next(), it_target.next());
    }
}
