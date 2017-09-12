package petablox.analyses.syntax;

import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.FloatConstant;

@Petablox(name="FloatConst")
public class DomFloatConst extends ProgramDom<Value> implements IExprVisitor {

	@Override
	public void visit(Unit q) {	}

	@Override
	public void visit(SootMethod m) {  }

	@Override
	public void visit(SootClass c) {	}

	@Override
	public void visit(Value e) {
		 if (e instanceof FloatConstant)
			 add(e);
	}
	

}
