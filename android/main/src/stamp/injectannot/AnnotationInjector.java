package stamp.injectannot;

import soot.Scene;
import soot.SootClass;
import soot.util.NumberedSet;

import shord.project.analyses.JavaAnalysis;
import shord.program.Program;

import chord.project.Chord;

import java.util.*;
import java.util.jar.*;
import java.io.*;

@Chord(name="inject-annot")
public class AnnotationInjector extends JavaAnalysis
{
	private static boolean alreadyRun = false;
	
	private Class[] visitorClasses = new Class[]{
		ContentProviderAnnotation.class
		,NativeMethodAnnotation.class
		,NetSigAnnotation.class
		,WidgetAnnotation.class
	};

	private PrintWriter writer;

	public void run()
	{
		if(alreadyRun)
			return;
		alreadyRun = true;
		try{			
			String stampOutDir = System.getProperty("stamp.out.dir");
			File annotFile = new File(stampOutDir, "stamp_annotations.txt");
			String icdfFlag = System.getProperty("stamp.icdf");
			
			writer = new PrintWriter(new FileWriter(annotFile, true));
			Visitor[] visitors = new Visitor[visitorClasses.length];
			int i = 0;
			for(Class visitorClass : visitorClasses){
				Visitor v = (Visitor) visitorClass.newInstance();
				v.writer = writer;
				visitors[i++] = v; 
			}

			NumberedSet fklasses = frameworkClasses();
			for(SootClass klass : Program.g().getClasses()){
				if(fklasses.contains(klass))
					continue;
				for(Visitor v : visitors){
					v.visit(klass);
				}
			}
			
			writer.close();
		}catch(Exception e){
			throw new Error(e);
		}		
	}
	
	NumberedSet frameworkClasses()
	{
		Scene scene = Scene.v();
		NumberedSet frameworkClasses = new NumberedSet(scene.getClassNumberer());
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

	
}
