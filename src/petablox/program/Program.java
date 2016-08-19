package petablox.program;

import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.io.PrintWriter;
import java.io.IOException;

import com.java2html.Java2HTML;

import petablox.instr.BasicInstrumentor;
import petablox.program.reflect.ExtReflectResolver;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.OutDirUtils;
import petablox.project.PetabloxException;
import petablox.runtime.BasicEventHandler;
import petablox.util.IndexSet;
import petablox.util.ProcessExecutor;
import petablox.util.Utils;
import petablox.util.soot.ICFG;
import petablox.util.soot.SSAUtilities;
import petablox.util.soot.SootUtilities;
import petablox.util.soot.StubMethodSupport;
import petablox.util.tuple.object.Pair;
import soot.*;
import soot.jimple.toolkits.typing.fast.Integer127Type;
import soot.jimple.toolkits.typing.fast.Integer1Type;
import soot.jimple.toolkits.typing.fast.Integer32767Type;
import soot.options.Options;
import soot.util.Chain;

/**
 * Quadcode intermediate representation of a Java program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Program {
    private static final String LOADING_CLASS =
        "INFO: Program: Loading class %s.";
    private static final String MAIN_CLASS_NOT_DEFINED =
        "ERROR: Program: Property petablox.main.class must be set to specify the main class of program to be analyzed.";
    private static final String MAIN_METHOD_NOT_FOUND =
        "ERROR: Program: Could not find main class '%s' or main method in that class.";
    private static final String CLASS_PATH_NOT_DEFINED =
        "ERROR: Program: Property petablox.class.path must be set to specify location(s) of .class files of program to be analyzed.";
    private static final String SRC_PATH_NOT_DEFINED =
        "ERROR: Program: Property petablox.src.path must be set to specify location(s) of .java files of program to be analyzed.";
    private static final String METHOD_NOT_FOUND =
        "ERROR: Program: Could not find method '%s'.";
    private static final String CLASS_NOT_FOUND =
        "ERROR: Program: Could not find class '%s'.";

    private static Program program = null;
    private boolean isBuilt;
    private IndexSet<SootMethod> methods;
    private IndexSet<RefLikeType> scopeClasses;
    private Reflect reflect;
    private IndexSet<RefLikeType> classes;
    private IndexSet<Type> types;
    private Map<String, Type> nameToTypeMap;
    private Map<String, RefLikeType> nameToClassMap;
    private Map<String, SootMethod> signToMethodMap;
    private Map<Unit, SootMethod> unitToMethodMap;
    private SootMethod mainMethod;
    private boolean HTMLizedJavaSrcFiles;
    private ClassHierarchy ch;
    private Type[] basicTypes = {
			BooleanType.v(),
			ByteType.v(),
			CharType.v(),
			DoubleType.v(),
			FloatType.v(),
			Integer127Type.v(),
			Integer1Type.v(),
			Integer32767Type.v(),
			IntType.v(),
			LongType.v(),
			ShortType.v()
	};

    private Program() {
    	if (Config.verbose >= 3)
        	soot.options.Options.v().set_verbose(true);
        String ssaKind = Config.ssaKind;
        if (ssaKind.equals("nophi"))
            SSAUtilities.doSSA(false, false);
        else if (ssaKind.equals("phi"))
            SSAUtilities.doSSA(true, false);
        else if (ssaKind.equals("nomove")) 
            SSAUtilities.doSSA(false, true);
		else if (ssaKind.equals("nomovephi"))
			SSAUtilities.doSSA(true, true);
    	
    	//List<String> excluded = new ArrayList<String>();
    	//Options.v().set_coffi(true);
    	//Options.v().set_exclude(excluded);
    	Options.v().set_include_all(true);
    	Options.v().set_keep_line_number(true);
    	Options.v().set_keep_offset(true);
    	Options.v().set_whole_program(true);
    	Options.v().set_allow_phantom_refs(true);
    	
		if (Config.reflectKind.equals("external")) {
			ExtReflectResolver extReflectResolver = new ExtReflectResolver();
			if (Config.reuseScope == false)
				extReflectResolver.run();
			else
				extReflectResolver.setUserClassPath();
		}
		String stdlibClPath = System.getProperty("sun.boot.class.path");
		Options.v().set_soot_classpath(Config.userClassPathName + File.pathSeparator + 
				                       Scene.v().defaultClassPath() + File.pathSeparator +
		                               stdlibClPath);
    	Scene.v().addBasicClass(Config.mainClassName);
    	Scene.v().loadBasicClasses();
    	SootClass mainCl = Scene.v().getSootClass(Config.mainClassName);
    	Scene.v().setMainClass(mainCl);
    	if(Config.verbose >= 1) {
    		Chain<SootClass> chc = Scene.v().getClasses();
    		System.out.println("NUMBER OF CLASSES IN SCENE: " + chc.size());
    	}
    }

    /**
     * Provides the program's quadcode representation.
     *
     * @return The program's quadcode representation.
     */
    public static Program g() {
        if (program == null)
            program = new Program();
        return program;
    }

    /**
     * Provides the program's class hierarchy.
     *
     * @return The program's class hierarchy.
     */
    public ClassHierarchy getClassHierarchy() {
        if (ch == null)
            ch = new ClassHierarchy();
        return ch;
    }

    /**
     * Constructs the program's quadcode representation.
     *
     * Users need not call this method explicitly as it is called by each
     * method in this class that requires the representation to be built.
     */
    public void build() {
        if (!isBuilt) {
            buildClasses();
            isBuilt = true;
        }
    }

    /************************************************************************
     * Private helper functions
     ************************************************************************/

    private void buildMethods() {
        assert (methods == null);
        assert (reflect == null);
        File methodsFile = new File(Config.methodsFileName);
        File reflectFile = new File(Config.reflectFileName);
        File typesFile = new File(Config.typesFileName);
        if (Config.reuseScope && methodsFile.exists() && reflectFile.exists() && typesFile.exists()) {
        	// loadTypesFile needs to be called before loadMethodsFile and loadReflectFile
        	loadTypesFile(typesFile);
            loadMethodsFile(methodsFile);
            loadReflectFile(reflectFile);
        } else {
            String scopeKind = Config.scopeKind;
            ScopeBuilder b = null;
            if (scopeKind.equals("rta")) {
                b = new RTA(Config.reflectKind);
            } else if (scopeKind.equals("dynamic")) {
                //b = new DynamicBuilder();
            	System.out.println("Unsupported kind of scope construction!!");
            	throw new RuntimeException();
            } else if (scopeKind.equals("cha")) {
            	System.out.println("Unsupported kind of scope construction !!");
            	throw new RuntimeException();
                //b = new CHA(getClassHierarchy());
            } else {
                try {
                    Class<?> scopeBuildClass = Class.forName(scopeKind);
                    b = (ScopeBuilder) scopeBuildClass.newInstance();
                } catch(Exception e) {
                    System.err.println("didn't recognize scope builder named " + scopeKind +
                            ". Expected 'rta', 'cha', 'dynamic', or the name of a class implementing ScopeBuilder.");
                    System.exit(1);
                }
            }
            methods = b.getMethods();
            scopeClasses = b.getClasses();
            reflect = b.getReflect();

            saveMethodsFile(methodsFile);
            saveReflectFile(reflectFile);
            saveTypesFile(typesFile);
        }
        buildSignToMethodMap();
        //Set up Soot Class hierarchy object in SootUtilities
        Hierarchy h = new Hierarchy();
        SootUtilities.h = h;
    }

    private List<Type> getBasicTypes(){
    	List<Type> types = new ArrayList<Type>();
    	for(Type t : basicTypes){
    		types.add(t);
    		types.add(ArrayType.v(t, 1));
    	}
    	/*String[] basicClassNames = { "java.lang.Object",
    	        "java.lang.Class",
    	        "java.lang.String",
    	        "java.lang.System",
    	        "java.lang.Throwable",
    	        //"java.lang.Address",
    	        "java.lang.Exception",
    	        "java.lang.ArrayStoreException",
    	        "java.lang.Error",
    	        "java.lang.RuntimeException",
    	        "java.lang.NullPointerException",
    	        "java.lang.IndexOutOfBoundsException",
    	        "java.lang.NegativeArraySizeException",
    	        "java.lang.ArithmeticException",
    	        "java.lang.IllegalMonitorStateException",
    	        "java.lang.ClassCastException",
    	        "java.lang.ClassLoader",
    	        "java.lang.reflect.Field",
    	        "java.lang.reflect.Method",
    	        "java.lang.reflect.Constructor",
    	        "java.lang.Thread",
    	        "java.lang.ref.Finalizer"};
    	
    	for (String className : basicClassNames){
            types.add(SootResolver.v().makeClassRef(className).getType());
        }*/
    	Iterator<RefLikeType> itr = scopeClasses.iterator();
    	while(itr.hasNext()){
    		types.add(itr.next());
    	}
    	return types;
    }
    
    private void buildClasses() {
    	if (methods == null)
            buildMethods();
        assert (classes == null);
        assert (types == null);
        List<Type> typesAry = getBasicTypes();
        int numTypes = typesAry.size();
        Collections.sort(typesAry, comparator);
        types = new IndexSet<Type>(numTypes+1);
        classes = new IndexSet<RefLikeType>();
        types.add(NullType.v());
        for (int i = 0; i < numTypes; i++) {
            Type t = typesAry.get(i);
            assert (t != null);
            types.add(t);
            if (t instanceof RefLikeType) {
            	RefLikeType r = (RefLikeType)t;
                classes.add(r);
            }
        }
        buildNameToClassMap();
        buildNameToTypeMap();
    }

    private boolean isExcluded(SootMethod m) {
    	SootClass c = m.getDeclaringClass();
    	if (Config.isExcludedFromScope(c.getName()))
    		return true;
    	return false;
    }
    
    private SootMethod getMethodItr(SootClass c,String subsign){
        SootMethod ret = null;
        while(true){
            try{
                ret= c.getMethod(subsign);
                break;
            }catch(Exception e){
                if(!c.hasSuperclass()){
                    System.out.println("WARN: Method "+subsign+" not found");
                    break;
                }else{
                    c = c.getSuperclass();
                }
            }
        }
        return ret;
    }
    
    private void setScopeExclusion(String excl) {
    	// This method is called when petablox.reuse.scope = true
    	String[] recordedScopeAry = Utils.toArray(excl);
    	if (Config.scopeExcludeStr.equals("")) {
    		//No explicitly-specified scope - since you are "reusing", use recorded scope and do vanilla reuse_scope actions.
    		Config.scopeExcludeStr = excl;
    		Config.scopeExcludeAry = recordedScopeAry;
    	} else {
    		Set<String> recorded = new HashSet<String>();
    		for (int i = 0; i < recordedScopeAry.length; i++) recorded.add(recordedScopeAry[i]);
    		Set<String> present = new HashSet<String>();
    		for (int j = 0; j < Config.scopeExcludeAry.length; j++) present.add(Config.scopeExcludeAry[j]);
    		if (present.equals(recorded)) {
    			// Nothing extra needs to be done - just vanilla reuse_scope actions.
    		} else {
    			String str = excl;
    			if (excl.equals("")) str = "\"\"";
    			throw new PetabloxException("Not possible to reuse scope. Recorded scope exclusion " + str 
    					 + " different from required scope exclusion " + Config.scopeExcludeStr );
    		}
    	}
    }

    private void loadMethodsFile(File file) {
    	List<String> l = Utils.readFileToList(file);
        methods = new IndexSet<SootMethod>(l.size());
    	String first = l.remove(0);
    	// "first" is the scope exclude string
    	String[] parts = first.split("PETABLOX_SCOPE_EXCLUDE_STR=");
    	String scopeExclStr;
    	if (parts.length < 2)
    		scopeExclStr = "";
    	else
    		scopeExclStr = parts[1];
        setScopeExclusion(scopeExclStr);
        for (String s : l) {
        	int colonIdx  = s.indexOf(':');
            String cName = s.substring(1, colonIdx); // exclude the initial '<' character
            String subsig = s.substring(colonIdx + 2, s.length() - 1); // get subsignature and exclude end char '>'
            SootClass c = Scene.v().getSootClass(cName);
            SootMethod m = getMethodItr(c, subsig);
            assert (m != null);
          	SootMethod sm = null;
        	if(StubMethodSupport.methodToStub.containsKey(m) || StubMethodSupport.methodToStub.containsValue(m)){
        		sm = m;
        	}else{
    	    	if(StubMethodSupport.toReplace(m)){
    	    		sm = StubMethodSupport.getStub(m);
    	    	}else if(isExcluded(m)){
    	    		sm = StubMethodSupport.emptyStub(m);
    	    	}else{
    	    		sm = m;
    	    	}
        	}
            if (sm.isConcrete()) {
                sm.retrieveActiveBody();
                // This is required for methods to have their final bodies in case SSA is turned on.
                ICFG cfg = SootUtilities.getCFG(sm);
            }
            methods.add(sm);
        }
    }

    private void saveMethodsFile(File file) {
        try {
            PrintWriter out = new PrintWriter(file);
            out.println("PETABLOX_SCOPE_EXCLUDE_STR=" + Config.scopeExcludeStr);
            for (SootMethod m : methods)
                out.println(m.getSignature());
            out.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void saveTypesFile(File file) {
    	 try {
             PrintWriter out = new PrintWriter(file);
             for (Type t : scopeClasses)
                 out.println(t.toString());
             out.close();
         } catch (IOException ex) {
             throw new RuntimeException(ex);
         }
    }
    
    // loadTypesFile needs to be called before loadMethodsFile and loadReflectFile
    private void loadTypesFile(File file) {
     	List<String> l = Utils.readFileToList(file);
    	scopeClasses = new IndexSet<RefLikeType>(l.size());
    	for (String s : l) {
    		RefLikeType t = loadTypeString (s);
    		if (t != null) scopeClasses.add(t);
    	}
    }
    
    private RefLikeType loadTypeString(String s) {
    	Type r = null;
		int dimPos = s.indexOf('[');
		if (dimPos > 0) {
    		String sm = s.substring(0, dimPos);
    		boolean isBasicType = false;
    		int i;
    		for (i = 0; i < basicTypes.length; i++) {
    			if (sm.equals(basicTypes[i].toString())) {
    				isBasicType = true;
    				break;
    			}	
    		}
        	if (isBasicType)
        		r = basicTypes[i];
        	else {
        		r = loadClass(sm);
        		if (r != null) {
	        		assert (r instanceof RefType);
	        		scopeClasses.add((RefType) r);
        		}
        	}
        	int dim = s.split("\\[").length - 1;
        	ArrayType arr = null;
        	if (r != null) arr = ArrayType.v(r, dim);
            return (RefLikeType)arr;
    	} else {
    		r = loadClass(s);
    		if (r != null) 
    			assert (r instanceof RefType);
    		return (RefLikeType)r;
    	}
    }
    
    private List<Pair<Unit, List<RefLikeType>>> loadResolvedSites(BufferedReader in) {
        List<Pair<Unit, List<RefLikeType>>> l = new ArrayList<Pair<Unit, List<RefLikeType>>>();
        String s;
        try {
            while ((s = in.readLine()) != null) {
                if (s.startsWith("#"))
                    break;
                
                if (Utils.buildBoolProperty("petablox.reflect.exclude", false)) {
                    boolean excludeLine = false;
                    String cName = strToClassName(s);
                    for (String c : Config.scopeExcludeAry) {
                        if (cName.startsWith(c)) {
                            excludeLine = true;
                            break;
                        }
                    }
                    if (excludeLine)
                        continue;
                }
                
                Pair<Unit, List<RefLikeType>> site = strToSite(s);
                
                l.add(site);
            }
        } catch (IOException ex) {
            Messages.fatal(ex);
        }
        return l;
    }

    private void saveResolvedSites(List<Pair<Unit, List<RefLikeType>>> l, PrintWriter out) {
    	if (l.size() > 0 && unitToMethodMap == null)
    		buildUnitToMethodMap();
        for (Pair<Unit, List<RefLikeType>> p : l) {
            String s = siteToStr(p);
            out.println(s);
        }
    }

    private void loadReflectFile(File file) {
        BufferedReader in = null;
        String s = null;
        try {
            in = new BufferedReader(new FileReader(file));
            s = in.readLine();
        } catch (IOException ex) {
            Messages.fatal(ex);
        }
        List<Pair<Unit, List<RefLikeType>>> resolvedClsForNameSites;
        List<Pair<Unit, List<RefLikeType>>> resolvedObjNewInstSites;
        List<Pair<Unit, List<RefLikeType>>> resolvedConNewInstSites;
        List<Pair<Unit, List<RefLikeType>>> resolvedAryNewInstSites;
        if (s == null) {
            resolvedClsForNameSites = Collections.emptyList();
            resolvedObjNewInstSites = Collections.emptyList();
            resolvedConNewInstSites = Collections.emptyList();
            resolvedAryNewInstSites = Collections.emptyList();
        } else {
            resolvedClsForNameSites = loadResolvedSites(in);
            resolvedObjNewInstSites = loadResolvedSites(in);
            resolvedConNewInstSites = loadResolvedSites(in);
            resolvedAryNewInstSites = loadResolvedSites(in);
        }
        reflect = new Reflect(resolvedClsForNameSites, resolvedObjNewInstSites,
            resolvedConNewInstSites, resolvedAryNewInstSites);
    }

    private void saveReflectFile(File file) {
        try {
            PrintWriter out = new PrintWriter(file);
            out.println("# resolvedClsForNameSites");
            saveResolvedSites(reflect.getResolvedClsForNameSites(), out);
            out.println("# resolvedObjNewInstSites");
            saveResolvedSites(reflect.getResolvedObjNewInstSites(), out);
            out.println("# resolvedConNewInstSites");
            saveResolvedSites(reflect.getResolvedConNewInstSites(), out);
            out.println("# resolvedAryNewInstSites");
            saveResolvedSites(reflect.getResolvedAryNewInstSites(), out);
            out.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String strToClassName(String s) {
        String[] a = s.split("->");
        assert (a.length == 2);
        int clNameStart = a[0].indexOf("!<") + 2;
        int clNameEnd = a[0].indexOf(':');
        String clName = a[0].substring(clNameStart, clNameEnd);
        return clName;
    }
    
    private Pair<Unit, List<RefLikeType>> strToSite(String s) {
        String[] a = s.split("->");
        assert (a.length == 2);
        int bciEnd = a[0].indexOf('!');
        int bciFromStr = Integer.parseInt(a[0].substring(0, bciEnd));
        SootMethod m = Scene.v().getMethod(a[0].substring(bciEnd + 1).trim());
        Unit u = getUnit(m, bciFromStr);
        assert (u != null);
        String[] rNames = a[1].split(",");
        List<RefLikeType> rTypes = new ArrayList<RefLikeType>(rNames.length);
        for (String rName : rNames) {
        	RefLikeType t = loadTypeString (rName);
        	if (t != null) {
	    		scopeClasses.add(t);
	            rTypes.add(t);
        	}
        }
        return new Pair<Unit, List<RefLikeType>>(u, rTypes);
    }

    private String siteToStr(Pair<Unit, List<RefLikeType>> p) {
        List<RefLikeType> l = p.val1;
        assert (l != null);
        int n = l.size();
        Iterator<RefLikeType> it = l.iterator();
        assert (n > 0);
        Unit u = p.val0;
        int bci = SootUtilities.getBCI(u);
        SootMethod m = unitToMethodMap.get(u);
        String s = bci + "!" + m.getSignature() + "->" + it.next();
        for (int i = 1; i < n; i++)
            s += "," + it.next();
        return s;
    }

    private void buildNameToTypeMap() {
        assert (nameToTypeMap == null);
        assert (types != null);
        nameToTypeMap = new HashMap<String, Type>();
        for (Type t : types) {
            nameToTypeMap.put(t.toString(), t);
        }
    }

    private void buildNameToClassMap() {
        assert (nameToClassMap == null);
        assert (classes != null);
        nameToClassMap = new HashMap<String, RefLikeType>();
        for (RefLikeType c : classes) {
        	nameToClassMap.put(c.toString(), c);
        }
    }

    private void buildSignToMethodMap() {
        assert (signToMethodMap == null);
        assert (methods != null);
        signToMethodMap = new HashMap<String, SootMethod>();
        for (SootMethod m : methods) {
            String sign = m.getSignature();
            signToMethodMap.put(sign, m);
        }
    }
    
    private void buildUnitToMethodMap() {
    	unitToMethodMap = new HashMap<Unit, SootMethod>();
    	for (SootMethod m : methods) {
    		if (m.isConcrete()) {
	    		PatchingChain<Unit> upc = m.retrieveActiveBody().getUnits();
	    		Iterator<Unit> it = upc.iterator();
	    		while (it.hasNext())
	    			unitToMethodMap.put(it.next(), m);
    		}
    	}
    	return;
    }
    
    private static Comparator<Type> comparator = new Comparator<Type>() {
        @Override
        public int compare(Type t1, Type t2) {
            String s1 = t1.toString();
            String s2 = t2.toString();
            return s1.compareTo(s2);
        }
    };

    /**
     * Loads the given class, if it is not already loaded, and provides its quadcode representation.
     *
     * @param s The name of the class to be loaded.  It may be provided in any of several formats, e.g.,
     *        "{@code [I}", "{@code int[]}", "{@code java.lang.String[]}", "{@code [Ljava/lang/String;}".
     *
     * @return The quadcode representation of the given class.
     *
     * @throws Error If the class loading failed.
     */
    public RefType loadClass(String s) throws Error {
    	if (Config.verbose >= 2)
            Messages.log(LOADING_CLASS, s);
    	SootClass c = null;
    	if(Scene.v().containsClass(s)){
    		c = Scene.v().getSootClass(s);
    	}else{
    		Scene.v().addBasicClass(s,SootClass.BODIES);
    		Scene.v().loadBasicClasses();
    		if(!Scene.v().containsClass(s)){
    			System.out.println("WARN: Could not load class " + s);
    			return null;
    		}else{
    			c = Scene.v().getSootClass(s);
    		}
    	}
    	c.setApplicationClass();
        return c.getType();
    }

    /**
     * Provides the quadcode representation of all types deemed reachable.
     * A type is deemed reachable if it is referenced in any loaded class.
     *
     * @return The quadcode representation of all types deemed reachable.
     */
    public IndexSet<Type> getTypes() {
        if (types == null)
            buildClasses();
        return types;
    }

    /**
     * Provides the quadcode representation of all methods deemed reachable by analysis scope construction.
     *
     * @return The quadcode representation of all methods deemed reachable by analysis scope construction.
     */
    public IndexSet<SootMethod> getMethods() {
        if (methods == null)
            buildMethods();
        return methods;
    }

    /**
     * Provides reflection information resolved by analysis scope construction.
     *
     * @return Reflection information resolved by analysis scope construction.
     */
    public Reflect getReflect() {
        if (reflect == null)
            buildMethods();
        return reflect;
    }

    /**
     * Provides the quadcode representation of all classes deemed reachable by analysis scope construction.
     *
     * @return The quadcode representation of all classes deemed reachable by analysis scope construction.
     */
    public IndexSet<RefLikeType> getClasses() {
        if (classes == null)
            buildClasses();
        return classes;
    }

    /**
     * Provides the quadcode representation of the given class, if it is deemed reachable, and null otherwise.
     *
     * @return The quadcode representation of the given class, if it is deemed reachable, and null otherwise.
     */
    public RefLikeType getClass(String name) {
        if (classes == null)
            buildClasses();
        return nameToClassMap.get(name);
    }

    /**
     * Provides the quadcode representation of the given method, if it is deemed reachable, and null otherwise.
     *
     * @param sign Signature of the method in format {@code mName:mDesc@cName} specifying its name (mName),
     * its descriptor (mDesc), and its declaring class (cName).
     *
     * @return The quadcode representation of the given method, if it is deemed reachable, and null otherwise.
     */
    public SootMethod getMethod(String sign) {
        if (methods == null)
            buildMethods();
        return signToMethodMap.get(sign);
    }

    /**
     * Provides the quadcode representation of the main method of the program, if it exists, and exits otherwise.
     */
    public SootMethod getMainMethod() {
    	if (methods == null)
    		buildMethods();
        if (mainMethod == null) {
            if (!Scene.v().hasMainClass())
                Messages.fatal(MAIN_CLASS_NOT_DEFINED);
            mainMethod = Scene.v().getMainMethod();
            if (mainMethod == null)
                Messages.fatal(MAIN_METHOD_NOT_FOUND, Scene.v().getMainClass().getName());
        }
        return mainMethod;
    }

    /**
     * Provides the quadcode representation of the {@code start()} method of class {@code java.lang.Thread},
     * if it is deemed reachable, and null otherwise.
     *
     * @return The quadcode representation of the {@code start()} method of class {@code java.lang.Thread},
     * if it is deemed reachable, and null otherwise.
     */
    public SootMethod getThreadStartMethod() {
    	return getMethod("<java.lang.Thread: void start()>");
    }

    /**
     * Provides the quadcode representation of the given type, if it is deemed reachable, and null otherwise.
     *
     * @return The quadcode representation of the given type, if it is deemed reachable, and null otherwise.
     */
    public Type getType(String name) {
        if (classes == null)
            buildClasses();
        return nameToTypeMap.get(name);
    }

    public Unit getUnit(SootMethod m, int bci) {
    	Unit u = null;
		PatchingChain<Unit> upc = m.retrieveActiveBody().getUnits();
		Iterator<Unit> it = upc.iterator();
		while (it.hasNext()) {
			u = it.next();
			if (SootUtilities.getBCI(u) == bci)
				break;
		}
    	return u;
    }
   
    /**
     * Provides a human-readable string that corresponds to the given bytecode string encoding a list of zero
     * or more types, if it is well-formed, and null otherwise.
     * Example: Converts "{@code [Ljava/lang/String;I}" to "{@code java.lang.String[],int}".
     *
     * @return A human-readable string that corresponds to the given bytecode string encoding a list of zero
     * or more types, if it is well-formed, and null otherwise.
     */
    public static String typesToStr(String typesStr) {
        String result = "";
        boolean needsSep = false;
        while (typesStr.length() != 0) {
            boolean isArray = false;
            int numDim = 0;
            String baseType;
            // Handle array case
            while (typesStr.startsWith("[")) {
                isArray = true;
                numDim++;
                typesStr = typesStr.substring(1);
            }
            // Determine base type
            if (typesStr.startsWith("B")) {
                baseType = "byte";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("C")) {
                baseType = "char";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("D")) {
                baseType = "double";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("F")) {
                baseType = "float";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("I")) {
                baseType = "int";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("J")) {
                baseType = "long";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("L")) {
                int index = typesStr.indexOf(';');
                if (index == -1)
                    return null;
                String className = typesStr.substring(1, index);
                baseType = className.replace('/', '.');
                typesStr = typesStr.substring(index + 1);
            } else if (typesStr.startsWith("S")) {
                baseType = "short";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("Z")) {
                baseType = "boolean";
                typesStr = typesStr.substring(1);
            } else if (typesStr.startsWith("V")) {
                baseType = "void";
                typesStr = typesStr.substring(1);
            } else
                return null;
            if (needsSep)
                result += ",";
            result += baseType;
            if (isArray) {
                for (int i = 0; i < numDim; i++)
                    result += "[]";
            }
            needsSep = true;
        }
        return result;
    }

    /**
     * Executes the program and provides a list of all dynamically loaded classes.
     *
     * @return A list of all dynamically loaded classes.
     */
    public List<String> getDynamicallyLoadedClasses() {
        String mainClassName = Config.mainClassName;
        if (mainClassName == null)
            Messages.fatal(MAIN_CLASS_NOT_DEFINED);
        String classPathName = Config.userClassPathName;
        if (classPathName == null)
            Messages.fatal(CLASS_PATH_NOT_DEFINED);
        String[] runIDs = Config.runIDs.split(Utils.LIST_SEPARATOR);
        assert(runIDs.length > 0);
        List<String> classNames = new ArrayList<String>();
        String fileName = Config.classesFileName;
        
        String runBefore = System.getProperty("petablox.dynamic.runBeforeCmd");
        Process beforeProc = null;
        try { 
            
            if (runBefore != null) {
                System.out.println("for dynamic analysis, running pre-command " + runBefore);
                beforeProc = ProcessExecutor.executeAsynch( new String[] {runBefore}, null, null);
            }
        } catch(Throwable ex) {
            ex.printStackTrace();
        }
        
        List<String> basecmd = new ArrayList<String>();
        basecmd.add("java");
        basecmd.addAll(Utils.tokenize(Config.runtimeJvmargs));
        Properties props = System.getProperties();
        basecmd.add("-cp");
        basecmd.add(classPathName);
        String cAgentArgs = "=classes_file=" + Config.classesFileName;
        if (Config.useJvmti)
            basecmd.add("-agentpath:" + Config.cInstrAgentFileName + cAgentArgs);
        else {
            String jAgentArgs = cAgentArgs +
                "=" + BasicInstrumentor.INSTRUMENTOR_CLASS_KEY +
                "=" + LoadedClassesInstrumentor.class.getName().replace('.', '/') +
                "=" + BasicInstrumentor.EVENT_HANDLER_CLASS_KEY +
                "=" + BasicEventHandler.class.getName().replace('.', '/');
            basecmd.add("-javaagent:" + Config.jInstrAgentFileName + jAgentArgs);
            for (Map.Entry e : props.entrySet()) {
                String key = (String) e.getKey();
                if (key.startsWith("petablox."))
                    basecmd.add("-D" + key + "=" + e.getValue());
            }
        }
        basecmd.add(mainClassName);
        
        for (String runID : runIDs) {
            String args = System.getProperty("petablox.args." + runID, "");
            List<String> fullcmd = new ArrayList<String>(basecmd);
            fullcmd.addAll(Utils.tokenize(args));
            OutDirUtils.executeWithWarnOnError(fullcmd, Config.dynamicTimeout);
            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                String s;
                while ((s = in.readLine()) != null) {
                    // convert "Ljava/lang/Object;" to "java.lang.Object"
                    String cName = Config.useJvmti ? typesToStr(s) : s;
                    classNames.add(cName);
                }
                in.close();
            } catch (Exception ex) {
                Messages.fatal(ex);
            }
        }
        if (beforeProc != null)
            beforeProc.destroy();
        return classNames;
    }

    /**
     * Converts and dumps the program's Java source files specified by property {@code petablox.src.path}
     * to HTML files in the directory specified by property {@code petablox.out.dir}.
     */
    public void HTMLizeJavaSrcFiles() {
        if (!HTMLizedJavaSrcFiles) {
            String srcPathName = Config.srcPathName;
            if (srcPathName == null)
                Messages.fatal(SRC_PATH_NOT_DEFINED);
            String[] srcDirNames = srcPathName.split(Utils.PATH_SEPARATOR);
            try {
                Java2HTML java2HTML = new Java2HTML();
                java2HTML.setMarginSize(4);
                java2HTML.setTabSize(4);
                java2HTML.setJavaDirectorySource(srcDirNames);
                java2HTML.setDestination(Config.outDirName);
                java2HTML.buildJava2HTML();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            HTMLizedJavaSrcFiles = true;
        }
    }
    
    /************************************************************************
     * Functions for printing methods and classes
     ************************************************************************/

    private void printClass(Type r) {
        System.out.println("*** Class: " + r);
        if (r instanceof ArrayType)
            return;
        SootClass c = ((RefType) r).getSootClass();
        for (SootMethod m : c.getMethods()) {
            printMethod(m);
        }
        /*for (jq_Method m : c.getDeclaredStaticMethods()) {
            printMethod(m);
        }*/
    }
    
    private void printMethod(SootMethod m) {
        System.out.println("Method: " + m);
        if (!m.isAbstract()) {
        	Body b = m.retrieveActiveBody();
            System.out.println(b.toString());
        }
    }

    /**
     * Prints the quadcode representation of the given method, if it is deemed reachable, and exits otherwise.
     */
    public void printMethod(String sign) {
        SootMethod m = getMethod(sign);
        if (m == null)
            Messages.fatal(METHOD_NOT_FOUND, sign);
        printMethod(m);
    }

    /**
     * Prints the quadcode representation of the given class, if it is deemed reachable, and exits otherwise.
     */
    public void printClass(String className) {
        RefLikeType c = getClass(className);
        if (c == null)
            Messages.fatal(CLASS_NOT_FOUND, className);
        printClass(c);
    }

    /**
     * Prints the quadcode representation of all reachable classes.
     */
    public void printAllClasses() {
        for (RefLikeType c : getClasses())
            printClass(c);
    }
}

