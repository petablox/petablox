package petablox.android.injectannot;

import java.io.File;
import soot.*;
import soot.jimple.*;
import soot.util.*;
import soot.jimple.toolkits.annotation.defs.ReachingDefsTagger;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.internal.VariableBox;
import soot.tagkit.LinkTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import petablox.program.Program;
import java.util.*;
import edu.stanford.droidrecord.logreader.events.info.ParamInfo;

import stamp.droidrecord.DroidrecordProxy;
import stamp.droidrecord.StampCallArgumentValueAnalysis;
import petablox.android.analyses.ReachingDefsAnalysis;

/**
 * Visitor class for instrumenting the app code befere they are fed to petablox. 
 * Procedure: For each bundleId, inject getter/setter to Bundle.classs; 
 * For get/putString, replace to get/put_$bundleId;
 * Then build the connection between inter-components through static field "intent",
 * e.g, in source: call "$target.set_Intent(intent)";
 * Currently we only consider the following method invocations:
 * public Intent (String action, Uri uri, Context packageContext, Class<?> cls) 
 * public Intent setClass(Context packageContext, Class<?> cls) 
 * public Intent setClassName (Context packageContext, String className) 
 * public Intent setClassName (String packageName, String className) 
 * public Intent setComponent (ComponentName component)
 *
 * @author Yu Feng (yufeng@cs.wm.edu)
 * @date Jun 15, 2013
 */

public class InterComponentInstrument extends AnnotationInjector.Visitor
{

	private String intentClass = "android.content.Intent";
	
	private String bundleClass = "android.os.Bundle";
	
	//turn on/off inter-activity instrument.
	private Boolean inferInterActivity = false;
	
	//Total bundle operations.
	private static int bundleOperCnt = 0;
	
	//Successfully find.
	private static int succBunOperCnt = 0;
		 
	//need statistics or not.
	private static Boolean needStat = true;

    private SootClass rootClass;
	
	//cache the temporary result from new ComponentName.
	private Map<Value, String> arg2CompnentName = new HashMap<Value, String>();
	
    private StampCallArgumentValueAnalysis cavAnalysis = null;
	
    private static HashSet<String> unknownBundleSrcMethods = new HashSet<String>();
    
    private SootMethod currentMethod;
    
    public InterComponentInstrument()
    {
        DroidrecordProxy droidrecord = stamp.droidrecord.DroidrecordProxy.g();
        if(droidrecord.isAvailable()) {
            cavAnalysis = droidrecord.getCallArgumentValueAnalysis();
            cavAnalysis.run();
        }
    }
    
    private List<ParamInfo> queryArgumentValues(SootMethod caller, Stmt stmt, 
                                                int argNum) {
        if(cavAnalysis != null)
            return cavAnalysis.queryArgumentValues(caller, stmt, argNum);
        else
            return java.util.Collections.EMPTY_LIST;
    }
	
    protected void visit(SootClass klass)
    {
		//filter out android.support.v4.jar
		if (klass.getName().contains("android.support")) return;
		this.rootClass = klass;
		Collection<SootMethod> methodsCopy = new ArrayList(klass.getMethods());
		for(SootMethod method : methodsCopy)
			visitMethod(method);
    }
	
