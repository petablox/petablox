package petablox.util.soot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import petablox.project.ClassicProject;
import petablox.project.Config;
import petablox.project.analyses.ProgramDom;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.object.Pair;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.SwitchStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.shimple.PhiExpr;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.toolkits.graph.Block;
import soot.util.Chain;

public class SootUtilities {
    private static HashMap <Unit, SootMethod> PMMap = null;
    private static HashMap <SootMethod,ICFG> methodToCFG
        = new HashMap<SootMethod,ICFG>();
    private static HashMap <Unit, Block> unitToBlockMap = null;
    private static HashMap<SootClass, LinkedList<SootMethod>> virtualMethodCache
        = new HashMap<SootClass, LinkedList<SootMethod>>();
    private static HashMap<SootClass, HashSet<SootClass>> classCache
        = new HashMap<SootClass, HashSet<SootClass>>();
    private static HashMap<SootClass, HashSet<SootClass>> interfaceCache
        = new HashMap<SootClass, HashSet<SootClass>>();
    public static Hierarchy h = null;

    public static SootClass loadClass(String s){
        SootClass c = Scene.v().getSootClass(s);
        if(c == null)
            c = Scene.v().loadClass(s, SootClass.BODIES);
        if(c.isPhantomClass())
            return null;
        else{
            c.setApplicationClass();
            return c;
        }
    }

    public static ICFG getCFG(SootMethod m){
        if(methodToCFG.containsKey(m)){
            return methodToCFG.get(m);
        }else{
            SSAUtilities.process(m);
            ICFG cfg;
            if (Config.cfgKind.equals("exception"))
                cfg = new ECFG(m);
            else 
                cfg = new BCFG(m);
            methodToCFG.put(m, cfg);
            makeUnitToBlockMap(cfg);
            return cfg;
        }
    }
    public static SootMethod getMethod (Unit u) {
        SootMethod m = null;
        if (PMMap == null) {
            ProgramDom d = (ProgramDom)ClassicProject.g().getTrgt("P");
            if(!ClassicProject.g().isTrgtDone(d)){
                ClassicProject.g().getTaskProducingTrgt(d).run();
            }
            ProgramRel pmRel = (ProgramRel) ClassicProject.g().getTrgt("PM");
            if(!ClassicProject.g().isTrgtDone(pmRel)){
                ClassicProject.g().getTaskProducingTrgt(pmRel).run();
            }
            pmRel.load();
            PMMap = new HashMap <Unit, SootMethod>();
            Iterable<Pair<Unit, SootMethod>> tuples = pmRel.getAry2ValTuples();
            for (Pair<Unit, SootMethod> t : tuples){
                PMMap.put(t.val0, t.val1);
            }
            pmRel.close();
            m = PMMap.get(u);
        } else {
            m = PMMap.get(u);
        }
        if(m == null){
            System.err.println("Method for Unit not found!!");
            StackTraceElement[] ste = Thread.currentThread().getStackTrace();
            for(int i=0;i<ste.length;i++){
                System.out.println(ste[i]);
            }
        }
        return m;
    }

