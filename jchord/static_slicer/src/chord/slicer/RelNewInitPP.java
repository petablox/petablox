package chord.slicer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import chord.program.visitors.INewInstVisitor;
import chord.project.Chord;
import chord.project.Config;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple(p0, p1) such that p0 is a "new" operation 
 * and p1 is a corresponding constructor invocation.
 * @author sangmin
 *
 */
@Chord(
		name = "newInitPP",
		sign = "P0,P1:P0_P1"
)
public class RelNewInitPP extends ProgramRel implements INewInstVisitor {
	private String currentClassName;
	private jq_Method currentMethod;
	private boolean processed = false;
	
	private static boolean filterOut(String className) {
		for (String s : Config.checkExcludeAry) {
			if (className.startsWith(s)) return true;
		}
		return false;
	}
	
	public void visitNewInst(Quad q) {
		if (processed) return;
		processed = true;
		// map from object creation quad ("new") to set of registers that stores reference to the object
		HashMap<Quad, HashSet<Register>> registerAliasMap = new HashMap<Quad, HashSet<Register>>();
		// set of all registers stored in the registerAliasMap
		HashSet<BasicBlock> visitedBB = new HashSet<BasicBlock>();	
		HashSet<Register> wholeRegs = new HashSet<Register>();
		search(currentMethod.getCFG().entry(), visitedBB, registerAliasMap, wholeRegs);
	}

	/**
	 * Perform depth first search for "new" and "<init>" pair
	 * @param bb : basic block we are visiting
	 * @param registerAliasMap
	 * @param wholeRegs
	 */
	private void search(BasicBlock bb, HashSet<BasicBlock> visitedBB,
			HashMap<Quad,HashSet<Register>> registerAliasMap, HashSet<Register> wholeRegs) {
		if (!visitedBB.add(bb)) return;
		for (Quad q : bb.getQuads()) {
			if (q.getOperator() instanceof Operator.New) {
				Register reg = New.getDest(q).getRegister();
				HashSet<Register> set = new HashSet<Register>();
				set.add(reg);
				registerAliasMap.put(q, set);
				wholeRegs.add(reg);
			} else if (q.getOperator() instanceof Operator.Invoke.InvokeStatic) {
				jq_Method callee = Invoke.getMethod(q).getMethod();
				if (!callee.isStatic() && callee.getName().toString().equals("<init>")) {
					Register paramReg = Operator.Invoke.InvokeStatic.getParam(q, 0).getRegister();
					Quad p = findQuad(paramReg, registerAliasMap);
					if (p != null) {
						add(p,q);
						wholeRegs.removeAll(registerAliasMap.get(p));
						registerAliasMap.remove(p);
					}
				}
			} else if (q.getOperator() instanceof Operator.Move.MOVE_A) {
				Operand src = Move.getSrc(q);
				Register dstReg = Move.getDest(q).getRegister();
				if (wholeRegs.contains(dstReg)) {
					removeReg(dstReg, registerAliasMap);
					wholeRegs.remove(dstReg);
				}
				if (src instanceof RegisterOperand) {
					Register srcReg = ((RegisterOperand)src).getRegister();
					if (wholeRegs.contains(srcReg)) {
						addReg(srcReg, dstReg, registerAliasMap);
						wholeRegs.add(dstReg);
					}
				}
			}

		}

		if (bb.getSuccessors().size() == 1) {
			BasicBlock successor = bb.getSuccessors().get(0);
			if (!visitedBB.contains(successor)) {
				search(successor, visitedBB, registerAliasMap, wholeRegs);
			}
		} else {
			for (Object bo : bb.getSuccessors()) {
				BasicBlock nextBB = (BasicBlock) bo;
				if (visitedBB.contains(nextBB)) continue;
				HashMap<Quad, HashSet<Register>> newMap = new HashMap<Quad, HashSet<Register>>();
				for (Quad q : registerAliasMap.keySet()) {
					newMap.put(q, (HashSet<Register>) registerAliasMap.get(q).clone());
				}
				search(nextBB, (HashSet<BasicBlock>) visitedBB.clone(),
					newMap, (HashSet<Register>) wholeRegs.clone());
			}
		}

	}

	/**
	 * Find a Quad from which registerAliasMap maps to a set containing the register 
	 * @param reg register we are interested
	 * @param registerAliasMap
	 * @return
	 */
	private Quad findQuad(Register reg, HashMap<Quad, HashSet<Register>> registerAliasMap) {
		for (Entry<Quad, HashSet<Register>> entry : registerAliasMap.entrySet()) {
			HashSet<Register> set = entry.getValue();
			if (set.contains(reg))
				return entry.getKey();
		}
		return null;
	}

	private void removeReg(Register reg, HashMap<Quad, HashSet<Register>> registerAliasMap) {
		for (HashSet<Register> set : registerAliasMap.values()) {
			if (set.contains(reg)) {
				set.remove(reg);
				return;
			}
		}
	}

	private void addReg(Register srcReg, Register newReg, HashMap<Quad, HashSet<Register>> registerAliasMap) {
		for (HashSet<Register> set : registerAliasMap.values()) {
			if (set.contains(srcReg)) {
				set.add(newReg);
				return;
			}
		}
	}

	public void visit(jq_Method m) {
		if (filterOut(currentClassName)) {
			processed = true;
		} else {
			currentMethod = m;
			processed = false;
		}
	}

	public void visit(jq_Class c) {
		currentClassName = c.getName();
	}

}
