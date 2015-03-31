package chord.analyses.inficfa.clients;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.inficfa.BitAbstractState;
import chord.analyses.inficfa.BitEdge;
import chord.analyses.inficfa.alloc.AllocEnvCFAAnalysis;
import chord.analyses.typestate.Edge;
import chord.analyses.typestate.Helper;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

@Chord(name = "allocEnvCFAClients-java", consumes = { "HT", "sub", "H", "M", "FH", "HFH", "IHM", "THfilter", "rootM", "NonTerminatingM",
		"conNewInstIH", "conNewInstIM", "objNewInstIH", "objNewInstIM", "aryNewInstIH" })
public class AllocEnvCFAClientsAnalysis extends AllocEnvCFAAnalysis {
	
	private boolean isInit = false;
	protected Map<Quad, Set<jq_Type>> HTMap;
	protected HashMap<jq_Type,Set<jq_Type>> TTFilterMap;
	protected HashMap<Quad,Set<jq_Method>> IMMap;
	protected HashSet<Pair<Register, jq_Type>> downCastQueries;
	protected HashSet<Pair<Register, jq_Type>> unsafeDownCastQueries;
	protected HashSet<Quad> monositeQueries;
	protected HashSet<Quad> failedMonositeQueries;
	
	@Override
	public void init() {
		// XXX: do not compute anything here which needs to be re-computed on each call to run() below.

		if (isInit) return;
		isInit = true;

		super.init();
		
		//build HTMap
		{
			HTMap = new HashMap<Quad, Set<jq_Type>>();
			ProgramRel relHT = (ProgramRel) ClassicProject.g().getTrgt("HT");
			relHT.load();

			Iterable<Pair<Quad, jq_Type>> tuples = relHT.getAry2ValTuples();
			for (Pair<Quad, jq_Type> t : tuples){
				Set<jq_Type> allocType = HTMap.get(t.val0);
				if (allocType == null) {
					allocType = new ArraySet<jq_Type>();
					HTMap.put(t.val0, allocType);
				}
				allocType.add(t.val1);
			}
			relHT.close();
		}
		
		//build VTTFilter
		{
			TTFilterMap = new HashMap<jq_Type,Set<jq_Type>>();
			ProgramRel relTTFilter = (ProgramRel) ClassicProject.g().getTrgt("sub");
			relTTFilter.load();

			Iterable<Pair<jq_Type, jq_Type>> tuples = relTTFilter.getAry2ValTuples();
			for (Pair<jq_Type, jq_Type> t : tuples){
				Set<jq_Type> filterTypes = TTFilterMap.get(t.val1);
				if (filterTypes == null) {
					filterTypes = new ArraySet<jq_Type>();
					TTFilterMap.put(t.val1, filterTypes);
					//filterTypes.add(null); //No need to add this since sub relation already
					//has jq_NullType.NULL_TYPE as subtype of all types
				}
				filterTypes.add(t.val0);
			}
			relTTFilter.close();
		}

		IMMap = new HashMap<Quad, Set<jq_Method>>();
		downCastQueries = new HashSet<Pair<Register,jq_Type>>();
		unsafeDownCastQueries = new HashSet<Pair<Register,jq_Type>>();
		monositeQueries = new HashSet<Quad>();
		failedMonositeQueries = new HashSet<Quad>();
	}
	
