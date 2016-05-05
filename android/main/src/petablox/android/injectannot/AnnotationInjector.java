package petablox.android.injectannot;

import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.util.NumberedSet;

import petablox.project.analyses.JavaAnalysis;
import petablox.program.Program;

import petablox.project.Petablox;

import java.util.*;
import java.util.jar.*;
import java.io.*;

@Petablox(name="inject-annot")
public class AnnotationInjector extends JavaAnalysis
{
	static abstract class Visitor
	{
		private PrintWriter writer;
		protected abstract void visit(SootClass klass);
		
		protected void writeAnnotation(String methSig, String from, String to)
		{
			writer.println(methSig + " " + from + " " + to);
		}
	}
	
	private Class[] visitorClasses = new Class[]{
		ContentProviderAnnotation.class
		,NativeMethodAnnotation.class
	};

	private PrintWriter writer;

	public void run()
	{
		try{			
			String stampOutDir = System.getProperty("stamp.out.dir");
			File annotFile = new File(stampOutDir, "stamp_annotations.txt");
			String icdfFlag = System.getProperty("stamp.icdf");
			
			writer = new PrintWriter(new FileWriter(annotFile, true));
			
			if ("true".equals(icdfFlag)) {
				visitorClasses = Arrays.copyOf(visitorClasses, visitorClasses.length + 1);
			    visitorClasses[visitorClasses.length-1] = InterComponentInstrument.class;
			}
			Visitor[] visitors = new Visitor[visitorClasses.length];
			int i = 0;
			for(Class visitorClass : visitorClasses){
				Visitor v = (Visitor) visitorClass.newInstance();
				v.writer = writer;
				visitors[i++] = v; 
			}

			NumberedSet fklasses = frameworkClasses();
			for(RefLikeType r : Program.g().getClasses()){               //PRT change to refliketype
				if(r instanceof RefType){
					SootClass klass = ((RefType)r).getSootClass();
					if(fklasses.contains(klass))
						continue;
					for(Visitor v : visitors){
						v.visit(klass);
					}
				}
					
			}
			
			if ("true".equals(icdfFlag)) {
                //at the very end we need to add load/store stmt to put/getUnknown to reason unknown src
			    InterComponentInstrument.injectUnknownSrc();
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
