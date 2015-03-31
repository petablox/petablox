package chord.slicer;

import java.util.HashMap;
import java.util.Map;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of all registers used. This is a superset of DomV.
 * DomV contains only registers of reference type whereas DomU contains all registers.
 * @author sangmin
 *
 */
@Chord(
		name = "U"
)

public class DomU extends ProgramDom<Register> implements IMethodVisitor{
	private Map<Register, jq_Method> varToMethodMap;
	private jq_Method ctnrMethod;

	public void init() {
		varToMethodMap = new HashMap<Register, jq_Method>();

	}
	private void addVar(Register v) {
		varToMethodMap.put(v, ctnrMethod);
		getOrAdd(v);
	}

	public jq_Method getMethod(Register v) {
		return varToMethodMap.get(v);
	}
	public String toUniqueString(Register v) {
		return v + "!" + getMethod(v);
	}

	public void visit(jq_Method m) {
		if(m.isAbstract())
			return;
		if (m.isAbstract())
			return;
		ctnrMethod = m;
		ControlFlowGraph cfg = m.getCFG();
		RegisterFactory rf = cfg.getRegisterFactory();

		for (int i = 0; i < rf.size(); i++) {
			Register v = rf.get(i);
			addVar(v);
		}
		
	}

	public void visit(jq_Class c) {
	}

}
