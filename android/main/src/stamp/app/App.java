package stamp.app;

import java.util.*;
import java.util.jar.*;
import java.io.*;

import soot.Modifier;
import soot.jimple.Stmt;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.AssignStmt;
import soot.Local;
import soot.Value;
import soot.SootClass;
import soot.SootMethod;
import soot.Body;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.dexpler.Util;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;


import shord.program.Program;

/**
 * @author Saswat Anand
 **/
public class App
{
	private List<Component> comps = new ArrayList();
	private Map<Integer,Layout> layouts = new HashMap();
	private Set<String> permissions = new HashSet();
	private List<String> classes = new ArrayList();
	private Set<String> frameworkClasses = new HashSet();
	private PublicXml publicXml;
	private StringXml stringXml;

	private String pkgName;
	private String version;
	private String iconPath;
	private String apkPath;

	public static App readApp(String apkPath, String apktoolOutDir)
	{
		App app = new App(apkPath, apktoolOutDir);
		return app;
	}

	public App(String apkPath, String apktoolOutDir)
	{
		assert apkPath.endsWith(".apk");
		this.apkPath = apkPath;

		File manifestFile = new File(apktoolOutDir, "AndroidManifest.xml");				
		ParseManifest pmf = new ParseManifest(manifestFile, this);

		File resDir = new File(apktoolOutDir, "res");
		this.publicXml = new PublicXml(new File(resDir, "values/public.xml"));
		this.stringXml = new StringXml(new File(resDir, "values/strings.xml"));

		List<Layout> layouts = parseLayouts(resDir);
		
		collectClassNames();
		computeFrameworkClasses();		
		process(apktoolOutDir, layouts);
	}

	public void process(String apktoolOutDir, List<Layout> layouts)
	{
		if(iconPath != null){
			if(iconPath.startsWith("@drawable/")){
				String icon = iconPath.substring("@drawable/".length()).concat(".png");
				File f = new File(apktoolOutDir.concat("/res/drawable"), icon);
				if(f.exists())
					iconPath = f.getPath();
				else {
					f = new File(apktoolOutDir.concat("/res/drawable-hdpi"), icon);
					if(f.exists())
						iconPath = f.getPath();
					else
						iconPath = null;
				}
			} else
				iconPath = null;
		}

		Set<String> widgetNames = new HashSet();
		for(Layout layout : layouts){
			for(Widget widget : layout.widgets)
				widgetNames.add(widget.getClassName());
		}

		List<Component> comps = this.comps;
		this.comps = new ArrayList();

		Set<String> compNames = new HashSet();
		for(Component c : comps){
			//System.out.println("@@ "+c.name);
			compNames.add(c.name);
		}

		filterDead(compNames, widgetNames);

		System.out.println("^^ "+compNames.size());
		
		for(Component c : comps){
			if(compNames.contains(c.name))
				this.comps.add(c);
		}
		
		for(Layout layout : layouts){
			this.layouts.put(layout.id, layout);
			
			List<Widget> widgets = layout.widgets;
			for(Iterator<Widget> it = widgets.iterator(); it.hasNext();){
				Widget widget = it.next();
				String widgetClassName = widget.getClassName();
				
				//check if it is a custom widget
				if(widgetNames.contains(widgetClassName)){
					widget.setCustom();
					continue;
				}

				//check if it is a framework widget
				if(frameworkClasses.contains(widgetClassName))
					continue;
				boolean isFrameworkWidget = false;
				widgetClassName = "."+widgetClassName;
				for(String fClass : frameworkClasses){
					if(fClass.endsWith(widgetClassName)){
						isFrameworkWidget = true;
						widget.setClassName(fClass);
						break;
					}
				}

				if(!isFrameworkWidget)
					it.remove(); //remove it if it neither a custom or framework widget
			}
		}
	}

	public List<Component> components()
	{
		return comps;
	}

	public Set<String> permissions()
	{
		return permissions;
	}

