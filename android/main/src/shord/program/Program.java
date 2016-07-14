package shord.program;

import soot.*;
import soot.options.Options;
import soot.util.Chain;
import soot.util.ArrayNumberer;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.NewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.tagkit.Tag;
import soot.toolkits.scalar.LocalSplitter;
import soot.dexpler.DalvikThrowAnalysis;
import soot.util.NumberedSet;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.*;
import java.io.*;

import shord.analyses.ContainerTag;

import stamp.app.App;

/*
 * @author Saswat Anand
 */
public class Program
{
	private static Program g;
	private SootMethod mainMethod;
	private NumberedSet frameworkClasses;
	private NumberedSet stubMethods;
	private ProgramScope scope;
	private App app;
	private Set<String> harnessClasses;
	private List<SootMethod> defaultEntryPoints = new ArrayList();

	public static Program g()
	{
		if(g == null){
			g = new Program();
		}
		return g;
	}

	private Program()
	{
	}

	public void build(Set<String> harnesses, String widgetsClassName)
	{
        try {
			StringBuilder options = new StringBuilder();
			options.append("-full-resolver");
			options.append(" -allow-phantom-refs");
			options.append(" -src-prec apk");
			//options.append(" -p jb.tr use-older-type-assigner:true"); 
			//options.append(" -p cg implicit-entry:false");
			options.append(" -force-android-jar "+System.getProperty("user.dir"));
			options.append(" -soot-classpath "+System.getProperty("stamp.android.jar")+File.pathSeparator+System.getProperty("chord.class.path"));
			//options.append(" -f jimple");
			options.append(" -f none");
			options.append(" -d "+ System.getProperty("stamp.out.dir")+File.separator+"jimple");

			if (!Options.v().parse(options.toString().split(" ")))
				throw new CompilationDeathException(
													CompilationDeathException.COMPILATION_ABORTED,
													"Option parse error");
			//options.set_full_resolver(true);
			//options.set_allow_phantom_refs(true);
			
			//options.set_soot_classpath();

            Scene.v().loadBasicClasses();

			/*
			for(String h : harnesses){
				Scene.v().loadClassAndSupport(h);
			}

			Scene.v().loadClassAndSupport(widgetsClassName);
			*/
		
			this.harnessClasses = harnesses;
			for(String h : harnesses){
				//System.out.println("Loading harness class "+h);
				Scene.v().loadClassAndSupport(h);
			}

			//String mainClassName = System.getProperty("chord.main.class");
			//SootClass mainClass = Scene.v().loadClassAndSupport(mainClassName);
			//Scene.v().setMainClass(mainClass);

			//mainMethod = mainClass.getMethod(Scene.v().getSubSigNumberer().findOrAdd("void main(java.lang.String[])"));

			//Scene.v().setEntryPoints(Arrays.asList(new SootMethod[]{mainMethod}));


			Scene.v().loadDynamicClasses();

			LocalSplitter localSplitter = new LocalSplitter(DalvikThrowAnalysis.v());

			for(SootClass klass : Scene.v().getClasses()){
				for(SootMethod meth : klass.getMethods()){
					if(!meth.isConcrete())
						continue;
					localSplitter.transform(meth.retrieveActiveBody());
				}
			}

        } catch (Exception e) {
			throw new Error(e);
        }
	}

	public void setMainClass(String harness)
	{
		SootClass mainClass = Scene.v().getSootClass(harness);
		mainMethod = mainClass.getMethod(Scene.v().getSubSigNumberer().findOrAdd("void main(java.lang.String[])"));
		Scene.v().setMainClass(mainClass);

		defaultEntryPoints.add(mainMethod);

		//workaround soot bug
		if(mainClass.declaresMethodByName("<clinit>"))
			defaultEntryPoints.add(mainClass.getMethodByName("<clinit>"));
	}

	public void runCHA()
	{
		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseFastHierarchy();
		G.v().ClassHierarchy_classHierarchyMap.clear();
		
		List<SootMethod> entryPoints = new ArrayList(defaultEntryPoints);
		for(String h : harnessClasses){
			if(!h.startsWith("stamp.harness.LayoutInflater$"))
				continue;
			SootMethod init = Scene.v().getSootClass(h).getMethod("void <init>(android.content.Context)");
			entryPoints.add(init);
		}
		Scene.v().setEntryPoints(entryPoints);

		Transform chaTransform = PackManager.v().getTransform( "cg.cha" );
		String defaultOptions = chaTransform.getDefaultOptions();
		StringBuilder options = new StringBuilder();
		options.append("enabled:true");
		options.append(" verbose:true");
		options.append(" "+defaultOptions);
		System.out.println("CHA options: "+options.toString());
		chaTransform.setDefaultOptions(options.toString());
		chaTransform.apply();	
	}

	public void runSpark(){
		runSpark("");
	}

	public void runSpark(String specialOptions)
	{
		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseFastHierarchy();
		G.v().MethodPAG_methodToPag.clear();
		G.v().ClassHierarchy_classHierarchyMap.clear();

		Scene.v().setEntryPoints(defaultEntryPoints);

		//run spark
		Transform sparkTransform = PackManager.v().getTransform( "cg.spark" );
		String defaultOptions = sparkTransform.getDefaultOptions();
		StringBuilder options = new StringBuilder();
		options.append("enabled:true");
		options.append(" verbose:true");
		options.append(" simulate-natives:false");//our models should take care of this
		if(specialOptions.trim().length() > 0)
			options.append(" "+specialOptions);
		//options.append(" dump-answer:true");
		options.append(" "+defaultOptions);
		System.out.println("spark options: "+options.toString());
		sparkTransform.setDefaultOptions(options.toString());
		sparkTransform.apply();	
	}
	
