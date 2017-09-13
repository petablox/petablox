package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.MethodHandle;

@Petablox(name="MethodHandle", sign="EXPR0,M0:EXPR0_M0")
public class RelMethodHandle extends ProgramRel implements IExprVisitor {

	@Override
	public void visit(Unit q) {  }

	@Override
	public void visit(SootMethod m) {  }

	@Override
	public void visit(SootClass c) {  }

	@Override
	public void visit(Value e) {
		if (e instanceof MethodHandle) {
			MethodHandle mh = (MethodHandle) e;
			add(mh, mh.getMethodRef().resolve());
		}
	}

}
