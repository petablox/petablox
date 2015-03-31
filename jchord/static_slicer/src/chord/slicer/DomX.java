package chord.slicer;

import java.util.HashMap;
import java.util.HashSet;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.bddbddb.Rel.IntPairIterable;
import chord.bddbddb.Rel.IntTrioIterable;
import chord.analyses.heapacc.DomE;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.object.Trio;

/**
 * Any thing that can possibly be in actual-in, formal-in, actual-out, formal-out
 *  vertices of a system dependency graph
 *  Trio<e|f|u, i|method entry|method exit, 0|1>
 *  1st element - f for static field, e for instance field or array, u for register
 *  2nd element - i(quad for call site) for actual-in and actual-out, entry bb for formal-in, exit bb for formal-out
 *  3rd element - 0 for *-in, 1 for *-out 
 * @author sangmin
 *
 */

@Chord(
		name = "X",
		consumes = { "I", "M", "E", "U", "mods", "refs", "invkArg", "methArg", "invkRet", "methRet" }
)
public class DomX extends ProgramDom<Trio<Object,Inst,Integer>> {
	private static Integer ZERO = new Integer(0);
	private static Integer ONE = new Integer(1);

	private DomI domI;
	private DomM domM;
	private DomE domE;
	private DomU domU;

	private HashMap<Integer, HashSet<Integer>> modsMap;
	private HashMap<Integer, HashSet<Integer>> refsMap;
	private int numFormalIn, numFormalOut, numActualIn, numActualOut;