    private void visitMethod(SootMethod method)
    {
		this.currentMethod = method;
        
		if(!method.isConcrete())
			return;
		
		Body body = method.retrieveActiveBody();
		
		//run reaching def for each body, should combine with dynamic analysis result.
        //this.runReachingDef(body);
        ReachingDefsAnalysis.runReachingDef(body);
		
		Chain<Local> locals = body.getLocals();
		Chain<Unit> units = body.getUnits();
		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
	    
			//invocation statements
			if(stmt.containsInvokeExpr()){
				InvokeExpr ie = stmt.getInvokeExpr();
				String methodRefStr = ie.getMethodRef().toString();
                
				look4BundleKeyToInject(body, stmt);
				
				//turn on/off inter-activity instrument.
				if (!inferInterActivity) continue;
				
				// multiple cases to locate a target.
				// 1. new Intent(context, class)
				if (methodRefStr.contains(intentClass
						+ ": void <init>(android.content.Context,java.lang.Class)")) {
					this.lookforTgtArgToInject(units, stmt, ie.getArg(1));							
					
				}

				// 2. intent.setClass(context, class)
				if (methodRefStr.contains(intentClass + ": " + intentClass
						+ " setClass(android.content.Context,java.lang.Class)")) {
					this.lookforTgtArgToInject(units, stmt, ie.getArg(1));							
				}
				
				// 3. intent.setClassName(context, String)
				/* e.g, $r4.<android.content.Intent: android.content.Intent 
				 *	setClassName(android.content.Context,java.lang.String)>($r0, $r7);
				 */
				if (methodRefStr.contains(intentClass + ": " + intentClass
						+ " setClassName(android.content.Context,java.lang.String)")) {
					this.lookforTgtArgToInject(units, stmt, ie.getArg(1));							
				}
				
				// 4. intent.setClassName(String, String)
				/* e.g, $r4.<android.content.Intent: android.content.Intent 
				 *	setClassName(java.lang.String,java.lang.String)>($r6, $r7) 
				 */
				if (methodRefStr.contains(intentClass + ": " + intentClass
						+ " setClassName(java.lang.String,java.lang.String)")) {
					this.lookforTgtArgToInject(units, stmt, ie.getArg(1));							
				}
				
				// 5. intent.setComponent(ComponentName)
				/* e.g, $r4.<android.content.Intent: android.content.Intent 
				 *	setComponent(android.content.ComponentName)>($r3); 
				 *  right now I can not capture inter-procedure call, so I have to combine 
				 *  both of them, ask for the following call:
				 *  <android.content.ComponentName: void <init>(java.lang.String,java.lang.String)>($r6, $r7)
				 *  Notice that it's still not precise.
				 */
				if (methodRefStr.contains("android.content.ComponentName: void <init>(java.lang.String,java.lang.String)")) {
                    String tgtComptName = "";
					StringConstant strCont;
					if (ie.getArg(1) instanceof StringConstant) {
						strCont = (StringConstant) ie.getArg(1);
						tgtComptName = strCont.value;
					}else {// FIXME: too ugly to grab result in this way
						//otherwise we have to ask for reaching def.
						for (Tag tagEntity : stmt.getTags()) {
							if(!(tagEntity instanceof LinkTag)) continue; 
							LinkTag ttg = (LinkTag) tagEntity;
							if ( !(ttg.getLink() instanceof JAssignStmt)) continue;
							JAssignStmt asst = (JAssignStmt) ttg.getLink();
							if (asst.getLeftOp().equals(ie.getArg(1))) {
								// assert (asst.getRightOp() instanceof ClassConstant);
                                if (asst.getRightOp() instanceof StringConstant) {
									strCont = (StringConstant) asst.getRightOp();
									tgtComptName = strCont.value;
								}

							}
						}

					}
					JimpleLocalBox newBox = (JimpleLocalBox)ie.getUseBoxes().get(0);
					this.arg2CompnentName.put(newBox.getValue(), tgtComptName);
				}
				
				if (methodRefStr.contains(intentClass + ": " + intentClass
						+ " setComponent(android.content.ComponentName)")) {
					//FIXME need more information on setComponent, improve current reachingDef.
					String tgtComptName = this.arg2CompnentName.get(ie.getArg(0));
					
					if (tgtComptName != null) 
						tgtComptName = tgtComptName.replace(File.separatorChar, '.');
					///Check whether exist such class.
					if ( !Scene.v().containsClass(tgtComptName)) {
						//reportUnknownRegister(stmt, ie.getArg(0));
                        _tmp_reportUnknownRegisterDynInfo(stmt, ie.getArg(0), 1);
						return;
					}
				
					this.instruTgtCompt(tgtComptName);
		
					//begin to instument current invoke method of src.		
					JimpleLocalBox localBox = (JimpleLocalBox) ie
						.getUseBoxes().get(0);
					SootMethod toCall = Scene.v().getMethod(
						"<" + tgtComptName + ": void set_Intent("
							+ intentClass + ")>");
					Stmt invokeSetter = Jimple.v().newInvokeStmt(
						Jimple.v().newStaticInvokeExpr(
							toCall.makeRef(), localBox.getValue()));
					units.insertAfter(invokeSetter, stmt);
				}
				
			}
		}
    }
	
	/** 
	  * Setter in bundle
	  **/
	private boolean look4BundleSetter(Body body, Stmt stmt) {
		boolean result = false;
		Chain<Unit> units = body.getUnits();
		InvokeExpr ie = stmt.getInvokeExpr();	
		if (ie.getUseBoxes().size() < 2) return result;															
		ImmediateBox bundleLoc = (ImmediateBox) ie.getUseBoxes().get(1);
		Value putStringArg = bundleLoc.getValue();
		JimpleLocalBox bundleObj = (JimpleLocalBox) ie.getUseBoxes().get(0);
		ArrayList<String> bundleKeyList = readKeysFromTag(stmt, putStringArg);
		if (bundleKeyList.size() == 0) return result;	
		
		if (ie.getMethod().getParameterCount() < 2) {
			System.out.println("WARN:Could not analyze: " + stmt);
			return result;
		}
		Type keyType = getInstrumentType(ie.getMethod().getParameterType(1));
		
		for (String bundleKey : bundleKeyList) {
			//need to add getter/setter of bundlekey to Bundle.class!
			//modify bundle key.  key_type, for primitive.
			if (!keyType.toString().equals("java.lang.Object")) 
				bundleKey = bundleKey + "_" +keyType.toString();
			
			instrumentBundle(bundleKey, keyType);
			

			SootMethod toCall = Scene.v().getMethod(
				"<" + this.bundleClass + ": void put_"
					+ bundleKey + "("+keyType.toString()+")>");

			// System.out.println("tocall = " + toCall );
			InvokeStmt invokeSetter = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
					(Local) bundleObj.getValue(),
						toCall.makeRef(), ie.getArg(1)));
			units.insertAfter(invokeSetter, stmt);
			// writeUnknownToBundle(body, stmt, (Local)bundleObj.getValue(), (Local)ie.getArg(1));
		}
				
		if (bundleKeyList.size() > 0) {
			units.remove(stmt);	
			result = true;
		}
		
		return result;			
	}
	
	/** 
	  * Setter in intent
	  **/
	private boolean look4IntentSetter(Body body, Stmt stmt) {
		boolean result = false;
		Chain<Unit> units = body.getUnits();
		InvokeExpr ie = stmt.getInvokeExpr();
		if (ie.getUseBoxes().size() < 2) return result;	
		ImmediateBox bundleLoc = (ImmediateBox) ie.getUseBoxes().get(1);
		Value putStringArg = bundleLoc.getValue();		
		JimpleLocalBox intentObj = (JimpleLocalBox) ie.getUseBoxes().get(0);
		ArrayList<String> bundleKeyList = readKeysFromTag(stmt, putStringArg);
		if (bundleKeyList.size() == 0) return result;	
	
		//FIXME:Can not handler putExtra(bundle) or putExtra(intent)!
		if (ie.getMethod().getParameterCount() < 2) {
			System.out.println("WARN:Could not analyze: " + stmt);
			return result;
		}
		
		Type keyType = getInstrumentType(ie.getMethod().getParameterType(1));
		SootClass iKlass = Scene.v().getSootClass(intentClass);
    	SootField extrasField = iKlass.getFieldByName("extras");
		Local extrasLocal = Jimple.v().newLocal("r_Extras", extrasField.getType()); 
		body.getLocals().add(extrasLocal);
		AssignStmt assign2Extras = soot.jimple.Jimple.v().newAssignStmt(
						extrasLocal, Jimple.v().newStaticFieldRef(extrasField.makeRef()));
		units.insertBefore(assign2Extras, stmt);

		
		for (String bundleKey : bundleKeyList) {
			
			//modify bundle key.  key_type, for primitive.
			if (!keyType.toString().equals("java.lang.Object")) 
				bundleKey = bundleKey + "_" +keyType.toString();
			
			//need to add getter/setter of bundlekey to Bundle.class!
			instrumentBundle(bundleKey, keyType);
			
			//invoke extra.put_deviceId()
			SootMethod putExtrasCall = Scene.v().getMethod(
				"<" + this.bundleClass + ": void put_"
					+ bundleKey + "("+keyType.toString()+")>");
			
			InvokeStmt putExtraStmt = Jimple.v().newInvokeStmt(
				Jimple.v().newVirtualInvokeExpr(
					extrasLocal,
					putExtrasCall.makeRef(), ie.getArg(1)));
			units.insertAfter(putExtraStmt, stmt);
			//write value to unknown field.
			// writeUnknownToBundle(body, stmt, extrasLocal, (Local)ie.getArg(1));
			
		}

		//Remove this will reduce the false alarm like deviceId=>intent, but potentially buggy,
		//e.g, what if I assign current expr to an new intent object?
		if (bundleKeyList.size() > 0) {
			units.remove(stmt);	
			result = true;
		}
		
		return result;			
	}
	
	/** 
	  * Getter in bundle
	  **/
	private boolean look4BundleGetter(Body body, Stmt stmt) {
		boolean result = false;
		Chain<Unit> units = body.getUnits();
		InvokeExpr ie = stmt.getInvokeExpr();	
		if (ie.getUseBoxes().size() < 2) return result;															
		ImmediateBox bundleLoc = (ImmediateBox) ie.getUseBoxes().get(1);
		Value putStringArg = bundleLoc.getValue();
		ArrayList<String> bundleKeyList = readKeysFromTag(stmt, putStringArg);
		if (bundleKeyList.size() == 0) return result;	
		
		JimpleLocalBox bundleObj = (JimpleLocalBox) ie.getUseBoxes().get(0);
		Type keyType = getInstrumentType(ie.getMethod().getReturnType());
			
		for (String bundleKey : bundleKeyList) {
			//modify bundle key.  key_type, for primitive.
			if (!keyType.toString().equals("java.lang.Object")) 
				bundleKey = bundleKey + "_" +keyType.toString();
			
			instrumentBundle(bundleKey, keyType);
			// invoke
			SootMethod toCall = Scene.v().getMethod(
				"<" + this.bundleClass
					+ ": "+keyType.toString()+" get_" + bundleKey + "()>");

			VirtualInvokeExpr invoke = 
				Jimple.v().newVirtualInvokeExpr(
					(Local) bundleObj.getValue(),
						toCall.makeRef(), Arrays.asList(new Value[] {}));
				
			//FIXME: what if we have multiple defboxes?
			if (stmt.getDefBoxes().size() > 0) {
				VariableBox orgCallSite = (VariableBox)stmt.getDefBoxes().get(0);
				AssignStmt invokeAssign = Jimple.v().newAssignStmt(orgCallSite.getValue(), invoke);			
				units.insertAfter(invokeAssign, stmt);
			} else {
				units.insertAfter(Jimple.v().newInvokeStmt(invoke), stmt);
			}
		}

		if (bundleKeyList.size() > 0) {
			units.remove(stmt);	
			result = true;
		}
		return result;	
	}
	
	/** 
	  * Getter in intent
	  **/
	private boolean look4IntentGetter(Body body, Stmt stmt) {
		boolean result = false;
		Chain<Unit> units = body.getUnits();
		InvokeExpr ie = stmt.getInvokeExpr();
		if (ie.getUseBoxes().size() < 2) return result;
		ImmediateBox bundleLoc = (ImmediateBox) ie.getUseBoxes().get(1);
		Value putStringArg = bundleLoc.getValue();
		ArrayList<String> bundleKeyList = readKeysFromTag(stmt, putStringArg);
		if (bundleKeyList.size() == 0) return result;	
		
		JimpleLocalBox intentObj = (JimpleLocalBox) ie.getUseBoxes().get(0);
		Type keyType = getInstrumentType(ie.getMethod().getReturnType());
		
		SootClass iKlass = Scene.v().getSootClass(intentClass);
    	SootField extrasField = iKlass.getFieldByName("extras");
		Local extrasLocal = Jimple.v().newLocal("r_Extras", extrasField.getType()); 
		body.getLocals().add(extrasLocal);
		AssignStmt assign2Extras = soot.jimple.Jimple.v().newAssignStmt(
						extrasLocal, Jimple.v().newStaticFieldRef(extrasField.makeRef()));
		units.insertBefore(assign2Extras, stmt);
		
		for (String bundleKey : bundleKeyList) {
			//modify bundle key.  key_type, for primitive.
			if (!keyType.toString().equals("java.lang.Object")) 
				bundleKey = bundleKey + "_" +keyType.toString();
			instrumentBundle(bundleKey, keyType);	

			SootMethod getObjCall = Scene.v().getMethod("<" + this.bundleClass 
				+ ": "+keyType.toString()+" get_" + bundleKey + "()>");

			VirtualInvokeExpr invokeGetStr = 
				Jimple.v().newVirtualInvokeExpr(
					 extrasLocal, 
						 getObjCall.makeRef(), Arrays.asList(new Value[] {}));
			
			//FIXME: what if we have multiple defboxes?
			// assert (stmt.getDefBoxes().size > 0);
			if (stmt.getDefBoxes().size() == 0) {
				// reportUnknownRegister(stmt, extrasLocal);
                _tmp_reportUnknownRegisterDynInfo(stmt, extrasLocal, 1);

				return result;
			}
				
			VariableBox orgCallSite = (VariableBox)stmt.getDefBoxes().get(0);
			AssignStmt invokeAssign = Jimple.v().newAssignStmt(orgCallSite.getValue(), invokeGetStr);	
			units.insertAfter(invokeAssign, stmt);
		}

		if (bundleKeyList.size() > 0) {
			units.remove(stmt);	
			result = true;
		}
		return result;	
	}
	
	/**
	 * Even if we find the bundle key, should also record the value to its corresponding unknown fields.
	 * primitive types go to unknown_T, and all other ref types go to "unknown" obj.
	 * extras.unknown_T = arg;
	 **/
	private void writeUnknownToBundle(Body body, Stmt stmt, Local extras, Local arg) {		
		InvokeExpr ie = stmt.getInvokeExpr();	
		String methodRefStr = ie.getMethodRef().toString();
		SootClass bKlass = Scene.v().loadClassAndSupport(bundleClass);
		SootField unknownField;
		if (methodRefStr.contains("boolean)")) {
			unknownField = bKlass.getFieldByName("unknown_boolean");  
			
			} else if (methodRefStr.contains("byte)")) {
				unknownField = bKlass.getFieldByName("unknown_byte");  
			} else if (methodRefStr.contains("char)")) {
				unknownField = bKlass.getFieldByName("unknown_char");  
			} else if (methodRefStr.contains("short)")) {
				unknownField = bKlass.getFieldByName("unknown_short");  
			} else if (methodRefStr.contains("int)")) {
				unknownField = bKlass.getFieldByName("unknown_int");  
			} else if (methodRefStr.contains("long)")) {
				unknownField = bKlass.getFieldByName("unknown_long");  
			} else if (methodRefStr.contains("float)")) {
				unknownField = bKlass.getFieldByName("unknown_float");  
			} else if (methodRefStr.contains("double)")) {
				unknownField = bKlass.getFieldByName("unknown_double");  									
			} else {
	   		    unknownField = bKlass.getFieldByName("unknown");  										
			}
		     
		SootFieldRef unknownFieldRef = unknownField.makeRef();

		soot.jimple.FieldRef unknownInt = soot.jimple.Jimple.v()
			.newInstanceFieldRef(extras, unknownFieldRef);

		soot.jimple.AssignStmt assign = soot.jimple.Jimple.v()
			.newAssignStmt(unknownInt, arg);
		body.getUnits().insertAfter(assign, stmt);
	}
	
	/* Read the reaching def values of the regester.*/
	private ArrayList<String> readKeysFromTag(Stmt stmt, Value arg) {
		ArrayList<String> reachingDef = new ArrayList<String>();
		// Value putStringArg = bundleLoc.getValue();
		StringConstant strVal = StringConstant.v("dummy");
			
		if (arg instanceof StringConstant){
			strVal = (StringConstant) arg;
			reachingDef.add(strVal.value);
		} else {
			// otherwise we have to ask for reaching def.
			for (Tag tagEntity : stmt.getTags()) {
				if(!(tagEntity instanceof LinkTag)) continue; 
				LinkTag ttg = (LinkTag) tagEntity;
				if ( !(ttg.getLink() instanceof JAssignStmt)) continue;
				JAssignStmt asst = (JAssignStmt) ttg.getLink();
					
				// FIXME:can not deal with inter-proc now!
				if (asst.getLeftOp().equals(arg)) {
					// assert (asst.getRightOp() instanceof StringConstant);
					if (asst.getRightOp() instanceof StringConstant) {
						strVal = (StringConstant) asst.getRightOp();
						String bundleKey = strVal.value;
						bundleKey = bundleKey.replaceAll("[\\s]+", "_");
						reachingDef.add(bundleKey);			
					} else { //may be function call or private field from inter-proc.
						//reportUnknownRegister(stmt, arg);
                        _tmp_reportUnknownRegisterDynInfo(stmt, arg, 2);
					}
				} 
			}
		}
	
		return reachingDef;
	}
	
	/* Looking for the right setter/getter to instument. */
	private void look4BundleKeyToInject(Body body, Stmt stmt) {
		Chain<Unit> units = body.getUnits();
		InvokeExpr ie = stmt.getInvokeExpr();	
		String methodRefStr = ie.getMethodRef().toString();
		
		if (methodRefStr.matches("^<android.content.Intent: .* get.*Extra.*") && !methodRefStr.contains("getExtras()>")) {
	        if(look4IntentGetter(body, stmt)) succBunOperCnt++;	
			bundleOperCnt++;
		}
		
		if (methodRefStr.matches("^<android.os.Bundle: .* get.*")) {
	        if(look4BundleGetter(body, stmt)) succBunOperCnt++;	
			bundleOperCnt++;
		}
		
		if (methodRefStr.matches("^<android.content.Intent: android.content.Intent put.*")) {
	        if(look4IntentSetter(body, stmt)) succBunOperCnt++;	
			bundleOperCnt++;
		}
		
		if (methodRefStr.matches("^<android.os.Bundle: void put.*")) {
	        if(look4BundleSetter(body, stmt)) succBunOperCnt++;
			bundleOperCnt++;
		}
	}
	
	
	/* Search for the target component based on reachingDef stored in tag. */	
	private void lookforTgtArgToInject(Chain<Unit> units, Stmt stmt, Value arg) {
		
		String tgtComptName = "";
		ClassConstant clazz = ClassConstant.v("dummy");
		StringConstant strCont = StringConstant.v("dummy");
		InvokeExpr ie = stmt.getInvokeExpr();
		
		if (arg instanceof ClassConstant) {
			clazz = (ClassConstant) arg;
			tgtComptName = clazz.value;
		} else if (arg instanceof StringConstant) {
			strCont = (StringConstant) arg;
			tgtComptName = strCont.value;
		}else {// FIXME: too ugly to grab result in this way
			//otherwise we have to ask for reaching def.
			for (Tag tagEntity : stmt.getTags()) {
				// String tagStr = arg.toString();
				if(!(tagEntity instanceof LinkTag)) continue; 
				LinkTag ttg = (LinkTag) tagEntity;
				if ( !(ttg.getLink() instanceof JAssignStmt)) continue;
				JAssignStmt asst = (JAssignStmt) ttg.getLink();
				if (asst.getLeftOp().equals(arg)) {
					// assert (asst.getRightOp() instanceof ClassConstant);
					if (asst.getRightOp() instanceof ClassConstant) {
						clazz = (ClassConstant) asst.getRightOp();		
						tgtComptName = clazz.value;	
					} else if (asst.getRightOp() instanceof StringConstant) {
						strCont = (StringConstant) asst.getRightOp();
						tgtComptName = strCont.value;
					}

				}
			}

		}
		
		if (tgtComptName.equals("")) {
			// System.out.println("Can we do better.........** query dynamic analysis.");
			// 		    List<ParamInfo> paramList =  queryArgumentValues(SootMethod caller, stmt, int argNum);
			// for(ParamInfo info : paramList) {
			// 	System.out.println("Analysis result......" + info);
			// }
			//reportUnknownRegister(stmt, arg);
            _tmp_reportUnknownRegisterDynInfo(stmt, arg, 2);
			return;
		}
		
		tgtComptName = tgtComptName.replace(File.separatorChar, '.');
		///Check whether exist such class.
		if ( !Scene.v().containsClass(tgtComptName)) {
            //reportUnknownRegister(stmt, arg);
            _tmp_reportUnknownRegisterDynInfo(stmt, arg, 2);
			return;
		}
				
		this.instruTgtCompt(tgtComptName);
		
		//begin to instument current invoke method of src.		
		JimpleLocalBox localBox = (JimpleLocalBox) ie
			.getUseBoxes().get(0);
		SootMethod toCall = Scene.v().getMethod(
			"<" + tgtComptName + ": void set_Intent("
				+ intentClass + ")>");
		Stmt invokeSetter = Jimple.v().newInvokeStmt(
			Jimple.v().newStaticInvokeExpr(
				toCall.makeRef(), localBox.getValue()));
		units.insertAfter(invokeSetter, stmt);
	}

    /* Inject get/set-Intent method bodies for target component.*/
	private void instruTgtCompt(String klassName) {
		String intentInstName = "intent";
		klassName = klassName.replace(File.separatorChar, '.');
		SootClass klass =  Scene.v().getSootClass(klassName);		
		
		//Check whether this class has already been instrumented
		if(klass.declaresFieldByName(intentInstName)) {
			System.out.println("WARN: class has already been instrumented.." + klassName);
			return;
		}
		
		Collection<SootMethod> methodsCopy = new ArrayList(klass.getMethods());
		Body body;
		Chain<Unit> units;

		// add public static Intent intent;
		// "private static android.content.Intent intent;"
		SootField intentField = new SootField(intentInstName,
				RefType.v(this.intentClass), Modifier.PUBLIC | Modifier.STATIC);
		klass.addField(intentField);

		// "public android.content.Intent getIntent() {return intent; }"
		SootMethod m_getter = new SootMethod("getIntent", Arrays.asList(),
				RefType.v(this.intentClass), Modifier.PUBLIC);
		// stmt
		body = Jimple.v().newBody(m_getter);
		m_getter.setActiveBody(body);
		units = body.getUnits();
		//add this ref!!!!, otherwise it will be static..
		ThisRef thisRef = new ThisRef(klass.getType());
		Local thisLocal = Jimple.v().newLocal("r0", thisRef.getType());   
		body.getLocals().add(thisLocal);

		units.add(Jimple.v().newIdentityStmt(thisLocal,
		            Jimple.v().newThisRef((RefType)thisRef.getType())));
      
		SootFieldRef sFieldRef = intentField.makeRef();
		Local new_obj = Jimple.v().newLocal("$r1", intentField.getType());

		body.getLocals().add(new_obj);
		Value v = Jimple.v().newStaticFieldRef(sFieldRef);
		soot.jimple.AssignStmt g_assign = soot.jimple.Jimple.v().newAssignStmt(
				new_obj, v);

		body.getUnits().add(g_assign);
		Stmt retStmt = soot.jimple.Jimple.v().newReturnStmt(new_obj);
		units.add(retStmt);
		klass.addMethod(m_getter);
		// System.out.println("targetBody====getter==" + body);

		// "public static void set_Intent(android.content.Intent i) {intent = i;}",
		SootMethod m_setter = new SootMethod("set_Intent",
				Arrays.asList(RefType.v(this.intentClass)), VoidType.v(),
				Modifier.PUBLIC | Modifier.STATIC);
		// stmt
		body = Jimple.v().newBody(m_setter);
		m_setter.setActiveBody(body);
		units = body.getUnits();
		Local paramLocal = Jimple.v().newLocal("r0",
				RefType.v(this.intentClass));
		body.getLocals().add(paramLocal);
		// add "r0 = @parameter0"
		soot.jimple.ParameterRef paramRef = soot.jimple.Jimple.v()
				.newParameterRef(RefType.v(this.intentClass), 0);
		soot.jimple.Stmt idStmt = soot.jimple.Jimple.v().newIdentityStmt(
				paramLocal, paramRef);
		body.getUnits().add(idStmt);

		soot.jimple.FieldRef fieldRef = soot.jimple.Jimple.v()
				.newStaticFieldRef(sFieldRef);

		soot.jimple.AssignStmt assign = soot.jimple.Jimple.v().newAssignStmt(
				fieldRef, paramLocal);
		body.getUnits().add(assign);

		units.add(Jimple.v().newReturnVoidStmt());

		klass.addMethod(m_setter);
		// System.out.println("targetBody====setter==" + body);
		// 
		// System.out.println("Target===field===>!!" + intentField);
		// System.out.println("Target===getIntent===>!!" + m_getter);
		// System.out.println("Target******add set_Intent===>!!" + m_setter);

	}
	
	private Type getInstrumentType(Type keyType) {
		String[] primitiveArray = {"int", "double", "byte", "short", "long", "boolean", "char"};
        ArrayList<String> list = new ArrayList<String>();
        for( int i=0; i < primitiveArray.length; i++){
            list.add(primitiveArray[i]);
        }
		
		if (!list.contains(keyType.toString())) keyType = RefType.v("java.lang.Object");
		return keyType;
	}
	
	/* Inject field + getter/setter to Bundle.class based on
	 * the bundleId collected from app. */
	private void instrumentBundle(String bundleKey, Type keyType) {
		//Look up for the Bundle sootclass at first. What if i can't get the bundle?
		SootClass klass = Scene.v().loadClassAndSupport(bundleClass);

		//already contain this bundleKey?
		if (klass.declaresFieldByName(bundleKey)) {
			System.out.println("already found the key**, return.");
			return;
		}
		
        // keyType = getInstrumentType(keyType);
		// add key field, e.g, "public Object deviceId;"
		SootField keyField = new SootField(bundleKey,
			keyType, Modifier.PUBLIC);

		klass.addField(keyField);

		keyField = klass.getFieldByName(bundleKey);
		// getter, e.g, "public Object get_deviceId() {return deviceId; }",
		SootMethod m_getter = new SootMethod("get_" + bundleKey,
			Arrays.asList(), keyType,
				Modifier.PUBLIC);
		// stmt
		JimpleBody body = Jimple.v().newBody(m_getter);
		m_getter.setActiveBody(body);
		Chain units = body.getUnits();
		
		//
		ThisRef thisRef = new ThisRef(klass.getType());
		Local thisLocal = Jimple.v().newLocal("r0", thisRef.getType());   
		body.getLocals().add(thisLocal);

		units.add(Jimple.v().newIdentityStmt(thisLocal,
		            Jimple.v().newThisRef((RefType)thisRef.getType())));
       
		SootFieldRef sFieldRef = keyField.makeRef();
		Local returnLocal = Jimple.v().newLocal("r1", keyField.getType());
		body.getLocals().add(returnLocal);
			
		Value v = Jimple.v().newInstanceFieldRef(thisLocal, sFieldRef);
		soot.jimple.AssignStmt returnAssign = soot.jimple.Jimple.v().newAssignStmt(
			returnLocal, v);
		body.getUnits().add(returnAssign);
		Stmt retStmt = soot.jimple.Jimple.v().newReturnStmt(returnLocal);
		units.add(retStmt);
		klass.addMethod(m_getter);
		// System.out.println("inject field and methods into bundle....done: getter: " + body);
		

		// setter, e.g,  "public void put_deviceId(Object v) {deviceId = v; }",
		SootMethod m_setter = new SootMethod("put_" + bundleKey,
			Arrays.asList(keyType), VoidType.v(),
				Modifier.PUBLIC);
		body = Jimple.v().newBody(m_setter);
		m_setter.setActiveBody(body);
		units = body.getUnits();
		
		//r0 should actually point to "this".
		ThisRef sThisRef = new ThisRef(klass.getType());
		Local sThisLocal = Jimple.v().newLocal("r0", sThisRef.getType());   
		body.getLocals().add(sThisLocal);

		units.add(Jimple.v().newIdentityStmt(sThisLocal,
			Jimple.v().newThisRef((RefType)sThisRef.getType())));

		Local paramLocal = Jimple.v().newLocal("r1",
			RefType.v("java.lang.Object"));
		body.getLocals().add(paramLocal);
		// add "l0 = @parameter0"
		soot.jimple.ParameterRef paramRef = soot.jimple.Jimple.v()
			.newParameterRef(keyType, 0);
		soot.jimple.Stmt idStmt = soot.jimple.Jimple.v().newIdentityStmt(
			paramLocal, paramRef);
		body.getUnits().add(idStmt);

		soot.jimple.FieldRef instFieldRef = soot.jimple.Jimple.v()
			.newInstanceFieldRef(sThisLocal, sFieldRef);

		soot.jimple.AssignStmt assign = soot.jimple.Jimple.v()
			.newAssignStmt(instFieldRef, paramLocal);
		body.getUnits().add(assign);

		units.add(Jimple.v().newReturnVoidStmt());
		klass.addMethod(m_setter);
		// System.out.println("inject field and methods into bundle....done: ");
		

	}
	
	/** (1) get_XXX method return value of field XXX. done.
	  * (2) put_XXX method stores the value into field XXX and the unknown
	  * field that is type-compatible
	  * (3) put method stores the value into field unknown and all XXX fields
	  * that are type compatible
	  * (4) get method gets the value of the field unknown only */
	public static void injectUnknownSrc() {
		
		String bundleClass = "android.os.Bundle";
		String intentClass = "android.content.Intent";
		//load bundle.java
		SootClass bKlass = Scene.v().loadClassAndSupport(bundleClass);
		SootClass iKlass = Scene.v().loadClassAndSupport(intentClass);			
		Body body;
		Chain<Unit> units;
		Iterator<Unit> uit;
		Stmt stmt;
		
		//for all put_XXX methods
		for (SootMethod instumentedMeth : bKlass.getMethods()) {
			if (instumentedMeth.getName().contains("put_")) {
				
				body = instumentedMeth.retrieveActiveBody();
				units = body.getUnits();
				uit = units.snapshotIterator();
				while(uit.hasNext()) {
					stmt = (Stmt) uit.next();	
					if(stmt.containsFieldRef()) {
						if (stmt.getUseBoxes().size() < 2) continue;
						ValueBox extrasBox = stmt.getUseBoxes().get(0);
						ValueBox argBox = stmt.getUseBoxes().get(1);
						
						for (SootField instumentedField : bKlass.getFields()) {
				            if (instumentedField.getName().contains("unknown") &&
							    instumentedField.getType().equals(instumentedMeth.getParameterType(0))){
							
								FieldRef instFieldRef = Jimple.v()
									.newInstanceFieldRef((Local)extrasBox.getValue(), instumentedField.makeRef());

								AssignStmt assign = soot.jimple.Jimple.v()
									.newAssignStmt(instFieldRef, (Local)argBox.getValue());
								units.insertAfter(assign, stmt);
						        
				            }
						}	
						
					}
				}
						
			}

		}	
		
		//for all putXX methods in bundle.
		for (SootMethod orgMeth : bKlass.getMethods()) {
			if (orgMeth.toString().matches("^<android.os.Bundle: void put.*") && 
			    !orgMeth.getName().contains("put_") && !orgMeth.getName().contains("putAll")) {
				
				body = orgMeth.retrieveActiveBody();
				units = body.getUnits();
				uit = units.snapshotIterator();
				while(uit.hasNext()) {
					stmt = (Stmt) uit.next();	
					if(stmt.containsFieldRef()) {
						if (stmt.getUseBoxes().size() < 2) continue;
						ValueBox extrasBox = stmt.getUseBoxes().get(0);
						ValueBox argBox = stmt.getUseBoxes().get(1);
						
						for (SootField instumentedField : bKlass.getFields()) {
							
				            if (!instumentedField.getName().contains("unknown") && 
							    !instumentedField.getName().contains("EMPTY") &&
							    !instumentedField.getName().contains("CREATOR") &&
							     checkType(instumentedField.getType(), orgMeth.getParameterType(1)) ) {
	
								FieldRef instFieldRef = Jimple.v()
									.newInstanceFieldRef((Local)extrasBox.getValue(), instumentedField.makeRef());
								
								AssignStmt assign = soot.jimple.Jimple.v()
									.newAssignStmt(instFieldRef, (Local)argBox.getValue());
								units.insertAfter(assign, stmt);
						        
				            }
						}	
						
					}
				}
				
			}
		}	
			
		//for all putXX methods in intent.
		for (SootMethod orgIntentMeth : iKlass.getMethods()) {
			if (orgIntentMeth.toString().matches("^<android.content.Intent: android.content.Intent put.*") && 
			     (orgIntentMeth.getParameterCount() > 1) ) {
					 
				body = orgIntentMeth.retrieveActiveBody();
				units = body.getUnits();
				uit = units.snapshotIterator();
				while(uit.hasNext()) {
					stmt = (Stmt) uit.next();	
					if(stmt.containsFieldRef()) {
						if (stmt.getUseBoxes().size() < 2) continue;
						ValueBox extrasBox = stmt.getUseBoxes().get(0);
						ValueBox argBox = stmt.getUseBoxes().get(1);
						
						for (SootField instumentedField : bKlass.getFields()) {
							
				            if (!instumentedField.getName().contains("unknown") && 
							    !instumentedField.getName().contains("EMPTY") &&
							    !instumentedField.getName().contains("CREATOR") &&
							     checkType(instumentedField.getType(), orgIntentMeth.getParameterType(1)) ) {
	
								FieldRef instFieldRef = Jimple.v()
									.newInstanceFieldRef((Local)extrasBox.getValue(), instumentedField.makeRef());
								
								AssignStmt assign = soot.jimple.Jimple.v()
									.newAssignStmt(instFieldRef, (Local)argBox.getValue());
								units.insertAfter(assign, stmt);
						        
				            }
						}	
						
					}
				}
				
			}
		}	
		
		//generate statistics.
        if(needStat) genStatistics();
	}
	
	/** - total number of bundle operations (get* and put*)
	  * - in how many of those cases we can find the keys
	  * - reduction in number of intra-app flows as compared to the case of
	  * icdf=off. To compute, intra-app flows ignore flows that STAMP reports
	  * to and from Bundle/Intent/
	  **/
	private static void genStatistics() {
		System.out.println("ICDF Stat: " + "Total bundle operations: " 
			+ bundleOperCnt + " Find:" + succBunOperCnt);
	}
	
	private static boolean checkType(Type t1, Type t2) {
		String t1Name = t1.toString();
		String t2Name = t2.toString();	
		String[] primitiveArray = {"int", "double", "byte", "short", "long", "boolean", "char"};
        ArrayList<String> list = new ArrayList<String>();
        for( int i=0; i < primitiveArray.length; i++){
            list.add(primitiveArray[i]);
        }
	
		if (list.contains(t2Name)) return t1Name.equals(t2Name);
		else return t1Name.equals("java.lang.Object");
		
	}

	
	/* Output the related information of the register which can't be handled by intro-proc reaching def. */
	private void reportUnknownRegister(Stmt stmt, Value v) {
		if (!stmt.containsInvokeExpr()) return;
		System.out.println("ERROR: Fails to find arg: " + v + " in Class: " 
			+ this.rootClass + " | Method: " + stmt.getInvokeExpr().getMethod() + " | Stmt: " + stmt );	
		// System.out.println("ERROR:Current class: " + this.rootClass + " || Statement: " + stmt + 
		// 	 "|| reachingDef: " + stmt.getTags());
	}
    
    private void _tmp_reportUnknownRegisterDynInfo(Stmt stmt, Value v, int argNum) {
        reportUnknownRegister(stmt, v);
        List<ParamInfo> dynvals = queryArgumentValues(this.currentMethod, stmt, argNum);
        System.out.println("Method: " + this.currentMethod + "\tArg: #" + argNum);
        System.out.print("Dynamic Values: [ ");
        for(ParamInfo p : dynvals) System.out.print(p.toString() + ", ");
        System.out.println("]");
    }
    
}
