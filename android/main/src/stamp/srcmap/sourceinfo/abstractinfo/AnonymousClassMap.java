package stamp.srcmap.sourceinfo.abstractinfo;

import shord.program.Program;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.AssignStmt;
import soot.jimple.NewExpr;
import stamp.srcmap.sourceinfo.SourceInfo;

import java.util.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;

public class AnonymousClassMap {
	private Map<String,List<SootClass>> fNameToClasses = new HashMap();
	private Map<String,Set<String>> fNameToNonAnonymClasses = new HashMap();
	private Map<String,String> bcClNameToSrcClName = new HashMap();
	
	private SourceInfo sourceInfo;

	protected AnonymousClassMap(SourceInfo sourceInfo) {
		this.sourceInfo = sourceInfo;
		for(SootClass klass : Program.g().getClasses()){
			String srcFileName = sourceInfo.filePath(klass);
			List<SootClass> classes = fNameToClasses.get(srcFileName);
			if(classes == null){
				classes = new ArrayList();
				fNameToClasses.put(srcFileName, classes);
			}
			classes.add(klass);
		}
	}

	/**
	   @return  the name of corresponding class in source code. 
	            The returned named is same as the chord sig except for anonymous classes
				returns null when the source code file in not available or
				klass is anonymous, but we could not map it
	 */
	protected String srcClassName(SootClass klass) {
		String srcFileName = this.sourceInfo.filePath(klass);

		Set<String> nonAnonymClasses = fNameToNonAnonymClasses.get(srcFileName);
		if(nonAnonymClasses == null){
			//until now we have not mapped anonymous classes (if any)
			nonAnonymClasses = performMapping(srcFileName);
			fNameToNonAnonymClasses.put(srcFileName, nonAnonymClasses);
		}
		if(nonAnonymClasses.isEmpty()){
			//source file is not available
			return null;
		}
		String klassName = klass.getName();
		if(nonAnonymClasses.contains(klassName))
			return klassName;
		
		//klass is anonymous
		String srcClassName = bcClNameToSrcClName.get(klassName);
		return srcClassName;		
	}	
	

	/*
	  @param srcFileName name of the Java file
	  @param nonAnonymousClasses is filled with names of named classes
	  @param anonymousClasses is filled with (lineNum, concocted-name) for each anonoymous class
	 */
	public void getAllDeclaredClasses(String srcFileName, Set<String> nonAnonymousClasses, Map<Integer,String> anonymousClasses) {
		File file = this.sourceInfo.srcMapFile(srcFileName);
		assert file != null;

		try{
			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(file);
			Element root = doc.getDocumentElement();
			//System.out.println("matching " + className + " " + file);
			NodeList classElems = root.getElementsByTagName("class");
			int numClasses = classElems.getLength();
			for(int i = 0; i < numClasses; i++){
				Element classElem = (Element) classElems.item(i);
				String sig = classElem.getAttribute("chordsig");
				if(classElem.getAttribute("anonymous").equals(""))
					nonAnonymousClasses.add(sig);
				else{
					int lineNum = Integer.parseInt(sig.substring(sig.lastIndexOf('#')+1));
					anonymousClasses.put(lineNum, sig);
				}

			}
		}catch(Exception e){
			throw new Error(e);
		}
	}

	private Set<String> performMapping(String srcFileName) {
		if(!this.sourceInfo.hasSrcFile(srcFileName))
			return Collections.emptySet();

		Set<String> nonAnonymClasses = new HashSet();
		Map<Integer,String> anonymClasses = new HashMap();			
		getAllDeclaredClasses(srcFileName, nonAnonymClasses, anonymClasses);
		for(SootClass klass : fNameToClasses.get(srcFileName)){
			for(SootMethod meth : klass.getMethods())
				performMapping(meth, anonymClasses);
		}
		return nonAnonymClasses;
	}
	
	private void performMapping(SootMethod meth, Map<Integer,String> anonymClasses) {
		if(!meth.isConcrete())
			return;

		for(Unit u : meth.retrieveActiveBody().getUnits()){
			Stmt stmt = (Stmt) u;
			if(!(stmt instanceof AssignStmt))
				continue;
			Value rightOp = ((AssignStmt) stmt).getRightOp();
			if(!(rightOp instanceof NewExpr))
				continue;
			String className = rightOp.getType().toString();
			int lineNum = this.sourceInfo.stmtLineNum(stmt);

			String anonymClassName = anonymClasses.get(lineNum);
			if(anonymClassName == null)
				continue;
			String prevBinding = bcClNameToSrcClName.put(className, anonymClassName);
			System.out.println("binding " + className + " " + anonymClassName);
			if(prevBinding != null){
				System.out.println("Multiple anonymous classes in the same line " + anonymClassName);
				bcClNameToSrcClName.put(className, null);
			}
		}		
	}
}