	public Layout layoutWithId(int id)
	{
		return layouts.get(id);
	}

	public void setPackageName(String pkgName)
	{
		this.pkgName = pkgName;
	}

	public String getPackageName()
	{
		return this.pkgName;
	}
	
	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getVersion()
	{
		return this.version;
	}

	public void setIconPath(String icon)
	{
		this.iconPath = icon;
	}
	
	public String getIconPath()
	{
		return iconPath;
	}

	public String apkPath()
	{
		return apkPath;
	}

	public List<String> allClassNames()
	{
		return classes;
	}

	public Set<String> allFrameworkClassNames()
	{
		return frameworkClasses;
	}

	public Collection<Layout> allLayouts()
	{
		return layouts.values();
	}

	private void collectClassNames()
	{
		try{
			File f = new File(apkPath);
			DexBackedDexFile d = DexFileFactory.loadDexFile(f, 1);
			for (ClassDef c : d.getClasses()) {
				String name = Util.dottedClassName(c.getType());
				classes.add(name);
			}
		} catch(IOException e){
			throw new Error(e);
		}
	}

	private void filterDead(Set<String> compNames, Set<String> widgetNames)
	{
		Set<String> compNamesAvailable = new HashSet();
		Set<String> widgetNamesAvailable = new HashSet();

		for (String className : classes) {
			if(compNames.contains(className)) {
				compNamesAvailable.add(className);
				//System.out.println("%% "+tmp);
			}
			else if(widgetNames.contains(className))
				widgetNamesAvailable.add(className);
		}

		compNames.clear();
		compNames.addAll(compNamesAvailable);

		widgetNames.clear();
		widgetNames.addAll(widgetNamesAvailable);
	}

	public String toString()
	{
		StringBuilder builder = new StringBuilder("{");

		builder.append("\"package\": \""+pkgName+"\", ");
		builder.append("\"version\": \""+version+"\", ");

		int n = comps.size();
		builder.append("\"comps\" : [");
		int i = 0;
		for(Component c : comps){
			builder.append(c.toString());
			if(i < (n-1))
				builder.append(", ");
			i++;
		}
		builder.append("], ");

		builder.append("\"perms\": [");
		i = 0;
		n = permissions.size();
		for(String perm : permissions){
			builder.append("\""+perm+"\"");
			if(i < (n-1))
				builder.append(", ");
			i++;
		}
		builder.append("]");

		builder.append("}");
		return builder.toString();
	}

	public void findLayouts()
	{
		for(Component comp : comps)
			findLayoutsFor(comp);
	}

	private void findLayoutsFor(Component comp)
	{
		if(comp.type != Component.Type.activity)
			return;

		SootClass activity = Program.g().scene().getSootClass(comp.name);
		
		for(SootMethod m : activity.getMethods()){
			if(!m.isConcrete())
				continue;
			Body body = m.retrieveActiveBody();
			SimpleLocalDefs sld = null;
			for(Unit u : body.getUnits()){
				Stmt s = (Stmt) u;
				if(!s.containsInvokeExpr())
					continue;
				InvokeExpr ie = s.getInvokeExpr();
				if(!ie.getMethod().getSignature().equals("<android.app.Activity: void setContentView(int)>"))
					continue;

				if(m.isStatic()){
					System.out.println("WARN: setContentView called in a static method "+m.getSignature());
					continue;
				} 

				Value rcvr = ((InstanceInvokeExpr) ie).getBase();
				Local thisLocal = body.getThisLocal();
				if(!rcvr.equals(thisLocal)){
					if(sld == null)
						sld = new SimpleLocalDefs(new ExceptionalUnitGraph(body));

					boolean warn = true;
					if(rcvr instanceof Local){
						warn = false;
						for(Unit def : sld.getDefsOfAt((Local) rcvr, u)){
							if(!(def instanceof AssignStmt) || !thisLocal.equals(((AssignStmt) def).getRightOp())){
								warn = true;
								break;
							}
						}
					}

					if(warn){
						System.out.println("WARN: rcvr of setContentView is not equal to ThisLocal of method "+m.getSignature());
						continue;
					}
				}

				Value arg = ie.getArg(0);
				if(arg instanceof Constant){
					int layoutId = ((IntConstant) arg).value;
					Layout layout = layoutWithId(layoutId);
					if(layout != null){
						comp.addLayout(layout);
						System.out.println("Layout: "+comp.name+" "+layout.fileName);
					}
					else
						System.out.println("WARN: Did not found layout for id = "+layoutId);
				} else {
					if(sld == null)
						sld = new SimpleLocalDefs(new ExceptionalUnitGraph(body));

					//System.out.println("WARN: Argument of setContentView is not constant");					
					for(Unit def : sld.getDefsOfAt((Local) arg, u)){
						if(!(def instanceof AssignStmt))
							continue;
						Value rhs = ((AssignStmt) def).getRightOp();
						if(!(rhs instanceof IntConstant))
							continue;
						int layoutId = ((IntConstant) rhs).value;
						Layout layout = layoutWithId(layoutId);
						if(layout != null){
							comp.addLayout(layout);
							System.out.println("Layout: "+comp.name+" "+layout.fileName);
						}
						else
							System.out.println("WARN: Did not found layout for id = "+layoutId);
					}
				}
			}
		}
	}

