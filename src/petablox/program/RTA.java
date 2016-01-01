package petablox.program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.lang.NoClassDefFoundError;
import java.util.Iterator;

import soot.RefType;
import soot.Scene;
import soot.RefLikeType;
import soot.ArrayType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import petablox.program.reflect.DynamicReflectResolver;
import petablox.program.reflect.ExtReflectResolver;
import petablox.program.reflect.StaticReflectResolver;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.util.IndexSet;
import petablox.util.Timer;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;
import petablox.util.soot.StubMethodSupport;
import petablox.util.tuple.object.Pair;

/**
 * Rapid Type Analysis (RTA) based scope builder.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Omer Tripp (omertripp@post.tau.ac.il)
 */
public class RTA implements ScopeBuilder {
    private static final String MAIN_CLASS_NOT_DEFINED =
        "ERROR: RTA: Property chord.main.class must be set to specify the main class of program to be analyzed.";
    private static final String MAIN_METHOD_NOT_FOUND =
        "ERROR: RTA: Could not find main class '%s' or main method in that class.";
    private static final String METHOD_NOT_FOUND_IN_SUBTYPE =
        "WARN: RTA: Expected instance method %s in class %s implementing/extending interface/class %s.";
    private static final String METHOD_BODY_NOT_FOUND =
    		"WARN: RTA: Body of method %s in class %s could not be found";

    public static final boolean DEBUG = false;

    private final String reflectKind; // [none|static|static_cast|dynamic|external]

    /////////////////////////

    /*
     * Data structures used only if reflectKind == dynamic
     */

    private List<Pair<String, List<String>>> dynamicResolvedClsForNameSites;
    private List<Pair<String, List<String>>> dynamicResolvedObjNewInstSites;
    private List<Pair<String, List<String>>> dynamicResolvedConNewInstSites;
    private List<Pair<String, List<String>>> dynamicResolvedAryNewInstSites;

    /////////////////////////

    /*
     * Data structures used only if reflectKind == static
     */

    // Intra-procedural analysis for inferring the class loaded by calls to
    // {@code Class.forName(s)} and the class of objects allocated by calls to
    // {@code v.newInstance()}.  The analysis achieves this by
    // intra-procedurally tracking the flow of string constants to {@code s}
    // and the flow of class constants to {@code v}.
    private StaticReflectResolver staticReflectResolver;

    // Methods in which forName/newInstance sites have already been analyzed
    private Set<SootMethod> staticReflectResolved;

    //constructors invoked implicitly via reflection
    private LinkedHashSet<SootMethod> reflectiveCtors;

    /////////////////////////

    /*
     * Transient data structures reset after every iteration.
     */

    // Set of all classes whose clinits and super class/interface clinits
    // have been processed so far in current interation; this set is kept to
    // avoid repeatedly visiting super classes/interfaces within an
    // iteration (which incurs a huge runtime penalty) only to find that all
    // their clinits have already been processed in that iteration.
    private Set<SootClass> classesVisitedForClinit;

    // Set of all methods deemed reachable so far in current iteration.
    private IndexSet<SootMethod> methods;

    /////////////////////////

    /*
     * Persistent data structures not reset after iterations.
     */

    private Reflect reflect;

    // set of all classes deemed reachable so far
    private IndexSet<RefLikeType> classes;

    // set of all (concrete) classes deemed instantiated so far either
    // by a reachable new/newarray statement or due to reflection
    private IndexSet<RefLikeType> reachableAllocClasses;

    // worklist for methods seen so far in current iteration but whose
    // CFGs haven't been processed yet
    private List<SootMethod> methodWorklist;

    // handle to the representation of class java.lang.Object
    private SootClass javaLangObject;

    // flag indicating that another iteration is needed; it is set if
    // set reachableAllocClasses grows in the current iteration
    private boolean repeat = true;
    
    public RTA(String reflectKind) {
        this.reflectKind = reflectKind;
    }

    /**
     * @see petablox.program.ScopeBuilder#getMethods()
     */
    @Override
    public IndexSet<SootMethod> getMethods() {
        if (methods == null) build();
        return methods;
    }

