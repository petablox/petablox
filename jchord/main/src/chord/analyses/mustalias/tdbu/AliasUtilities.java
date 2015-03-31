package chord.analyses.mustalias.tdbu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.typestate.AccessPath;
import chord.analyses.typestate.Helper;
import chord.analyses.typestate.RegisterAccessPath;
import chord.program.Loc;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

public class AliasUtilities {
	/**
	 * TD
	 * 
	 * @param ms
	 * @param q
	 * @param m
	 * @return
	 */
	public static ArraySet<AccessPath> handleParametersTD(
			ArraySet<AccessPath> ms, Quad q, jq_Method m) {
		ParamListOperand args = Invoke.getParamList(q);
		RegisterFactory rf = m.getCFG().getRegisterFactory();
		ArraySet<AccessPath> ret = new ArraySet<AccessPath>(ms);
		for (int i = 0; i < args.length(); i++) {
			Register actualReg = args.get(i).getRegister();
			Register formalReg = rf.get(i);
			ret = handleMoveTD(ms, actualReg, formalReg);
		}
		return ret;
	}

	public static ArraySet<AccessPath> handleMoveTD(ArraySet<AccessPath> ms,
			Register from, Register to) {
		ArraySet<AccessPath> ret = new ArraySet<AccessPath>(ms);
		for (int j = -1; (j = Helper.getPrefixIndexInAP(ms, to, j)) >= 0;) {
			AccessPath rmAP = ms.get(j);
			ret.remove(rmAP);
		}
		for (int j = -1; (j = Helper.getPrefixIndexInAP(ms, from, j)) >= 0;) {
			AccessPath oldAP = ms.get(j);
			AccessPath newAP = new RegisterAccessPath(to, oldAP.fields);
			ret.add(newAP);
		}
		return ret;
	}

	/**
	 * TD
	 * 
	 * @param ms
	 * @param r
	 * @return
	 */
	public static ArraySet<AccessPath> killRegisterTD(ArraySet<AccessPath> ms,
			Register r) {
		ArraySet<AccessPath> ret = new ArraySet<AccessPath>();
		for (AccessPath ap : ms) {
			if (ap instanceof RegisterAccessPath) {
				RegisterAccessPath rap = (RegisterAccessPath) ap;
				if (!rap.var.equals(r))
					ret.add(rap);
			} else
				ret.add(ap);
		}
		return ret;
	}
	
	private static Map<jq_Method, Set<Register>> variableMap = new HashMap<jq_Method,Set<Register>>(); 
	
	/**
	 * Kill the local registers except for the return registers
	 * @param ms
	 * @param m
	 * @return
	 */
	public static ArraySet<AccessPath> killLocalRegisterTD(ArraySet<AccessPath> ms, jq_Method m){
		ArraySet<AccessPath> ret = new ArraySet<AccessPath>();
		Set<Register> vSet = getLocals(m);
		for (AccessPath ap : ms) {
			if (ap instanceof RegisterAccessPath) {
				RegisterAccessPath rap = (RegisterAccessPath) ap;
				if (!vSet.contains(rap.var)||rap.isRet)
					ret.add(rap);
			} else
				ret.add(ap);
		}
		return ret;
	}

	/**
	 * Kill the return register
	 * @param ms
	 * @param m
	 * @return
	 */
	public static ArraySet<AccessPath> killRetRegisterTD(ArraySet<AccessPath> ms){
		ArraySet<AccessPath> ret = new ArraySet<AccessPath>();
		for (AccessPath ap : ms) {
			if (ap instanceof RegisterAccessPath) {
				RegisterAccessPath rap = (RegisterAccessPath) ap;
				if (!rap.isRet)
					ret.add(rap);
			} else
				ret.add(ap);
		}
		return ret;
	}

	
	private static Set<Register> getLocals(jq_Method m){
		Set<Register> vSet = variableMap.get(m);
		if(vSet == null){
			vSet = new HashSet<Register>();
			for(Object o: m.getCFG().getRegisterFactory()){
				Register r = (Register)o;
				vSet.add(r);
			}
			variableMap.put(m, vSet);
		}
		return vSet;
	}
	
	public static MustAliasBUEdge liftBUPE(MustAliasBUEdge bupe,jq_Method m){
		int argNum = m.getParamTypes().length;
		Set<Register> args = new ArraySet<Register>();
		for(int i = 0; i < argNum; i++){
			args.add(m.getCFG().getRegisterFactory().get(i));
		}
		return bupe.lift(args);
	}
	
	public static MustAliasBUEdge checkBUPE(MustAliasBUEdge bupe,jq_Method m){
		int argNum = m.getParamTypes().length;
		Set<Register> args = new HashSet<Register>();
		for(int i = 0; i < argNum; i++){
			args.add(m.getCFG().getRegisterFactory().get(i));
		}
		return bupe.checkValid(args);
	}
	
	public static boolean firstContain(Set<Pair<Variable, Variable>> ps,
			Variable v) {
		for (Pair<Variable, Variable> p : ps)
			if (p.val0.equals(v))
				return true;
		return false;
	}

	public static boolean secondContain(Set<Pair<Variable, Variable>> ps,
			Variable v) {
		for (Pair<Variable, Variable> p : ps)
			if (p.val1.equals(v))
				return true;
		return false;
	}

	public static Loc quadToLoc(Quad q) {
		BasicBlock bb = q.getBasicBlock();
		return new Loc(q, bb.getQuadIndex(q));
	}
	
	public static FieldBitSet union(FieldBitSet l, FieldBitSet r){
		FieldBitSet ret = new FieldBitSet(l);
		ret.addAll(r);
		return ret;
	}
}