	public void fill() {
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domM = (DomM) ClassicProject.g().getTrgt("M");
		domE = (DomE) ClassicProject.g().getTrgt("E");
		domU = (DomU) ClassicProject.g().getTrgt("U");

		//////////

		buildModsMap();
		buildRefsMap();
		System.out.println("Number of formal-in:  " + numFormalIn );
		System.out.println("Number of formal-out: " + numFormalOut);

		//////////

		System.out.println("Start searching Actual-in/out for static field and instance fields");
		long starttime = System.currentTimeMillis();
		ProgramRel relIM = (ProgramRel) ClassicProject.g().getTrgt("IM");
		relIM.load();		
		int sizeIM = relIM.size(), done = 0, fivePercents = sizeIM/20 + 1;		
		System.out.println("rel IM size: " + sizeIM);
		IntPairIterable tuplesIM = relIM.getAry2IntTuples();
		for (IntPair tuple : tuplesIM) {
			int iIdx = tuple.idx0;
			int mIdx = tuple.idx1;
			HashSet<Integer> modE = modsMap.get(mIdx);
			if (modE != null) {
				for (Integer eIdx : modE) {
					Quad q = domE.get(eIdx);
					Operator op = q.getOperator();
					if (op instanceof Operator.Putstatic) {
						// actual-out for static field f written in this method
						jq_Field f = Operator.Putstatic.getField(q).getField();
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdx), ONE));
						numActualOut++;
						// actual-in for static field f written in this method
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdx), ZERO));
						numActualIn++;
					} else {
						assert (op instanceof Operator.Putfield || op instanceof Operator.AStore);
						// actual-out for instance field or array written in this method
						getOrAdd(new Trio<Object,Inst,Integer>(q, domI.get(iIdx), ONE));
						numActualOut++;
					}					
				}
			}
			HashSet<Integer> refE = refsMap.get(mIdx);
			if (refE != null) {
				for (Integer eIdx : refE) {
					Quad q = domE.get(eIdx);
					Operator op = q.getOperator();
					if (op instanceof Operator.Getstatic) {
						// actual-in for static field f read in this method
						jq_Field f = Operator.Getstatic.getField(q).getField();
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdx), ZERO));
						numActualIn++;
					} else {
						assert (op instanceof Operator.Getfield || op instanceof Operator.ALoad);
						// actual-in for instance field or array read in this method
						getOrAdd(new Trio<Object,Inst,Integer>(q, domI.get(iIdx), ZERO));
						numActualIn++;
					}	
				}
			}
			done++;
			if (done%fivePercents == 0) {
				long time = System.currentTimeMillis() - starttime;
				System.out.println(((double)done/(double)sizeIM)*100 + " % done taking " + time + " ms.");
			}
		}

		modsMap.clear();
		refsMap.clear();

		System.out.println("End searching Actual-in/out for static field and instance fields");
		System.out.println("Number of actual-in : " + numActualIn );
		System.out.println("Number of actual-out: " + numActualOut);

		//////////

		// actual-in for registers used as parameters
		ProgramRel relInvkArg = (ProgramRel) ClassicProject.g().getTrgt("invkArg");
		relInvkArg.load();
		IntTrioIterable tuplesInvkArg = relInvkArg.getAry3IntTuples();
		for (IntTrio tuple : tuplesInvkArg) {
			int iIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domI.get(iIdx), ZERO));        	
		}
		relInvkArg.close();

		// formal-in for registers used as arguments
		ProgramRel relMethArg = (ProgramRel) ClassicProject.g().getTrgt("methArg");
		relMethArg.load();
		IntTrioIterable tuplesMArg = relMethArg.getAry3IntTuples();
		for (IntTrio tuple : tuplesMArg) {
			int mIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domM.get(mIdx).getCFG().entry(), ZERO));      	
		}
		relMethArg.close();
		
		// actual-out for registers used as return values
		ProgramRel relInvkRet = (ProgramRel) ClassicProject.g().getTrgt("invkRet");
		relInvkRet.load();
		IntPairIterable tuplesInvkRet = relInvkRet.getAry2IntTuples();
		for (IntPair tuple : tuplesInvkRet) {
			int iIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domI.get(iIdx), ONE));
		}
		relInvkRet.close();
		
		// formal-out for registers used as return values
		ProgramRel relMethRet = (ProgramRel) ClassicProject.g().getTrgt("methRet");
		relMethRet.load();
		IntPairIterable tuplesMethRet = relMethRet.getAry2IntTuples();
		for (IntPair tuple : tuplesMethRet) {
			int mIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domM.get(mIdx).getCFG().exit(), ONE));
		}
		relMethRet.close();
	}

	// populates modsMap using relation mods, populates domain X with formal-in and formal-out,
	// and modifies counters numFormalIn and numFormalOut
	private void buildModsMap() {
	 	modsMap = new HashMap<Integer, HashSet<Integer>>();
		System.out.println("Start searching relation mods for Formal-in/out");
		ProgramRel relMods = (ProgramRel) ClassicProject.g().getTrgt("mods");
		relMods.load();
		System.out.println("mods rel size: " + relMods.size());
		IntPairIterable modsTuples = relMods.getAry2IntTuples();
		for (IntPair tuple : modsTuples) {
			int mIdx = tuple.idx0;
			int eIdx = tuple.idx1;	
			HashSet<Integer> mods = modsMap.get(mIdx);
			if (mods == null) {
				mods = new HashSet<Integer>();
				modsMap.put(mIdx, mods);
			}			
			mods.add(eIdx);
			Quad q = domE.get(eIdx);
			Operator op = q.getOperator();
			if (op instanceof Operator.Putstatic) {
				// formal-out for static field f modified in this method
				jq_Field f = Operator.Putstatic.getField(q).getField();			
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdx).getCFG().exit(), ONE));
				numFormalOut++;
				// for static fields, we also create formal-in to handle the case where
				// the static field may or may not be modified by the called method
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdx).getCFG().entry(), ZERO));
				numFormalIn++;
			} else {
				assert (op instanceof Operator.Putfield || op instanceof Operator.AStore);
				// formal-out for instance field or array written in this method				
				getOrAdd(new Trio<Object,Inst,Integer>(q, domM.get(mIdx).getCFG().exit(), ONE));
				numFormalOut++;
			}
		}
		relMods.close();
	}

	// populates refsMap using relation refs, populates domain X with formal-in,
	// and modifies counter numFormalIn
	private void buildRefsMap() {	
		refsMap = new HashMap<Integer, HashSet<Integer>>();
		System.out.println("Start searching relation refs for Formal-in");
		ProgramRel relRefs = (ProgramRel) ClassicProject.g().getTrgt("refs");
		relRefs.load();
		System.out.println("refs rel size: " + relRefs.size());
		IntPairIterable refsTuples = relRefs.getAry2IntTuples();
		for (IntPair tuple : refsTuples) {
			int mIdx = tuple.idx0;
			int eIdx = tuple.idx1;			
			HashSet<Integer> refs = refsMap.get(mIdx);
			if (refs == null) {
				refs = new HashSet<Integer>();
				refsMap.put(mIdx, refs);
			}			
			refs.add(eIdx);
			Quad q = domE.get(eIdx);
			Operator op = q.getOperator();
			if (op instanceof Operator.Getstatic) {
				// formal-in for static field f read in this method
				jq_Field f = Operator.Getstatic.getField(q).getField();
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdx).getCFG().entry(), ZERO));
				numFormalIn++;
			} else {
				assert (op instanceof Operator.Getfield || op instanceof Operator.ALoad);
				// formal-in for instance field or array read in this method
				getOrAdd(new Trio<Object,Inst,Integer>(q, domM.get(mIdx).getCFG().entry(), ZERO));
				numFormalIn++;
			}
		}
		relRefs.close();
	}

	public String toUniqueString(Trio<Object,Inst,Integer> x) {
		
		String ret = super.toUniqueString(x);
		if (x.val1 instanceof BasicBlock) {
			ret = "<" + x.val0 + ", " + x.val1 + ":" + ((BasicBlock)x.val1).getMethod() + ", " + x.val2 +">";
		}
		
		return ret;
	}

}
