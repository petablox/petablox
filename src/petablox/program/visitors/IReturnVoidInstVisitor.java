package petablox.program.visitors;

import soot.Unit;
import soot.jimple.internal.JReturnVoidStmt;

public interface IReturnVoidInstVisitor extends IMethodVisitor {
    public void visit(JReturnVoidStmt s);
}
