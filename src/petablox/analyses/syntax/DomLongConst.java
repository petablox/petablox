package petablox.analyses.syntax;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.dexpler.typing.UntypedLongOrDoubleConstant;
import soot.jimple.LongConstant;
import petablox.program.visitors.IExprVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;

@Petablox(name = "LongConst")
public class DomLongConst extends ProgramDom<Value> implements IExprVisitor {
    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) { }

    @Override
    public void visit(Unit u) { }

    public void visit(Value e) {
        if (e instanceof LongConstant)
            add(e);
        
        // TODO: This may be incorrect?
        if (e instanceof UntypedLongOrDoubleConstant) 
        		add(e);
    }
}
