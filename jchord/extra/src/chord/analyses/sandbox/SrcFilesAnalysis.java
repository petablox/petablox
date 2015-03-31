/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.sandbox;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

import chord.util.Utils;
import chord.program.MethodSign;
import chord.program.Program;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "src-files-java"
)
public class SrcFilesAnalysis extends JavaAnalysis {
	public void run() {
		String methodsFileName = System.getProperty("chord.reach.methods.file");
		Iterable<jq_Method> methods;
		ProgramRel relReachableM;
		if (methodsFileName != null) {
			relReachableM = null;
			List<String> methodList = Utils.readFileToList(methodsFileName);
			Set<jq_Method> methodSet = new HashSet<jq_Method>(methodList.size());
			for (String s : methodList) {
				jq_Method m = Program.g().getMethod(MethodSign.parse(s));
				if (m == null)
					throw new RuntimeException("Method: " + s + " not found");
				methodSet.add(m);
			}
			methods = methodSet;
		} else {
			DomM domM = (DomM) ClassicProject.g().getTrgt("M");
			ClassicProject.g().runTask(domM);
			relReachableM = (ProgramRel) ClassicProject.g().getTrgt("reachableM");
			relReachableM.load();
			methods = relReachableM.getAry1ValTuples();
		}
		Set<String> fileNames = new HashSet<String>();
		Set<jq_Class> seenClasses = new HashSet<jq_Class>();
		long numBytecodes = 0;
		for (jq_Method m : methods) {
			jq_Class c = m.getDeclaringClass();
			if(Utils.prefixMatch(c.getName(), Config.checkExcludeAry))
				continue;
			byte[] bc = m.getBytecode();
			if (bc != null) {
				numBytecodes += bc.length;
				System.out.println("METHOD: " + m + " " + bc.length);
			} else	
				System.out.println("METHOD: " + m + " 0");
			
			if (seenClasses.add(c)) {
				if (c.getName().contains("$"))
					continue;
				String fileName = c.getSourceFileName();
				if (fileName == null) {
					System.out.println("WARNING: file not found for class: " + c);
					continue;
				}
				fileNames.add(fileName);
			}
		}
		for (jq_Class c : seenClasses)
			System.out.println("CLASS: " + c);
		if (methodsFileName == null)
			relReachableM.close();
		System.out.println("NUM BYTECODES: " + numBytecodes);
		for (String fileName : fileNames) {
			System.out.println("FILE: " + fileName);
		}
	}
}

