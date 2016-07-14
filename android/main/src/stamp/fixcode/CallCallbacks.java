package stamp.fixcode;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Local;
import soot.FastHierarchy;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.util.Chain;

import shord.project.analyses.JavaAnalysis;
import shord.program.Program;

import chord.project.Chord;

import java.util.*;

/*
 * @author Saswat Anand
*/
@Chord(name="callbacks-fix")
public class CallCallbacks extends JavaAnalysis
{
	private final Map<SootClass,SootMethod> callCallbacksMethMap = new HashMap();
	
	public void run()
	{
		SootClass objClass = Scene.v().getSootClass("java.lang.Object");
		identifyFrameworkClassesWithCallbacks(objClass, null);

		Program prog = Program.g();
		for(SootClass klass : prog.getClasses()){
			if(prog.isFrameworkClass(klass))
				continue;
			SootClass superClass = klass.getSuperclass();

			//check superClass is a framework class and 
			//declares or inherits "void callCallback()" method and 
			SootMethod callCallbacksMeth = callCallbacksMethMap.get(superClass);
			if(callCallbacksMeth == null)
				continue;

			for(SootMethod method : klass.getMethods()){
				if(!method.getName().equals("<init>"))
					continue;
				Chain<Unit> units = method.retrieveActiveBody().getUnits();
				Iterator<Unit> uit = units.snapshotIterator();
				while(uit.hasNext()){
					Stmt stmt = (Stmt) uit.next();
					if(!stmt.containsInvokeExpr())
						continue;
					InvokeExpr ie = stmt.getInvokeExpr();
					SootMethod callee = ie.getMethod();
					SootClass calleeClass = callee.getDeclaringClass();
					
					//check if it is a call to superclass's constructor
					if(!callee.getName().equals("<init>") || !calleeClass.equals(superClass))
						continue;

					Local base = (Local) ((SpecialInvokeExpr) ie).getBase();
					Stmt callStmt = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(base, callCallbacksMeth.makeRef()));
					units.insertAfter(callStmt, stmt);
				}
			}
		}
	}
	
	void identifyFrameworkClassesWithCallbacks(SootClass klass, SootMethod callCallbacksMeth)
	{
		if(klass.declaresMethod("void callCallbacks()"))
			callCallbacksMeth = klass.getMethod("void callCallbacks()");
		
		if(callCallbacksMeth != null)
			callCallbacksMethMap.put(klass, callCallbacksMeth);

		FastHierarchy fh = Program.g().scene().getOrMakeFastHierarchy();
		for(Iterator cIt = fh.getSubclassesOf(klass).iterator(); cIt.hasNext();) {
			final SootClass c = (SootClass) cIt.next();
			if(Program.g().isFrameworkClass(klass))
				identifyFrameworkClassesWithCallbacks(c, callCallbacksMeth);
		}
	}
}