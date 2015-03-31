package chord.analyses.mustalias.bu;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
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
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.ICICG;
import chord.analyses.mustalias.tdbu.AliasConstraint;
import chord.analyses.mustalias.tdbu.AliasUtilities;
import chord.analyses.mustalias.tdbu.MustAliasBUEdge;
import chord.analyses.mustalias.tdbu.Variable;
import chord.analyses.typestate.Edge;
import chord.program.Loc;
import chord.project.analyses.tdbu.Constraint;
import chord.project.analyses.tdbu.StandaloneBUAnalysis;
import chord.util.Timer;
import chord.util.tuple.object.Pair;

public class MustAliasBottomUpAnalysis extends StandaloneBUAnalysis<Edge, Edge, MustAliasBUEdge, MustAliasBUEdge> {
	private MyQuadVisitor qv;
	private Timer timer;

	public MustAliasBottomUpAnalysis(ICICG callGraph, Map<jq_Method, Set<jq_Method>> rmsMap) {
		super(callGraph, rmsMap);
		qv = new MyQuadVisitor();
		timer = new Timer();
	}
	
	public Map<jq_Method,Set<MustAliasBUEdge>> getSummaries(){
		return super.summEdges;
	}

	@Override
	public void run() {
		System.out.println("Start BU:");
		timer.init();
		super.run();
		timer.done();
		long inclusiveTime = timer.getInclusiveTime();
		System.out.println("BU Total running time: "
				+ Timer.getTimeStr(inclusiveTime));
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
	protected MustAliasBUEdge lift(MustAliasBUEdge bupe, jq_Method m) {
		MustAliasBUEdge ret = AliasUtilities.liftBUPE(bupe, m);
		return ret;
	}

	@Override
	protected Constraint getTrue() {
		return new AliasConstraint(true);
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
                	output.iterator().next().setRet(tgtR);
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
}
