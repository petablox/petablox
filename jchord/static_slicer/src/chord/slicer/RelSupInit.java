package chord.slicer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import chord.util.ArraySet;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Branch;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Relation containing each tuple (p0,p1) such that p0 is a head of <init>
 * method and p1 is an invocation of <init> of the super class. 
 * @author sangmin
 *
 */
@Chord(
	name = "supInitPP",
	sign = "P0,P1:P0_P1"
)
public class RelSupInit extends ProgramRel {
	private final Set<Quad> superInit = new HashSet<Quad>();
	private final Set<BasicBlock> visitedBB = new HashSet<BasicBlock>();
	private jq_Class superClass;

	public void fill() {
		for (jq_Method m : Program.g().getMethods()) {
			if (!m.isAbstract() && m.getName().toString().equals("<init>")) {
				superClass = m.getDeclaringClass().getSuperclass();
				if (superClass == null)
					continue;
				superInit.clear();
				visitedBB.clear();
				ControlFlowGraph cfg = m.getCFG();
				BasicBlock bb = cfg.entry();
				Set<Register> r0Clones = new ArraySet<Register>();
				r0Clones.add(cfg.getRegisterFactory().get(0));
				search(bb, r0Clones, new ArraySet<Quad>());
				for (Quad q: superInit)
					add(bb, q);
			}
		}
	}
	
	private void search(BasicBlock bb, Set<Register> r0Clones, Set<Quad> quads) {
		if (!visitedBB.add(bb) || bb.isExit()) return;
		for (Quad q : bb.getQuads()) {
			Operator operator = q.getOperator();
			if (operator instanceof Move) {
				Operand src = Move.getSrc(q);
				if (src instanceof RegisterOperand) {
					Register srcReg = ((RegisterOperand) src).getRegister();
					if (r0Clones.contains(srcReg)) {
						r0Clones.add(Move.getDest(q).getRegister());
						quads.add(q);
					}
					
				}
			} else if (operator instanceof Phi) {
				ParamListOperand paramList = Phi.getSrcs(q);
				int len = paramList.length();
				for (int i = 0; i < len; i++) {
					Register srcReg = Phi.getSrc(q, i).getRegister();
					if (r0Clones.contains(srcReg)) {
						Register dstReg = Phi.getDest(q).getRegister();
						r0Clones.add(dstReg);
						quads.add(q);
						break;
					}					
				}								
			} else if (operator instanceof InvokeStatic) {				
				jq_Method callee = InvokeStatic.getMethod(q).getMethod();							
				if (!callee.isStatic() &&
						callee.getName().toString().equals("<init>") &&
						r0Clones.contains(InvokeStatic.getParam(q, 0).getRegister()) &&
						superClass == callee.getDeclaringClass()) {
					quads.add(q);
					superInit.addAll(quads);
					return;
				}
				
			}		
		}
		Quad lastQuad = bb.getLastQuad();
		if (lastQuad != null && lastQuad.getOperator() instanceof Branch)
			quads.add(lastQuad);
		for (Object bo2 : bb.getSuccessors()) {
			BasicBlock bb2 = (BasicBlock) bo2;
			search(bb2, new ArraySet<Register>(r0Clones), new ArraySet<Quad>(quads));
		}						
	}
}
