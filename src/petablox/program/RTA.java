package petablox.program;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

import soot.*;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.toolkits.graph.Block;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.AnnotationTag;
import petablox.program.reflect.DynamicReflectResolver;
import petablox.program.reflect.StaticReflectResolver;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.util.IndexSet;
import petablox.util.Timer;
import petablox.util.Utils;
import petablox.util.soot.ICFG;
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
        "ERROR: RTA: Property petablox.main.class must be set to specify the main class of program to be analyzed.";
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
    
    //set of classes containing methods that are likely entry points
    private HashSet<SootClass> entryClasses;
    
    //set of methods that are likely entry points
    private HashSet<SootMethod> entryMethods;

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

    @Override
    public HashSet<SootMethod> getEntryMethods() {
        if (methods == null) build();
        return entryMethods;
    }
    
    @Override
    public HashSet<SootClass> getEntryClasses() {
        if (methods == null) build();
        return entryClasses;
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
        	// do nothing; "external" reflection handler has already been run in the constructor of Program
        	// and Config.userClassPathName has been pointed to the application classes modified to inline reflective calls.
        }
        System.out.println("Soot class path:"+Scene.v().getSootClassPath());
        reflect = new Reflect();   
        Scene.v().loadNecessaryClasses();
        if(!Scene.v().containsClass("java.lang.Object")){
        	System.out.println("FATAL: Could not load java.lang.Ojbect");
        	throw new RuntimeException();
        }
        javaLangObject = Scene.v().getSootClass("java.lang.Object");
         
        entryClasses = new HashSet<SootClass>(); 
        entryMethods = new HashSet<SootMethod>();
        if(Config.populate)
        	entryPointGen();
	      //extractJUnitTests();
        prepEntrypoints(); 
        Scene.v().loadBasicClasses();
        
        for (int i = 0; repeat; i++) {
            if (Config.verbose >= 1) System.out.println("Iteration: " + i);
            repeat = false;
            classesVisitedForClinit.clear();
            methods.clear();
            
            for (SootClass cl: entryClasses) visitClinits(cl);
            for (SootMethod m: entryMethods) visitMethod(m);

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
        System.out.println("RTA Number of Classes: "+classes.size());
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
     * In populate phase, go over all class files and if they are public
     * add them to the entry point file
     */
    protected void entryPointGen(){
    	String petabloxClassPath = Config.userClassPathName;
    	String[] classPathElems = petabloxClassPath.split(File.pathSeparator);
    	try{
    		File entryPointFile = new File(Config.workDirName + File.separator + Config.outDirName+ File.separator +"entryPoints.txt");
    		PrintWriter pw = new PrintWriter(entryPointFile);
    		for(String classPathElem : classPathElems){
    			for(String cl : SourceLocator.v().getClassesUnder(classPathElem)){
    				SootClass c = Scene.v().loadClassAndSupport(cl);
    				if(c.isPublic()){
    					pw.println(c);
    				}
    			}
    		}
    		pw.flush();
    		pw.close();
    		System.setProperty("petablox.entrypoints.file", Config.workDirName + File.separator + Config.outDirName+ File.separator +"entryPoints.txt");
    	}catch(Exception e){
    		System.out.println("WARN: RTA Could not generate entry points file");
    	}
    }

	protected List<SootMethod> extractJUnitTestsHelper(SootClass cl) {
		//System.out.println("prepare cl " + cl + ", tag_sz=" + cl.getTags().size() + ", cl.tags" + cl.getTags());

		ArrayList<SootMethod> testMethods = new ArrayList<SootMethod>();

		Iterator<SootMethod> it = cl.methodIterator();
		while (it.hasNext()) {
			SootMethod m = it.next();
			//System.out.println("method:" + m + ", isPublic:" + m.isPublic() +", isNative:" + m.isNative() + ", tags: " + m.getTags());
			boolean found = false;
			for(soot.tagkit.Tag tg : m.getTags()) {
				if(!found && tg instanceof soot.tagkit.VisibilityAnnotationTag) {
					VisibilityAnnotationTag vat = (VisibilityAnnotationTag) tg;
					for( AnnotationTag at : vat.getAnnotations()) {
						//System.out.println("type: " + at.getType());
						if(at.getType().equals("Lorg/junit/Test;")) {
							found = true;
							break;
						}
					}
				}
			}

			if(found) testMethods.add(m);
		}

		return testMethods;
	}

	protected void extractJUnitTests () {
		System.out.println("enter extractJUnitTests... ");
		String regs = System.getProperty("petablox.junit.regressions", "1");
		String testgroup = System.getProperty("petablox.junit.testgroup", "1");

		int regs_i = Integer.valueOf(regs);
		int testgroup_i = Integer.valueOf(testgroup);

		for(int r=0; r < regs_i; ++r) {
			String cName = "RegressionTest" + r;
			Scene.v().addBasicClass(cName, SootClass.BODIES);
		}

		ArrayList<SootMethod> res = new ArrayList<SootMethod>();

		for(int r=0; r < regs_i; ++r) {
			String cName = "RegressionTest" + r;

			if (Scene.v().containsClass(cName)) {
				System.out.println("collect methods defined in " + cName);

				SootClass cl = Scene.v().getSootClass(cName);
				entryClasses.add(cl);
				cl.setApplicationClass();
				prepareClass(cl.getType());

				res.addAll( extractJUnitTestsHelper(cl) );
			}
		}

		entryMethods.addAll( res.subList(0, testgroup_i) );
		//entryMethods.add( res.get( testgroup_i-1 ) );


		System.out.println("The following methods will be analyzed: ");
		for(SootMethod x : entryMethods) {
			System.out.println(x);
		}
	}

    /**
     * Invoked by RTA before starting iterations. 
     * 
     * Note that this is invoked AFTER the hosted JVM is set up.
     */
    protected void prepEntrypoints () {
    	String entryMethodsFile = System.getProperty("petablox.entrypoints.file", "");
    	if (!entryMethodsFile.equals("")) {
			HashSet<String> addedClasses = new HashSet<String>();
			List<String> l = Utils.readFileToList(entryMethodsFile);
			methods = new IndexSet<SootMethod>(l.size());
		    
			for (String s : l) {
		    	if (s.startsWith("#"))
		    		continue;
		    	String cName;
		    	int colonIdx  = s.indexOf(':');
		    	if (colonIdx == 0) {
		    		System.out.println("FATAL: Bad format (line starting with a colon) in file " + entryMethodsFile);
		        	throw new RuntimeException();
		    	} 
		    	if (colonIdx > 0) 
		            cName = s.substring(1, colonIdx); // exclude the initial '<' character
		        else
		        	cName = s.trim();
		    	
		    	SootClass cl;
		        if (!Config.isExcludedFromScope(cName)) {
		        	if (!addedClasses.contains(cName)) {
		        		Scene.v().addBasicClass(cName, SootClass.BODIES);
		        		addedClasses.add(cName);
		        		cl = Scene.v().getSootClass(cName);
		        		entryClasses.add(cl);
		           	 	cl.setApplicationClass();
		                prepareClass(cl.getType());
		        	} else {
		        		cl = Scene.v().getSootClass(cName);
		        	}
		        	if (colonIdx > 0) {
			            String msig = s.substring(colonIdx + 2, s.length() - 1); // get subsignature and exclude end char '>'
			            SootMethod m = cl.getMethod(msig);
			            entryMethods.add(m);
		        	} else {
		        		//two cases: cl is an interface/abstract class or cl is a concrete class.
		                if (cl.isInterface() || cl.isAbstract()) {
		                	System.err.println("EntryPoints: Not supported: Cannot find entry points that are concrete implementations of " + cl.getName());
		                } else { //class is concrete
		                	 Iterator<SootMethod> it = cl.methodIterator();
		                     while (it.hasNext()) {
		                     	SootMethod m = it.next();
		                         if (m.isPublic() && !m.isNative())
		                             entryMethods.add(m);
		                     }
		                }
		        	}
		        }
		    }
    	} else {
    		 String mainClassName = Config.mainClassName;
    	        if (mainClassName == null)
    	            Messages.fatal(MAIN_CLASS_NOT_DEFINED);
    	        Scene.v().addBasicClass(mainClassName, SootClass.BODIES);
    	        if(!Scene.v().containsClass(mainClassName)){
    	        	Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
    	        }
    	        SootClass mainClass = Scene.v().getSootClass(mainClassName);
    	        mainClass.setApplicationClass();
    	        prepareClass(mainClass.getType());
    	        SootMethod mainMethod = mainClass.getMethod("void main(java.lang.String[])");
    	        if (mainMethod == null)
    	            Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
    	        entryClasses.add(mainClass);
    	        entryMethods.add(mainMethod);
    	}
        return;
    }

    /**
     * Invoked by RTA each iteration. A hook so subclasses can
     * add additional things to visit.
     */
    protected void visitAdditionalEntrypoints() {
    	
    }   
    
    /**
     * Called whenever RTA sees a method.
     * Adds to worklist if it hasn't previously been seen on this iteration.
     * @param m
     */
    protected void visitMethod(SootMethod m) {
    	  SootMethod s = StubMethodSupport.getStub(m);
        if(s == null) s = m;
        if (s.isConcrete() && methods.add(s)) {
            if (DEBUG) System.out.println("\tAdding method: " + s);
                s.retrieveActiveBody();
            methodWorklist.add(s);
        }
    }

    private static boolean isClassDefined(Unit u, RefType r) {  
        try {
        	SootClass c = r.getSootClass();
        	if(!c.isInScene()){
        		Scene.v().addBasicClass(c.getName(), SootClass.BODIES);
        		if (!c.isInScene() || c.isPhantomClass())
        			return false;
        		else
        			return true;
        	} else if(c.isPhantomClass()) 
        		return false;
        	else
        		return true;
        } catch(NoClassDefFoundError e) {
        	Messages.log("WARN: Failed to load class " + r.toString() + " for dynamic reflection");
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
        SootClass c = r.getSootClass();
           
        //two cases: call was Constructor.newInstance or call was Class.newInstance
        //Static reflection analysis folds these together, so we pull them apart here
        //String cName = ((InvokeStmt)u).getInvokeExpr().getMethod().getDeclaringClass().getName();
        String cName = SootUtilities.getInvokeExpr(u).getMethod().getDeclaringClass().getName();
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

    private void processResolvedAryNewInstSite(Unit u, RefLikeType r, RefType elemCl) {
        if (!isClassDefined(u, elemCl))
            return;
        reflect.addResolvedAryNewInstSite(u, r);
        visitClass(r);
        // It is possible that arrays are created with an abstract class as element type.
        // The actual allocation for elements may be from concrete implementations.
        if (elemCl.getSootClass().isConcrete() && reachableAllocClasses.add(elemCl))
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
        /*try{
        	if(!m.hasActiveBody())
        		m.retrieveActiveBody(); // TODO: Hack to ignore methods which have no source
        }catch(Exception e){
        	Messages.log(METHOD_BODY_NOT_FOUND, m.getSubSignature(),m.getDeclaringClass().getName());
        	return;
        }*/
        Iterator<Local> itr = SootUtilities.getLocals(m).iterator();
        while(itr.hasNext()){
        	Local l = itr.next();
        	if(l.getType() instanceof RefLikeType){
        		visitClass((RefLikeType)l.getType());
        	}
        }
        ICFG cfg = SootUtilities.getCFG(m);
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
        int offset = SootUtilities.getBCI(u);
        boolean flag = me.mName.equals(m.getName().toString()) &&
        	m.getBytecodeSignature().contains(me.mDesc) &&                                              
            me.cName.equals(m.getDeclaringClass().getName()) &&
            offset == me.offset;  
        if (flag) {
        	if (Config.verbose >= 2) {
	        	System.out.println("MATCH: " + uStr + "  " + m.getName() + "  " + m.getDeclaringClass().getName());
	        	System.out.println("dyn instr offset:" + me.offset + "  Soot offset:" + SootUtilities.getBCI(u));
        	}
        	return flag;
        } 
    	// The following fix is to bypass a bug in soot. Ideally needs to be fixed in soot.
		if (u instanceof JAssignStmt) {
			JAssignStmt as = (JAssignStmt)u;
			if (as.rightBox.getValue() instanceof InvokeExpr) {
				Value v = as.leftBox.getValue();
				if (v instanceof Local) {
					offset -= 3;
					if (offset >= 0) {
						flag = me.mName.equals(m.getName().toString()) &&
						        	m.getBytecodeSignature().contains(me.mDesc) &&                                              
						            me.cName.equals(m.getDeclaringClass().getName()) &&
						            offset == me.offset;  
				        if (flag) {
				        	if (Config.verbose >= 2) {
					        	System.out.println("MATCH (SUB): " + uStr + "  " + m.getName() + "  " + m.getDeclaringClass().getName());
					        	System.out.println("dyn instr offset:" + me.offset + "  Soot offset:" + SootUtilities.getBCI(u));
				        	}
				        	return flag;
				        }
					}
				}
			}
		}
    	if (me.mName.equals(m.getName().toString()) &&
    	    m.getBytecodeSignature().contains(me.mDesc) &&                                               
            me.cName.equals(m.getDeclaringClass().getName())) {
    		if (Config.verbose >= 2) {
	        	System.out.println("NO MATCH: " + uStr + "  " + m.getName() + "  " + m.getDeclaringClass().getName());
	        	System.out.println("dyn instr offset:" + me.offset + "  Soot offset:" + SootUtilities.getBCI(u));
    		}
    	}
        return flag;
    }
    
    private SootMethod getMethodItr(SootClass c,String subsign){
      ArrayList<SootClass> queue = new ArrayList<SootClass>();
      SootMethod ret = null;
      queue.add(c);
      while(!queue.isEmpty()){
        SootClass tos = queue.remove(0);
        ret= tos.getMethodUnsafe(subsign);
        if(ret == null){
          for(SootClass inter : tos.getInterfaces()){
            queue.add(inter); 
          }
          if(tos.hasSuperclass()){
            queue.add(tos.getSuperclass());
          }
        }else break;
      }
      if(ret == null)
        System.out.println("WARN: RTA method not found "+subsign);
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
        String subsig = n.getSubSignature();
        visitMethod(n);
        visitExceptions(n);
        String cName = c.getName();
        if (cName.equals("java.lang.Class")) {
            if (dynamicResolvedObjNewInstSites != null &&
            		subsig.equals("java.lang.Object newInstance()")){  
                for (Pair<String, List<String>> p : dynamicResolvedObjNewInstSites) {
                	if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	SootClass r = SootUtilities.loadClass(s);
                            if (r != null)
                                processResolvedObjNewInstSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        } else if (cName.equals("java.lang.reflect.Constructor")) {
            if (dynamicResolvedConNewInstSites != null &&
            		subsig.equals("java.lang.Object newInstance(java.lang.Object[])")) {
                for (Pair<String, List<String>> p : dynamicResolvedConNewInstSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	SootClass r = SootUtilities.loadClass(s);
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
            		SootMethod m2 = this.getMethodItr(d,subsig); 
                if(m2 == null){
              		// TODO : Verify, Soot shows the method only in the class
              		// where it is defined and not in sub-classes
            	  	Messages.log(METHOD_NOT_FOUND_IN_SUBTYPE,
                            subsig, d.getName(), c.getName());
                }else	visitMethod(m2);
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
                        	SootClass r = SootUtilities.loadClass(s);
                            if (r != null)
                                processResolvedClsForNameSite(u, r.getType());
                        }
                        break;
                    }
                }
            }
        } else if (cName.equals("java.lang.reflect.Array")) {
            if (dynamicResolvedAryNewInstSites != null &&
            		n.getSubSignature().equals("java.lang.Object newInstance(java.lang.Class,int)")) {
                for (Pair<String, List<String>> p : dynamicResolvedAryNewInstSites) {
                    if (matches(p.val0, m, u)) {
                        for (String s : p.val1) {
                        	String sm = s.substring(0, s.indexOf('['));
                        	SootClass r = SootUtilities.loadClass(sm);
                        	int dim = s.split("\\[").length - 1;
                        	assert (dim > 0);
                            if(r!=null){
                                ArrayType arr = ArrayType.v(r.getType(), dim);
                                if (arr != null)
                                    processResolvedAryNewInstSite(u, arr, r.getType());
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private void prepareClass(RefLikeType r) {
        if (classes.add(r)) {                             
            if (DEBUG) System.out.println("\tAdding class: " + r);
            if(r instanceof ArrayType) {
            	Type bType = ((ArrayType) r).baseType;
            	if (bType instanceof RefType) {
                    SootClass d = ((RefType)bType).getSootClass();
                    visitClass((RefType)bType);
                    int numDimensions = ((ArrayType)r).numDimensions;
                    if(d.hasSuperclass()){
                        visitClass(ArrayType.v(d.getSuperclass().getType(),numDimensions));
                    }
                    for(SootClass i : d.getInterfaces())
                        visitClass(ArrayType.v(i.getType(),numDimensions));
                }
            	return;
            }
            SootClass c = SootUtilities.loadClass(((RefType)r).getSootClass().getName());
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
        if (!c.isPhantomClass())
        	visitClinits(c);
    }

    protected void visitClinits(SootClass c) {
        if (classesVisitedForClinit.add(c)) {
	        	SootMethod m=c.getMethodUnsafe("void <clinit>()");
	            // m is null for classes without class initializer method
	            if (m != null)
	                visitMethod(m);
        	if(c.hasSuperclass()){
	            SootClass d = c.getSuperclass();
	            visitClinits(d);
        	}
            for (SootClass i : c.getInterfaces())
                visitClinits(i);
        }
    }
}