    /**
     * @see petablox.program.ScopeBuilder#getClasses()
     */
    @Override
    public IndexSet<RefLikeType> getClasses() {
        if (methods == null) build();
        return classes;
    }
    
    /**
     * @see petablox.program.ScopeBuilder#getReflect()
     */
    @Override
    public Reflect getReflect() {
        if (reflect == null) build();
        return reflect;
    }

    protected void build() {
        classes = new IndexSet<RefLikeType>();
        classesVisitedForClinit = new HashSet<SootClass>();
        reachableAllocClasses = new IndexSet<RefLikeType>();
        methods = new IndexSet<SootMethod>();
        methodWorklist = new ArrayList<SootMethod>();
    
        if (Config.verbose >= 1) System.out.println("ENTER: RTA");
        Timer timer = new Timer();
        timer.init();
        if (reflectKind.equals("static")) {
            /*staticReflectResolver = new StaticReflectResolver();
            staticReflectResolved = new HashSet<SootMethod>();
            reflectiveCtors = new LinkedHashSet<SootMethod>();*/
        	System.out.println("Unsupported option!");
        } else if (reflectKind.equals("static_cast")) {
            /*staticReflectResolved = new HashSet<SootMethod>();
            reflectiveCtors = new LinkedHashSet<SootMethod>();
            staticReflectResolver = new CastBasedStaticReflect(reachableAllocClasses, staticReflectResolved);*/
        	System.out.println("Unsupported option!");
        } else if (reflectKind.equals("dynamic")) {
            DynamicReflectResolver dynamicReflectResolver = new DynamicReflectResolver();
            dynamicReflectResolver.run();
            dynamicResolvedClsForNameSites = dynamicReflectResolver.getResolvedClsForNameSites();
            dynamicResolvedObjNewInstSites = dynamicReflectResolver.getResolvedObjNewInstSites();
            dynamicResolvedConNewInstSites = dynamicReflectResolver.getResolvedConNewInstSites();
            dynamicResolvedAryNewInstSites = dynamicReflectResolver.getResolvedAryNewInstSites();
            reflectiveCtors = new LinkedHashSet<SootMethod>();
        } else if (reflectKind.equals("external")) {
        	ExtReflectResolver extReflectResolver = new ExtReflectResolver();
        	extReflectResolver.run();
        	Options.v().set_soot_classpath(Scene.v().defaultClassPath()+File.pathSeparator+
        			Config.userClassPathName);
        	
        }
        System.out.println("Soot class path:"+Scene.v().getSootClassPath());
        reflect = new Reflect();   
        Scene.v().addBasicClass("java.lang.Object", SootClass.BODIES);
        Scene.v().loadBasicClasses();
        if(!Scene.v().containsClass("java.lang.Object")){
        	System.out.println("FATAL: Could not load java.lang.Ojbect");
        	throw new RuntimeException();
        }
        javaLangObject = Scene.v().getSootClass("java.lang.Object");
        String mainClassName = Config.mainClassName;
        if (mainClassName == null)
            Messages.fatal(MAIN_CLASS_NOT_DEFINED);
        Scene.v().addBasicClass(mainClassName, SootClass.BODIES);
        Scene.v().loadBasicClasses();
        if(!Scene.v().containsClass(mainClassName)){
        	Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
        }
        SootClass mainClass = Scene.v().getSootClass(mainClassName);
        mainClass.setApplicationClass();
        prepareClass(mainClass.getType());
        SootMethod mainMethod = mainClass.getMethod("void main(java.lang.String[])");
        if (mainMethod == null)
            Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
        
        prepAdditionalEntrypoints(); //called for subclasses
        
        for (int i = 0; repeat; i++) {
            if (Config.verbose >= 1) System.out.println("Iteration: " + i);
            repeat = false;
            classesVisitedForClinit.clear();
            methods.clear();
            visitClinits(mainClass);
            visitMethod(mainMethod);

            visitAdditionalEntrypoints(); //called for subclasses
            
            if (reflectiveCtors != null)
                for (SootMethod m: reflectiveCtors) {
                    visitMethod(m);
                }
            
            while (!methodWorklist.isEmpty()) {
                int n = methodWorklist.size();
                SootMethod m = methodWorklist.remove(n - 1);
                if (DEBUG) System.out.println("Processing CFG of " + m);
                processMethod(m);
            }

            if (staticReflectResolver != null) {
                staticReflectResolver.startedNewIter();
            }
        }

        timer.done();
        if (Config.verbose >= 1) {
            System.out.println("LEAVE: RTA");
            System.out.println("Time: " + timer.getInclusiveTimeStr());
        }
        System.out.println("AK: RTA Number of Classes"+classes.size());
        if(Config.verbose >=3){
        	Iterator<RefLikeType> itr = classes.iterator();
        	while(itr.hasNext()){
        		RefLikeType rlt = itr.next();
        		System.out.println("RTA:Class:"+rlt.toString());
        	}
        }
        staticReflectResolver = null; // no longer in use; stop referencing it
    }

