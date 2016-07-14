package stamp.harnessgen;

import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.SootField;
import soot.SootFieldRef;
import soot.Scene;
import soot.Modifier;
import soot.VoidType;
import soot.Local;
import soot.Immediate;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

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
public class GenerateInflaterClass
{
	private final Layout layout;
	private final Set<Widget> allWidgets;
	private final Map<String,SootField> widgetIdToFld = new HashMap();

	private SootClass inflaterClass;

	public GenerateInflaterClass(Layout layout)
	{
		this.layout = layout;
		this.allWidgets = layout.allWidgets();
		String inflaterClassName = "stamp.harness.LayoutInflater$"+layout.id;
		inflaterClass = new SootClass(inflaterClassName, Modifier.PUBLIC);
		inflaterClass.setSuperclass(Scene.v().getSootClass("android.view.StampLayoutInflater"));
		Scene.v().addClass(inflaterClass);
		addFields();
		addInit();
	}

	private void addFields()
	{
		for(Widget widget : allWidgets){
			if(widget.id < 0)
				continue; //dont add fields for widgets without id's
			SootField f = new SootField(GenerateAbstractInflaterClass.widgetMethNameFor(widget),  
										RefType.v(widget.getClassName()), 
										Modifier.PRIVATE | Modifier.FINAL);
			if(inflaterClass.declaresField(f.getSubSignature()))
				continue;
			inflaterClass.addField(f);
			widgetIdToFld.put(widget.idStr, f);
		}
	}

