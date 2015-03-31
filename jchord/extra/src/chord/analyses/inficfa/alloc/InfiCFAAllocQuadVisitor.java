package chord.analyses.inficfa.alloc;

import java.util.BitSet;
import java.util.Map;

import joeq.Class.jq_Array;
import joeq.Class.jq_Field;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.inficfa.BitAbstractState;
import chord.analyses.inficfa.BitEnv;
import chord.program.Program;
import chord.util.tuple.object.Pair;

public class InfiCFAAllocQuadVisitor extends QuadVisitor.EmptyVisitor {
	public BitAbstractState istate;    // immutable, will never be null
	public BitAbstractState ostate;    // mutable, initially ostate == istate
	public boolean isHeapModified;
	protected Map<jq_Type,BitSet> THFilterMap;
	protected Map<jq_Field, BitSet> FHMap;
	protected Map<Pair<Integer, jq_Field>, BitSet> HFHMap;
	protected BitSet trackedAlloc;
	protected DomH domH;
	protected boolean isHeapEditable;
	protected boolean useExtraFilters;
	private jq_Reference javaLangObject;
	
	
	public InfiCFAAllocQuadVisitor(Map<jq_Type,BitSet> THFilterMap, Map<jq_Field, BitSet> FHMap,
			Map<Pair<Integer, jq_Field>, BitSet> HFHMap, BitSet trackedAlloc, DomH domH, boolean isHeapEditable, boolean useExtraFilters){
		javaLangObject = Program.g().getClass("java.lang.Object");
		assert (javaLangObject != null);
		assert(THFilterMap != null);
		this.THFilterMap = THFilterMap;
		this.FHMap = FHMap;
		this.HFHMap = HFHMap;
		this.trackedAlloc = trackedAlloc;
		this.domH = domH;
		this.isHeapEditable = isHeapEditable;
		this.useExtraFilters = useExtraFilters;
		isHeapModified = false;
	}
	
	@Override
	public void visitCheckCast(Quad q) {
		Register dstR = CheckCast.getDest(q).getRegister();
		jq_Type dstRType = CheckCast.getType(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;
		
		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		if (CheckCast.getSrc(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) CheckCast.getSrc(q)).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);
			if(srcRPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(srcRPtsTo);
				BitSet filterSet = THFilterMap.get(dstRType);
				if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				if(!dstFiltered.isEmpty()){
					newLocalEnv.insert(dstR, dstFiltered);
					ostate = new BitAbstractState(newLocalEnv);
					return;
				}
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}
	}

