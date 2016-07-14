package stamp.injectannot;

import stamp.util.IPAddressValidator;
import stamp.util.URIValidator;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import java.util.*;

public class ContentProviderAnnotation extends Visitor
{
	private URIValidator uriValidator = new URIValidator();
	private IPAddressValidator ipValidator = new IPAddressValidator();

    public ContentProviderAnnotation()
    {
    }
	
    protected void visit(SootClass klass)
    {
		super.visit(klass);
		Collection<SootMethod> methodsCopy = new ArrayList(klass.getMethods());
		for(SootMethod method : methodsCopy)
			visit(method);
    }
	
    protected void visit(SootMethod method)
    {
		if(!method.isConcrete())
			return;
		super.visit(method);

		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
	    
			//invocation statements
			if(stmt.containsInvokeExpr()){
				InvokeExpr ie = stmt.getInvokeExpr();
				List args = ie.getArgs();
				int i = 0;
				for(Iterator ait = args.iterator(); ait.hasNext();){
					Immediate arg = (Immediate) ait.next();
					if(arg instanceof StringConstant){
						Local newArg = insertLabelIfNecessary((StringConstant) arg, stmt);
						if(newArg != null){
							ie.setArg(i, newArg);
						}
					}
					i++;
				}
			}
			else if(stmt instanceof AssignStmt){
				Value rhs = ((AssignStmt) stmt).getRightOp();
				if(rhs instanceof StringConstant){
					Local newRhs = insertLabelIfNecessary((StringConstant) rhs, stmt);
					if(newRhs != null)
						((AssignStmt) stmt).setRightOp(newRhs);
				}
			}
		}
    }

	private Local insertLabelIfNecessary(StringConstant strConst, Stmt currentStmt)
	{
		String str = strConst.value;
		
		if(!uriValidator.validate(str) && !ipValidator.validate(str))
			return null;
		
		return insertLabelIfNecessary(strConst, currentStmt, str, true, true, false);
	}

}