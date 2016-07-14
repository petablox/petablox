package stamp.injectannot;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import java.util.*;
import java.io.PrintWriter;

public abstract class Visitor
{
	private final Map<String,SootMethod> srcLabelToLabelMethod = new HashMap();
	protected PrintWriter writer;

    private SootClass klass;
    private int newLocalCount;
	
	protected Body body;
	protected Chain<Local> locals;
	protected Chain<Unit> units;

	protected void visit(SootClass klass)
	{
		this.klass = klass;
		this.srcLabelToLabelMethod.clear();
		this.newLocalCount = 0;		
	}
	
	protected void visit(SootMethod method)
	{
		body = method.retrieveActiveBody();
		locals = body.getLocals();
		units = body.getUnits();
	}
	
	protected void writeAnnotation(String methSig, String from, String to)
	{
		writer.println(methSig + " " + from + " " + to);
	}

	protected Local insertLabelIfNecessary(Immediate imm, Unit currentStmt, String label, boolean srcLabel, boolean sinkLabel, boolean insertAfter)
    {
		SootMethod meth = getOrCreateLabelMethodFor(imm.getType(), label, srcLabel, sinkLabel);
		Local temp = Jimple.v().newLocal("stamp$stamp$tmp"+newLocalCount++, imm.getType());
		locals.add(temp);
		Stmt toInsert = Jimple.v().newAssignStmt(temp, Jimple.v().newStaticInvokeExpr(meth.makeRef(), imm));
		if(!insertAfter)
			units.insertBefore(toInsert, currentStmt);
		else
			units.insertAfter(toInsert, currentStmt);
		return temp;
    }
	
    
    private SootMethod getOrCreateLabelMethodFor(Type objType, String label, boolean srcLabel, boolean sinkLabel)
    {
		SootMethod meth = srcLabelToLabelMethod.get(label);
		if(meth == null){
			//RefType stringType = RefType.v("java.lang.String");
			List paramTypes = Arrays.asList(new Type[]{objType});
			
			int i = -1;
			String methName;
			do{
				i++;
				methName = "stamp$stamp$"+i;
			} while(klass.declaresMethodByName(methName));
				
			meth = new SootMethod(methName, paramTypes, objType, Modifier.STATIC | Modifier.PRIVATE);
			klass.addMethod(meth);
			srcLabelToLabelMethod.put(label, meth);
			
			JimpleBody body = Jimple.v().newBody(meth);
			meth.setActiveBody(body);
			
			Local param = Jimple.v().newLocal("l0", objType);
			body.getLocals().add(param);
			
			Chain units = body.getUnits();
			units.add(Jimple.v().newIdentityStmt(param, 
												 Jimple.v().newParameterRef(objType, 0)));
			
			Local ret = Jimple.v().newLocal("l1", objType);
			body.getLocals().add(ret);

			if(!objType.equals(RefType.v("java.lang.String")))
				units.add(Jimple.v().newAssignStmt(ret, param));
			else{
				units.add(Jimple.v().newAssignStmt(ret,
											   Jimple.v().newNewExpr(RefType.v("java.lang.String"))));			
				SootMethodRef mref = Scene.v().getMethod("<java.lang.String: void <init>(java.lang.String)>").makeRef();
				units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(ret, mref, param)));
			}

			units.add(Jimple.v().newReturnStmt(ret));
			
			//System.out.println("%%% "+meth.getSignature());
			String type = "L"+objType.toString().replace('.', '/')+";";
			label = String.format("$stamp$stamp$%s$stamp$stamp$", label);
			if(srcLabel)
				writeAnnotation(methName+":("+type+")"+type+"@"+klass.getName(), "$"+label, "-1");
			if(sinkLabel)
				writeAnnotation(methName+":("+type+")"+type+"@"+klass.getName(), "!"+label, "-1");
		}
		
		return meth;
    }
}