    /**
     * Invoked by RTA before starting iterations. A hook so subclasses can
     * add additional things to visit.
     * 
     * Note that this is invoked AFTER the hosted JVM is set up.
     */
    protected void prepAdditionalEntrypoints() {
        
    }

    /**
     * Invoked by RTA each iteration. A hook so subclasses can
     * add additional things to visit.
     */
    protected void visitAdditionalEntrypoints() {
        
    }

    private boolean isExcluded(SootMethod m) {
    	SootClass c = m.getDeclaringClass();
    	if (Config.isExcludedFromScope(c.getName()))
    		return true;
    	return false;
    }
    /**
     * Called whenever RTA sees a method.
     * Adds to worklist if it hasn't previously been seen on this iteration.
     * @param m
     */
    protected void visitMethod(SootMethod m) {
    	SootMethod s = null;
    	if(StubMethodSupport.methodToStub.containsKey(m) || StubMethodSupport.methodToStub.containsValue(m)){
    		s = m;
    	}else{
	    	if(StubMethodSupport.toReplace(m)){
	    		s = StubMethodSupport.getStub(m);
	    	}else if(isExcluded(m)){
	    		s = StubMethodSupport.emptyStub(m);
	    	}else{
	    		s = m;
	    	}
    	}
        if (methods.add(s)) {
            if (DEBUG) System.out.println("\tAdding method: " + s);
            if (!s.isAbstract()) {
                methodWorklist.add(s);
            }
        }
    }

    private static boolean isClassDefined(Unit u, RefType r) {  
        try {
        	SootClass c = r.getSootClass();
        	if(!c.isInScene()){
        		Scene.v().addBasicClass(c.getName(), SootClass.BODIES);
        		Scene.v().loadBasicClasses();
        	}
            return true;
        } catch(NoClassDefFoundError e) {
        	int line = ((LineNumberTag)u.getTag("LineNumberTag")).getLineNumber();
            String qpos = SootUtilities.getMethod(u).getDeclaringClass() + " " +  SootUtilities.getMethod(u) + ":" + line;       
            Messages.log(qpos + " references class "+ r + " via reflection. Class not found in classpath");
            return false;
        }
    }

    /*
     * It can happen that we see Class.forName("something not in classpath").
     * Should handle this gracefully.
     */

    private void processResolvedClsForNameSite( Unit u, RefType r) { //Quad q, RefType r) {
        if (isClassDefined(u, r)) {
            reflect.addResolvedClsForNameSite(u, r);
            visitClass(r);
        }
    }

