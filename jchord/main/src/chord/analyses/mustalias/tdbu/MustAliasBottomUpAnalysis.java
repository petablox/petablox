package chord.analyses.mustalias.tdbu;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.ICICG;
import chord.analyses.typestate.AbstractState;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.Edge;
import chord.analyses.typestate.EdgeKind;
import chord.program.Loc;
import chord.project.analyses.tdbu.BottomUpAnalysis;
import chord.project.analyses.tdbu.CaseTDSEComparator;
import chord.project.analyses.tdbu.Constraint;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

public class MustAliasBottomUpAnalysis extends
		BottomUpAnalysis<Edge, Edge, MustAliasBUEdge, MustAliasBUEdge> {
	private MyQuadVisitor qv;
	
	public MustAliasBottomUpAnalysis(ICICG callGraph, int bULimit,int bUPELimit, Map<jq_Method, Set<jq_Method>> rmsMap, Set<jq_Method> noTDSEMs) {
		super(callGraph, bULimit, bUPELimit, rmsMap, noTDSEMs);
		qv = new MyQuadVisitor();
	}

	@Override
	protected Set<MustAliasBUEdge> transfer(MustAliasBUEdge inEdge, Quad q) {
		qv.identity(inEdge);
		q.accept(qv);
		return qv.output;
	}

	@Override
	protected Pair<Loc, Set<MustAliasBUEdge>> getInitialBUEdge(jq_Method m) {
		BasicBlock bb = m.getCFG().entry();
        Loc loc = new Loc(bb, -1);
        Set<MustAliasBUEdge> retEdges = new HashSet<MustAliasBUEdge>();
        Pair<Loc, Set<MustAliasBUEdge>> ret = new Pair<Loc, Set<MustAliasBUEdge>>(loc,retEdges);
        MustAliasBUEdge initEdge = new MustAliasBUEdge();
        retEdges.add(initEdge);
		return ret;
	}

	@Override
	protected Set<MustAliasBUEdge> getDefaultSummaries(jq_Method m) {
		return new HashSet<MustAliasBUEdge>();
	}

	@Override
	protected CaseTDSEComparator<Edge> getCaseCMP(Set<Edge> tdses) {
		return new AliasCaseTDSECMP(tdses);
	}

	@Override
	protected MustAliasBUEdge lift(MustAliasBUEdge bupe, jq_Method m) {
		MustAliasBUEdge ret = AliasUtilities.liftBUPE(bupe, m);
		return ret;
	}

	@Override
	protected Constraint getTrue() {
		return new AliasConstraint(true);
	}

	public void printSummaries(PrintStream out){
		for(Map.Entry<jq_Method, Set<MustAliasBUEdge>> entry: this.summEdges.entrySet()){
			jq_Method m = entry.getKey();
			System.out.println("Method: "+m);
			System.out.print("Parameters:");
			RegisterFactory rf = m.getCFG().getRegisterFactory();
			for(int i = 0; i < m.getParamTypes().length; i++){
				System.out.print(" "+rf.get(i));
			}
			System.out.println();
			for(MustAliasBUEdge edge: entry.getValue())
			System.out.println(edge);
		}
	}
	
	@Override
	public int countEffectiveTDSE(Set<Edge> seSet) {
		if(seSet == null)
			return 0;
		int ret = 0;
		for(Edge e:seSet)
			if(e.type.equals(EdgeKind.FULL))
				ret++;
		return ret;
	}

	public int getBUSENumber(jq_Method m){
		Set<Constraint> consSet = new ArraySet<Constraint>();
		Set<MustAliasBUEdge> seSet = this.summEdges.get(m);
		if(seSet!=null)
			for(MustAliasBUEdge buse : seSet){
				consSet.add(buse.getConstraint());
			}
		return consSet.size();
	}
	
	public int getTotalBUSENumber(){
		int ret = 0;
		for(jq_Method m : summEdges.keySet())
			ret += getBUSENumber(m);
		return ret;
	}
	
	class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
		MustAliasBUEdge input;
		Set<MustAliasBUEdge> output = new HashSet<MustAliasBUEdge>();

		public void identity(MustAliasBUEdge input) {
			this.input = input;
			output.clear();
			output.add(input);
		}

		@Override
		public void visitALoad(Quad obj) {
			RegisterOperand ro = ALoad.getDest(obj);
			if(!ro.getType().isReferenceType()){
				return;
			}
			output.clear();
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(new Variable(ro.getRegister()));
			applyTrans(transSet);
		}

		@Override
		public void visitCheckCast(Quad obj) {
			this.visitMove(obj);
		}

		@Override
		public void visitGetfield(Quad obj) {
			jq_Field f = Getfield.getField(obj).getField();
			if(!f.getType().isReferenceType()){
				return;
			}
			output.clear();
			RegisterOperand tro = Getfield.getDest(obj);
			Variable to = new Variable(tro.getRegister());
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructGetField(f, to);
			applyTrans(transSet);
		}

		@Override
		public void visitGetstatic(Quad obj) {
			jq_Field f = Getstatic.getField(obj).getField();
			if(!f.getType().isReferenceType()){
				return;
			}
			output.clear();
			Variable from = new Variable(f);
			RegisterOperand tro = Getfield.getDest(obj);
			Variable to = new Variable(tro.getRegister());
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMove(from, to);
			applyTrans(transSet);
		}

		@Override
		public void visitMove(Quad obj) {
			RegisterOperand lo = Move.getDest(obj);
			if (!lo.getType().isReferenceType()){
				return;
				}
			output.clear();
			Variable to = new Variable(lo.getRegister());
			Operand ro = Move.getSrc(obj);
			if (ro instanceof RegisterOperand) {
				RegisterOperand cro = (RegisterOperand) ro;
				Variable from = new Variable(cro.getRegister());
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMove(
						from, to);
				applyTrans(transSet);
			}
			else{
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(to);
				applyTrans(transSet);
			}
		}

		@Override
		public void visitNew(Quad obj) {
			output.clear();
			Variable to = new Variable(New.getDest(obj).getRegister());
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(to);
			applyTrans(transSet);
		}

		@Override
		public void visitNewArray(Quad obj) {
			output.clear();
			Variable to = new Variable(New.getDest(obj).getRegister());
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(to);
			applyTrans(transSet);
		}

		@Override
		public void visitMultiNewArray(Quad obj) {
			output.clear();
			Variable to = new Variable(New.getDest(obj).getRegister());
			Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(to);
			applyTrans(transSet);
		}

		@Override
		public void visitPhi(Quad obj) {
			throw new RuntimeException("Check the chord options, phi instructions should be removed.");
		}

		@Override
		public void visitPutfield(Quad obj) {
			jq_Field f = Putfield.getField(obj).getField();
			if (!f.getType().isReferenceType()){
				return;
			}
			output.clear();
			Operand src = Putfield.getSrc(obj);
			if(src instanceof RegisterOperand){
				RegisterOperand fro = (RegisterOperand)src;
				Variable from = new Variable(fro.getRegister());
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructPutField(from, f);
				applyTrans(transSet);
			}else{
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructPutFieldNull(f);
				applyTrans(transSet);	
			}
		}

		@Override
		public void visitPutstatic(Quad obj) {
			jq_Field f = Putstatic.getField(obj).getField();
			if(!f.getType().isReferenceType()){
				return;
			}
			output.clear();
			Variable to = new Variable(f);
			Operand src = Putstatic.getSrc(obj);
			if(src instanceof RegisterOperand){
				RegisterOperand fro = (RegisterOperand)src;
				Variable from = new Variable(fro.getRegister());
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMove(from, to);
				applyTrans(transSet);
			}else{
				Set<MustAliasBUEdge> transSet = MustAliasBUEdge.constructMoveNull(to);
				applyTrans(transSet);
			}
		}

		@Override
		public void visitReturn(Quad obj) {
			if (obj.getOperator() instanceof THROW_A){
                return;
			}
            if (Return.getSrc(obj) instanceof RegisterOperand) {
                Register tgtR = ((RegisterOperand) (Return.getSrc(obj))).getRegister();
                if(tgtR.getType().isReferenceType()){
                	for(MustAliasBUEdge bu : output)
                		bu.setRet(tgtR);
                }
            }
		}

		private void applyTrans(Set<MustAliasBUEdge> transSet) {
			for (MustAliasBUEdge trans : transSet) {
				MustAliasBUEdge pos = (MustAliasBUEdge) trans
						.apply(input, null);
				if (pos != null)
					output.add(pos);
			}
		}
		
	}


	@Override
	protected Set<Edge> getDefaultTDSESet(jq_Method m, Set<Edge> rootSumms) {
		AbstractState abs = new AbstractState(true,null,new ArraySet<AccessPath>());
		Quad dftH = null;
		for(Edge tdse:rootSumms)
			if(tdse.type == EdgeKind.FULL){
				dftH = tdse.h;
				break;
			}
		Edge retEdge = new Edge(abs,abs,EdgeKind.FULL,dftH);
		Set<Edge> ret = new ArraySet<Edge>();
		ret.add(retEdge);
		return ret;
	}

	@Override
	protected boolean checkExcludedClrPE(Edge clrPE) {
		if(clrPE.dstNode!=null)
			return clrPE.dstNode.ms.isEmpty();
		return true;
	}
}
