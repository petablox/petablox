package chord.analyses.collection;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.invk.DomI;
import chord.analyses.primtrack.DomU;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Contains tuples (i, u, v) where i is a method call that returns 
 * a prim value describing the collection referenced by v.
 * 
 * For example, size() and isEmpty()
 * @author asrabkin
 *
 */
@Chord(
		name = "IColSize",
		sign = "I0,U0,V0:I0_U0_V0"
)
public class RelCSize extends ProgramRel implements IInvokeInstVisitor {

	DomI domI;
	DomV domV;
	DomU domU; 
	jq_Method method;

	public void init() {
		domI = (DomI) doms[0];
		domU = (DomU) doms[1];
		domV = (DomV) doms[2];
	}

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		method = m;
	}

	@Override
	public void visitInvokeInst(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		jq_Class cl = meth.getDeclaringClass();
		if(!meth.isStatic() && RelINewColl.isCollectionType(cl)) {
			String mname = meth.getName().toString();
			if(mname.equals("size") || mname.equals("isEmpty")) {
				int uIdx = domU.indexOf(Invoke.getDest(q).getRegister());
				int vIdx = domV.indexOf(Invoke.getParam(q, 0).getRegister());
				int iIdx = domI.indexOf(q);
				if(uIdx > 0 && vIdx > 0 && iIdx > 0)
					super.add(iIdx, uIdx, vIdx);
				else
					System.out.println("WARN: missing elements in RelCSize. (" + iIdx + ","+uIdx + ","+vIdx +
							"). While visiting " + method);
			}
		}
	}

}