    private void processResolvedObjNewInstSite(Unit u, RefType r) {
        if (!isClassDefined(u, r))
            return;

        reflect.addResolvedObjNewInstSite(u, r);
        visitClass(r);
        if (reachableAllocClasses.add(r) ||
                (staticReflectResolver != null && staticReflectResolver.needNewIter()))
            repeat = true;
        //if (r instanceof jq_Class) {                              
        SootClass c = r.getSootClass();
           
        //two cases: call was Constructor.newInstance or call was Class.newInstance
        //Static reflection analysis folds these together, so we pull them apart here
        String cName = ((InvokeStmt)u).getInvokeExpr().getMethod().getDeclaringClass().getName();
        if(cName.equals("java.lang.reflect.Constructor")) {
            processResolvedConNewInstSite(u, r);
        } else {
            SootMethod n = c.getMethod("void <init>()");
            if (n != null) {
                visitMethod(n);
                reflectiveCtors.add(n);
            }
        }
    }

    private void processResolvedAryNewInstSite(Unit u, RefType r) {
        if (!isClassDefined(u, r))
            return;
        reflect.addResolvedAryNewInstSite(u, r);
        visitClass(r);
        if (reachableAllocClasses.add(r))
            repeat = true;
    }

    private void processResolvedConNewInstSite(Unit u, RefType r) {
        if (!isClassDefined(u, r))
            return;
        reflect.addResolvedConNewInstSite(u, r);
        visitClass(r);
        if (reachableAllocClasses.add(r))
            repeat = true;
        SootClass c = r.getSootClass();
        List <SootMethod> meths = c.getMethods();  //getDeclaredInstanceMethods();                        
        // this is imprecise in that we are visiting all constrs instead of the called one
        // this is also unsound because we are not visiting constrs in superclasses
        for (SootMethod m : meths) {
        	if(!m.isStatic()){
        		if (m.getName().toString().equals("<init>")) {            
                    visitMethod(m);
                    reflectiveCtors.add(m);
                }
        	}
        }
    }

    private void processMethod(SootMethod m) {
        if (staticReflectResolved != null && staticReflectResolved.add(m)) {
            /*staticReflectResolver.run(m);
            Set<Pair<Unit, RefType>> resolvedClsForNameSites =
                staticReflectResolver.getResolvedClsForNameSites();
            Set<Pair<Unit, RefType>> resolvedObjNewInstSites =
                staticReflectResolver.getResolvedObjNewInstSites();
            for (Pair<Unit, RefType> p : resolvedClsForNameSites)
                processResolvedClsForNameSite(p.val0, p.val1);
            for (Pair<Unit, RefType> p : resolvedObjNewInstSites)
                processResolvedObjNewInstSite(p.val0, p.val1);*/
        }
        try{
        	if(!m.hasActiveBody())
        		m.retrieveActiveBody(); // TODO: Hack to ignore methods which have no source
        }catch(Exception e){
        	Messages.log(METHOD_BODY_NOT_FOUND, m.getSubSignature(),m.getDeclaringClass().getName());
        	return;
        }
        Iterator<Local> itr = m.getActiveBody().getLocals().iterator();
        while(itr.hasNext()){
        	Local l = itr.next();
        	if(l.getType() instanceof RefLikeType){
        		visitClass((RefLikeType)l.getType());
        	}
        }
        CFG cfg = SootUtilities.getCFG(m);
        for ( Block bb : cfg.reversePostOrder()){
        	Iterator<Unit> uit=bb.iterator();
        	while(uit.hasNext()){
        		Unit u=uit.next();
        		if (DEBUG) System.out.println("Unit: " + u);
                if(SootUtilities.isInvoke(u)){
                	if (SootUtilities.isVirtualInvoke(u) || SootUtilities.isInterfaceInvoke(u)
                			|| SootUtilities.isSpecialInvoke(u))             
                		processVirtualInvk(m, u);
                	else
                		processStaticInvk(m, u);
                }
                else if(u instanceof JAssignStmt){
                	JAssignStmt as = (JAssignStmt)u;
                    if (SootUtilities.isStaticGet(as)) {
                        SootField f = as.getFieldRef().getField();
                        SootClass c = f.getDeclaringClass();
                        visitClass(c.getType());
                    } else if (SootUtilities.isStaticPut(as)) {
                    	SootField f = as.getFieldRef().getField();
                        SootClass c = f.getDeclaringClass();
                        visitClass(c.getType());
                    } else if (SootUtilities.isNewStmt(as)) {                         
                    	RefType ref=((NewExpr)as.rightBox.getValue()).getBaseType();
                        visitClass(ref);
                        if (reachableAllocClasses.add(ref))
                            repeat = true;
                    } else if (SootUtilities.isNewArrayStmt(as)) {
                    	ArrayType arr = (ArrayType)((NewArrayExpr)as.rightBox.getValue()).getType();
            			visitClass(arr);
                        if (reachableAllocClasses.add(arr))
                            repeat = true;
    /*
                    } else if (op instanceof Move) {
                        Operand ro = Move.getSrc(q);
                        if (ro instanceof AConstOperand) {
                            Object c = ((AConstOperand) ro).getValue();
                            if (c instanceof Class) {
                                String s = ((Class) c).getName();
                                // s is in encoded form only if it is an array type
                                RefType d = (RefType) jq_Type.parseType(s);
                                if (d != null)
                                    visitClass(d);
                            }
                        }
    */
                    }
                }
        	}
        }
    }

