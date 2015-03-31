package chord.analyses.logging;

import java.util.*;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;


/**
 * toString contains (i, r,l) if r = l.toString()
 * @author asrabkin
 *
 */
@Chord(
		name = "LogStmts",
		consumes = { "I","V"},
		produces = { "Error", "Warn", "Info", "Debug", "println", "toString" },
		signs = {"I0","I0","I0","I0", "I0","I0,V0,V1:I0_V0_V1"},
		namesOfSigns = { "Error", "Warn", "Info", "Debug", "println", "toString" },
		namesOfTypes = { "I" ,"V"},
		types = { DomI.class, DomV.class }
)
public class RelLogStmts extends JavaAnalysis {
	DomI domI;
	DomV domV;
	jq_Method method;

	private ProgramRel relError, relWarn,  relInfo, relDebug, relPrintln, relToString;
	private List<ProgramRel> rels = new ArrayList<ProgramRel>();

	public void init() {
		ClassicProject project = ClassicProject.g();
		domI = (DomI) project.getTrgt("I");
		domV = (DomV) project.getTrgt("V");


		relError = (ProgramRel) project.getTrgt("Error");
		relWarn = (ProgramRel) project.getTrgt("Warn");
		relInfo = (ProgramRel) project.getTrgt("Info");
		relDebug = (ProgramRel) project.getTrgt("Debug");
		relPrintln = (ProgramRel) project.getTrgt("println");
		relToString = (ProgramRel) project.getTrgt("toString");


		rels.add(relError);
		rels.add(relWarn);
		rels.add(relInfo);
		rels.add(relDebug);
		rels.add(relPrintln);
		rels.add(relToString);


		for(ProgramRel rel: rels) 
			rel.zero();

	}

	@Override
	public void run() {

		init();

		System.out.println("running; " + domI.size() + " invokes to visit");
		for (Inst inst : domI) {
			visitInvokeInst((Quad) inst);
		}

		for(ProgramRel rel: rels) 
			rel.save();
	}


	public void visitInvokeInst(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		String classname = meth.getDeclaringClass().getName();
		String methname = meth.getName().toString();
		int iid = domI.indexOf(q);
		//    System.out.println("classname: " + classname + " methname: " + methname);
		if(classname.equals("org.apache.log4j.Category") || classname.equals("org.apache.commons.logging.Log")) {

			if(methname.equals("error"))
				relError.add(iid);
			else if(methname.equals("warn"))
				relWarn.add(iid);
			else if(methname.equals("info"))
				relInfo.add(iid);
			else if(methname.equals("debug"))
				relDebug.add(iid);
			else if(methname.equals("trace"))	//FIXME: could break these into separate relations
				relDebug.add(iid);
			else if(methname.equals("fatal"))
				relError.add(iid);
		} else if(classname.equals("rice.environment.logging.Logger") && methname.equals("log"))
			relInfo.add(iid);
		else if(classname.equals("jchord.project.Messages") && methname.equals("log"))
			relInfo.add(iid);
		else if(classname.equals("jchord.project.Messages") && methname.equals("fatal"))
			relError.add(iid);
		else if(classname.equals("org.apache.tools.ant.ProjectComponent") && methname.equals("log"))
			relInfo.add(iid);
		else if(methname.equals("println")) {
			String containingMethodName = q.getMethod().getName().toString();
			String containingClassName = q.getMethod().getDeclaringClass().getName();
			if(!containingMethodName.equals("log") ||   //get the log statement either in parent or child, not both
					(!containingClassName.equals("org.apache.tools.ant.ProjectComponent") &&
							!classname.equals("rice.environment.logging.Logger")))
				relPrintln.add(iid);
		} else if(methname.equals("toString")) {
			RegisterOperand r0 = Invoke.getParam(q, 0);
			int v0 = domV.indexOf(r0.getRegister());
			RegisterOperand vOutR = Invoke.getDest(q);
			if(vOutR != null) {
				int vOut = domV.indexOf(vOutR.getRegister());
				if(v0 != -1 && vOut != -1)
					relToString.add(iid, vOut, v0);
			}
		}
	}

	public static boolean isLogStmt(String classname, String methname) {
		//    if(classname.equals("org.apache.log4j.Category") || classname.equals("org.apache.commons.logging.Log")) {
		return methname.equals("error") || methname.equals("warn") || methname.equals("info") || methname.equals("debug");
		//   } else 
		//      return false;
	}
}
