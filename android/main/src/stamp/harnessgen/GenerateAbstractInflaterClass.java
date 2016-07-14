package stamp.harnessgen;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SootField;
import soot.Scene;
import soot.Modifier;
import soot.VoidType;
import soot.Local;
import soot.IntType;
import soot.RefType;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.Jimple;
import soot.jimple.IntConstant;
import soot.jimple.NullConstant;
import soot.jimple.ThisRef;
import soot.jimple.ParameterRef;
import soot.util.Chain;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import stamp.app.App;
import stamp.app.Layout;
import stamp.app.Widget;

/*
 * @author Saswat Anand
 */
public class GenerateAbstractInflaterClass
{
	private final App app;
	private SootClass inflaterClass;

	public GenerateAbstractInflaterClass(App app)
	{
		this.app = app;
		inflaterClass = new SootClass("android.view.StampLayoutInflater", Modifier.PUBLIC | Modifier.ABSTRACT);
		inflaterClass.setSuperclass(Scene.v().getSootClass("android.view.LayoutInflater"));
		Scene.v().addClass(inflaterClass);

		SootField rootField = new SootField("root", RefType.v("android.view.View"), Modifier.PUBLIC | Modifier.FINAL);
		inflaterClass.addField(rootField);

		addInit();
	}

	private void addInit()
	{
		Type contextType = RefType.v("android.content.Context");
		SootMethod init = new SootMethod("<init>", Arrays.asList(new Type[]{contextType}), VoidType.v(), Modifier.PUBLIC);
		inflaterClass.addMethod(init);
		init.setActiveBody(Jimple.v().newBody(init));
		Chain units = init.getActiveBody().getUnits();
		Chain<Local> locals = init.getActiveBody().getLocals();
		Local thisLocal = Jimple.v().newLocal("r0", inflaterClass.getType());
		locals.add(thisLocal);
		units.add(Jimple.v().newIdentityStmt(thisLocal, new ThisRef(inflaterClass.getType())));
		Local paramLocal = Jimple.v().newLocal("r1", contextType);
		locals.add(paramLocal);
		units.add(Jimple.v().newIdentityStmt(paramLocal, new ParameterRef(contextType, 0)));
		SootMethodRef superInit = Scene.v().getMethod("<android.view.LayoutInflater: void <init>(android.content.Context)>").makeRef();
		units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal, superInit, paramLocal)));
		units.add(Jimple.v().newReturnVoidStmt());
	}


	public SootClass getFinalSootClass() throws IOException
	{
		String widgetsListFile = System.getProperty("stamp.widgets.file");
		PrintWriter writer = new PrintWriter(new FileWriter(new File(widgetsListFile)));
		writer.println(inflaterClass.getName());

		for(Layout layout : app.allLayouts()){
			//add a accessor method for every widget that has an id
			for(Widget widget : layout.allWidgets()){
				if(widget.id < 0)
					continue;
				String widgetClassName = widget.getClassName();
				SootClass wClass = Scene.v().getSootClass(widgetClassName);
				
				SootMethod m = new SootMethod(widgetMethNameFor(widget), 
											  Collections.<Type> emptyList(),
											  RefType.v("android.view.View"), 
											  Modifier.PUBLIC);
				
				if(inflaterClass.declaresMethod(m.getSubSignature()))
					continue;

				inflaterClass.addMethod(m);
				writer.println(widget.id + ","+wClass.getType()+" "+m.getName());

				m.setActiveBody(Jimple.v().newBody(m));
				Chain units = m.getActiveBody().getUnits();
				Local thisLocal = Jimple.v().newLocal("r0", inflaterClass.getType());
				m.getActiveBody().getLocals().add(thisLocal);
				units.add(Jimple.v().newIdentityStmt(thisLocal, new ThisRef(inflaterClass.getType())));
				units.add(Jimple.v().newReturnStmt(NullConstant.v()));
			}
		}
		writer.close();
		return inflaterClass;
	}


	public static String widgetMethNameFor(Widget w)
	{
		return w.idStr.replace(':','$').replace('/','$');
	}

}