	void computeFrameworkClasses()
	{
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
					frameworkClasses.add(entryName);
				}
			}
		}
	}

	List<Layout> parseLayouts(File resDir)
	{
		File layoutDir = new File(resDir, "layout");
		File[] layoutFiles = layoutDir.listFiles(new FilenameFilter(){
				public boolean accept(File dir, String name){
					return name.endsWith(".xml");
				}
			});
		
		List<Layout> layouts = new ArrayList();
		Map<String,Layout> nameToLayout = new HashMap();
		if(layoutFiles != null){
			for(File lf : layoutFiles){
				String layoutFileName = lf.getName();
				layoutFileName = layoutFileName.substring(0, layoutFileName.length()-4); //drop ".xml"
				//System.out.println("++ "+layoutFileName);
				Integer id = publicXml.layoutIdFor(layoutFileName);
				Layout layout = new Layout(id, lf, publicXml, stringXml);
				layouts.add(layout);
				nameToLayout.put(layoutFileName, layout);
			}
		}
		
		/*
		for(Layout layout : layouts){
			//System.out.println("main layout: "+layout.fileName);
			for(String includedLayoutName : layout.includedLayouts){
				Layout includedLayout = nameToLayout.get(includedLayoutName);
				//System.out.println("included layout: "+includedLayout.fileName);
				//System.out.println("adding "+includedLayout.widgets.size()+" widgets and "+includedLayout.callbacks+" callbacks.");
				layout.widgets.addAll(includedLayout.widgets);
				layout.callbacks.addAll(includedLayout.callbacks);
			}
		}
		*/
		for(Layout layout : layouts){
			for(Widget widget : layout.widgets){
				List<Widget> childrenWidgets = new ArrayList();
				for(Object c : layout.widgetToChildren.get(widget)){
					if(c instanceof String){
						Layout includedLayout = nameToLayout.get((String) c);
						Widget includedLayoutRootWidget = includedLayout.rootWidget;
						//System.out.println(includedLayoutRootWidget+" "+includedLayout+" "+c);
						if(includedLayoutRootWidget.getClassName().equals("merge")){
							for(Object l : includedLayout.widgetToChildren.get(includedLayoutRootWidget))
								childrenWidgets.add((Widget) l);
						} else 
							childrenWidgets.add(includedLayoutRootWidget);
						layout.callbacks.addAll(includedLayout.callbacks); //TODO: multiple levels of include
					} else{
						Widget childWidget = (Widget) c;
						childrenWidgets.add(childWidget);
					}
				}
				widget.setChildren(childrenWidgets);
			}
		}

		return layouts;
	}


}
