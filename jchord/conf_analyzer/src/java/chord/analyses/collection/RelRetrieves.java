package chord.analyses.collection;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * (i,u,v) if u is retrieved from v
 *
 */
@Chord(
		name = "IRetrieve",
		sign = "I0,V0,V1:I0_V0_V1"
)
public class RelRetrieves extends ProgramRel implements IInvokeInstVisitor {

	DomI domI;
	DomV domV;
	jq_Method method;
	jq_Type OBJ_T;
	public void init() {
		domI = (DomI) doms[0];
		domV = (DomV) doms[1];
		OBJ_T = jq_Type.parseType("java.lang.Object");
		RelINewColl.tInit();
	}

	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		method = m;
	}

	public boolean isRetrieve(Quad q) {
		jq_Method meth = Invoke.getMethod(q).getMethod();
		jq_Class cl = meth.getDeclaringClass();
		String mname = meth.getName().toString();

		if(mname.equals("toArray"))
			return true;
		
		return (RelINewColl.isCollectionType(cl) && !meth.isStatic() ) &&
		meth.getReturnType().equals(OBJ_T);
	}

	@Override
	public void visitInvokeInst(Quad q) {
		RegisterOperand vo = Invoke.getDest(q);
		int args = Invoke.getParamList(q).length();
		if (vo != null && args > 0) {
			Register v = vo.getRegister();
			if (v.getType().isReferenceType()) {
				if(isRetrieve(q)) {
					Register thisObj = Invoke.getParam(q, 0).getRegister();
					int iID = domI.indexOf(q);
					int vID = domV.indexOf(v);
					int thisID = domV.indexOf(thisObj);
					super.add(iID, vID, thisID);
				}
			}
		}
	}

}
