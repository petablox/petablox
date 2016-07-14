package stamp.analyses;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import stamp.srcmap.sourceinfo.abstractinfo.AbstractSourceInfo;

import java.util.Iterator;

import chord.project.Chord;

/*
 * @author Saswat Anand
**/
@Chord(name = "dynfeatures-java")
public class DynamicFeaturesAnalysis extends JavaAnalysis
{
	public int stmtCount = 0;
	public int invokeCount = 0;
	public int readFieldCount = 0;

	public void run()
	{
		measureReflection();
	}
	
	public void measureReflection()
	{
		Iterator mIt = Program.g().scene().getReachableMethods().listener();
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