	public void printAllClasses()
	{
		for(SootClass klass : Scene.v().getClasses()){
			PackManager.v().writeClass(klass);
			//System.out.println(klass.getName());
		}
	}
	
	public void printClass(String className)
	{
	}
	
	public Chain<SootClass> getClasses()
	{
		return Scene.v().getClasses();
	}
	
	public Iterator<SootMethod> getMethods()
	{
		return Scene.v().getMethodNumberer().iterator();
	}

	public ArrayNumberer<Type> getTypes()
	{
		return (ArrayNumberer<Type>) Scene.v().getTypeNumberer();
	}

	public Scene scene()
	{
		return Scene.v();
	}
	
	/*
	public ArrayNumberer<SootField> getFields()
	{
		return (ArrayNumberer<SootField>) Scene.v().getFieldNumberer();
		}*/

	public SootMethod getMainMethod()
	{
		return mainMethod;
	}

	public static SootMethod containerMethod(Stmt stmt)
	{
		for(Tag tag : stmt.getTags()){
			if(tag instanceof ContainerTag)
				return ((ContainerTag) tag).method;
		}
		return null;
	}

	public static String unitToString(Unit u) {
		SootMethod m = (u instanceof Stmt) ? containerMethod((Stmt) u) : null;
		return (m == null) ? u.toString() : u + "@" + m;
	}
	
	public App app()
	{
		if(app == null){
			String apktoolOutDir = System.getProperty("stamp.apktool.out.dir");
			String apkPath = System.getProperty("stamp.apk.path");
			app = App.readApp(apkPath, apktoolOutDir);
			app.findLayouts();
			//System.out.println(app.toString());
		}
		return app;
	}

	public NumberedSet frameworkClasses()
	{
		if(frameworkClasses != null)
			return frameworkClasses;

		Scene scene = Scene.v();
		frameworkClasses = new NumberedSet(scene.getClassNumberer());
		String androidJar = System.getProperty("stamp.android.jar");
		JarFile archive;
		try{
			archive = new JarFile(androidJar);
		}catch(IOException e){
			throw new Error(e);
		}
		for (Enumeration entries = archive.entries(); entries.hasMoreElements();) {
			JarEntry entry = (JarEntry) entries.nextElement();
			String entryName = entry.getName();
			int extensionIndex = entryName.lastIndexOf('.');
			if (extensionIndex >= 0) {
				String entryExtension = entryName.substring(extensionIndex);
				if (".class".equals(entryExtension)) {
					entryName = entryName.substring(0, extensionIndex);
					entryName = entryName.replace('/', '.');
					if(scene.containsClass(entryName))
						frameworkClasses.add(scene.getSootClass(entryName));
				}
			}
		}
		return frameworkClasses;
	}
	
	public boolean isFrameworkClass(SootClass k)
	{
		return frameworkClasses().contains(k);
	}

	public boolean exclude(SootMethod m)
	{
		if(scope != null)
			return scope.exclude(m);
		else
			return false;
	}

	public boolean isStub(SootMethod m)
	{
		if(stubMethods == null)
			identifyStubMethods();
		return stubMethods.contains(m);
	}
	
	public boolean ignoreStub()
	{
		return scope != null ? scope.ignoreStub() : false;
	}

	private void identifyStubMethods()
	{
		stubMethods = new NumberedSet(Scene.v().getMethodNumberer());
		Iterator<SootMethod> mIt = getMethods();
		while(mIt.hasNext()){
			SootMethod m = mIt.next();
			if(checkIfStub(m)){
				System.out.println("Stub: "+m.getSignature());
				stubMethods.add(m);
			}
		}		
	}

	private boolean checkIfStub(SootMethod method)
	{
		if(!method.isConcrete())
			return false;
		PatchingChain<Unit> units = method.retrieveActiveBody().getUnits();
		Unit unit = units.getFirst();
		while(unit instanceof IdentityStmt)
			unit = units.getSuccOf(unit);

		//if method is <init>, then next stmt could be a call to super.<init>
		if(method.getName().equals("<init>")){
			if(unit instanceof InvokeStmt){
				if(((InvokeStmt) unit).getInvokeExpr().getMethod().getName().equals("<init>"))
					unit = units.getSuccOf(unit);
			}
		}

		if(!(unit instanceof AssignStmt))
			return false;
		Value rightOp = ((AssignStmt) unit).getRightOp();
		if(!(rightOp instanceof NewExpr))
			return false;
		//System.out.println(method.retrieveActiveBody().toString());
		if(!((NewExpr) rightOp).getType().toString().equals("java.lang.RuntimeException"))
			return false;
		Local e = (Local) ((AssignStmt) unit).getLeftOp();
		
		//may be there is an assignment (if soot did not optimized it away)
		Local f = null;
		unit = units.getSuccOf(unit);
		if(unit instanceof AssignStmt){
			f = (Local) ((AssignStmt) unit).getLeftOp();
			if(!((AssignStmt) unit).getRightOp().equals(e))
				return false;
			unit = units.getSuccOf(unit);
		}
		//it should be the call to the constructor
		Stmt s = (Stmt) unit;
		if(!s.containsInvokeExpr())
			return false;
		if(!s.getInvokeExpr().getMethod().getSignature().equals("<java.lang.RuntimeException: void <init>(java.lang.String)>"))
			return false;
		unit = units.getSuccOf(unit);
		if(!(unit instanceof ThrowStmt))
			return false;
		Immediate i = (Immediate) ((ThrowStmt) unit).getOp();
		return i.equals(e) || i.equals(f);
	}

	public void setScope(ProgramScope ps)
	{
		this.scope = ps;
	}
	
}