    // does qStr (in format bci!mName:mDesc@cName) correspond to unit u in method m?
    private static boolean matches(String uStr, SootMethod m, Unit u) {                           
        MethodElem me = MethodElem.parse(uStr);
        return me.mName.equals(m.getName().toString()) &&
        	m.getBytecodeSignature().contains(me.mDesc)&&
            //me.mDesc.equals(m.getDesc().toString()) &&                                                
            me.cName.equals(m.getDeclaringClass().getName()) &&
            SootUtilities.getBCI(u) == me.offset;                                                                  
    }

    private SootMethod getMethodItr(SootClass c,String subsign){
        SootMethod ret = null;
        while(true){
            try{
                ret= c.getMethod(subsign);
                break;
            }catch(Exception e){
                if(!c.hasSuperclass()){
                    System.out.println("WARN: RelCHA: Method "+subsign+" not found");
                    break;
                }else{
                    c = c.getSuperclass();
                }
            }
        }
        return ret;
    }

    private void processVirtualInvk(SootMethod m, Unit u) {
    	InvokeExpr invExpr = SootUtilities.getInvokeExpr(u);
    	if(invExpr instanceof AbstractInstanceInvokeExpr){
    		Value v = ((AbstractInstanceInvokeExpr)invExpr).getBase();
    		RefLikeType vrt = (RefLikeType)v.getType();
    		visitClass(vrt);
    	}
    	SootMethodRef nr = invExpr.getMethodRef();
        SootClass c = nr.declaringClass();
        visitClass(c.getType());
        SootMethod n = invExpr.getMethod();
        visitMethod(n);
        visitExceptions(n);
        String cName = c.getName();
        if (cName.equals("java.lang.Class")) {
            if (dynamicResolvedObjNewInstSites != null &&
            		n.getSubSignature().equals("java.lang.Object newInstance()")){            
                for (Pair<String, List<String>> p : dynamicResolvedObjNewInstSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                            //RefType r = (RefType) jq_Type.parseType(s);
                        	SootClass r = Scene.v().getSootClass(s);
                            if (r != null)
                                processResolvedObjNewInstSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        } else if (cName.equals("java.lang.reflect.Constructor")) {
            if (dynamicResolvedConNewInstSites != null &&
            		n.getSubSignature().equals("java.lang.Object newInstance(java.lang.Object)")) {
                for (Pair<String, List<String>> p : dynamicResolvedConNewInstSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	//RefType r = (RefType) jq_Type.parseType(s);
                        	SootClass r = Scene.v().getSootClass(s);
                            if (r != null)
                                processResolvedConNewInstSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        }

 
        boolean isInterface = c.isInterface();
        for (RefLikeType r : reachableAllocClasses) {
            if (r instanceof ArrayType)                                         
                continue;
            SootClass d = ((RefType)r).getSootClass();
            assert (!d.isInterface());
            assert (!d.isAbstract());
            boolean matches = isInterface ? SootUtilities.implementsInterface(d,c) : SootUtilities.extendsClass(d,c);
            if (matches) {
            	try{
            		SootMethod m2 = this.getMethodItr(d,n.getSubSignature());
                    if(m2 == null)
                        throw new Exception();
            		visitMethod(m2);
            	}catch(Exception e){
            		// TODO : Verify, Soot shows the method only in the class
            		// where it is defined and not in sub-classes
            		Messages.log(METHOD_NOT_FOUND_IN_SUBTYPE,
                            n.getSubSignature(), d.getName(), c.getName());
            	}
            }
        }
    }

    private void processStaticInvk(SootMethod m, Unit u) {
    	InvokeExpr invExpr = SootUtilities.getInvokeExpr(u);
    	if(invExpr instanceof DynamicInvokeExpr) return; //TODO: May need to handle this in the future
    	SootMethod n = invExpr.getMethod();
        SootClass c = n.getDeclaringClass();
        visitClass(c.getType());
        visitMethod(n);
        visitExceptions(n);
        String cName = c.getName();
        if (cName.equals("java.lang.Class")) { 
            if (dynamicResolvedClsForNameSites != null &&
            		n.getSubSignature().equals("java.lang.Class forName(java.lang.String)")) {
                for (Pair<String, List<String>> p : dynamicResolvedClsForNameSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	SootClass r = Scene.v().getSootClass(s);
                            if (r != null)
                                processResolvedClsForNameSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        } else if (cName.equals("java.lang.reflect.Array")) {
            if (dynamicResolvedAryNewInstSites != null &&
            		n.getSubSignature().equals("java.lang.Object newInstance(java.lang.Class,I)")) {
               for (Pair<String, List<String>> p : dynamicResolvedAryNewInstSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	SootClass r = Scene.v().getSootClass(s);
                            if (r != null)
                                processResolvedAryNewInstSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        }
    }
    private SootClass loadClass(String cName){
    	SootClass c = null;
    	if(Scene.v().containsClass(cName)){
    		c = Scene.v().getSootClass(cName);
    	}else{
    		Scene.v().addBasicClass(cName,SootClass.BODIES);
    		Scene.v().loadBasicClasses();
    		if(!Scene.v().containsClass(cName)){
    			System.out.println("WARN: RTA could not load class "+cName);
    			return c;
    		}else{
    			c = Scene.v().getSootClass(cName);
    		}
    	}
    	c.setApplicationClass();
    	return c;
    }
    private void prepareClass(RefLikeType r) {
        if (classes.add(r)) {                             
            if (DEBUG) System.out.println("\tAdding class: " + r);
            if(r instanceof ArrayType) return;
            SootClass c = loadClass(((RefType)r).getSootClass().getName());
            if(c==null)
            	return;
            if(c.hasSuperclass()){
	            SootClass d = c.getSuperclass();
	            if (d == null)
	                assert (c == javaLangObject);
	            else
	                prepareClass(d.getType());
            }
            for (SootClass i : c.getInterfaces())
                prepareClass(i.getType());
        }
    }
    
    protected void visitExceptions(SootMethod m){
    	List<SootClass> exceptions = m.getExceptions();
    	for(SootClass c: exceptions){
    		visitClass(c.getType());
    	}
    }
    
    protected void visitClass(RefLikeType r) {
        prepareClass(r);
        if (r instanceof ArrayType) return;
        SootClass c = ((RefType)r).getSootClass();
        visitClinits(c);
    }

    protected void visitClinits(SootClass c) {
        if (classesVisitedForClinit.add(c)) {
        	try{
	        	SootMethod m=c.getMethod("void <clinit>()");
	            // m is null for classes without class initializer method
	            if (m != null)
	                visitMethod(m);
        	}catch(Exception e){ } // TODO: Check that this is equivalent. Soot doesn't return null
        	if(c.hasSuperclass()){
	            SootClass d = c.getSuperclass();
	            visitClinits(d);
        	}
            for (SootClass i : c.getInterfaces())
                visitClinits(i);
        }
    }
}