	private void addInit()
	{
		Type contextType = RefType.v("android.content.Context");
		SootMethod init = new SootMethod("<init>", Arrays.asList(new Type[]{contextType}), VoidType.v(), Modifier.PUBLIC);
		inflaterClass.addMethod(init);
		init.setActiveBody(Jimple.v().newBody(init));
		Chain<Unit> units = init.getActiveBody().getUnits();
		Chain<Local> locals = init.getActiveBody().getLocals();
		Local thisLocal = Jimple.v().newLocal("r0", inflaterClass.getType());
		locals.add(thisLocal);
		units.add(Jimple.v().newIdentityStmt(thisLocal, new ThisRef(inflaterClass.getType())));
		Local paramLocal = Jimple.v().newLocal("r1", contextType);
		locals.add(paramLocal);
		units.add(Jimple.v().newIdentityStmt(paramLocal, new ParameterRef(contextType, 0)));
		SootMethodRef superInit = Scene.v().getMethod("<android.view.StampLayoutInflater: void <init>(android.content.Context)>").makeRef();
		units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(thisLocal, superInit, paramLocal)));

		Local contextLocal = Jimple.v().newLocal("context", RefType.v("android.content.Context"));
		locals.add(contextLocal);
		SootFieldRef contextFld = Scene.v().getSootClass("android.view.LayoutInflater").getFieldByName("context").makeRef();
		units.add(Jimple.v().newAssignStmt(contextLocal, Jimple.v().newInstanceFieldRef(thisLocal, contextFld)));

		//instantiate every widget and store it if widget has an id
		Local rootWidgetLocal = (Local) instantiateWidget(layout.rootWidget, thisLocal, contextLocal, units, locals);
		assert rootWidgetLocal != null : layout.rootWidget.getClassName();
			
		SootFieldRef rootViewFld = Scene.v().getSootClass("android.view.StampLayoutInflater").getFieldByName("root").makeRef();	
		units.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal, rootViewFld), rootWidgetLocal));
		
		units.add(Jimple.v().newReturnVoidStmt());
	}

	private Immediate instantiateWidget(Widget widget, Local thisLocal, Local contextLocal, Chain units, Chain<Local> locals)
	{
		String widgetClassName = widget.getClassName();
		SootClass wClass = Scene.v().getSootClass(widgetClassName);
		if(wClass.isPhantom())
			return null;

		boolean isFragment = widget.isFragment;
		Local l = Jimple.v().newLocal(widget.idStr, wClass.getType());
		locals.add(l);
		Immediate w = initWidget(wClass, units, l, contextLocal, isFragment);

		if(w != null){
			//store this inflater instance in the "android.view.View.stamp_inflater" field
			SootFieldRef stamp_inflaterFld;
			if(isFragment)
				stamp_inflaterFld = Scene.v().getSootClass("android.app.Fragment").getFieldByName("stamp_inflater").makeRef();
			else
				stamp_inflaterFld = Scene.v().getSootClass("android.view.View").getFieldByName("stamp_inflater").makeRef();

			units.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(w, stamp_inflaterFld), thisLocal));

			if(widget.id >= 0 && !isFragment){
				SootMethodRef setIdMeth = Scene.v().getMethod("<android.view.View: void setId(int)>").makeRef();
				units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(l, setIdMeth, IntConstant.v(widget.id))));
			}
			
			//store widget instance in field if it has a "resourceId"
			SootField f = widgetIdToFld.get(widget.idStr);
			if(f != null){
				units.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(thisLocal, f.makeRef()), w));
			}
		} else
			System.out.println("WARNING: could not instantiate widget "+widgetClassName);
		
		if(!isFragment){
			List<Widget> children = widget.getChildren();
			if(children != null){
				SootFieldRef childViewFld = Scene.v().getSootClass("android.view.ViewGroup").getFieldByName("child").makeRef();
				for(Widget childWidget : children){
					Immediate childWidgetLocal = instantiateWidget(childWidget, thisLocal, contextLocal, units, locals);
					if(w != null && childWidgetLocal != null)
						units.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(w, childViewFld), childWidgetLocal));
				}
			}
		}
		return w;
	}

	private Local initWidget(SootClass wClass, Chain units, Local widgetLocal, Local contextLocal, boolean isFragment)
	{
		List<Type> paramTypes;
		List<Value> args;
		if(isFragment){
			paramTypes = Collections.<Type> emptyList();
			if(!wClass.declaresMethod("<init>", paramTypes))
				assert false : wClass+" "+wClass.getSuperclass();
			args = Collections.<Value> emptyList();
		} else {
			paramTypes = Arrays.asList(new Type[]{RefType.v("android.content.Context")});
			if(wClass.declaresMethod("<init>", paramTypes)){
				args = Arrays.asList(new Value[]{contextLocal});
			} else {
				paramTypes = Arrays.asList(new Type[]{RefType.v("android.content.Context"), 
													  RefType.v("android.util.AttributeSet")});
				if(wClass.declaresMethod("<init>", paramTypes)){
					args = Arrays.asList(new Value[]{contextLocal,
													 NullConstant.v()});
				} else {
					paramTypes = Arrays.asList(new Type[]{RefType.v("android.content.Context"), 
														  RefType.v("android.util.AttributeSet"), 
														  IntType.v()});
					if(wClass.declaresMethod("<init>", paramTypes)){
						args = Arrays.asList(new Value[]{contextLocal,
														 NullConstant.v(),
													 IntConstant.v(0)});
					} else {
						System.out.println("hello "+wClass.getName()+" "+wClass.isPhantom());for(SootMethod m : wClass.getMethods()) System.out.println(m.getSignature());
						return null;
					}
				}
			}
		}
		
		SootMethod init = wClass.getMethod("<init>", paramTypes);
		units.add(Jimple.v().newAssignStmt(widgetLocal, Jimple.v().newNewExpr(wClass.getType())));
		units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(widgetLocal, init.makeRef(), args)));	
		return widgetLocal;
	}

	public SootClass getFinalSootClass()
	{
		for(Widget widget : allWidgets){
			SootField f = widgetIdToFld.get(widget.idStr);
			if(f == null)
				continue;
			String widgetClassName = widget.getClassName();
			SootClass wClass = Scene.v().getSootClass(widgetClassName);
			
			SootMethod m = new SootMethod(f.getName(), 
										  Collections.<Type> emptyList(),
										  RefType.v("android.view.View"), 
										  Modifier.PUBLIC);
			
			if(inflaterClass.declaresMethod(m.getSubSignature()))
				continue;

			inflaterClass.addMethod(m);
			m.setActiveBody(Jimple.v().newBody(m));
			Chain units = m.getActiveBody().getUnits();
			Chain<Local> locals = m.getActiveBody().getLocals();

			Local thisLocal = Jimple.v().newLocal("r0", inflaterClass.getType());
			locals.add(thisLocal);
			units.add(Jimple.v().newIdentityStmt(thisLocal, new ThisRef(inflaterClass.getType())));

			Local retLocal = Jimple.v().newLocal("ret", m.getReturnType());
			locals.add(retLocal);
			units.add(Jimple.v().newAssignStmt(retLocal, Jimple.v().newInstanceFieldRef(thisLocal, f.makeRef())));
			units.add(Jimple.v().newReturnStmt(retLocal));			
		}

		return inflaterClass;
	}
}
