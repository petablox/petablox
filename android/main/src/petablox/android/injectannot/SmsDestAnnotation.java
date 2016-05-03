package petablox.android.injectannot;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.AbstractJasminClass;

import java.io.File;
import soot.*;
import soot.jimple.*;
import soot.util.*;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.internal.VariableBox;
import soot.tagkit.LinkTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import java.util.*;
import petablox.android.analyses.StringLocalDefs;
import petablox.android.analyses.ReachingDefsAnalysis;

/**
  * attach param values to smsnumber.
  **/
public class SmsDestAnnotation extends AnnotationInjector.Visitor
{

    private final Map<String,SootMethod> srcLabelToLabelMethod = new HashMap();

    private SootClass klass;
    private int newLocalCount;

	protected void postVisit()
	{
	}

    public SmsDestAnnotation()
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
                String methodRefStr = ie.getMethodRef().toString();

                if (methodRefStr.contains("sendTextMessage")) {
                    UnitGraph g = new ExceptionalUnitGraph(body);
                    StringLocalDefs sld = new StringLocalDefs(g, new SimpleLiveLocals(g));
                    //ReachingDefsAnalysis.runReachingDef(body);

                    Map<Value, Set<String>> valueMap = sld.getDefsOfAt(null, stmt);
                    //System.out.println("Begin to query...." + ie.getArg(0) + valueMap.get(ie.getArg(0)));
                    if (valueMap.get(ie.getArg(0)) != null) {
                        //System.out.println("i get it...." + valueMap.get(ie.getArg(0)));
                        StringConstant arg = StringConstant.v(valueMap.get(ie.getArg(0)).toString());
                        Local newArg = insertLabelIfNecessary(arg, locals, units, stmt);
                        if(newArg != null){
                            ie.setArg(2, newArg);
                        }
                    }
                }

               if (methodRefStr.contains("sendMultipartTextMessage")) {
                    /*UnitGraph g = new ExceptionalUnitGraph(body);
                    StringLocalDefs sld = new StringLocalDefs(g, new SimpleLiveLocals(g));

                    Map<Value, Set<String>> valueMap = sld.getDefsOfAt(null, stmt);
                    if (valueMap.get(ie.getArg(0)) != null) {
                        StringConstant arg = StringConstant.v(valueMap.get(ie.getArg(0)).toString());
                        //FIXME: ugly.
                        Local newArg = insertMultiLabelIfNecessary(arg, locals, units, stmt);
                        if(newArg != null){
                            ie.setArg(2, newArg);
                        }
                    }*/
               }

               if (methodRefStr.contains("sendDataMessage")) {
                   //TODO
               }

            }
        }
    }
    private Local insertMultiLabelIfNecessary(StringConstant strConst, Chain<Local> locals, Chain<Unit> units, Unit currentStmt)
    {
        String str = strConst.value;

        SootMethod meth = getOrCreateMultiLabelMethodFor(str);
        Local temp = Jimple.v().newLocal("stamp$stamp$tmp"+newLocalCount++, 
        RefType.v("java.util.ArrayList"));
        locals.add(temp);
        Stmt stmt = (Stmt) currentStmt;
        InvokeExpr ie = stmt.getInvokeExpr();

        Stmt toInsert = Jimple.v().newAssignStmt(temp, Jimple.v().newStaticInvokeExpr(meth.makeRef(), ie.getArg(2)));
        units.insertBefore(toInsert, currentStmt);
        return temp;
    }


    private SootMethod getOrCreateMultiLabelMethodFor(String label)
    {
        SootMethod meth = srcLabelToLabelMethod.get(label);
        if(meth == null){
            RefType stringType = RefType.v("java.util.ArrayList");
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

            SootMethodRef mref = Scene.v().getMethod("<java.util.ArrayList: void <init>()>").makeRef();
            units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(ret, mref, param)));

            units.add(Jimple.v().newReturnStmt(ret));

            System.out.println("%%% "+meth.getSignature());
            label = "smsdest(" +label+ ")";
            writeAnnotation(methName+":(Ljava/lang/String;)Ljava/lang/String;@"+klass.getName(), "!"+label, "-1");
        }

        return meth;
    }


    private Local insertLabelIfNecessary(StringConstant strConst, Chain<Local> locals, Chain<Unit> units, Unit currentStmt)
    {
        String str = strConst.value;

        SootMethod meth = getOrCreateLabelMethodFor(str);
        Local temp = Jimple.v().newLocal("stamp$stamp$tmp"+newLocalCount++, 
        RefType.v("java.lang.String"));
        locals.add(temp);
        Stmt stmt = (Stmt) currentStmt;
        InvokeExpr ie = stmt.getInvokeExpr();

        Stmt toInsert = Jimple.v().newAssignStmt(temp, Jimple.v().newStaticInvokeExpr(meth.makeRef(), ie.getArg(2)));
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
            label = "smsdest(" +label+ ")";
            writeAnnotation(methName+":(Ljava/lang/String;)Ljava/lang/String;@"+klass.getName(), "!"+label, "-1");
            //writeAnnotation(methName+":(Ljava/lang/String;)Ljava/lang/String;@"+klass.getName(), "1", "!"+label);
        }

        return meth;
    }

}
