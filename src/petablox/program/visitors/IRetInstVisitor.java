package petablox.program.visitors;

import soot.Unit;
import soot.jimple.internal.JRetStmt;

public interface IRetInstVisitor extends IMethodVisitor {
    public void visit(JRetStmt s);
}
