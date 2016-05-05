package petablox.android.harnessgen;

import java.util.*;
import java.util.jar.*;
import java.io.*;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.DexFile; 

import soot.Modifier;

/**
 * @author Saswat Anand
 **/
public class App
{
	protected final Map<String,List<String>> components = new HashMap();
	protected final Map<String,List<String>> customGUIs = new HashMap();

	private final Set<String> activities = new HashSet();
	private final Set<String> otherComps = new HashSet();
	private final Set<String> xmlCallbacks = new HashSet();
	private final Set<String> guiElems = new HashSet();

	public App(String classPath, String androidJar, String outDir)
	{
		File layoutDir = new File(outDir, "res/layout");
		new ParseLayout().process(layoutDir, xmlCallbacks, guiElems);

		File manifestFile = new File(outDir, "AndroidManifest.xml");				
		new ParseManifest().process(manifestFile, activities, otherComps);

		process(classPath);
	}

	private void process(String classPath)
	{
		try{
			for(String cpElem : classPath.split(":")) {
				File f = new File(cpElem);
				if(!(f.exists())){
					System.out.println("WARNING: "+cpElem +" does not exists!");
					continue;
				}
				if(cpElem.endsWith(".jar")){
					continue;
				} 
				if(cpElem.endsWith(".apk")){
					DexFile dexFile = new DexFile(f);
					for (ClassDefItem defItem : dexFile.ClassDefsSection.getItems()) {
						String className = defItem.getClassType().getTypeDescriptor();
						if(className.charAt(0) == 'L'){
							int len = className.length();
							assert className.charAt(len-1) == ';';
							className = className.substring(1, len-1);
						}
						className = className.replace('/','.');
						String tmp = className.replace('$', '.');
						if(activities.contains(tmp))
							components.put(className, findCallbackMethods(defItem, xmlCallbacks));
						else if(otherComps.contains(tmp))
							components.put(className, Collections.EMPTY_LIST);
						else if(guiElems.contains(tmp))
							customGUIs.put(className, findConstructors(defItem));
					}
				} else 
					assert false : cpElem;
			}
		} catch(IOException e){
			throw new Error(e);
		}
	}


	private List<String> findCallbackMethods(ClassDefItem classDef, Set<String> callbacks)
	{
		List<String> ret = null;
		ClassDataItem classData = classDef.getClassData();
		for(ClassDataItem.EncodedMethod method : classData.getVirtualMethods()) {
			String name = method.method.getMethodName().getStringValue();
			if(!callbacks.contains(name))
				continue;
			if(!Modifier.isPublic(method.accessFlags))
				continue;
			char c = method.method.getPrototype().getReturnType().getTypeDescriptor().charAt(0);
			if(c != 'V') //must be void type
				continue;
			if(method.method.getPrototype().getParameters() == null){
				continue;
			}
			List<TypeIdItem> paramTypes = method.method.getPrototype().getParameters().getTypes();
			if(paramTypes.size() != 1)
				continue;
			String p0Type = paramTypes.get(0).getTypeDescriptor();
			if(!p0Type.equals("Landroid/view/View;"))
				continue;
			
			if(ret == null)
				ret = new LinkedList();
			ret.add(name);
		}		
		return ret == null ? Collections.EMPTY_LIST : ret;
	} 
	
	private List<String> findConstructors(ClassDefItem classDef)
	{
		List<String> ret = new ArrayList();
		//System.out.println("custom GUI "+classDef); 
		ClassDataItem classData = classDef.getClassData();
		for(ClassDataItem.EncodedMethod method : classData.getDirectMethods()) {
			String name = method.method.getMethodName().getStringValue();
			if(!name.equals("<init>"))
				continue;
			//if(!Modifier.isPublic(method.accessFlags))
			//continue;
			//System.out.println("sig: "+name+"*"+method.method.getShortMethodString());
			ret.add(method.method.getShortMethodString());
		}
		return ret;
	}

	
}