    public static boolean isStaticGet(JAssignStmt a){
        if(a.containsFieldRef()) {
            FieldRef fr = a.getFieldRef();
            ValueBox vb = a.rightBox;
            Value v = vb.getValue();
            if(fr.getField().isStatic()) {
                if(vb.getValue().toString().equals(fr.toString())){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isStaticPut(JAssignStmt a){
        if (a.containsFieldRef()) {
            FieldRef fr = a.getFieldRef();
            ValueBox vb = a.leftBox;
            Value v = vb.getValue();
            if (fr.getField().isStatic())
                if (vb.getValue().toString().equals(fr.toString()))
                    return true;
        }
        return false;
    }

    public static boolean isLoadInst(JAssignStmt a){
        Value right = a.rightBox.getValue();
        if(right instanceof JArrayRef)
            return true;
        return false;
    }

    public static boolean isStoreInst(JAssignStmt a){
        Value left = a.leftBox.getValue();
        if(left instanceof JArrayRef)
            return true;
        return false;
    }

    public static boolean isFieldLoad(JAssignStmt a){
        Value right = a.rightBox.getValue();
        if(a.containsFieldRef()){
            FieldRef fr = a.getFieldRef();
            if(right.toString().equals(fr.toString()))
                return true;
        }
        return false;
    }

    public static boolean isFieldStore(JAssignStmt a){
        Value left = a.leftBox.getValue();
        if(a.containsFieldRef()){
            FieldRef fr = a.getFieldRef();
            if(left.toString().equals(fr.toString()))
                return true;
        }
        return false;
    }

    public static boolean isAssign (Unit q) {
        if (q instanceof JAssignStmt)
            if ((((JAssignStmt)q).rightBox.getValue()) instanceof InvokeExpr)
                return false;
            else return true;
        else return false;
    }

    public static boolean isInvoke(Unit q){
        if (q instanceof JInvokeStmt)
            return true;
        else if (q instanceof JAssignStmt)
            if ((((JAssignStmt)q).rightBox.getValue()) instanceof InvokeExpr)
                return true;
        return false;
    }

    public static boolean isVirtualInvoke(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        return ie != null && ie instanceof JVirtualInvokeExpr;
    }

    public static boolean isInterfaceInvoke(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        return ie != null && ie instanceof JInterfaceInvokeExpr;
    }

    public static boolean isInstanceInvoke(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        if (ie != null && ie instanceof InstanceInvokeExpr)
            return true;
        return false;
    }

    public static boolean isStaticInvoke(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        if (ie != null && ie instanceof StaticInvokeExpr)
            return true;
        return false;
    }

    public static InvokeExpr getInvokeExpr(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        return ie;
    }

    public static Value getInstanceInvkBase(Unit q){
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        InvokeExpr ie;
        if (q instanceof JInvokeStmt)
            ie = ((JInvokeStmt)q).getInvokeExpr();
        else if (q instanceof JAssignStmt)
            ie = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue()));
        else
            ie = null;
        if (ie != null && ie instanceof InstanceInvokeExpr)
            return ((InstanceInvokeExpr)ie).getBase();
        return null;
    }

    public static List<Value> getInvokeArgs (Unit q) {
        assert (q instanceof JInvokeStmt || q instanceof JAssignStmt);
        List<Value> args;
        if (q instanceof JInvokeStmt)
            args = ((JInvokeStmt)q).getInvokeExpr().getArgs();
        else if (q instanceof JAssignStmt)
            args = ((InvokeExpr)(((JAssignStmt)q).rightBox.getValue())).getArgs();
        else
            args = null;
        return args;
    }

    public static boolean isNewStmt(JAssignStmt a){
        Value right=a.rightBox.getValue();
        if(right instanceof JNewExpr)
            return true;
        return false;
    }

    public static boolean isNewArrayStmt(JAssignStmt a){
        Value right=a.rightBox.getValue();
        if(right instanceof JNewArrayExpr)
            return true;
        return false;
    }

    public static boolean isNewMultiArrayStmt(JAssignStmt a){
        Value right=a.rightBox.getValue();
        if(right instanceof JNewMultiArrayExpr)
            return true;
        return false;
    }

    public static void cacheHierarchy(SootClass c, SootClass sup) {
        LinkedList<SootMethod> vmList = new LinkedList<SootMethod>();
        for (SootMethod m : c.getMethods()) {
            if (!m.isPrivate() && !m.isConstructor())
                vmList.addLast(m);
        }
        HashSet<SootClass> cSet = null;
        HashSet<SootClass> iSet = null;
        if (sup != null) {  /* normal classes */
            for (SootMethod m : virtualMethodCache.get(sup))
                if (c.getMethodUnsafe(m.getSubSignature()) == null)
                   vmList.addLast(m);
            cSet = (HashSet<SootClass>)(classCache.get(sup).clone());
            iSet = (HashSet<SootClass>)(interfaceCache.get(sup).clone());
            cSet.add(sup);
        } else {            /* java.lang.Object */
            cSet = new HashSet<SootClass>();
            iSet = new HashSet<SootClass>();
        }
        for (SootClass i : c.getInterfaces()){
            /* c also implements j where i extends j */
            HashSet<SootClass> j = (HashSet<SootClass>) interfaceCache.get(i);
            if (j != null)
                iSet.addAll(j);
            iSet.add(i);
        }

        virtualMethodCache.put(c, vmList);
        classCache.put(c, cSet);
        interfaceCache.put(c, iSet);
    }

    public static SootMethod getVirtualMethod (SootClass c, SootMethod vm) {
        for (SootMethod m : virtualMethodCache.get(c)) {
            if (m.getName().equals(vm.getName())
                    && m.getReturnType().equals(vm.getReturnType())
                    && m.getParameterTypes().equals(vm.getParameterTypes()))
                return m;
        }
        System.out.println("WARN: RTA method not found "+vm.getName());
        return null;
    }

    public static boolean extendsClass(SootClass c, SootClass sup){
        HashSet<SootClass> sups = classCache.get(c);
        if (sups == null)   // when c is a phantom class
            return false;
        else
            return sups.contains(sup);
    }

    public static boolean implementsInterface(SootClass c, SootClass inter){
        HashSet<SootClass> inters = interfaceCache.get(c);
        if (inters == null) // when c is a phantom class
            return false;
        else
            return inters.contains(inter);
    }
 
    public static boolean isSubtypeOf(RefLikeType i, RefLikeType j){
        if(i instanceof ArrayType && j instanceof ArrayType){
            ArrayType ia = (ArrayType)i;
            ArrayType ja = (ArrayType)j;
            if(ia.numDimensions == ja.numDimensions){
                Type basei = ia.baseType;
                Type basej = ja.baseType;
                if(basei == basej)
                    return true;
                else if(basei instanceof RefType && basej instanceof RefType){
                    RefType baseir = (RefType)basei;
                    RefType basejr = (RefType)basej;
                    return isSubtypeOf(baseir.getSootClass(),basejr.getSootClass());
                }
            }else if(ia.numDimensions > ja.numDimensions) {
                Type basej = ja.baseType;
                if(basej instanceof RefType){
                    SootClass c = ((RefType)basej).getSootClass();
                    if(c.getName().equals("java.lang.Object"))
                        return true;
                }
                return false;
            }else{
                return false;
            }
        }else if(i instanceof ArrayType && j instanceof RefType){
            RefType jr = (RefType)j;
            String cName = jr.getSootClass().getName();
            return cName.equals("java.lang.Object") || cName.equals("java.lang.Cloneable")
                || cName.equals("java.io.Serializable");
        }else if(i instanceof RefType && j instanceof ArrayType){
            return false;
        }else if(i instanceof RefType && j instanceof RefType){
            return isSubtypeOf(((RefType)i).getSootClass(), ((RefType)j).getSootClass());
        }
        return false;
    }

    public static boolean isSubtypeOf(SootClass j, SootClass k) {
        if(k.getName().equals("java.lang.Object"))
            return true;
        if (j.getName().equals(k.getName()))
            return true;
        if(j.isInterface() && k.isInterface()){
            return h.isInterfaceSubinterfaceOf(j,k);
        }else if(j.isInterface() && !(k.isInterface()))
            return false;
        else if(!(j.isInterface()) && k.isInterface()){
            Iterator<SootClass> inters = j.getInterfaces().iterator();
            while(inters.hasNext()){
                SootClass c = inters.next();
                if(c.getName().equals(k.getName()))
                    return true;
                else{
                    boolean temp = false;
                    temp = h.isInterfaceSubinterfaceOf(c,k);
                    if(temp) return temp;
                }
            }
            if(j.hasSuperclass())
                return isSubtypeOf(j.getSuperclass(),k);
        }else{
            // Both j and k are concrete classes
            if(!j.hasSuperclass())
                return false;
            return SootUtilities.isSubtypeOf(j.getSuperclass(),k);
        }
        return false;
    }

    public static boolean isMoveInst(JAssignStmt a){
        Value left = a.leftBox.getValue();
        Value right = a.rightBox.getValue();
        if(left instanceof Local && right instanceof Local)
            return true;
        return false;
    }

    public static boolean isPhiInst(JAssignStmt a){
        Value right = a.rightBox.getValue();
        if(right instanceof PhiExpr)
            return true;
        return false;
    }

    public static boolean isBranch(Unit u){
        if(u instanceof IfStmt ||
                u instanceof GotoStmt ||
                u instanceof SwitchStmt ||
                u instanceof ThrowStmt ||
                u instanceof ReturnStmt ||
                u instanceof ReturnVoidStmt)
            return true;
        return false;
    }

    /*
     * Returns the local variables corresponding to the arguments of the method
     */
    public static Local[] getMethArgLocals(SootMethod m){
        int numLocals = m.getParameterCount();
        List<Local> regs;
        try{
            regs= m.getActiveBody().getParameterLocals();
            if(!m.isStatic()) {
                numLocals++; // Done to consider the "this" parameter passed
                regs.add(0,m.getActiveBody().getThisLocal());
            }
            Local[] locals = new Local[numLocals];
            for(int i=0;i<regs.size();i++){
                locals[i] = regs.get(i);
            }
            return locals;
        } catch (RuntimeException e) {
            System.out.println("Method body not found for method: "+m.getSignature());
        };
        return null;
    }

    /*
     * Returns the local variables of the method - arguments first, followed by temporaries
     */
    public static List<Local> getLocals(SootMethod m){
        Body b = m.getActiveBody();
        List<Local> regs = b.getParameterLocals();
        if(!m.isStatic())
            regs.add(0, b.getThisLocal());

        List<Local> temps = new ArrayList<Local>();
        Chain<Local> allLocals = b.getLocals();
        Iterator<Local> it = allLocals.iterator();
        while (it.hasNext()) {
            Local l = it.next();
            if (!regs.contains(l)) 
                temps.add(l);
        }
        regs.addAll(temps);
        return regs;
    }

    /*
     * 	Returns the local variable returned by method m 
     *	Returns null if method does not have a return statement or returns a constant
     */
    public static Local getReturnLocal(SootMethod m){
        try{
            Body body = m.retrieveActiveBody();
            for(Unit unit : body.getUnits()){
                Stmt s = (Stmt) unit;
                if(s instanceof ReturnStmt){
                    Immediate retOp = (Immediate) ((ReturnStmt) s).getOp();
                    if(retOp instanceof Local)
                        return (Local)retOp;
                }
            }
        }catch(RuntimeException e){
            System.out.println("Method body not found for method: "+m.getSignature());
        };
        return null;
    }

    public static int getBCI(Unit u){
        try{
            BytecodeOffsetTag bci = (BytecodeOffsetTag)u.getTag("BytecodeOffsetTag");
            return bci.getBytecodeOffset();
        }catch(Exception e){
            if (Config.verbose >= 2)
                System.out.println("WARN: SootUtilities cannot get BCI"+u);
        }
        return -1;
    }

    public static int getID(Unit u){                                  //TODO
        return 0;
    }

    public static String toByteLocStr(Unit u) {
        String x = Integer.toString(getBCI((Unit) u));
        return x + "!" + getMethod(u);
    }

    public static String toLocStr(Unit u) {                              //TODO 
        return "";
    }

    public static String toJavaLocStr(Unit u) {                              //TODO 
        return "";
    }

    public static String toVerboseStr(Unit u) {                              //TODO 
        //return toByteLocStr(u) + " (" + toJavaLocStr(u) + ") [" + printUnit(u) + "]";
        return "";
    }

    public static String toVerboseStrInst(Object i){
        String s = null;
        if(i instanceof Unit){
            s = SootUtilities.toVerboseStr(((Unit)i));
        }else if(i instanceof Block){
            Unit head = ((Block)i).getHead();
            s = SootUtilities.toVerboseStr(head);
        }
        return s;
    }

    public static List<Integer> getLineNumber(SootMethod m, Local v){      //TODO
        return null;
    }

    public static List<Integer> getLineNumber(SootMethod m, int bci){      //TODO
        return null;
    }

    public static int hashCode(Unit u) {									//TODO
        //if (DETERMINISTIC) return getID();
        //else return System.identityHashCode(this);
        return u.hashCode();
    }

    public static String printUnit(Unit u){
        SootMethod m = SootUtilities.getMethod(u);
        Body b=m.retrieveActiveBody();
        UnitPrinter up =new NormalUnitPrinter(b);
        u.toString(up);
        return up.toString();
    }

    public static List<String> getRegName(SootMethod m,Local v){              //TODO
        return null;
    }

    public static Block getBasicBlock(Unit u){
        Block b = unitToBlockMap.get(u);
        if (b == null) {
            SootMethod m = getMethod(u);
            ICFG cfg = getCFG(m);
            makeUnitToBlockMap(cfg);
        }
        return unitToBlockMap.get(u);
    }

    private static void makeUnitToBlockMap(ICFG cfg){
        if (unitToBlockMap == null) {
            unitToBlockMap = new HashMap<Unit, Block>();
        }
        for (Block b : cfg.reversePostOrder()){
            Iterator<Unit> uit = b.iterator();
            while(uit.hasNext()){
                Unit u = uit.next();
                unitToBlockMap.put(u,b);
            }
        }
    }

    public static Map<Unit,Integer> getBCMap(SootMethod m){					//TODO
        return null;
    }

    public static Map<String,List<Pair<String,String>>> parseVisibilityAnnotationTag(VisibilityAnnotationTag v){
        Map<String,List<Pair<String,String>>> result = new HashMap<String,List<Pair<String,String>>>();
        List<AnnotationTag> aTags = v.getAnnotations();
        for(AnnotationTag a : aTags){
            String annotationName = a.getType();
            List<Pair<String,String>> elems = null;
            if(!result.containsKey(annotationName)){
                elems = new ArrayList<Pair<String,String>>();
                result.put(annotationName, elems);
            }else
                elems = result.get(annotationName);
            for(AnnotationElem ae : a.getElems()){
                if(ae.getKind() == 's'){
                    AnnotationStringElem ase = (AnnotationStringElem)ae;
                    Pair<String,String> keyValue = new Pair<String,String>(ase.getName(),ase.getValue());
                    elems.add(keyValue);
                }
            }
        }
        return result;
    }
}