	@Override
	public void visitMove(Quad q) {
		Register dstR = Move.getDest(q).getRegister();
		jq_Type dstRType = Move.getDest(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);
		
		if (Move.getSrc(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) Move.getSrc(q)).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);
			if(srcRPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(srcRPtsTo);
				BitSet filterSet = THFilterMap.get(dstRType);
				if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				if(!dstFiltered.isEmpty()){
					newLocalEnv.insert(dstR, dstFiltered);
					ostate = new BitAbstractState(newLocalEnv);
					return;
				}
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}
	}
	
	

	@Override
	public void visitPhi(Quad q) {
		assert false : "Use no PHI version of quad code!";
		Register dstR = Phi.getDest(q).getRegister();
		jq_Type dstRType = Phi.getDest(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;
		
		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);
		
		ParamListOperand ros = Phi.getSrcs(q);
		int n = ros.length();

		BitSet dstRPtsTo = null;

		for (int i = 0; i < n; i++) {
			RegisterOperand ro = ros.get(i);
			if (ro == null) continue;
			Register srcR = ((RegisterOperand) ro).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);

			if(srcRPtsTo != null){
				if(dstRPtsTo == null) dstRPtsTo = new BitSet();
				dstRPtsTo.or(srcRPtsTo);
			}
		}
		
		if(dstRPtsTo != null){
			BitSet filterSet = THFilterMap.get(dstRType);
			if(filterSet != null) dstRPtsTo.and(filterSet); else dstRPtsTo.clear();

			if(!dstRPtsTo.isEmpty()){
				newLocalEnv.insert(dstR, dstRPtsTo);
				ostate = new BitAbstractState(newLocalEnv);
				return;
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}

	}

	@Override
	public void visitNew(Quad q) {
		Register dstR = New.getDest(q).getRegister();

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		BitSet pointsTo = new BitSet();

		pointsTo.set(domH.indexOf(q));
		pointsTo.and(trackedAlloc);
		if(pointsTo.isEmpty()){
			pointsTo.set(0);
		}

		newLocalEnv.insert(dstR, pointsTo);	
		ostate = new BitAbstractState(newLocalEnv);
	}

	@Override
	public void visitNewArray(Quad q) {
		Register dstR = NewArray.getDest(q).getRegister();

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		BitSet pointsTo = new BitSet();

		pointsTo.set(domH.indexOf(q));
		pointsTo.and(trackedAlloc);
		if(pointsTo.isEmpty()){
			pointsTo.set(0);
		}

		newLocalEnv.insert(dstR, pointsTo);	
		ostate = new BitAbstractState(newLocalEnv);
	}

	@Override
	public void visitMultiNewArray(Quad q) {
		Register dstR = MultiNewArray.getDest(q).getRegister();

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		BitSet pointsTo = new BitSet();

		pointsTo.set(domH.indexOf(q));
		pointsTo.and(trackedAlloc);
		if(pointsTo.isEmpty()){
			pointsTo.set(0);
		}
		
		newLocalEnv.insert(dstR, pointsTo);	
		ostate = new BitAbstractState(newLocalEnv);
	}

	@Override
	public void visitALoad(Quad q) {
		Register dstR = ALoad.getDest(q).getRegister();
		jq_Type dstRType = ALoad.getDest(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		BitSet dstRPtsTo = null;
		
		if (ALoad.getBase(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) ALoad.getBase(q)).getRegister();
			BitSet basePointsTo = istate.envLocal.get(srcR);

			if(basePointsTo != null){
				for (int quad = basePointsTo.nextSetBit(0); quad >= 0; quad = basePointsTo.nextSetBit(quad+1)) {
					if(quad == 0){
						if(dstRPtsTo == null) dstRPtsTo = new BitSet();
						dstRPtsTo.set(0);
					}else{
						Pair<Integer, jq_Field> pair = new Pair<Integer, jq_Field>(quad, null);
						BitSet fieldPointsTo = HFHMap.get(pair);
						if(fieldPointsTo != null){
							if(dstRPtsTo == null) dstRPtsTo = new BitSet();
							dstRPtsTo.or(fieldPointsTo);
						}
					}
				}
			}

		}
		
		if(dstRPtsTo != null){
			//If the heap is not editable, the pointsTo information in the heap
			//is obtained via 0cfa and might contain untracked allocSites. This
			//necessitates checking if all the sites added to dstRPtsTo are tracked
			// and if not, adding the null site to dstRPtsTo
			int cardinality = dstRPtsTo.cardinality();
			dstRPtsTo.and(trackedAlloc);
			cardinality = cardinality - dstRPtsTo.cardinality();
			if(cardinality > 0)
				dstRPtsTo.set(0);
			
			BitSet filterSet = THFilterMap.get(dstRType);
			if(filterSet != null) dstRPtsTo.and(filterSet); else dstRPtsTo.clear();

			if(!dstRPtsTo.isEmpty()){
				newLocalEnv.insert(dstR, dstRPtsTo);
				ostate = new BitAbstractState(newLocalEnv);
				return;
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}
	}

	@Override
	public void visitAStore(Quad q) {
		if(!isHeapEditable)
			return;		
		
		if(!(AStore.getBase(q) instanceof RegisterOperand))
			return;
		
		
		Register dstR = ((RegisterOperand)AStore.getBase(q)).getRegister();
		jq_Type dstRType = ((RegisterOperand)AStore.getBase(q)).getType();
		if(dstRType == null)
			dstRType = javaLangObject;
		else if(dstRType.isArrayType())
			dstRType = ((jq_Array)dstRType).getElementType();

		if (AStore.getValue(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) AStore.getValue(q)).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);
			BitSet dstRPtsTo = istate.envLocal.get(dstR);
			if(srcRPtsTo != null && dstRPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(srcRPtsTo);
				if(useExtraFilters){
					BitSet filterSet = THFilterMap.get(dstRType);
					if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				}
				if(!dstFiltered.isEmpty()){
					for (int dstQ = dstRPtsTo.nextSetBit(0); dstQ >= 0; dstQ = dstRPtsTo.nextSetBit(dstQ+1)) {
						Pair<Integer,jq_Field> p = new Pair<Integer, jq_Field>(dstQ, null);
						BitSet fPointsTo = HFHMap.get(p);
						if(fPointsTo == null){
							fPointsTo = new BitSet();
							HFHMap.put(p, fPointsTo);
						}
						int cardinality = fPointsTo.cardinality();
						fPointsTo.or(dstFiltered);
						cardinality = fPointsTo.cardinality() - cardinality;
						if(cardinality > 0)
							isHeapModified = true;
					}
				}
			}
		}
	}

	//Not tracking globals, so get info via 0-cfa
	@Override
	public void visitGetstatic(Quad q) {
		Register dstR = Getstatic.getDest(q).getRegister();
		jq_Type dstRType = Getstatic.getDest(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;
		
		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		jq_Field srcF = Getstatic.getField(q).getField();
		BitSet staticPointsTo = FHMap.get(srcF);
		
		if(staticPointsTo != null){
			BitSet dstFiltered = new BitSet();
			dstFiltered.or(staticPointsTo);
			
			//If the heap is not editable, the pointsTo information in the heap
			//is obtained via 0cfa and might contain untracked allocSites. This
			//necessitates checking if all the sites added to dstFiltered are tracked
			// and if not, adding the null site to dstFiltered
			int cardinality = dstFiltered.cardinality();
			dstFiltered.and(trackedAlloc);
			cardinality = cardinality - dstFiltered.cardinality();
			if(cardinality > 0)
				dstFiltered.set(0);
			
			BitSet filterSet = THFilterMap.get(dstRType);
			if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();			
			if(!dstFiltered.isEmpty()){
				newLocalEnv.insert(dstR, dstFiltered);
				ostate = new BitAbstractState(newLocalEnv);
				return;
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}
	}

	//Not tracking globals
	@Override
	public void visitPutstatic(Quad q) { 
		if(!isHeapEditable)
			return;
		
		jq_Field dstF = Putstatic.getField(q).getField();
		jq_Type dstFType = Putstatic.getField(q).getField().getType();
		dstFType = dstFType != null ? dstFType : javaLangObject;

		if (Putstatic.getSrc(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) Putstatic.getSrc(q)).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);
			if(srcRPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(srcRPtsTo);
				if(useExtraFilters){
					BitSet filterSet = THFilterMap.get(dstFType);
					if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				}
				if(!dstFiltered.isEmpty()){
					BitSet fPointsTo = FHMap.get(dstF);
					if(fPointsTo == null){
						fPointsTo = new BitSet();
						FHMap.put(dstF, fPointsTo);
					}
					int cardinality = fPointsTo.cardinality();
					fPointsTo.or(dstFiltered);
					cardinality = fPointsTo.cardinality() - cardinality;
					if(cardinality > 0)
						isHeapModified = true;
				}
			}
		}
	}

	//Not tracking heap
	@Override
	public void visitPutfield(Quad q) {
		if(!isHeapEditable)
			return;
		
		if(!(Putfield.getBase(q) instanceof RegisterOperand))
			return;
		
		Register dstR = ((RegisterOperand)Putfield.getBase(q)).getRegister();
		jq_Field dstF = Putfield.getField(q).getField();
		jq_Type dstFType = Putfield.getField(q).getField().getType();
		dstFType = dstFType != null ? dstFType : javaLangObject;

		if (Putfield.getSrc(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
			BitSet srcRPtsTo = istate.envLocal.get(srcR);
			BitSet dstRPtsTo = istate.envLocal.get(dstR);
			if(srcRPtsTo != null && dstRPtsTo != null){
				BitSet dstFiltered = new BitSet();
				dstFiltered.or(srcRPtsTo);
				if(useExtraFilters){
					BitSet filterSet = THFilterMap.get(dstFType);
					if(filterSet != null) dstFiltered.and(filterSet); else dstFiltered.clear();
				}
				if(!dstFiltered.isEmpty()){
					for (int dstQ = dstRPtsTo.nextSetBit(0); dstQ >= 0; dstQ = dstRPtsTo.nextSetBit(dstQ+1)) {
						Pair<Integer,jq_Field> p = new Pair<Integer, jq_Field>(dstQ, dstF);
						BitSet fPointsTo = HFHMap.get(p);
						if(fPointsTo == null){
							fPointsTo = new BitSet();
							HFHMap.put(p, fPointsTo);
						}
						int cardinality = fPointsTo.cardinality();
						fPointsTo.or(dstFiltered);
						cardinality = fPointsTo.cardinality() - cardinality;
						if(cardinality > 0)
							isHeapModified = true;
					}
				}
			}
		}
	}

	@Override
	public void visitGetfield(Quad q) {
		Register dstR = Getfield.getDest(q).getRegister();
		jq_Type dstRType = Getfield.getDest(q).getType();
		dstRType = dstRType != null ? dstRType : javaLangObject;

		BitEnv<Register> newLocalEnv = new BitEnv<Register>(istate.envLocal);

		BitSet dstRPtsTo = null;
		
		if (Getfield.getBase(q) instanceof RegisterOperand) {
			Register srcR = ((RegisterOperand) Getfield.getBase(q)).getRegister();
			jq_Field srcF = Getfield.getField(q).getField();
			
			BitSet basePointsTo = istate.envLocal.get(srcR);
			
			if(basePointsTo != null){
				for (int quad = basePointsTo.nextSetBit(0); quad >= 0; quad = basePointsTo.nextSetBit(quad+1)) {
					if(quad == 0){
						if(dstRPtsTo == null) dstRPtsTo = new BitSet();
						dstRPtsTo.set(0);
					}else{
						Pair<Integer, jq_Field> pair = new Pair<Integer, jq_Field>(quad, srcF);
						BitSet fieldPointsTo = HFHMap.get(pair);
						if(fieldPointsTo != null){
							if(dstRPtsTo == null) dstRPtsTo = new BitSet();
							dstRPtsTo.or(fieldPointsTo);
						}
					}
				}
			}
		}
		
		if(dstRPtsTo != null){
			//If the heap is not editable, the pointsTo information in the heap
			//is obtained via 0cfa and might contain untracked allocSites. This
			//necessitates checking if all the sites added to dstRPtsTo are tracked
			// and if not, adding the null site to dstRPtsTo
			int cardinality = dstRPtsTo.cardinality();
			dstRPtsTo.and(trackedAlloc);
			cardinality = cardinality - dstRPtsTo.cardinality();
			if(cardinality > 0)
				dstRPtsTo.set(0);
			
			BitSet filterSet = THFilterMap.get(dstRType);
			if(filterSet != null) dstRPtsTo.and(filterSet); else dstRPtsTo.clear();

			if(!dstRPtsTo.isEmpty()){
				newLocalEnv.insert(dstR, dstRPtsTo);
				ostate = new BitAbstractState(newLocalEnv);
				return;
			}
		}
		
		if(newLocalEnv.remove(dstR) != null){
			ostate = new BitAbstractState(newLocalEnv);
		}
	}

	@Override
	public void visitReturn(Quad q) {
		if (q.getOperator() instanceof THROW_A) return;

		if (Return.getSrc(q) instanceof RegisterOperand) {
			Register tgtR = ((RegisterOperand) (Return.getSrc(q))).getRegister();
			BitSet tgtRPtsTo = istate.envLocal.get(tgtR);
			if(tgtRPtsTo != null)
				ostate = new BitAbstractState(istate.envLocal, tgtRPtsTo);
		}
	}
}
