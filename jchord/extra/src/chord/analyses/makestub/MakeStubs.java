/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
 
 package chord.analyses.makestub;

import java.util.LinkedHashSet;
import java.io.*;

import chord.project.Chord;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.util.Utils;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;

/***
* Tool for generating stub code for a given class.
* Requires config option chord.stubs.toMake, which should be a list of classes 
* to generate harnesses for. Will write a set of classes with names of the form
* StubXYZ.java, to chord output dir.
* 
* For each class XYZ, the generated harness has one method, 
* exercise(), that takes an object of type XYZ and will call every public method
* on that object.
*/

@Chord(
		name = "MakeStubs",
		consumes = "M"
	)
public class MakeStubs extends JavaAnalysis {
	
	static LinkedHashSet<jq_Method> methods;

	public void run() {
		try {
			String[] classesToDump = Utils.toArray(System.getProperty("chord.stubs.toMake", ""));
			for(String s: classesToDump)
				dumpSkelForClass(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Dump a skeleton for a java class or interface
	 * @param skelJavaName
	 * @param cl
	 * @throws IOException
	 */
	static void dumpSkelForClass(String clName) throws IOException {
		
		jq_Class cl  =  (jq_Class) jq_Type.parseType(clName);

		if(cl == null) {
			System.err.println("ERR: no such class " + clName);
			return;
		} else
			cl.prepare();
		
		String fullName = cl.getName(); //possibly redundant?
		String shortName = fullName.substring(1 + fullName.lastIndexOf('.')); //if lastindex is -1, this still works
		PrintWriter out = OutDirUtils.newPrintWriter("Stub"+shortName+".java");
		out.println("/*This code is automatically generated for use as a stub/test harness");
		out.println(" * for the configuration debug tools.  */");
		out.println("package conf_analyzer.stubs;");
		out.println("public class Stub"+shortName+ " {");
		out.println("  public static void exercise(" + cl.getName()  + " inst) throws Exception {");
		
		//dump an exec for every runner
		for(jq_Method m: cl.getDeclaredInstanceMethods()) {
			if(m.isPublic() && ! "<init>".equals(m.getName().toString())) {
				out.print("    inst."+m.getName()+" (");
				//now the call
				StringBuilder args = new StringBuilder();
				jq_Type[] argTypes = m.getParamTypes();
				for(int i =1; i < argTypes.length; ++i) {
					jq_Type ty = argTypes[i];
					if(ty.isReferenceType())
						args.append("null,");
					else if(ty.equals(joeq.Class.jq_Primitive.BOOLEAN))
						args.append("false,");
					else if(ty.equals(joeq.Class.jq_Primitive.SHORT))
						args.append("(short) 0,");
					else
						args.append("0,");
				}
				if(args.length() > 0)
					out.print(args.substring(0, args.length() -1)); //remove comma at end of list
				out.println(");");
			}
		}

		
		out.println("  }\n}");
		out.close();
	}

}
