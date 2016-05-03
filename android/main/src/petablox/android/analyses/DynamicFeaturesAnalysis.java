package petablox.android.analyses;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.Scene;

import petablox.program.Program;
import petablox.project.analyses.JavaAnalysis;
import petablox.android.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

import java.util.Iterator;

import petablox.project.Petablox;

/*
 * @author Saswat Anand
**/
@Petablox(name = "dynfeatures-java")
public class DynamicFeaturesAnalysis extends JavaAnalysis
{
	public void run()
	{
		measureReflection();
	}
	
	void measureReflection()
	{
		int stmtCount = 0;
		int invokeCount = 0;
		int readFieldCount = 0;

		Iterator mIt = Scene.v().getReachableMethods().listener();
		while(mIt.hasNext()){
			SootMethod m = (SootMethod) mIt.next();
			if(!m.isConcrete())
				continue;
			SootClass declKlass = m.getDeclaringClass();
			if(AbstractSourceInfo.isFrameworkClass(declKlass))
				continue;
			
			for(Unit unit : m.retrieveActiveBody().getUnits()){
				Stmt s = (Stmt) unit;
				stmtCount++;
				if(!s.containsInvokeExpr())
					continue;
				SootMethod callee = s.getInvokeExpr().getMethod();
				String calleeName = callee.getName();
				String calleeClassName = callee.getDeclaringClass().getName();
				if(calleeClassName.equals("java.lang.reflect.Method") &&
				   calleeName.equals("invoke")){
					invokeCount++;
				} else if(calleeClassName.equals("java.lang.reflect.Field") &&
						  calleeName.startsWith("get") &&
						  !calleeName.equals("getType")){
					readFieldCount++;
				}
			}
		}

		System.out.println("stmt count = "+stmtCount);
		System.out.println("invoke count = "+invokeCount);
		System.out.println("field read count = "+readFieldCount);
	}
}
