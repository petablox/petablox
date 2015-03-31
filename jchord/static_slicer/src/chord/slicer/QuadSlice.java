package chord.slicer;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

import joeq.Class.jq_Array;
import joeq.Class.jq_Class;
import joeq.Class.jq_Initializer;
import joeq.Class.jq_Method;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Primitive;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Config;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;

/**
 * Slice out quads that are not in PSlice.
 * This keeps class initializers intact. 
 * (This is a temporary solution to include class initializers.)
 * This does not slice java system libraries.
 * This initializes all registers included in slices  
 * 
 * @author sangmin
 *
 */
@Chord(
		name="qslice-java",
		consumes = { "M", "PSlice","MSlice", "supInitPP" }
)
public class QuadSlice extends JavaAnalysis {
	ProgramRel relPSlice;
	ProgramRel relMSlice;
	ProgramRel relSupInitPP;
	
	public void run() {
		relPSlice = (ProgramRel) ClassicProject.g().getTrgt("PSlice");
		relPSlice.load();
		relMSlice = (ProgramRel) ClassicProject.g().getTrgt("MSlice");
		relMSlice.load();
		relSupInitPP = (ProgramRel) ClassicProject.g().getTrgt("supInitPP");
		relSupInitPP.load();
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		
		Program program = Program.g();
		IndexSet<jq_Reference> classes = program.getClasses();		
		for (jq_Reference r : classes) {
			if (r instanceof jq_Array)
				continue;

			jq_Class c = (jq_Class) r;
			String cname = c.getName();
			// Filter out classes
			if (filterOut(cname))
				continue;

			// process static methods
			for (jq_Method m : c.getDeclaredStaticMethods()) {
				if (domM.contains(m))
					processMethod(m);
			}

			// process non-static methods
			for (jq_Method m : c.getDeclaredInstanceMethods()) {
				if (domM.contains(m))
					processMethod(m);
			}


		}

		printProg();


	}

	private void printProg() {
		Program program = Program.g();
		IndexSet<jq_Reference> classes = program.getClasses();		
		for (jq_Reference r : classes) {
			if (r instanceof jq_Array)
				continue;

			jq_Class c = (jq_Class) r;
			String cname = c.getName();
			// Filter out classes
			if (filterOut(cname))
				continue;
			// process static methods
			for (jq_Method m : c.getDeclaredStaticMethods()) {
				System.out.println("####### DUMPING SLICE");
				System.out.println(m.getCFG().fullDump());
			}

			// process non-static methods
			for (jq_Method m : c.getDeclaredInstanceMethods()) {
				System.out.println("####### DUMPING SLICE");
				System.out.println(m.getCFG().fullDump());
			}

		}
	}
	
	/**
	 * return any super() call in the constructor
	 * @param init
	 * @return
	 */
	private Quad getDefaultSuperInit(jq_Initializer init) {
		if (init.getDeclaringClass().getSuperclass() == null) return null;
		ControlFlowGraph cfg = init.getCFG();		
		BasicBlock entry = cfg.entry();
		for (BasicBlock bb : cfg.reversePostOrder()) {
			for (Quad q : bb.getQuads()) {
				if (relSupInitPP.contains(entry,q))
					return q;				
			}
		}
		assert false;
		return null;
	}

	private void processMethod(jq_Method m) {
		// keep class initializers intact
		if (m instanceof jq_ClassInitializer)
			return;
		
		Quad neededInit = null;
		
		if (m instanceof jq_Initializer && !relMSlice.contains(m)) {
			// if any of statements of a constructor in a slice,
			// include a super() call.
			neededInit = getDefaultSuperInit((jq_Initializer) m);
		}

		ControlFlowGraph cfg = m.getCFG();
		// identify formal arguments so that we don't initialize them
		RegisterFactory rf = cfg.getRegisterFactory();
		int numArgs = m.getParamTypes().length;
		Set<Register> formalArgs = new HashSet<Register>();
		for (int i=0; i < numArgs; i++) {
			formalArgs.add(rf.get(i));
		}

		Set<Register> registersUsedNotDefined = new HashSet<Register>();
		Set<Register> registersDefinedBeforeUse = new HashSet<Register>();
		for (BasicBlock bb : cfg.reversePostOrder()) {
			Set<Quad> quadsToRemove = new HashSet<Quad>();
			registersDefinedBeforeUse.clear();
			// find out quads that are not in slice
			for (Quad q : bb.getQuads()) {
				Operator operator = q.getOperator();
				// We keep all return statements and exception saving statements	
				if (!relPSlice.contains(q) 
						&& !(operator instanceof Operator.Return)
						&& !(operator instanceof Operator.Special.GET_EXCEPTION)
						&& q != neededInit) {
					quadsToRemove.add(q);										
				} else {
					// Check if any of used register has not been defined.
					List<RegisterOperand> usedRegList = q.getUsedRegisters();
					int size = usedRegList.size();
					for (int i=0; i < size; i++) {
						RegisterOperand ro = usedRegList.get(i);
						if (!registersDefinedBeforeUse.contains(ro.getRegister())) {
							registersUsedNotDefined.add(ro.getRegister());
						}
					}

					List<RegisterOperand> definedRegList = q.getDefinedRegisters();
					size = definedRegList.size();
					for (int i=0; i < size; i++) {
						RegisterOperand ro = definedRegList.get(i);
						registersDefinedBeforeUse.add(ro.getRegister());
					}

				}
			}

			for (Quad q : quadsToRemove) {
				bb.removeQuad(q);
			}											
		}

		if (registersUsedNotDefined.size() > 0 && !formalArgs.containsAll(registersUsedNotDefined)) {
			// Initialize all the used registers without being defined (except for formal arguments).
			// We create a basic block that contains all these register initialization statements
			// and insert it after an entry basic block. 
			BasicBlock entry = cfg.entry();
			assert entry.getSuccessors().size() == 1;
			BasicBlock newBB = cfg.createBasicBlock(1, 1, registersUsedNotDefined.size(), null);
			BasicBlock orgSuccessor = entry.getSuccessors().get(0);
			newBB.addSuccessor(orgSuccessor);
			orgSuccessor.removePredecessor(entry);
			orgSuccessor.addPredecessor(newBB);
			newBB.addPredecessor(entry);
			entry.removeSuccessor(0);
			entry.addSuccessor(newBB);			
			for (Register reg : registersUsedNotDefined) {
				if (!formalArgs.contains(reg)) {
					Operator.Move opMove = Operator.Move.getMoveOp(reg.getType());
					Operand src = getInitialVal(reg.getType());						
					Quad q = Operator.Move.create(cfg.getNewQuadID(), newBB, opMove, new RegisterOperand(reg, reg.getType()), src);
					newBB.addAtEnd(cfg, q);
				}
			}
		}	

	}	

	private static Operand getInitialVal(jq_Type type) {		
		Operand ret = null;
		if (type.isReferenceType())	ret = new Operand.AConstOperand(null);
		if (type.isIntLike())	ret =  new Operand.IConstOperand(0);        
		if (type == jq_Primitive.FLOAT)	ret = new Operand.FConstOperand(0);
		if (type == jq_Primitive.LONG) ret = new Operand.LConstOperand(0);
		if (type == jq_Primitive.DOUBLE) ret = new Operand.DConstOperand(0);
		assert ret != null : type;
		return ret;
	}

	/**
	 * Filter out classes which we don't want to slice 
	 * @param className
	 * @return true if we want to filter out this class
	 */
	private static boolean filterOut(String className) {
		for (String s : Config.checkExcludeAry) {
			if (className.startsWith(s)) return true;
		}
		return false;
	}
}
