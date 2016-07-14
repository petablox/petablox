package stamp.harnessgen;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SootField;
import soot.Scene;
import soot.Modifier;
import soot.VoidType;
import soot.IntType;
import soot.ArrayType;
import soot.RefType;
import soot.Type;
import soot.Unit;
import soot.Local;
import soot.Value;
import soot.jimple.Jimple;
import soot.jimple.JasminClass;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.jimple.IntConstant;
import soot.util.Chain;
import soot.util.JasminOutputStream;
import soot.jimple.StringConstant;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

import stamp.app.Component;
import stamp.app.Layout;
import stamp.app.Widget;

/*
 * @author Saswat Anand
 */
public class Harness
{
	private final SootClass sClass;
	private final Chain units;
	private final Chain<Local> locals;
	private int count = 0;

	public Harness(String className, List<Component> components)
	{
		sClass = new SootClass(className, Modifier.PUBLIC);
		sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
		Scene.v().addClass(sClass);
		SootMethod method = new SootMethod("main",
								Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
								VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
		sClass.addMethod(method);

		method.setActiveBody(Jimple.v().newBody(method));
		units = method.getActiveBody().getUnits();
		locals = method.getActiveBody().getLocals();

		Local arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
		locals.add(arg);
		units.add(Jimple.v().newIdentityStmt(arg, Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0)));
        //addFields(components);
	}

	public SootClass getFinalSootClass()
	{
		//invoke callCallbacks method
		//SootMethod callCallbacks = Scene.v().getMethod("<edu.stanford.stamp.harness.ApplicationDriver: void callCallbacks()>");
		//units.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(callCallbacks.makeRef())));

		units.add(Jimple.v().newReturnVoidStmt());
		return sClass;
	}

	public void addComponent(Component comp)
	{		
		List<Layout> layouts = comp.layouts;
		SootClass compClass = Scene.v().getSootClass(comp.name);
		
		//call the constructor of the comp
		Local c = init(compClass, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
		if(c == null) return;

		for(Layout layout : layouts) {
			//call the callbacks defined in XML
			for(String cbName : layout.callbacks){
				if (compClass.declaresMethod(cbName, Arrays.asList(new Type[]{RefType.v("android.view.View")}))) {
                    SootMethod cbMethod = compClass.getMethod(cbName, Arrays.asList(new Type[]{RefType.v("android.view.View")}));
                    units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(c, cbMethod.makeRef(), NullConstant.v())));
                } else {
                    System.out.println("ERROR**********Invalid callback:" + cbName);
                }
			}

		}
	}

	private Local init(SootClass klass, List<Type> paramTypes, List<Value> args)
	{
		if(!klass.declaresMethod("<init>", paramTypes)) {
			// This should never happen for a well-
			System.out.println("ERROR**********Init doesn't exist for <activity> node in AndroidManifest.xml");
			return null;
		}
		SootMethod init = klass.getMethod("<init>", paramTypes);
		Local c = Jimple.v().newLocal("c"+count++, klass.getType());
		locals.add(c);
		units.add(Jimple.v().newAssignStmt(c, Jimple.v().newNewExpr(klass.getType())));
		units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(c, init.makeRef(), args)));	
		return c;
	}
	
	/*
	private void addFields(List<Component> comps)
	{
		SootMethod clinit = new SootMethod("<clinit>", Collections.EMPTY_LIST, VoidType.v(), Modifier.STATIC);
		sClass.addMethod(clinit);
		
		Type classType = RefType.v("java.lang.Class");
		Type stringType = RefType.v("java.lang.String");
		SootMethodRef forNameMethod = Scene.v().getMethod("<java.lang.Class: java.lang.Class forName(java.lang.String)>").makeRef();

		clinit.setActiveBody(Jimple.v().newBody(clinit));
		Chain<Unit> stmts = clinit.getActiveBody().getUnits();
		Chain<Local> ls = clinit.getActiveBody().getLocals();
		int n = 0;

		for(Component comp : comps){
			//add fields corresponding to components
			SootField f = new SootField(componentFieldNameFor(comp.name), 
										classType, 
										Modifier.FINAL | Modifier.STATIC | Modifier.PUBLIC);
			sClass.addField(f);

			//initialize them in the clinit
			Local l = Jimple.v().newLocal("c"+n++, classType);
			ls.add(l);
			Local s = Jimple.v().newLocal("s"+n++, stringType);
			ls.add(s);
			stmts.add(Jimple.v().newAssignStmt(s, StringConstant.v(comp.name)));
			//can't support stringconst as an argument now.
			//stmts.add(Jimple.v().newAssignStmt(l, Jimple.v().newStaticInvokeExpr(forNameMethod, StringConstant.v(comp))));
			stmts.add(Jimple.v().newAssignStmt(l, Jimple.v().newStaticInvokeExpr(forNameMethod, s)));
			stmts.add(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(f.makeRef()), l));
		}
		stmts.add(Jimple.v().newReturnVoidStmt());
	}

	public static String componentFieldNameFor(String componentName)
	{
		return componentName.replace('.', '$');
	}
	*/

}
