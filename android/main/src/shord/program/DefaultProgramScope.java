package shord.program;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import stamp.analyses.SootUtils;

import java.util.regex.Pattern;
import java.util.*;
import java.io.*;

public class DefaultProgramScope extends ProgramScope
{
	private Set<SootMethod> annotatedMethods = new HashSet();
	private Pattern excludePattern;

	public DefaultProgramScope(Program prog)
	{
		super(prog);
		identifyMethodsWithAnnotations();
		this.excludePattern = readExcludePattern();
	}

	private Pattern readExcludePattern()
	{
		String excludeFile = System.getProperty("stamp.scope.excludefile");
		if(excludeFile == null)
			return null;
		File f = new File(excludeFile);
		if(!f.exists()){
			System.out.println("Exclusion file "+excludeFile+" does not exist.");
			return null;
		}
		try{
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			StringBuilder builder = new StringBuilder();
			boolean first = true;
			while((line = reader.readLine()) != null){
				if(first)
					first = false;
				else
					builder.append("|");
				builder.append(line);
			}
			reader.close();
			return Pattern.compile(builder.toString());
		} catch(IOException e){
			throw new Error(e);
		}
	}

	public boolean exclude(SootMethod method)
	{
		boolean excluded = false;
		if(prog.isStub(method))
			excluded = !annotatedMethods.contains(method);
		else if(excludePattern != null)
			excluded = excludePattern.matcher(method.getDeclaringClass().getName()).matches();
		if(excluded)
			System.out.println("Excluding "+method+" from analysis.");
		return excluded;
	}

	public boolean ignoreStub()
	{
		return true;
	}
	
	private void identifyMethodsWithAnnotations()
	{
		Scene scene = Scene.v();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(System.getProperty("stamp.out.dir"), "stamp_annotations.txt")));
			String line;
			while((line = reader.readLine()) != null){
				String chordMethodSig = line.split(" ")[0];
				int atSymbolIndex = chordMethodSig.indexOf('@');
				String className = chordMethodSig.substring(atSymbolIndex+1);
				if(scene.containsClass(className)){
					SootClass klass = scene.getSootClass(className);
					String subsig = SootUtils.getSootSubsigFor(chordMethodSig.substring(0,atSymbolIndex));
					SootMethod meth = klass.getMethod(subsig);
					annotatedMethods.add(meth);
				}
			}
			reader.close();
		}catch(IOException e){
			throw new Error(e);
		}
	}
	
}