package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.collection.*;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.invk.DomI;
import chord.analyses.type.RelScopeExcludedT;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
		name = "APIMethod",
		sign = "I0:I0"
)
public class RelAPIMethod extends ProgramRel implements IInvokeInstVisitor {
	DomI domI;
	//  DomV domV;
	jq_Method method;
	public void init() {
		domI = (DomI) doms[0];
		RelINewColl.tInit();
		//   domV = (DomV) doms[1];
	}

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		method = m;
	}

	/*
	 * Some notes:
	 *   String version is invoked both statically and dynamically.
	 *   Quad-version happens only statically.
	 *   
	 *   In the static case, we filter out collection calls.
	 *   Here, they're treated as API calls.
	 *
	 */
	public static final boolean isAPI(String classname, String methname) {
		if(ConfDefines.isConf(classname, methname) && !methname.equals("toArray"))
			return false;

		if(classname.equals("java.lang.Thread")) //handled separately
			return false;
		if(classname.startsWith("java.lang") && methname.equals("newInstance"))
			return false;

		if(classname.equals("org.apache.hadoop.fs.Path"))// && RelScopeExcludedT.isExcluded(classname))
			return true;
		if(classname.equals("org.apache.tools.ant.types.Path"))
			return true;

		if(classname.startsWith("joeq") || classname.startsWith("net.sf.bddb")) //for analyzing jchord itself
			return true;
		
		if(classname.startsWith("org.mortbay.jetty"))
			return true;
		
			//debugging
		if(classname.equals("conf_analyzer.ScopeExcludedAPITest$MyPath"))
			return true;
		
		return classname.startsWith("java") 
&& (!classname.startsWith("java.io") || classname.equals("java.io.File"));//io is mostly bad, File is ok

	}

	private boolean isCollectionGet(Quad q) {
		return RelInserts.isInsert(q) ;
		//we treat gets as APIReadOnly calls, for now. Handles case where
		//a tainted collection is returned somehow or created.
	}

	/**
	 * Shouldn't taint collections when there's a put. .
	 */
	@Override
	public void visitInvokeInst(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		String classname = meth.getDeclaringClass().getName();
		String methname = meth.getName().toString();
		
		//Helps, even with context-sensitive analysis. Might be unneeded
		//if we switched to doing analysis in the callee-side instead of caller side.
		if(classname.startsWith("java.util") && classname.contains("Map"))
			return;
		
		if(isAPI(classname, methname) && !isCollectionGet(q)) {
			int iIdx = domI.indexOf(q);
			super.add(iIdx);
		}
	}
}
