package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JInvokeStmt;

import petablox.program.visitors.IInvokeInstVisitor;
import petablox.project.Petablox;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,i) such that the instruction at
 * program point p is method invocation i.
 */
@Petablox(name = "InvokeInst", sign = "P0,Invoke0:P0xInvoke0")
public class RelInvokeInst extends ProgramRel implements IInvokeInstVisitor {
    @Override
    public void visit(SootClass m) { }

    @Override
    public void visit(SootMethod m) { }

    public void visitInvokeInst(Unit u) { }

    public void visit(JInvokeStmt s) {
        add(s, s.getInvokeExpr());
    }
}
