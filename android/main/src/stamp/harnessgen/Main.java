package stamp.harnessgen;

import java.io.*;
import java.util.*;

import stamp.app.App;
import stamp.app.Component;
import stamp.app.Layout;
import stamp.app.Widget;

import soot.Scene;
import soot.CompilationDeathException;
import soot.options.Options;
import soot.Modifier;
import soot.SootClass;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import com.google.gson.stream.JsonWriter;

/*
* @author Saswat Anand
*/
public class Main
{
	private static App app;

	public static void main(String[] args) throws Exception
	{
		String apktoolOutDir = System.getProperty("stamp.apktool.out.dir");
		String apkPath = System.getProperty("stamp.apk.path");
		String driverDirName = System.getProperty("stamp.driver.dir");
		String androidJar = System.getProperty("stamp.android.jar");
		String harnessListFile = System.getProperty("stamp.harnesslist.file");
		int numCompsPerHarness = Integer.parseInt(System.getProperty("stamp.max.harness.size"));

		app = App.readApp(apkPath, apktoolOutDir);
		List<Component> comps = app.components();
		initSoot(apkPath, androidJar, comps, app.allLayouts());
		app.findLayouts();

		//dump the app description
		dumpApp(app.toString());
		dumpClassHierarchy(app);
		
		File driverDir = new File(driverDirName);//, "stamp/harness");
		//driverDir.mkdirs();
		
		PrintWriter writer = new PrintWriter(new FileWriter(new File(harnessListFile)));

		int numComps = comps.size();
		System.out.println("number of components = "+numComps);
		int harnessCount = 0;
		int i = 0;
		while(i < numComps){
			harnessCount++;
			String harnessClassName = "stamp.harness.Main"+harnessCount;
			writer.println(harnessClassName);
			Harness h = new Harness(harnessClassName, comps);
			for(int j = 0; j < numCompsPerHarness && i < numComps; j++, i++){
				Component comp = comps.get(i);
				h.addComponent(comp);
			}
			writeClass(h.getFinalSootClass(), driverDir);
		}
		writer.close();

		writeClass(new GenerateAbstractInflaterClass(app).getFinalSootClass(), driverDir);

		for(Layout layout : app.allLayouts()){
			String rootWidgetClassName = layout.rootWidget.getClassName();
			if(rootWidgetClassName.equals("merge"))
				continue;
			if(rootWidgetClassName.equals("PreferenceScreen"))
				continue;
			SootClass rootWidgetClass = Scene.v().getSootClass(rootWidgetClassName);
			if(rootWidgetClass.isPhantom()){
				System.out.println("Phantom RootWidgetClass "+rootWidgetClassName);
				continue;
			}
			writeClass(new GenerateInflaterClass(layout).getFinalSootClass(), driverDir);
		}

		//SootClass viewClass = new GenerateInflaterClass(app).getFinalSootClass();
		//writeClass(viewClass, driverDirName);

		//SootClass gClass = new GClass(app).getFinalSootClass();
		//writeClass(gClass, driverDirName);

		//GuiFix gfix = new GuiFix(app, gClass);
		//gfix.perform();
	}


	private static void dumpApp(String appDesc) throws IOException
	{
		String stampOutDir = System.getProperty("stamp.out.dir");
		PrintWriter writer = new PrintWriter(new FileWriter(new File(stampOutDir, "app.json")));
		writer.println(appDesc);
		writer.close();
	}

	private static void dumpClassHierarchy(App app) throws IOException
	{
		String stampOutDir = System.getProperty("stamp.out.dir");
		JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(new File(stampOutDir, "hierarchy.json"))));
		writer.setIndent("  ");
		Scene scene = Scene.v();		

		writer.beginObject();

		//write app classes
		writer.name("app");
		writer.beginArray();
		Set<String> frameworkClasses = app.allFrameworkClassNames();
		List<SootClass> workList = new ArrayList();
		for(String className : app.allClassNames()){
			if(!scene.containsClass(className))
				continue;

			SootClass klass = scene.getSootClass(className);
			SootClass superClass = klass.getSuperclass();
			String superClassName = superClass.getName();
			if(frameworkClasses.contains(superClassName))
				workList.add(superClass);
		
			writer.beginObject();
			writer.name("class").value(className);
			writer.name("super").value(superClassName);
			writer.endObject();
		}
		writer.endArray();

		//write framework classes
		writer.name("framework");
		writer.beginArray();
		Set<SootClass> visited = new HashSet();
		while(!workList.isEmpty()){
			SootClass klass = workList.remove(0);
			if(visited.contains(klass))
				continue;
			visited.add(klass);
			if(!klass.hasSuperclass())
				continue;
			SootClass superClass = klass.getSuperclass();
			writer.beginObject();
			writer.name("class").value(klass.getName());
			writer.name("super").value(superClass.getName());
			writer.endObject();
		}
		writer.endArray();

		writer.endObject();
		writer.close();
	}

	private static void writeClass(SootClass klass, File driverDir) throws IOException
	{
		File file = new File(driverDir, klass.getName().replace('.','/').concat(".class"));
		file.getParentFile().mkdirs();
        OutputStream streamOut = new JasminOutputStream(new FileOutputStream(file));
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(klass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
	}

	private static void initSoot(String apkPath, String androidJar, List<Component> comps, Collection<Layout> layouts)
	{
        try {
			StringBuilder options = new StringBuilder();
			options.append("-allow-phantom-refs");
			options.append(" -src-prec apk");
			//options.append(" -p jb.tr use-older-type-assigner:true"); 
			//options.append(" -p cg implicit-entry:false");
			options.append(" -force-android-jar "+System.getProperty("user.dir"));
			options.append(" -soot-classpath "+androidJar+File.pathSeparator+apkPath);
			//options.append(" -f jimple");
			options.append(" -f none");

			if (!Options.v().parse(options.toString().split(" ")))
				throw new CompilationDeathException(
													CompilationDeathException.COMPILATION_ABORTED,
													"Option parse error");
            Scene.v().loadBasicClasses();

			for(Component c : comps){
				Scene.v().loadClassAndSupport(c.name);
			}
			
			for(Layout layout : layouts){
				for(Widget widget : layout.allWidgets())
					Scene.v().loadClassAndSupport(widget.getClassName());
			}
			

			Scene.v().loadDynamicClasses();
        } catch (Exception e) {
			throw new Error(e);
        }
	}
}
