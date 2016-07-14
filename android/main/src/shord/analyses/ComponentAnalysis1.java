package shord.analyses;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramDom;
import shord.project.ClassicProject;

import stamp.analyses.SootUtils;
import stamp.app.Component;
import stamp.app.App;
import stamp.app.IntentFilter;
import stamp.app.Data;

import chord.project.Chord;

import java.util.jar.*;
import java.io.*;
import java.util.*;

/**
 * Generating relations related to ICCG.
 * @author Yu Feng (yufeng@cs.stanford.edu)
 * @author Saswat Anand
 */

@Chord(name="comp-java-1",
       produces={"SC"},
       namesOfTypes = {"SC"},
       types = {DomSC.class}
       )
public class ComponentAnalysis1 extends JavaAnalysis
{
	private DomSC domSC;
    private int newLocalCount;
	
    public void run()
    {
		populateSC();
		instrument();
	}

	private void instrument()
	{
		NumberedSet fklasses = frameworkClasses();
		for(SootClass klass : Program.g().getClasses()){
			if(fklasses.contains(klass))
				continue;
			newLocalCount = 0;
			for(SootMethod method : klass.getMethods())
				visitMethod(method);
		}
	}

    private void visitMethod(SootMethod method)
    {
		if(!method.isConcrete())
			return;
		Body body = method.retrieveActiveBody();
		Chain<Local> locals = body.getLocals();
		Chain<Unit> units = body.getUnits();
		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
	    
			//invocation statements
			if(stmt.containsInvokeExpr()){
				InvokeExpr ie = stmt.getInvokeExpr();
				List args = ie.getArgs();
				int i = 0;
				for(Iterator ait = args.iterator(); ait.hasNext();){
					Immediate arg = (Immediate) ait.next();
					if(arg instanceof ClassConstant){
						Local newArg = instrumentIfNecessary((ClassConstant) arg, locals, units, stmt);
						if(newArg != null){
							ie.setArg(i, newArg);
						}
					}
					i++;
				}
			}
			else if(stmt instanceof AssignStmt){
				Value rhs = ((AssignStmt) stmt).getRightOp();
				if(rhs instanceof ClassConstant){
					Local newRhs = instrumentIfNecessary((ClassConstant) rhs, locals, units, stmt);
					if(newRhs != null)
						((AssignStmt) stmt).setRightOp(newRhs);
				}
			}
		}
    }
    
    private Local instrumentIfNecessary(ClassConstant classConstant, Chain<Local> locals, Chain<Unit> units, Unit currentStmt)
    {
		String name = classConstant.value.replaceAll("/", ".");

		if(!domSC.contains(name))
			return null;

		Local temp = Jimple.v().newLocal("stamp$stamp$cni"+newLocalCount++, RefType.v("java.lang.Class"));
		locals.add(temp);

		SootMethodRef forNameMethod = Scene.v().getMethod("<java.lang.Class: java.lang.Class forName(java.lang.String)>").makeRef();
		Stmt toInsert = Jimple.v().newAssignStmt(temp, Jimple.v().newStaticInvokeExpr(forNameMethod, StringConstant.v(name)));
		units.insertBefore(toInsert, currentStmt);

		return temp;
    }


	private void populateSC()
	{
		domSC = (DomSC) ClassicProject.g().getTrgt("SC");

		App app = Program.g().app();

		List<Component> comps = new ArrayList();
		SystemComponents.add(comps);
		comps.addAll(app.components());

		for(Component comp : comps){
			domSC.add(comp.name);

			for(IntentFilter filter : comp.intentFilters){
				for(String act : filter.actions){
					domSC.add(act);
				}

				for(Data dt : filter.data){
					if(dt.mimeType != null)
						domSC.add(dt.mimeType);
				}
			}
		}

		domSC.save();
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