	@Override
	protected void generateQueries(){
		System.out.println("ENTER generateQueries");

		//1. Callgraph client
		super.generateQueries();
	
		//Construct IMMap from CICM
		{
			for(Pair<BitAbstractState,Quad> p1 : CICM.keySet()){
				Set<jq_Method> mSet = IMMap.get(p1.val1);
				if(mSet == null){
					mSet = new ArraySet<jq_Method>();
					IMMap.put(p1.val1, mSet);
				}
				for(Pair<BitAbstractState, jq_Method> p2 : CICM.get(p1)){
					mSet.add(p2.val1);
				}
			}
		}
		
		//Downcast & Monosite Clients
		for (Inst x : pathEdges.keySet()) {
			if (x instanceof Quad) {
				Quad i = (Quad) x;
				Operator op = i.getOperator();
				
				//Downcast
				if (op instanceof Move || op instanceof Phi || op instanceof CheckCast) {
					Set<BitEdge<Quad>> peSet = pathEdges.get(i);
					assert (peSet != null);
					RegisterOperand dstReg = null;
					Set<RegisterOperand> srcRegs = new ArraySet<RegisterOperand>();
					Set<Pair<Register, jq_Type>> localDowncastQueries = new HashSet<Pair<Register,jq_Type>>();

					if(op instanceof Move){
						Operand rx = Move.getSrc(i);
						if (rx instanceof RegisterOperand) {
							RegisterOperand ro = (RegisterOperand) rx;
							if (ro.getType().isReferenceType()) {
								srcRegs.add(ro);
								dstReg = Move.getDest(i);;
							}
						}
					}else if(op instanceof CheckCast){
						Operand rx = CheckCast.getSrc(i);
						if (rx instanceof RegisterOperand) {
							RegisterOperand ro = (RegisterOperand) rx;
							if (ro.getType().isReferenceType()) {
								srcRegs.add(ro);
								dstReg = CheckCast.getDest(i);
							}
						}
					}else{
						RegisterOperand lo = Phi.getDest(i);
						if (lo.getType().isReferenceType()) {
							dstReg = lo;
							ParamListOperand ros = Phi.getSrcs(i);
							int n = ros.length();
							for (int dstIndx = 0; dstIndx < n; dstIndx++) {
								RegisterOperand ro = ros.get(dstIndx);
								if (ro != null)
									srcRegs.add(ro);
							}
						}
					}

					if(dstReg != null && !srcRegs.isEmpty()){
						jq_Type dstRType = dstReg.getType();
						dstRType = (dstRType != null) ? dstRType : javaLangObject;

						for(RegisterOperand srcReg : srcRegs){
							jq_Type srcRType = srcReg.getType();
							srcRType = (srcRType != null) ? srcRType : javaLangObject;
							Set<jq_Type> dstSub = TTFilterMap.get(dstRType);
							if(dstSub == null || !dstSub.contains(srcRType)){
								localDowncastQueries.add(new Pair<Register, jq_Type>(srcReg.getRegister(), dstRType));
							}
						}
						
						downCastQueries.addAll(localDowncastQueries);

						for(Pair<Register, jq_Type> dquery : localDowncastQueries){
							boolean qUnsafe = false;
							for (BitEdge<Quad> pe : peSet) {
								if(qUnsafe) break;
								assert (pe.dstNode != null);
								BitSet srcPtsTo = pe.dstNode.envLocal.get(dquery.val0);
								if(srcPtsTo != null){
									for (int srcQ = srcPtsTo.nextSetBit(0); srcQ >= 0 && !qUnsafe; srcQ = srcPtsTo.nextSetBit(srcQ+1)) {
										Set<jq_Type> interTypes = new HashSet<jq_Type>(HTMap.get(domH.get(srcQ)));
										Set<jq_Type> dstSub = TTFilterMap.get(dquery.val1);
										if(dstSub != null)
											interTypes.removeAll(dstSub);
										
										if(!interTypes.isEmpty()){
											unsafeDownCastQueries.add(dquery);
											qUnsafe = true;
										}
									}
								}
							}
						}
					}

				}

				if(op instanceof Invoke){
					if(!(op instanceof InvokeStatic)){
						monositeQueries.add(i);
						Set<jq_Method> calledMethods = IMMap.get(i);
						if(calledMethods != null && calledMethods.size() > 1)
							failedMonositeQueries.add(i);
					}
				}
			}
		}
		
		PrintWriter out6 = OutDirUtils.newPrintWriter("allDowncastQueries_CFA2.txt");
		for(Pair<Register,jq_Type> p : downCastQueries){
			out6.println(p.val0 + " " + p.val1);
		}
		out6.close();
		
		PrintWriter out7 = OutDirUtils.newPrintWriter("unsafeDowncastQueries_CFA2.txt");
		for(Pair<Register,jq_Type> p : unsafeDownCastQueries){
			out7.println(p.val0 + " " + p.val1);
		}
		out7.close();
		
		PrintWriter out8 = OutDirUtils.newPrintWriter("allMonositeQueries_CFA2.txt");
		for(Quad q : monositeQueries){
			out8.println(q);
		}
		out8.close();
		
		PrintWriter out9 = OutDirUtils.newPrintWriter("failedMonositeQueries_CFA2.txt");
		for(Quad q : failedMonositeQueries){
			out9.println(q);
		}
		out9.close();
		
		System.out.println("EXIT generateQueries");
	}
}
