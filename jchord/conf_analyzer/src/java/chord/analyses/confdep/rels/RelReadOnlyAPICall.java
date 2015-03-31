package chord.analyses.confdep.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.collection.*;
import chord.analyses.invk.DomI;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Set of API calls to be treated as read only.
 * 
 * This is a filter around RelAPIMethod; methods should be in BOTH
 * 
 * @author asrabkin
 *
 */
@Chord(
		name = "APIReadOnly",
		sign = "I0:I0"
)
public class RelReadOnlyAPICall extends ProgramRel implements IInvokeInstVisitor {
	DomI domI;
	//DomV domV;
	jq_Method method;
	jq_Type OBJ_T;
	public void init() {
		domI = (DomI) doms[0];
	}

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		method = m;
	}
	
	/**
	 * Assume all collection API methods are read only.
	 *  note that collection write isn't an API method...
	 */
	boolean collectionMethod(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		jq_Class cl = meth.getDeclaringClass();
	
		return false;
//		return RelINewColl.isCollectionType(cl) ;
//		return (RelINewColl.isCollectionType(cl) && !meth.isStatic() ) &&
//		meth.getReturnType().getName().equals("java.lang.Object");
	}

	@Override
	public void visitInvokeInst(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		String classname = meth.getDeclaringClass().getName();
		String methname = meth.getName().toString();
		if(isReadOnly(classname, methname) || collectionMethod(q)) {
			int iIdx = domI.indexOf(q);
			super.add(iIdx);
		}
	}

	public static boolean isReadOnly(String classname, String methname) {
		return methname.equals("equals") || methname.equals("compareTo") || methname.startsWith("get") ||
		methname.equals("toArray") || methname.equals("isAbsolute") ||
		methname.equals("toUri") || 
		classname.startsWith("joeq") || classname.startsWith("net.sf.bddb") ;
	}

}
