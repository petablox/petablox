package chord.slicer;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import chord.program.visitors.IAcqLockInstVisitor;
import chord.program.visitors.IInvokeInstVisitor;
import chord.program.visitors.IRelLockInstVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;

/**
 * Relation that containing lock acquire/release, method invocations on thread objects
 * @author sangmin
 *
 */
@Chord(
		name = "PConc",
		sign = "P0"
)
public class RelPConc extends ProgramRel implements IAcqLockInstVisitor, IRelLockInstVisitor, IInvokeInstVisitor {	
	private String currentClassName;
	
	private static boolean filterOut(String className){
		for(String s : Config.checkExcludeAry){
			if(className.startsWith(s)) return true;
		}

		return false;
	}
	
	public void visitAcqLockInst(Quad q) {
		if(filterOut(currentClassName)){
			return;
		}
		add(q);
	}

	public void visitRelLockInst(Quad q) {
		if(filterOut(currentClassName)){
			return;
		}
		add(q);
	}

	public void visitInvokeInst(Quad q) {
		if(filterOut(currentClassName)){
			return;
		}
		jq_Method m = Operator.Invoke.getMethod(q).getMethod();
		if(m.getDeclaringClass().getName().equals("java.lang.Thread")){
			add(q);
		}		
	}

	public void visit(jq_Method m) {
	}

	public void visit(jq_Class c) {
		currentClassName = c.getName();
	}

}
