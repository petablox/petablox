package petablox.android.injectannot;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import java.util.*;

public class ContentProviderAnnotation extends AnnotationInjector.Visitor
{
    private final Map<String,SootMethod> srcLabelToLabelMethod = new HashMap();

    private SootClass klass;
    private int newLocalCount;

    public ContentProviderAnnotation()
    {
    }
	
    protected void visit(SootClass klass)
    {
		this.klass = klass;
		this.srcLabelToLabelMethod.clear();
		this.newLocalCount = 0;
		Collection<SootMethod> methodsCopy = new ArrayList(klass.getMethods());
		for(SootMethod method : methodsCopy)
			visitMethod(method);
    }
	
    private void visitMethod(SootMethod method)
    {
		if(!method.isConcrete())
			return;
		Body body = method.retrieveActiveBody();
		Chain<Local> locals = body.getLocals();
		Chain<Unit> units = body.getUnits();
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
						Local newArg = insertLabelIfNecessary((StringConstant) arg, locals, units, stmt);
						if(newArg != null){
							ie.setArg(0, newArg);
						}
					}
					i++;
				}
			}
			else if(stmt instanceof AssignStmt){
				Value rhs = ((AssignStmt) stmt).getRightOp();
				if(rhs instanceof StringConstant){
					Local newRhs = insertLabelIfNecessary((StringConstant) rhs, locals, units, stmt);
					if(newRhs != null)
						((AssignStmt) stmt).setRightOp(newRhs);
				}
			}
		}
    }
    
    private Local insertLabelIfNecessary(StringConstant strConst, Chain<Local> locals, Chain<Unit> units, Unit currentStmt)
    {
		String str = strConst.value;
		if(!str.startsWith("content://"))
			return null;
		if(str.equals("content://"))
			return null;

		SootMethod meth = getOrCreateLabelMethodFor(str);
		Local temp = Jimple.v().newLocal("stamp$stamp$tmp"+newLocalCount++, 
										 RefType.v("java.lang.String"));
		locals.add(temp);
		Stmt toInsert = Jimple.v().newAssignStmt(temp, Jimple.v().newStaticInvokeExpr(meth.makeRef(), strConst));
		units.insertBefore(toInsert, currentStmt);
		return temp;
    }
	
    
    private SootMethod getOrCreateLabelMethodFor(String label)
    {
		SootMethod meth = srcLabelToLabelMethod.get(label);
		if(meth == null){
			RefType stringType = RefType.v("java.lang.String");
			List paramTypes = Arrays.asList(new Type[]{stringType});
			String methName = "stamp$stamp$"+srcLabelToLabelMethod.size();
			meth = new SootMethod(methName, paramTypes, stringType, Modifier.STATIC | Modifier.PRIVATE);
			klass.addMethod(meth);
			srcLabelToLabelMethod.put(label, meth);
			
			JimpleBody body = Jimple.v().newBody(meth);
			meth.setActiveBody(body);
			
			Local param = Jimple.v().newLocal("l0", stringType);
			body.getLocals().add(param);
			
			Chain units = body.getUnits();
			units.add(Jimple.v().newIdentityStmt(param, 
												 Jimple.v().newParameterRef(stringType, 0)));
			
			Local ret = Jimple.v().newLocal("l1", stringType);
			body.getLocals().add(ret);
			units.add(Jimple.v().newAssignStmt(ret,
											   Jimple.v().newNewExpr(stringType)));
			
			SootMethodRef mref = Scene.v().getMethod("<java.lang.String: void <init>(java.lang.String)>").makeRef();
			units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(ret, mref, param)));
			
			units.add(Jimple.v().newReturnStmt(ret));
			
			System.out.println("%%% "+meth.getSignature());
			writeAnnotation(methName+":(Ljava/lang/String;)Ljava/lang/String;@"+klass.getName(), "$"+label, "-1");
		}
		
		return meth;
    }
}