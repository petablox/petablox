package chord.slicer;

import java.util.HashSet;
import java.util.List;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.program.MethodSign;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * Relation containing slicing seeds.
 * For non-array type seeds, we pick Getstatic of given fields
 * For array type seeds, we pick Aload that uses given field as base 
 * @author sangmin
 *
 */
@Chord(
		name = "Seeds",
		sign = "P0"
)
public class SeedReader extends ProgramRel {

	// set of registers that reference the seed of array type
	HashSet<Integer> registerSet = new HashSet<Integer>();
	boolean arrayTypeSeed = false;

	public void fill() {
		List<String> fStrList = Utils.readFileToList("seeds.txt");
		int n = fStrList.size();
		assert (n > 0);
		Program program = Program.g();

		String methodSignStr = fStrList.get(0);
		MethodSign mSign = MethodSign.parse(methodSignStr);
		jq_Method m = program.getMethod(mSign);
		if (m == null) {
			Messages.fatal("Failed to find method %s", methodSignStr);
		}

		for (int i = 1; i < n; i++) {
			String fStr = fStrList.get(i);				
			MethodSign sign = MethodSign.parse(fStr);
			jq_Reference r = program.getClass(sign.cName);
			if (r == null) {
				Messages.fatal("ERROR: Cannot slice on field %s: " +
						" its declaring class was not found.", fStr);
				continue;
			}

			assert (r instanceof jq_Class);
			jq_Class c = (jq_Class) r;
			jq_Field f = (jq_Field) c.getDeclaredMember(sign.mName, sign.mDesc);
			if (f == null) {
				Messages.fatal("ERROR: Cannot slice on field %s: " +
						"it was not found in its declaring class.", fStr);
			}
			assert(f.isStatic());				
			if (f.getType().isArrayType()) {
				arrayTypeSeed = true;
				registerSet.clear();
			}
			System.out.println("AAA: searching " + f + " in " + m);
			searchFieldAccess(m,f);
			arrayTypeSeed = false;
		}

	}

	private void searchFieldAccess(jq_Method m, jq_Field f) {
		assert (f.isStatic());
		for (BasicBlock bb : m.getCFG().reversePostOrder()) {
			for (Quad q : bb.getQuads()) {
				if (arrayTypeSeed && registerSet.size() > 0) {
					// register is overwritten
					for (RegisterOperand ro : q.getDefinedRegisters()) {
						Register defined = ro.getRegister();
						registerSet.remove(defined.getNumber());
					}
				}

				if (q.getOperator() instanceof Operator.Getstatic) {
					if (Getstatic.getField(q).getField().toString().equals(f.toString())) {
						// if seed is an array type, we want to search for array load
						if (arrayTypeSeed) {
							// store the register
							registerSet.add(Getstatic.getDest(q).getRegister().getNumber());
						} else {
							System.out.println("ADDING: " + q);
							add(q);
						}
					}
				} else if (q.getOperator() instanceof Operator.ALoad) {
					if (arrayTypeSeed) {
						Operand base = Operator.ALoad.getBase(q);
						if (base instanceof Operand.RegisterOperand) {
							int regNum = ((Operand.RegisterOperand)base).getRegister().getNumber();
							if (registerSet.contains(regNum)) {
								System.out.println("ADDING: " + q);
								add(q);
							}
						}
					}
				} else if (q.getOperator() instanceof Operator.Move.MOVE_A) { 
					Operand src = Move.getSrc(q);
					int dstRegNum = Move.getDest(q).getRegister().getNumber();
					registerSet.remove(dstRegNum);

					if (src instanceof RegisterOperand) {						
						int srcRegNum = ((RegisterOperand)src).getRegister().getNumber();
						assert dstRegNum != srcRegNum;
						if (registerSet.contains(srcRegNum)) {
							registerSet.add(dstRegNum);
						}
					}
				} else if (q.getOperator() instanceof Operator.Invoke.InvokeStatic) {
					// This is to handle the case like the following:
					// public static void dummyRead() {
					// 			dummyRead0();
					//			dummyRead1();
					// }
					jq_Method method = Invoke.getMethod(q).getMethod();
					if (method.getDeclaringClass().toString().equals(m.getDeclaringClass().toString())) {
						searchFieldAccess(method, f);
					}
				}

			}																				
		}

	}

}
