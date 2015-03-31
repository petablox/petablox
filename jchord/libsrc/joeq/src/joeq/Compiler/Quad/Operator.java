// Operator.java, created Fri Jan 11 16:42:38 2002 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Compiler.Quad;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_Primitive;
import joeq.Class.jq_Type;
import joeq.Compiler.BytecodeAnalysis.BytecodeVisitor;
import joeq.Compiler.Quad.Operand.BasicBlockTableOperand;
import joeq.Compiler.Quad.Operand.ConditionOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.IConstOperand;
import joeq.Compiler.Quad.Operand.IntValueTableOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.TargetOperand;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Memory.Address;
import jwutil.util.Assert;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: Operator.java,v 1.46 2004/09/22 22:17:26 joewhaley Exp $
 */
public abstract class Operator implements Serializable {
	private static <T> List<T> singletonList(T v) {
		List<T> l = new ArrayList<T>(1);
		l.add(v); return l;
	}
	private static <T> List<T> doubletonList(T v1, T v2) {
		List<T> l = new ArrayList<T>(2);
		l.add(v1); l.add(v2); return l;
	}
	private static <T> List<T> tripletonList(T v1, T v2, T v3) {
		List<T> l = new ArrayList<T>(3);
		l.add(v1); l.add(v2); l.add(v3); return l;
	}
	private static <T> List<T> quadrupleList(T v1, T v2, T v3, T v4) {
		List<T> l = new ArrayList<T>(4);
		l.add(v1); l.add(v2); l.add(v3); l.add(v4); return l;
	}
	
    public void accept(Quad q, QuadVisitor qv) {
        qv.visitQuad(q);
    }
    public List<jq_Class> getThrownExceptions() {
        return noexceptions;
    }
    public List<RegisterOperand> getDefinedRegisters(Quad q) {
        return noregisters;
    }
    public List<RegisterOperand> getUsedRegisters(Quad q) {
        return noregisters;
    }

    public abstract boolean hasSideEffects();
    
    public static List<RegisterOperand> getReg1(Quad q) {
        return singletonList((RegisterOperand)q.getOp1());
    }
    public static List<RegisterOperand> getReg1_check(Quad q) {
        if (q.getOp1() instanceof RegisterOperand)
            return singletonList((RegisterOperand)q.getOp1());
        else
            return noregisters;
    }
    public static List<RegisterOperand> getReg2(Quad q) {
        if (q.getOp2() instanceof RegisterOperand)
            return singletonList((RegisterOperand)q.getOp2());
        else
            return noregisters;
    }
    public static List<RegisterOperand> getReg3(Quad q) {
        if (q.getOp3() instanceof RegisterOperand)
            return singletonList((RegisterOperand)q.getOp3());
        else
            return noregisters;
    }
    protected static List<RegisterOperand> getReg12(Quad q) {
        if (q.getOp1() instanceof RegisterOperand) {
            if (q.getOp2() instanceof RegisterOperand)
                return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2());
            else
                return singletonList((RegisterOperand)q.getOp1());
        } else {
            if (q.getOp2() instanceof RegisterOperand)
                return singletonList((RegisterOperand)q.getOp2());
            else
                return noregisters;
        }
    }
    protected static List<RegisterOperand> getReg23(Quad q) {
        if (q.getOp2() instanceof RegisterOperand) {
            if (q.getOp3() instanceof RegisterOperand)
                return doubletonList((RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3());
            else
                return singletonList((RegisterOperand)q.getOp2());
        } else {
            if (q.getOp3() instanceof RegisterOperand)
                return singletonList((RegisterOperand)q.getOp3());
            else
                return noregisters;
        }
    }
    protected static List<RegisterOperand> getReg24(Quad q) {
        if (q.getOp2() instanceof RegisterOperand) {
            if (q.getOp4() instanceof RegisterOperand)
                return doubletonList((RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp4());
            else
                return singletonList((RegisterOperand)q.getOp2());
        } else {
            if (q.getOp4() instanceof RegisterOperand)
                return singletonList((RegisterOperand)q.getOp4());
            else
                return noregisters;
        }
    }
    protected static List<RegisterOperand> getReg124(Quad q) {
        if (q.getOp1() instanceof RegisterOperand) {
            if (q.getOp2() instanceof RegisterOperand) {
                if (q.getOp4() instanceof RegisterOperand)
                    return tripletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp4());
                else
                    return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2());
            } else {
                if (q.getOp4() instanceof RegisterOperand)
                    return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp4());
                else
                    return noregisters;
            }
        } else return getReg24(q);
    }
    protected static List<RegisterOperand> getReg123(Quad q) {
        if (q.getOp1() instanceof RegisterOperand) {
            if (q.getOp2() instanceof RegisterOperand) {
                if (q.getOp3() instanceof RegisterOperand)
                    return tripletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3());
                else
                    return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2());
            } else {
                if (q.getOp3() instanceof RegisterOperand)
                    return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp3());
                else
                    return singletonList((RegisterOperand)q.getOp1());
            }
        } else return getReg23(q);
    }
    protected static List<RegisterOperand> getReg234(Quad q) {
        if (q.getOp2() instanceof RegisterOperand) {
            if (q.getOp3() instanceof RegisterOperand) {
                if (q.getOp4() instanceof RegisterOperand)
                    return tripletonList((RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3(), (RegisterOperand)q.getOp4());
                else
                    return doubletonList((RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3());
            } else {
                if (q.getOp4() instanceof RegisterOperand)
                    return doubletonList((RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp4());
                else
                    return singletonList((RegisterOperand)q.getOp2());
            }
        } else {
            if (q.getOp3() instanceof RegisterOperand) {
                if (q.getOp4() instanceof RegisterOperand)
                    return doubletonList((RegisterOperand)q.getOp3(), (RegisterOperand)q.getOp4());
                else
                    return singletonList((RegisterOperand)q.getOp3());
            } else {
                if (q.getOp4() instanceof RegisterOperand)
                    return singletonList((RegisterOperand)q.getOp4());
                else
                    return noregisters;
            }
        }
    }
    protected static List<RegisterOperand> getReg1234(Quad q) {
        if (q.getOp1() instanceof RegisterOperand) {
            if (q.getOp2() instanceof RegisterOperand) {
                if (q.getOp3() instanceof RegisterOperand) {
                    if (q.getOp4() instanceof RegisterOperand)
                        return quadrupleList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3(), (RegisterOperand)q.getOp4());
                    else
                        return tripletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp3());
                } else {
                    if (q.getOp4() instanceof RegisterOperand)
                        return tripletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2(), (RegisterOperand)q.getOp4());
                    else
                        return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp2());
                }
            } else {
                if (q.getOp3() instanceof RegisterOperand) {
                    if (q.getOp4() instanceof RegisterOperand)
                        return tripletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp3(), (RegisterOperand)q.getOp4());
                    else
                        return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp3());
                } else {
                    if (q.getOp4() instanceof RegisterOperand)
                        return doubletonList((RegisterOperand)q.getOp1(), (RegisterOperand)q.getOp4());
                    else
                        return singletonList((RegisterOperand)q.getOp1());
                }
            }
        } else return getReg234(q);
    }
    
    public static final List<RegisterOperand> noregisters;
    public static final List<jq_Class> noexceptions;
    public static final List<jq_Class> anyexception;
    public static final List<jq_Class> resolutionexceptions;
    public static final List<jq_Class> nullptrexception;
    public static final List<jq_Class> arrayboundsexception;
    public static final List<jq_Class> arithexception;
    public static final List<jq_Class> arraystoreexception;
    public static final List<jq_Class> negativesizeexception;
    public static final List<jq_Class> classcastexceptions;
    public static final List<jq_Class> illegalmonitorstateexception;
    static {
        noregisters = Collections.emptyList();
        noexceptions = Collections.emptyList();
        anyexception = singletonList(PrimordialClassLoader.getJavaLangThrowable());
        resolutionexceptions = anyexception; // a little conservative
        nullptrexception = singletonList(
        	PrimordialClassLoader.getJavaLangNullPointerException());
        arrayboundsexception = singletonList(
        	PrimordialClassLoader.getJavaLangArrayIndexOutOfBoundsException());
        arraystoreexception = singletonList(
            PrimordialClassLoader.getJavaLangArrayStoreException());
        negativesizeexception = singletonList(
            PrimordialClassLoader.getJavaLangNegativeArraySizeException());
        arithexception = singletonList(
            PrimordialClassLoader.getJavaLangArithmeticException());
        classcastexceptions = singletonList(
            PrimordialClassLoader.getJavaLangThrowable());
        illegalmonitorstateexception = singletonList(
            PrimordialClassLoader.getJavaLangIllegalMonitorStateException());
    }
    
    public abstract static class Move extends Operator {
        
        public static Quad create(int id, BasicBlock bb, Move operator, RegisterOperand dst, Operand src) {
            return new Quad(id, bb, operator, dst, src);
        }
        public static Quad create(int id, BasicBlock bb, Register r1, Register r2, jq_Type t) {
            Move mv = getMoveOp(t);
            RegisterOperand o1 = new RegisterOperand(r1, t);
            RegisterOperand o2 = new RegisterOperand(r2, t);
            Quad s = create(id, bb, mv, o1, o2);
            return s;
        }
        public static Move getMoveOp(jq_Type type) {
            if (type.isAddressType()) return MOVE_P.INSTANCE;
            if (type.isReferenceType()) return MOVE_A.INSTANCE;
            if (type.isIntLike()) return MOVE_I.INSTANCE;
            if (type == jq_Primitive.FLOAT) return MOVE_F.INSTANCE;
            if (type == jq_Primitive.LONG) return MOVE_L.INSTANCE;
            if (type == jq_Primitive.DOUBLE) return MOVE_D.INSTANCE;
            Assert.UNREACHABLE(); return null;
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitMove(q);
            super.accept(q, qv);
        }
        
        public static class MOVE_I extends Move {
            public static final MOVE_I INSTANCE = new MOVE_I();
            private MOVE_I() { }
            public String toString() { return "MOVE_I"; }
        }
        public static class MOVE_F extends Move {
            public static final MOVE_F INSTANCE = new MOVE_F();
            private MOVE_F() { }
            public String toString() { return "MOVE_F"; }
        }
        public static class MOVE_L extends Move {
            public static final MOVE_L INSTANCE = new MOVE_L();
            private MOVE_L() { }
            public String toString() { return "MOVE_L"; }
        }
        public static class MOVE_D extends Move {
            public static final MOVE_D INSTANCE = new MOVE_D();
            private MOVE_D() { }
            public String toString() { return "MOVE_D"; }
        }
        public static class MOVE_A extends Move {
            public static final MOVE_A INSTANCE = new MOVE_A();
            private MOVE_A() { }
            public String toString() { return "MOVE_A"; }
        }
        public static class MOVE_P extends Move {
            public static final MOVE_P INSTANCE = new MOVE_P();
            private MOVE_P() { }
            public String toString() { return "MOVE_P"; }
        }
    }

    public abstract static class Phi extends Operator {
        public static Quad create(int id, BasicBlock bb, Phi operator, RegisterOperand res, int length) {
            return new Quad(id, bb, operator, res, 
                    new ParamListOperand(new RegisterOperand[length]), 
                    new BasicBlockTableOperand(new BasicBlock[length]));
        }
        public static void setSrc(Quad q, int i, RegisterOperand t) {
            ((ParamListOperand)q.getOp2()).set(i, t);
        }
        public static void setPred(Quad q, int i, BasicBlock o) {
            ((BasicBlockTableOperand)q.getOp3()).set(i, o);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static RegisterOperand getSrc(Quad q, int i) { return ((ParamListOperand)q.getOp2()).get(i); }
        public static ParamListOperand getSrcs(Quad q) { return (ParamListOperand)q.getOp2(); }
        public static BasicBlock getPred(Quad q, int i) { return ((BasicBlockTableOperand)q.getOp3()).get(i); }
        public static BasicBlockTableOperand getPreds(Quad q) { return (BasicBlockTableOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrcs(Quad q, ParamListOperand o) { q.setOp2(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) {
            ParamListOperand plo = getSrcs(q);
            List<RegisterOperand> a = new ArrayList<RegisterOperand>(plo.length());
            for (int i=0; i<plo.length(); ++i) a.add(plo.get(i));
            return a;
        }
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitPhi(q);
            super.accept(q, qv);
        }
        public static class PHI extends Phi {
            public static final PHI INSTANCE = new PHI();
            private PHI() { }
            public String toString() { return "PHI"; }
        }
    }
    
    public abstract static class Binary extends Operator {
        
        public static Quad create(int id, BasicBlock bb, Binary operator, RegisterOperand dst, Operand src1, Operand src2) {
            return new Quad(id, bb, operator, dst, src1, src2);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc1(Quad q) { return q.getOp2(); }
        public static Operand getSrc2(Quad q) { return q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc1(Quad q, Operand o) { q.setOp2(o); }
        public static void setSrc2(Quad q, Operand o) { q.setOp3(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg23(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitBinary(q);
            super.accept(q, qv);
        }
        
        public static class ADD_I extends Binary {
            public static final ADD_I INSTANCE = new ADD_I();
            private ADD_I() { }
            public String toString() { return "ADD_I"; }
        }
        public static class SUB_I extends Binary {
            public static final SUB_I INSTANCE = new SUB_I();
            private SUB_I() { }
            public String toString() { return "SUB_I"; }
        }
        public static class MUL_I extends Binary {
            public static final MUL_I INSTANCE = new MUL_I();
            private MUL_I() { }
            public String toString() { return "MUL_I"; }
        }
        public static class DIV_I extends Binary {
            public static final DIV_I INSTANCE = new DIV_I();
            private DIV_I() { }
            public String toString() { return "DIV_I"; }
        }
        public static class REM_I extends Binary {
            public static final REM_I INSTANCE = new REM_I();
            private REM_I() { }
            public String toString() { return "REM_I"; }
        }
        public static class AND_I extends Binary {
            public static final AND_I INSTANCE = new AND_I();
            private AND_I() { }
            public String toString() { return "AND_I"; }
        }
        public static class OR_I extends Binary {
            public static final OR_I INSTANCE = new OR_I();
            private OR_I() { }
            public String toString() { return "OR_I"; }
        }
        public static class XOR_I extends Binary {
            public static final XOR_I INSTANCE = new XOR_I();
            private XOR_I() { }
            public String toString() { return "XOR_I"; }
        }
        public static class SHL_I extends Binary {
            public static final SHL_I INSTANCE = new SHL_I();
            private SHL_I() { }
            public String toString() { return "SHL_I"; }
        }
        public static class SHR_I extends Binary {
            public static final SHR_I INSTANCE = new SHR_I();
            private SHR_I() { }
            public String toString() { return "SHR_I"; }
        }
        public static class USHR_I extends Binary {
            public static final USHR_I INSTANCE = new USHR_I();
            private USHR_I() { }
            public String toString() { return "USHR_I"; }
        }
        public static class SHL_L extends Binary {
            public static final SHL_L INSTANCE = new SHL_L();
            private SHL_L() { }
            public String toString() { return "SHL_L"; }
        }
        public static class SHR_L extends Binary {
            public static final SHR_L INSTANCE = new SHR_L();
            private SHR_L() { }
            public String toString() { return "SHR_L"; }
        }
        public static class USHR_L extends Binary {
            public static final USHR_L INSTANCE = new USHR_L();
            private USHR_L() { }
            public String toString() { return "USHR_L"; }
        }
        public static class ADD_L extends Binary {
            public static final ADD_L INSTANCE = new ADD_L();
            private ADD_L() { }
            public String toString() { return "ADD_L"; }
        }
        public static class SUB_L extends Binary {
            public static final SUB_L INSTANCE = new SUB_L();
            private SUB_L() { }
            public String toString() { return "SUB_L"; }
        }
        public static class MUL_L extends Binary {
            public static final MUL_L INSTANCE = new MUL_L();
            private MUL_L() { }
            public String toString() { return "MUL_L"; }
        }
        public static class DIV_L extends Binary {
            public static final DIV_L INSTANCE = new DIV_L();
            private DIV_L() { }
            public String toString() { return "DIV_L"; }
        }
        public static class REM_L extends Binary {
            public static final REM_L INSTANCE = new REM_L();
            private REM_L() { }
            public String toString() { return "REM_L"; }
        }
        public static class AND_L extends Binary {
            public static final AND_L INSTANCE = new AND_L();
            private AND_L() { }
            public String toString() { return "AND_L"; }
        }
        public static class OR_L extends Binary {
            public static final OR_L INSTANCE = new OR_L();
            private OR_L() { }
            public String toString() { return "OR_L"; }
        }
        public static class XOR_L extends Binary {
            public static final XOR_L INSTANCE = new XOR_L();
            private XOR_L() { }
            public String toString() { return "XOR_L"; }
        }
        public static class ADD_F extends Binary {
            public static final ADD_F INSTANCE = new ADD_F();
            private ADD_F() { }
            public String toString() { return "ADD_F"; }
        }
        public static class SUB_F extends Binary {
            public static final SUB_F INSTANCE = new SUB_F();
            private SUB_F() { }
            public String toString() { return "SUB_F"; }
        }
        public static class MUL_F extends Binary {
            public static final MUL_F INSTANCE = new MUL_F();
            private MUL_F() { }
            public String toString() { return "MUL_F"; }
        }
        public static class DIV_F extends Binary {
            public static final DIV_F INSTANCE = new DIV_F();
            private DIV_F() { }
            public String toString() { return "DIV_F"; }
        }
        public static class REM_F extends Binary {
            public static final REM_F INSTANCE = new REM_F();
            private REM_F() { }
            public String toString() { return "REM_F"; }
        }
        public static class ADD_D extends Binary {
            public static final ADD_D INSTANCE = new ADD_D();
            private ADD_D() { }
            public String toString() { return "ADD_D"; }
        }
        public static class SUB_D extends Binary {
            public static final SUB_D INSTANCE = new SUB_D();
            private SUB_D() { }
            public String toString() { return "SUB_D"; }
        }
        public static class MUL_D extends Binary {
            public static final MUL_D INSTANCE = new MUL_D();
            private MUL_D() { }
            public String toString() { return "MUL_D"; }
        }
        public static class DIV_D extends Binary {
            public static final DIV_D INSTANCE = new DIV_D();
            private DIV_D() { }
            public String toString() { return "DIV_D"; }
        }
        public static class REM_D extends Binary {
            public static final REM_D INSTANCE = new REM_D();
            private REM_D() { }
            public String toString() { return "REM_D"; }
        }
        public static class CMP_L extends Binary {
            public static final CMP_L INSTANCE = new CMP_L();
            private CMP_L() { }
            public String toString() { return "CMP_L"; }
        }
        public static class CMP_FL extends Binary {
            public static final CMP_FL INSTANCE = new CMP_FL();
            private CMP_FL() { }
            public String toString() { return "CMP_FL"; }
        }
        public static class CMP_FG extends Binary {
            public static final CMP_FG INSTANCE = new CMP_FG();
            private CMP_FG() { }
            public String toString() { return "CMP_FG"; }
        }
        public static class CMP_DL extends Binary {
            public static final CMP_DL INSTANCE = new CMP_DL();
            private CMP_DL() { }
            public String toString() { return "CMP_DL"; }
        }
        public static class CMP_DG extends Binary {
            public static final CMP_DG INSTANCE = new CMP_DG();
            private CMP_DG() { }
            public String toString() { return "CMP_DG"; }
        }
        public static class ADD_P extends Binary {
            public static final ADD_P INSTANCE = new ADD_P();
            private ADD_P() { }
            public String toString() { return "ADD_P"; }
        }
        public static class SUB_P extends Binary {
            public static final SUB_P INSTANCE = new SUB_P();
            private SUB_P() { }
            public String toString() { return "SUB_P"; }
        }
        public static class ALIGN_P extends Binary {
            public static final ALIGN_P INSTANCE = new ALIGN_P();
            private ALIGN_P() { }
            public String toString() { return "ALIGN_P"; }
        }
        public static class CMP_P extends Binary {
            public static final CMP_P INSTANCE = new CMP_P();
            private CMP_P() { }
            public String toString() { return "CMP_P"; }
        }
    }

    public abstract static class Unary extends Operator {
        
        public static Quad create(int id, BasicBlock bb, Unary operator, RegisterOperand dst, Operand src1) {
            return new Quad(id, bb, operator, dst, src1);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitUnary(q);
            super.accept(q, qv);
        }
        
        public static class NEG_I extends Unary {
            public static final NEG_I INSTANCE = new NEG_I();
            private NEG_I() { }
            public String toString() { return "NEG_I"; }
        }
        public static class NEG_F extends Unary {
            public static final NEG_F INSTANCE = new NEG_F();
            private NEG_F() { }
            public String toString() { return "NEG_F"; }
        }
        public static class NEG_L extends Unary {
            public static final NEG_L INSTANCE = new NEG_L();
            private NEG_L() { }
            public String toString() { return "NEG_L"; }
        }
        public static class NEG_D extends Unary {
            public static final NEG_D INSTANCE = new NEG_D();
            private NEG_D() { }
            public String toString() { return "NEG_D"; }
        }
        public static class INT_2LONG extends Unary {
            public static final INT_2LONG INSTANCE = new INT_2LONG();
            private INT_2LONG() { }
            public String toString() { return "INT_2LONG"; }
        }
        public static class INT_2FLOAT extends Unary {
            public static final INT_2FLOAT INSTANCE = new INT_2FLOAT();
            private INT_2FLOAT() { }
            public String toString() { return "INT_2FLOAT"; }
        }
        public static class INT_2DOUBLE extends Unary {
            public static final INT_2DOUBLE INSTANCE = new INT_2DOUBLE();
            private INT_2DOUBLE() { }
            public String toString() { return "INT_2DOUBLE"; }
        }
        public static class LONG_2INT extends Unary {
            public static final LONG_2INT INSTANCE = new LONG_2INT();
            private LONG_2INT() { }
            public String toString() { return "LONG_2INT"; }
        }
        public static class LONG_2FLOAT extends Unary {
            public static final LONG_2FLOAT INSTANCE = new LONG_2FLOAT();
            private LONG_2FLOAT() { }
            public String toString() { return "LONG_2FLOAT"; }
        }
        public static class LONG_2DOUBLE extends Unary {
            public static final LONG_2DOUBLE INSTANCE = new LONG_2DOUBLE();
            private LONG_2DOUBLE() { }
            public String toString() { return "LONG_2DOUBLE"; }
        }
        public static class FLOAT_2INT extends Unary {
            public static final FLOAT_2INT INSTANCE = new FLOAT_2INT();
            private FLOAT_2INT() { }
            public String toString() { return "FLOAT_2INT"; }
        }
        public static class FLOAT_2LONG extends Unary {
            public static final FLOAT_2LONG INSTANCE = new FLOAT_2LONG();
            private FLOAT_2LONG() { }
            public String toString() { return "FLOAT_2LONG"; }
        }
        public static class FLOAT_2DOUBLE extends Unary {
            public static final FLOAT_2DOUBLE INSTANCE = new FLOAT_2DOUBLE();
            private FLOAT_2DOUBLE() { }
            public String toString() { return "FLOAT_2DOUBLE"; }
        }
        public static class DOUBLE_2INT extends Unary {
            public static final DOUBLE_2INT INSTANCE = new DOUBLE_2INT();
            private DOUBLE_2INT() { }
            public String toString() { return "DOUBLE_2INT"; }
        }
        public static class DOUBLE_2LONG extends Unary {
            public static final DOUBLE_2LONG INSTANCE = new DOUBLE_2LONG();
            private DOUBLE_2LONG() { }
            public String toString() { return "DOUBLE_2LONG"; }
        }
        public static class DOUBLE_2FLOAT extends Unary {
            public static final DOUBLE_2FLOAT INSTANCE = new DOUBLE_2FLOAT();
            private DOUBLE_2FLOAT() { }
            public String toString() { return "DOUBLE_2FLOAT"; }
        }
        public static class INT_2BYTE extends Unary {
            public static final INT_2BYTE INSTANCE = new INT_2BYTE();
            private INT_2BYTE() { }
            public String toString() { return "INT_2BYTE"; }
        }
        public static class INT_2CHAR extends Unary {
            public static final INT_2CHAR INSTANCE = new INT_2CHAR();
            private INT_2CHAR() { }
            public String toString() { return "INT_2CHAR"; }
        }
        public static class INT_2SHORT extends Unary {
            public static final INT_2SHORT INSTANCE = new INT_2SHORT();
            private INT_2SHORT() { }
            public String toString() { return "INT_2SHORT"; }
        }
        public static class OBJECT_2ADDRESS extends Unary {
            public static final OBJECT_2ADDRESS INSTANCE = new OBJECT_2ADDRESS();
            private OBJECT_2ADDRESS() { }
            public String toString() { return "OBJECT_2ADDRESS"; }
        }
        public static class ADDRESS_2OBJECT extends Unary {
            public static final ADDRESS_2OBJECT INSTANCE = new ADDRESS_2OBJECT();
            private ADDRESS_2OBJECT() { }
            public String toString() { return "ADDRESS_2OBJECT"; }
        }
        public static class INT_2ADDRESS extends Unary {
            public static final INT_2ADDRESS INSTANCE = new INT_2ADDRESS();
            private INT_2ADDRESS() { }
            public String toString() { return "INT_2ADDRESS"; }
        }
        public static class ADDRESS_2INT extends Unary {
            public static final ADDRESS_2INT INSTANCE = new ADDRESS_2INT();
            private ADDRESS_2INT() { }
            public String toString() { return "ADDRESS_2INT"; }
        }
        public static class ISNULL_P extends Unary {
            public static final ISNULL_P INSTANCE = new ISNULL_P();
            private ISNULL_P() { }
            public String toString() { return "ISNULL_P"; }
        }
        public static class FLOAT_2INTBITS extends Unary {
            public static final FLOAT_2INTBITS INSTANCE = new FLOAT_2INTBITS();
            private FLOAT_2INTBITS() { }
            public String toString() { return "FLOAT_2INTBITS"; }
        }
        public static class INTBITS_2FLOAT extends Unary {
            public static final INTBITS_2FLOAT INSTANCE = new INTBITS_2FLOAT();
            private INTBITS_2FLOAT() { }
            public String toString() { return "INTBITS_2FLOAT"; }
        }
        public static class DOUBLE_2LONGBITS extends Unary {
            public static final DOUBLE_2LONGBITS INSTANCE = new DOUBLE_2LONGBITS();
            private DOUBLE_2LONGBITS() { }
            public String toString() { return "DOUBLE_2LONGBITS"; }
        }
        public static class LONGBITS_2DOUBLE extends Unary {
            public static final LONGBITS_2DOUBLE INSTANCE = new LONGBITS_2DOUBLE();
            private LONGBITS_2DOUBLE() { }
            public String toString() { return "LONGBITS_2DOUBLE"; }
        }
    }

    public abstract static class ALoad extends Operator {
        
        public static Quad create(int id, BasicBlock bb, ALoad operator, RegisterOperand dst, Operand base, Operand ind, Operand guard) {
            return new Quad(id, bb, operator, dst, base, ind, guard);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getBase(Quad q) { return q.getOp2(); }
        public static Operand getIndex(Quad q) { return q.getOp3(); }
        public static Operand getGuard(Quad q) { return q.getOp4(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setBase(Quad q, Operand o) { q.setOp2(o); }
        public static void setIndex(Quad q, Operand o) { q.setOp3(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp4(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg234(q); }
        
        public abstract jq_Type getType();
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitALoad(q);
            qv.visitArray(q);
            qv.visitLoad(q);
            super.accept(q, qv);
        }
        
        public static class ALOAD_I extends ALoad {
            public static final ALOAD_I INSTANCE = new ALOAD_I();
            private ALOAD_I() { }
            public String toString() { return "ALOAD_I"; }
            public jq_Type getType() { return jq_Primitive.INT; }
        }
        public static class ALOAD_L extends ALoad {
            public static final ALOAD_L INSTANCE = new ALOAD_L();
            private ALOAD_L() { }
            public String toString() { return "ALOAD_L"; }
            public jq_Type getType() { return jq_Primitive.LONG; }
        }
        public static class ALOAD_F extends ALoad {
            public static final ALOAD_F INSTANCE = new ALOAD_F();
            private ALOAD_F() { }
            public String toString() { return "ALOAD_F"; }
            public jq_Type getType() { return jq_Primitive.FLOAT; }
        }
        public static class ALOAD_D extends ALoad {
            public static final ALOAD_D INSTANCE = new ALOAD_D();
            private ALOAD_D() { }
            public String toString() { return "ALOAD_D"; }
            public jq_Type getType() { return jq_Primitive.DOUBLE; }
        }
        public static class ALOAD_A extends ALoad {
            public static final ALOAD_A INSTANCE = new ALOAD_A();
            private ALOAD_A() { }
            public String toString() { return "ALOAD_A"; }
            public jq_Type getType() { return PrimordialClassLoader.getJavaLangObject(); }
        }
        public static class ALOAD_P extends ALoad {
            public static final ALOAD_P INSTANCE = new ALOAD_P();
            private ALOAD_P() { }
            public String toString() { return "ALOAD_P"; }
            public jq_Type getType() { return Address._class; }
        }
        public static class ALOAD_B extends ALoad {
            public static final ALOAD_B INSTANCE = new ALOAD_B();
            private ALOAD_B() { }
            public String toString() { return "ALOAD_B"; }
            public jq_Type getType() { return jq_Primitive.BYTE; }
        }
        public static class ALOAD_C extends ALoad {
            public static final ALOAD_C INSTANCE = new ALOAD_C();
            private ALOAD_C() { }
            public String toString() { return "ALOAD_C"; }
            public jq_Type getType() { return jq_Primitive.CHAR; }
        }
        public static class ALOAD_S extends ALoad {
            public static final ALOAD_S INSTANCE = new ALOAD_S();
            private ALOAD_S() { }
            public String toString() { return "ALOAD_S"; }
            public jq_Type getType() { return jq_Primitive.SHORT; }
        }
    }
    
    public abstract static class AStore extends Operator {
        
        public static Quad create(int id, BasicBlock bb, AStore operator, Operand val, Operand base, Operand ind, Operand guard) {
            return new Quad(id, bb, operator, val, base, ind, guard);
        }
        public static Operand getValue(Quad q) { return q.getOp1(); }
        public static Operand getBase(Quad q) { return q.getOp2(); }
        public static Operand getIndex(Quad q) { return q.getOp3(); }
        public static Operand getGuard(Quad q) { return q.getOp4(); }
        public static void setValue(Quad q, Operand o) { q.setOp1(o); }
        public static void setBase(Quad q, Operand o) { q.setOp2(o); }
        public static void setIndex(Quad q, Operand o) { q.setOp3(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp4(o); }
        public boolean hasSideEffects() { return true; }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1234(q); }
        
        public abstract jq_Type getType();
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitAStore(q);
            qv.visitArray(q);
            qv.visitStore(q);
            super.accept(q, qv);
        }
        
        public static class ASTORE_I extends AStore {
            public static final ASTORE_I INSTANCE = new ASTORE_I();
            private ASTORE_I() { }
            public String toString() { return "ASTORE_I"; }
            public jq_Type getType() { return jq_Primitive.INT; }
        }
        public static class ASTORE_L extends AStore {
            public static final ASTORE_L INSTANCE = new ASTORE_L();
            private ASTORE_L() { }
            public String toString() { return "ASTORE_L"; }
            public jq_Type getType() { return jq_Primitive.LONG; }
        }
        public static class ASTORE_F extends AStore {
            public static final ASTORE_F INSTANCE = new ASTORE_F();
            private ASTORE_F() { }
            public String toString() { return "ASTORE_F"; }
            public jq_Type getType() { return jq_Primitive.FLOAT; }
        }
        public static class ASTORE_D extends AStore {
            public static final ASTORE_D INSTANCE = new ASTORE_D();
            private ASTORE_D() { }
            public String toString() { return "ASTORE_D"; }
            public jq_Type getType() { return jq_Primitive.DOUBLE; }
        }
        public static class ASTORE_A extends AStore {
            public static final ASTORE_A INSTANCE = new ASTORE_A();
            private ASTORE_A() { }
            public String toString() { return "ASTORE_A"; }
            public jq_Type getType() { return PrimordialClassLoader.getJavaLangObject(); }
        }
        public static class ASTORE_P extends AStore {
            public static final ASTORE_P INSTANCE = new ASTORE_P();
            private ASTORE_P() { }
            public String toString() { return "ASTORE_P"; }
            public jq_Type getType() { return Address._class; }
        }
        public static class ASTORE_B extends AStore {
            public static final ASTORE_B INSTANCE = new ASTORE_B();
            private ASTORE_B() { }
            public String toString() { return "ASTORE_B"; }
            public jq_Type getType() { return jq_Primitive.BYTE; }
        }
        public static class ASTORE_C extends AStore {
            public static final ASTORE_C INSTANCE = new ASTORE_C();
            private ASTORE_C() { }
            public String toString() { return "ASTORE_C"; }
            public jq_Type getType() { return jq_Primitive.CHAR; }
        }
        public static class ASTORE_S extends AStore {
            public static final ASTORE_S INSTANCE = new ASTORE_S();
            private ASTORE_S() { }
            public String toString() { return "ASTORE_S"; }
            public jq_Type getType() { return jq_Primitive.SHORT; }
        }
    }

    public static abstract class Branch extends Operator {
        public boolean hasSideEffects() { return true; }
    }
    
    public abstract static class IntIfCmp extends Branch {
        public static Quad create(int id, BasicBlock bb, IntIfCmp operator, Operand op0, Operand op1, ConditionOperand cond, TargetOperand target) {
            return new Quad(id, bb, operator, op0, op1, cond, target);
        }
        public static Operand getSrc1(Quad q) { return q.getOp1(); }
        public static Operand getSrc2(Quad q) { return q.getOp2(); }
        public static ConditionOperand getCond(Quad q) { return (ConditionOperand)q.getOp3(); }
        public static TargetOperand getTarget(Quad q) { return (TargetOperand)q.getOp4(); }
        public static void setSrc1(Quad q, Operand o) { q.setOp1(o); }
        public static void setSrc2(Quad q, Operand o) { q.setOp2(o); }
        public static void setCond(Quad q, ConditionOperand o) { q.setOp3(o); }
        public static void setTarget(Quad q, TargetOperand o) { q.setOp4(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg12(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitIntIfCmp(q);
            qv.visitCondBranch(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class IFCMP_I extends IntIfCmp {
            public static final IFCMP_I INSTANCE = new IFCMP_I();
            private IFCMP_I() { }
            public String toString() { return "IFCMP_I"; }
        }
        public static class IFCMP_A extends IntIfCmp {
            public static final IFCMP_A INSTANCE = new IFCMP_A();
            private IFCMP_A() { }
            public String toString() { return "IFCMP_A"; }
        }
        public static class IFCMP_P extends IntIfCmp {
            public static final IFCMP_P INSTANCE = new IFCMP_P();
            private IFCMP_P() { }
            public String toString() { return "IFCMP_P"; }
        }
    }
    
    public abstract static class Goto extends Branch {
        public static Quad create(int id, BasicBlock bb, Goto operator, TargetOperand target) {
            return new Quad(id, bb, operator, target);
        }
        public static TargetOperand getTarget(Quad q) { return (TargetOperand)q.getOp1(); }
        public static void setTarget(Quad q, TargetOperand o) { q.setOp1(o); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitGoto(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class GOTO extends Goto {
            public static final GOTO INSTANCE = new GOTO();
            private GOTO() { }
            public String toString() { return "GOTO"; }
        }
    }
    
    public abstract static class Jsr extends Branch {
        public static Quad create(int id, BasicBlock bb, Jsr operator, RegisterOperand loc, TargetOperand target, TargetOperand successor) {
            return new Quad(id, bb, operator, loc, target, successor);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static TargetOperand getTarget(Quad q) { return (TargetOperand)q.getOp2(); }
        public static TargetOperand getSuccessor(Quad q) { return (TargetOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setTarget(Quad q, TargetOperand o) { q.setOp2(o); }
        public static void setSuccessor(Quad q, TargetOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitJsr(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class JSR extends Jsr {
            public static final JSR INSTANCE = new JSR();
            private JSR() { }
            public String toString() { return "JSR"; }
        }
    }
    
    public abstract static class Ret extends Branch {
        public static Quad create(int id, BasicBlock bb, Ret operator, RegisterOperand loc) {
            return new Quad(id, bb, operator, loc);
        }
        public static RegisterOperand getTarget(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static void setTarget(Quad q, RegisterOperand o) { q.setOp1(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitRet(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class RET extends Ret {
            public static final RET INSTANCE = new RET();
            private RET() { }
            public String toString() { return "RET"; }
        }
    }
    
    public abstract static class TableSwitch extends Branch {
        public static Quad create(int id, BasicBlock bb, TableSwitch operator, Operand val, IConstOperand low, TargetOperand def, int length) {
            return new Quad(id, bb, operator, val, low, def, new BasicBlockTableOperand(new BasicBlock[length]));
        }
        public static void setTarget(Quad q, int i, BasicBlock t) {
            ((BasicBlockTableOperand)q.getOp4()).set(i, t);
        }
        public static Operand getSrc(Quad q) { return q.getOp1(); }
        public static IConstOperand getLow(Quad q) { return (IConstOperand)q.getOp2(); }
        public static TargetOperand getDefault(Quad q) { return (TargetOperand)q.getOp3(); }
        public static BasicBlock getTarget(Quad q, int i) { return ((BasicBlockTableOperand)q.getOp4()).get(i); }
        public static BasicBlockTableOperand getTargetTable(Quad q) { return (BasicBlockTableOperand)q.getOp4(); }
        public static void setSrc(Quad q, Operand o) { q.setOp1(o); }
        public static void setLow(Quad q, IConstOperand o) { q.setOp2(o); }
        public static void setDefault(Quad q, TargetOperand o) { q.setOp3(o); }
        public static void setTargetTable(Quad q, BasicBlockTableOperand o) { q.setOp4(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitTableSwitch(q);
            qv.visitCondBranch(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class TABLESWITCH extends TableSwitch {
            public static final TABLESWITCH INSTANCE = new TABLESWITCH();
            private TABLESWITCH() { }
            public String toString() { return "TABLESWITCH"; }
        }
    }
    
    public abstract static class LookupSwitch extends Branch {
        public static Quad create(int id, BasicBlock bb, LookupSwitch operator, Operand val, TargetOperand def, int length) {
            return new Quad(id, bb, operator, val, def, new IntValueTableOperand(new int[length]), new BasicBlockTableOperand(new BasicBlock[length]));
        }
        public static void setMatch(Quad q, int i, int t) {
            ((IntValueTableOperand)q.getOp3()).set(i, t);
        }
        public static void setTarget(Quad q, int i, BasicBlock t) {
            ((BasicBlockTableOperand)q.getOp4()).set(i, t);
        }
        public static Operand getSrc(Quad q) { return q.getOp1(); }
        public static TargetOperand getDefault(Quad q) { return (TargetOperand)q.getOp2(); }
        public static int getMatch(Quad q, int i) { return ((IntValueTableOperand)q.getOp3()).get(i); }
        public static BasicBlock getTarget(Quad q, int i) { return ((BasicBlockTableOperand)q.getOp4()).get(i); }
        public static IntValueTableOperand getValueTable(Quad q) { return (IntValueTableOperand)q.getOp3(); }
        public static BasicBlockTableOperand getTargetTable(Quad q) { return (BasicBlockTableOperand)q.getOp4(); }
        public static void setSrc(Quad q, Operand o) { q.setOp1(o); }
        public static void setDefault(Quad q, TargetOperand o) { q.setOp2(o); }
        public static void setValueTable(Quad q, IntValueTableOperand o) { q.setOp3(o); }
        public static void setTargetTable(Quad q, BasicBlockTableOperand o) { q.setOp4(o); }
        public static int getSize(Quad q) { return ((IntValueTableOperand)q.getOp3()).size(); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitLookupSwitch(q);
            qv.visitCondBranch(q);
            qv.visitBranch(q);
            super.accept(q, qv);
        }
        
        public static class LOOKUPSWITCH extends LookupSwitch {
            public static final LOOKUPSWITCH INSTANCE = new LOOKUPSWITCH();
            private LOOKUPSWITCH() { }
            public String toString() { return "LOOKUPSWITCH"; }
        }
    }
    
    public abstract static class Return extends Operator {
        public static Quad create(int id, BasicBlock bb, Return operator, Operand val) {
            return new Quad(id, bb, operator, val);
        }
        public static Quad create(int id, BasicBlock bb, Return operator) {
            return new Quad(id, bb, operator);
        }
        public static Operand getSrc(Quad q) { return q.getOp1(); }
        public static void setSrc(Quad q, Operand o) { q.setOp1(o); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitReturn(q);
            super.accept(q, qv);
        }
        
        public static class RETURN_V extends Return {
            public static final RETURN_V INSTANCE = new RETURN_V();
            private RETURN_V() { }
            public String toString() { return "RETURN_V"; }
        }
        public static class RETURN_I extends Return {
            public static final RETURN_I INSTANCE = new RETURN_I();
            private RETURN_I() { }
            public String toString() { return "RETURN_I"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class RETURN_F extends Return {
            public static final RETURN_F INSTANCE = new RETURN_F();
            private RETURN_F() { }
            public String toString() { return "RETURN_F"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class RETURN_L extends Return {
            public static final RETURN_L INSTANCE = new RETURN_L();
            private RETURN_L() { }
            public String toString() { return "RETURN_L"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class RETURN_D extends Return {
            public static final RETURN_D INSTANCE = new RETURN_D();
            private RETURN_D() { }
            public String toString() { return "RETURN_D"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class RETURN_A extends Return {
            public static final RETURN_A INSTANCE = new RETURN_A();
            private RETURN_A() { }
            public String toString() { return "RETURN_A"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class RETURN_P extends Return {
            public static final RETURN_P INSTANCE = new RETURN_P();
            private RETURN_P() { }
            public String toString() { return "RETURN_P"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        }
        public static class THROW_A extends Return {
            public static final THROW_A INSTANCE = new THROW_A();
            private THROW_A() { }
            public String toString() { return "THROW_A"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
            public List<jq_Class> getThrownExceptions() {
                return anyexception;
            }
        }
    }

    public abstract static class Getstatic extends Operator {
        public static Quad create(int id, BasicBlock bb, Getstatic operator, RegisterOperand dst, FieldOperand field) {
            return new Quad(id, bb, operator, dst, field);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static FieldOperand getField(Quad q) { return (FieldOperand)q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setField(Quad q, FieldOperand o) { q.setOp2(o); }
        public boolean hasSideEffects() { return false; }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitGetstatic(q);
            qv.visitStaticField(q);
            qv.visitLoad(q);
            super.accept(q, qv);
        }
        
        public static class GETSTATIC_I extends Getstatic {
            public static final GETSTATIC_I INSTANCE = new GETSTATIC_I();
            private GETSTATIC_I() { }
            public String toString() { return "GETSTATIC_I"; }
        }
        public static class GETSTATIC_F extends Getstatic {
            public static final GETSTATIC_F INSTANCE = new GETSTATIC_F();
            private GETSTATIC_F() { }
            public String toString() { return "GETSTATIC_F"; }
        }
        public static class GETSTATIC_L extends Getstatic {
            public static final GETSTATIC_L INSTANCE = new GETSTATIC_L();
            private GETSTATIC_L() { }
            public String toString() { return "GETSTATIC_L"; }
        }
        public static class GETSTATIC_D extends Getstatic {
            public static final GETSTATIC_D INSTANCE = new GETSTATIC_D();
            private GETSTATIC_D() { }
            public String toString() { return "GETSTATIC_D"; }
        }
        public static class GETSTATIC_A extends Getstatic {
            public static final GETSTATIC_A INSTANCE = new GETSTATIC_A();
            private GETSTATIC_A() { }
            public String toString() { return "GETSTATIC_A"; }
        }
        public static class GETSTATIC_P extends Getstatic {
            public static final GETSTATIC_P INSTANCE = new GETSTATIC_P();
            private GETSTATIC_P() { }
            public String toString() { return "GETSTATIC_P"; }
        }
        public static class GETSTATIC_Z extends Getstatic {
            public static final GETSTATIC_Z INSTANCE = new GETSTATIC_Z();
            private GETSTATIC_Z() { }
            public String toString() { return "GETSTATIC_Z"; }
        }
        public static class GETSTATIC_B extends Getstatic {
            public static final GETSTATIC_B INSTANCE = new GETSTATIC_B();
            private GETSTATIC_B() { }
            public String toString() { return "GETSTATIC_B"; }
        }
        public static class GETSTATIC_C extends Getstatic {
            public static final GETSTATIC_C INSTANCE = new GETSTATIC_C();
            private GETSTATIC_C() { }
            public String toString() { return "GETSTATIC_C"; }
        }
        public static class GETSTATIC_S extends Getstatic {
            public static final GETSTATIC_S INSTANCE = new GETSTATIC_S();
            private GETSTATIC_S() { }
            public String toString() { return "GETSTATIC_S"; }
        }
        public static class GETSTATIC_I_DYNLINK extends GETSTATIC_I {
            public static final GETSTATIC_I_DYNLINK INSTANCE = new GETSTATIC_I_DYNLINK();
            private GETSTATIC_I_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_I%"; }
        }
        public static class GETSTATIC_F_DYNLINK extends GETSTATIC_F {
            public static final GETSTATIC_F_DYNLINK INSTANCE = new GETSTATIC_F_DYNLINK();
            private GETSTATIC_F_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_F%"; }
        }
        public static class GETSTATIC_L_DYNLINK extends GETSTATIC_L {
            public static final GETSTATIC_L_DYNLINK INSTANCE = new GETSTATIC_L_DYNLINK();
            private GETSTATIC_L_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_L%"; }
        }
        public static class GETSTATIC_D_DYNLINK extends GETSTATIC_D {
            public static final GETSTATIC_D_DYNLINK INSTANCE = new GETSTATIC_D_DYNLINK();
            private GETSTATIC_D_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_D%"; }
        }
        public static class GETSTATIC_A_DYNLINK extends GETSTATIC_A {
            public static final GETSTATIC_A_DYNLINK INSTANCE = new GETSTATIC_A_DYNLINK();
            private GETSTATIC_A_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_A%"; }
        }
        public static class GETSTATIC_P_DYNLINK extends GETSTATIC_P {
            public static final GETSTATIC_P_DYNLINK INSTANCE = new GETSTATIC_P_DYNLINK();
            private GETSTATIC_P_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_P%"; }
        }
        public static class GETSTATIC_Z_DYNLINK extends GETSTATIC_Z {
            public static final GETSTATIC_Z_DYNLINK INSTANCE = new GETSTATIC_Z_DYNLINK();
            private GETSTATIC_Z_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_Z%"; }
        }
        public static class GETSTATIC_B_DYNLINK extends GETSTATIC_B {
            public static final GETSTATIC_B_DYNLINK INSTANCE = new GETSTATIC_B_DYNLINK();
            private GETSTATIC_B_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_B%"; }
        }
        public static class GETSTATIC_C_DYNLINK extends GETSTATIC_C {
            public static final GETSTATIC_C_DYNLINK INSTANCE = new GETSTATIC_C_DYNLINK();
            private GETSTATIC_C_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_C%"; }
        }
        public static class GETSTATIC_S_DYNLINK extends GETSTATIC_S {
            public static final GETSTATIC_S_DYNLINK INSTANCE = new GETSTATIC_S_DYNLINK();
            private GETSTATIC_S_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETSTATIC_S%"; }
        }
    }
    
    public abstract static class Putstatic extends Operator {
        public static Quad create(int id, BasicBlock bb, Putstatic operator, Operand src, FieldOperand field) {
            return new Quad(id, bb, operator, src, field);
        }
        public static Operand getSrc(Quad q) { return q.getOp1(); }
        public static FieldOperand getField(Quad q) { return (FieldOperand)q.getOp2(); }
        public static void setSrc(Quad q, Operand o) { q.setOp1(o); }
        public static void setField(Quad q, FieldOperand o) { q.setOp2(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1_check(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitPutstatic(q);
            qv.visitStaticField(q);
            qv.visitStore(q);
            super.accept(q, qv);
        }
        
        public static class PUTSTATIC_I extends Putstatic {
            public static final PUTSTATIC_I INSTANCE = new PUTSTATIC_I();
            private PUTSTATIC_I() { }
            public String toString() { return "PUTSTATIC_I"; }
        }
        public static class PUTSTATIC_F extends Putstatic {
            public static final PUTSTATIC_F INSTANCE = new PUTSTATIC_F();
            private PUTSTATIC_F() { }
            public String toString() { return "PUTSTATIC_F"; }
        }
        public static class PUTSTATIC_L extends Putstatic {
            public static final PUTSTATIC_L INSTANCE = new PUTSTATIC_L();
            private PUTSTATIC_L() { }
            public String toString() { return "PUTSTATIC_L"; }
        }
        public static class PUTSTATIC_D extends Putstatic {
            public static final PUTSTATIC_D INSTANCE = new PUTSTATIC_D();
            private PUTSTATIC_D() { }
            public String toString() { return "PUTSTATIC_D"; }
        }
        public static class PUTSTATIC_A extends Putstatic {
            public static final PUTSTATIC_A INSTANCE = new PUTSTATIC_A();
            private PUTSTATIC_A() { }
            public String toString() { return "PUTSTATIC_A"; }
        }
        public static class PUTSTATIC_P extends Putstatic {
            public static final PUTSTATIC_P INSTANCE = new PUTSTATIC_P();
            private PUTSTATIC_P() { }
            public String toString() { return "PUTSTATIC_P"; }
        }
        public static class PUTSTATIC_Z extends Putstatic {
            public static final PUTSTATIC_Z INSTANCE = new PUTSTATIC_Z();
            private PUTSTATIC_Z() { }
            public String toString() { return "PUTSTATIC_Z"; }
        }
        public static class PUTSTATIC_B extends Putstatic {
            public static final PUTSTATIC_B INSTANCE = new PUTSTATIC_B();
            private PUTSTATIC_B() { }
            public String toString() { return "PUTSTATIC_B"; }
        }
        public static class PUTSTATIC_S extends Putstatic {
            public static final PUTSTATIC_S INSTANCE = new PUTSTATIC_S();
            private PUTSTATIC_S() { }
            public String toString() { return "PUTSTATIC_S"; }
        }
        public static class PUTSTATIC_C extends Putstatic {
            public static final PUTSTATIC_C INSTANCE = new PUTSTATIC_C();
            private PUTSTATIC_C() { }
            public String toString() { return "PUTSTATIC_C"; }
        }
        public static class PUTSTATIC_I_DYNLINK extends PUTSTATIC_I {
            public static final PUTSTATIC_I_DYNLINK INSTANCE = new PUTSTATIC_I_DYNLINK();
            private PUTSTATIC_I_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_I%"; }
        }
        public static class PUTSTATIC_F_DYNLINK extends PUTSTATIC_F {
            public static final PUTSTATIC_F_DYNLINK INSTANCE = new PUTSTATIC_F_DYNLINK();
            private PUTSTATIC_F_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_F%"; }
        }
        public static class PUTSTATIC_L_DYNLINK extends PUTSTATIC_L {
            public static final PUTSTATIC_L_DYNLINK INSTANCE = new PUTSTATIC_L_DYNLINK();
            private PUTSTATIC_L_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_L%"; }
        }
        public static class PUTSTATIC_D_DYNLINK extends PUTSTATIC_D {
            public static final PUTSTATIC_D_DYNLINK INSTANCE = new PUTSTATIC_D_DYNLINK();
            private PUTSTATIC_D_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_D%"; }
        }
        public static class PUTSTATIC_A_DYNLINK extends PUTSTATIC_A {
            public static final PUTSTATIC_A_DYNLINK INSTANCE = new PUTSTATIC_A_DYNLINK();
            private PUTSTATIC_A_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_A%"; }
        }
        public static class PUTSTATIC_P_DYNLINK extends PUTSTATIC_P {
            public static final PUTSTATIC_P_DYNLINK INSTANCE = new PUTSTATIC_P_DYNLINK();
            private PUTSTATIC_P_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_P%"; }
        }
        public static class PUTSTATIC_Z_DYNLINK extends PUTSTATIC_Z {
            public static final PUTSTATIC_Z_DYNLINK INSTANCE = new PUTSTATIC_Z_DYNLINK();
            private PUTSTATIC_Z_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_Z%"; }
        }
        public static class PUTSTATIC_B_DYNLINK extends PUTSTATIC_B {
            public static final PUTSTATIC_B_DYNLINK INSTANCE = new PUTSTATIC_B_DYNLINK();
            private PUTSTATIC_B_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_B%"; }
        }
        public static class PUTSTATIC_C_DYNLINK extends PUTSTATIC_C {
            public static final PUTSTATIC_C_DYNLINK INSTANCE = new PUTSTATIC_C_DYNLINK();
            private PUTSTATIC_C_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_C%"; }
        }
        public static class PUTSTATIC_S_DYNLINK extends PUTSTATIC_S {
            public static final PUTSTATIC_S_DYNLINK INSTANCE = new PUTSTATIC_S_DYNLINK();
            private PUTSTATIC_S_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTSTATIC_S%"; }
        }
    }

    public abstract static class Getfield extends Operator {
        public static Quad create(int id, BasicBlock bb, Getfield operator, RegisterOperand dst, Operand base, FieldOperand field, Operand guard) {
            return new Quad(id, bb, operator, dst, base, field, guard);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getBase(Quad q) { return q.getOp2(); }
        public static FieldOperand getField(Quad q) { return (FieldOperand)q.getOp3(); }
        public static Operand getGuard(Quad q) { return q.getOp4(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setBase(Quad q, Operand o) { q.setOp2(o); }
        public static void setField(Quad q, FieldOperand o) { q.setOp3(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp4(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg24(q); }
        public boolean hasSideEffects() { return false; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitGetfield(q);
            qv.visitInstanceField(q);
            qv.visitLoad(q);
            super.accept(q, qv);
        }
        
        public static class GETFIELD_I extends Getfield {
            public static final GETFIELD_I INSTANCE = new GETFIELD_I();
            private GETFIELD_I() { }
            public String toString() { return "GETFIELD_I"; }
        }
        public static class GETFIELD_F extends Getfield {
            public static final GETFIELD_F INSTANCE = new GETFIELD_F();
            private GETFIELD_F() { }
            public String toString() { return "GETFIELD_F"; }
        }
        public static class GETFIELD_L extends Getfield {
            public static final GETFIELD_L INSTANCE = new GETFIELD_L();
            private GETFIELD_L() { }
            public String toString() { return "GETFIELD_L"; }
        }
        public static class GETFIELD_D extends Getfield {
            public static final GETFIELD_D INSTANCE = new GETFIELD_D();
            private GETFIELD_D() { }
            public String toString() { return "GETFIELD_D"; }
        }
        public static class GETFIELD_A extends Getfield {
            public static final GETFIELD_A INSTANCE = new GETFIELD_A();
            private GETFIELD_A() { }
            public String toString() { return "GETFIELD_A"; }
        }
        public static class GETFIELD_P extends Getfield {
            public static final GETFIELD_P INSTANCE = new GETFIELD_P();
            private GETFIELD_P() { }
            public String toString() { return "GETFIELD_P"; }
        }
        public static class GETFIELD_B extends Getfield {
            public static final GETFIELD_B INSTANCE = new GETFIELD_B();
            private GETFIELD_B() { }
            public String toString() { return "GETFIELD_B"; }
        }
        public static class GETFIELD_C extends Getfield {
            public static final GETFIELD_C INSTANCE = new GETFIELD_C();
            private GETFIELD_C() { }
            public String toString() { return "GETFIELD_C"; }
        }
        public static class GETFIELD_S extends Getfield {
            public static final GETFIELD_S INSTANCE = new GETFIELD_S();
            private GETFIELD_S() { }
            public String toString() { return "GETFIELD_S"; }
        }
        public static class GETFIELD_Z extends Getfield {
            public static final GETFIELD_Z INSTANCE = new GETFIELD_Z();
            private GETFIELD_Z() { }
            public String toString() { return "GETFIELD_Z"; }
        }
        public static class GETFIELD_I_DYNLINK extends GETFIELD_I {
            public static final GETFIELD_I_DYNLINK INSTANCE = new GETFIELD_I_DYNLINK();
            private GETFIELD_I_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_I%"; }
        }
        public static class GETFIELD_F_DYNLINK extends GETFIELD_F {
            public static final GETFIELD_F_DYNLINK INSTANCE = new GETFIELD_F_DYNLINK();
            private GETFIELD_F_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_F%"; }
        }
        public static class GETFIELD_L_DYNLINK extends GETFIELD_L {
            public static final GETFIELD_L_DYNLINK INSTANCE = new GETFIELD_L_DYNLINK();
            private GETFIELD_L_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_L%"; }
        }
        public static class GETFIELD_D_DYNLINK extends GETFIELD_D {
            public static final GETFIELD_D_DYNLINK INSTANCE = new GETFIELD_D_DYNLINK();
            private GETFIELD_D_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_D%"; }
        }
        public static class GETFIELD_A_DYNLINK extends GETFIELD_A {
            public static final GETFIELD_A_DYNLINK INSTANCE = new GETFIELD_A_DYNLINK();
            private GETFIELD_A_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_A%"; }
        }
        public static class GETFIELD_P_DYNLINK extends GETFIELD_P {
            public static final GETFIELD_P_DYNLINK INSTANCE = new GETFIELD_P_DYNLINK();
            private GETFIELD_P_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_P%"; }
        }
        public static class GETFIELD_B_DYNLINK extends GETFIELD_B {
            public static final GETFIELD_B_DYNLINK INSTANCE = new GETFIELD_B_DYNLINK();
            private GETFIELD_B_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_B%"; }
        }
        public static class GETFIELD_C_DYNLINK extends GETFIELD_C {
            public static final GETFIELD_C_DYNLINK INSTANCE = new GETFIELD_C_DYNLINK();
            private GETFIELD_C_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_C%"; }
        }
        public static class GETFIELD_S_DYNLINK extends GETFIELD_S {
            public static final GETFIELD_S_DYNLINK INSTANCE = new GETFIELD_S_DYNLINK();
            private GETFIELD_S_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_S%"; }
        }
        public static class GETFIELD_Z_DYNLINK extends GETFIELD_Z {
            public static final GETFIELD_Z_DYNLINK INSTANCE = new GETFIELD_Z_DYNLINK();
            private GETFIELD_Z_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public boolean hasSideEffects() { return true; }
            public String toString() { return "GETFIELD_Z%"; }
        }
    }
    
    public abstract static class Putfield extends Operator {
        public static Quad create(int id, BasicBlock bb, Putfield operator, Operand base, FieldOperand field, Operand src, Operand guard) {
            return new Quad(id, bb, operator, base, field, src, guard);
        }
        public static Operand getBase(Quad q) { return q.getOp1(); }
        public static FieldOperand getField(Quad q) { return (FieldOperand)q.getOp2(); }
        public static Operand getSrc(Quad q) { return q.getOp3(); }
        public static Operand getGuard(Quad q) { return q.getOp4(); }
        public static void setBase(Quad q, Operand o) { q.setOp1(o); }
        public static void setField(Quad q, FieldOperand o) { q.setOp2(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp3(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp4(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1234(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitPutfield(q);
            qv.visitInstanceField(q);
            qv.visitStore(q);
            super.accept(q, qv);
        }
        
        public static class PUTFIELD_I extends Putfield {
            public static final PUTFIELD_I INSTANCE = new PUTFIELD_I();
            private PUTFIELD_I() { }
            public String toString() { return "PUTFIELD_I"; }
        }
        public static class PUTFIELD_F extends Putfield {
            public static final PUTFIELD_F INSTANCE = new PUTFIELD_F();
            private PUTFIELD_F() { }
            public String toString() { return "PUTFIELD_F"; }
        }
        public static class PUTFIELD_L extends Putfield {
            public static final PUTFIELD_L INSTANCE = new PUTFIELD_L();
            private PUTFIELD_L() { }
            public String toString() { return "PUTFIELD_L"; }
        }
        public static class PUTFIELD_D extends Putfield {
            public static final PUTFIELD_D INSTANCE = new PUTFIELD_D();
            private PUTFIELD_D() { }
            public String toString() { return "PUTFIELD_D"; }
        }
        public static class PUTFIELD_A extends Putfield {
            public static final PUTFIELD_A INSTANCE = new PUTFIELD_A();
            private PUTFIELD_A() { }
            public String toString() { return "PUTFIELD_A"; }
        }
        public static class PUTFIELD_P extends Putfield {
            public static final PUTFIELD_P INSTANCE = new PUTFIELD_P();
            private PUTFIELD_P() { }
            public String toString() { return "PUTFIELD_P"; }
        }
        public static class PUTFIELD_B extends Putfield {
            public static final PUTFIELD_B INSTANCE = new PUTFIELD_B();
            private PUTFIELD_B() { }
            public String toString() { return "PUTFIELD_B"; }
        }
        public static class PUTFIELD_C extends Putfield {
            public static final PUTFIELD_C INSTANCE = new PUTFIELD_C();
            private PUTFIELD_C() { }
            public String toString() { return "PUTFIELD_C"; }
        }
        public static class PUTFIELD_S extends Putfield {
            public static final PUTFIELD_S INSTANCE = new PUTFIELD_S();
            private PUTFIELD_S() { }
            public String toString() { return "PUTFIELD_S"; }
        }
        public static class PUTFIELD_Z extends Putfield {
            public static final PUTFIELD_Z INSTANCE = new PUTFIELD_Z();
            private PUTFIELD_Z() { }
            public String toString() { return "PUTFIELD_Z"; }
        }
        public static class PUTFIELD_I_DYNLINK extends PUTFIELD_I {
            public static final PUTFIELD_I_DYNLINK INSTANCE = new PUTFIELD_I_DYNLINK();
            private PUTFIELD_I_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_I%"; }
        }
        public static class PUTFIELD_F_DYNLINK extends PUTFIELD_F {
            public static final PUTFIELD_F_DYNLINK INSTANCE = new PUTFIELD_F_DYNLINK();
            private PUTFIELD_F_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_F%"; }
        }
        public static class PUTFIELD_L_DYNLINK extends PUTFIELD_L {
            public static final PUTFIELD_L_DYNLINK INSTANCE = new PUTFIELD_L_DYNLINK();
            private PUTFIELD_L_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_L%"; }
        }
        public static class PUTFIELD_D_DYNLINK extends PUTFIELD_D {
            public static final PUTFIELD_D_DYNLINK INSTANCE = new PUTFIELD_D_DYNLINK();
            private PUTFIELD_D_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_D%"; }
        }
        public static class PUTFIELD_A_DYNLINK extends PUTFIELD_A {
            public static final PUTFIELD_A_DYNLINK INSTANCE = new PUTFIELD_A_DYNLINK();
            private PUTFIELD_A_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_A%"; }
        }
        public static class PUTFIELD_P_DYNLINK extends PUTFIELD_P {
            public static final PUTFIELD_P_DYNLINK INSTANCE = new PUTFIELD_P_DYNLINK();
            private PUTFIELD_P_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_P%"; }
        }
        public static class PUTFIELD_B_DYNLINK extends PUTFIELD_B {
            public static final PUTFIELD_B_DYNLINK INSTANCE = new PUTFIELD_B_DYNLINK();
            private PUTFIELD_B_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_B%"; }
        }
        public static class PUTFIELD_C_DYNLINK extends PUTFIELD_C {
            public static final PUTFIELD_C_DYNLINK INSTANCE = new PUTFIELD_C_DYNLINK();
            private PUTFIELD_C_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_C%"; }
        }
        public static class PUTFIELD_S_DYNLINK extends PUTFIELD_S {
            public static final PUTFIELD_S_DYNLINK INSTANCE = new PUTFIELD_S_DYNLINK();
            private PUTFIELD_S_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_S%"; }
        }
        public static class PUTFIELD_Z_DYNLINK extends PUTFIELD_Z {
            public static final PUTFIELD_Z_DYNLINK INSTANCE = new PUTFIELD_Z_DYNLINK();
            private PUTFIELD_Z_DYNLINK() { }
            public void accept(Quad q, QuadVisitor qv) {
                qv.visitExceptionThrower(q);
                super.accept(q, qv);
            }
            public List<jq_Class> getThrownExceptions() {
                return resolutionexceptions;
            }
            public String toString() { return "PUTFIELD_Z%"; }
        }
    }

    public abstract static class NullCheck extends Operator {
        public static Quad create(int id, BasicBlock bb, NullCheck operator, Operand dst, Operand src) {
            return new Quad(id, bb, operator, dst, src);
        }
        public static Operand getDest(Quad q) { return q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, Operand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1_check(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitNullCheck(q);
            qv.visitCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return nullptrexception;
        }
        
        public static class NULL_CHECK extends NullCheck {
            public static final NULL_CHECK INSTANCE = new NULL_CHECK();
            private NULL_CHECK() { }
            public String toString() { return "NULL_CHECK"; }
        }
    }

    public abstract static class ZeroCheck extends Operator {
        public static Quad create(int id, BasicBlock bb, ZeroCheck operator, Operand dst, Operand src) {
            return new Quad(id, bb, operator, dst, src);
        }
        public static Operand getDest(Quad q) { return q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, Operand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1_check(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitZeroCheck(q);
            qv.visitCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return arithexception;
        }
        
        public static class ZERO_CHECK_I extends ZeroCheck {
            public static final ZERO_CHECK_I INSTANCE = new ZERO_CHECK_I();
            private ZERO_CHECK_I() { }
            public String toString() { return "ZERO_CHECK_I"; }
        }

        public static class ZERO_CHECK_L extends ZeroCheck {
            public static final ZERO_CHECK_L INSTANCE = new ZERO_CHECK_L();
            private ZERO_CHECK_L() { }
            public String toString() { return "ZERO_CHECK_L"; }
        }
    }
    
    public abstract static class BoundsCheck extends Operator {
        public static Quad create(int id, BasicBlock bb, BoundsCheck operator, Operand ref, Operand idx, Operand guard) {
            return new Quad(id, bb, operator, ref, idx, guard);
        }
        public static Operand getRef(Quad q) { return q.getOp1(); }
        public static Operand getIndex(Quad q) { return q.getOp2(); }
        public static Operand getGuard(Quad q) { return q.getOp3(); }
        public static void setRef(Quad q, Operand o) { q.setOp1(o); }
        public static void setIndex(Quad q, Operand o) { q.setOp2(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg3(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg123(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitBoundsCheck(q);
            qv.visitCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return arrayboundsexception;
        }
        
        public static class BOUNDS_CHECK extends BoundsCheck {
            public static final BOUNDS_CHECK INSTANCE = new BOUNDS_CHECK();
            private BOUNDS_CHECK() { }
            public String toString() { return "BOUNDS_CHECK"; }
        }
    }
    
    public abstract static class StoreCheck extends Operator {
        public static Quad create(int id, BasicBlock bb, StoreCheck operator, Operand ref, Operand elem, Operand guard) {
            return new Quad(id, bb, operator, ref, elem, guard);
        }
        public static Operand getRef(Quad q) { return q.getOp1(); }
        public static Operand getElement(Quad q) { return q.getOp2(); }
        public static Operand getGuard(Quad q) { return q.getOp3(); }
        public static void setRef(Quad q, Operand o) { q.setOp1(o); }
        public static void setElement(Quad q, Operand o) { q.setOp2(o); }
        public static void setGuard(Quad q, Operand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg3(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg123(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitStoreCheck(q);
            qv.visitTypeCheck(q);
            qv.visitCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return arraystoreexception;
        }
        
        public static class ASTORE_CHECK extends StoreCheck {
            public static final ASTORE_CHECK INSTANCE = new ASTORE_CHECK();
            private ASTORE_CHECK() { }
            public String toString() { return "ASTORE_CHECK"; }
        }
    }
    
    public abstract static class Invoke extends Operator {
        public static Quad create(int id, BasicBlock bb, Invoke operator, RegisterOperand res, MethodOperand mo, int length) {
            return new Quad(id, bb, operator, res, mo, new ParamListOperand(new RegisterOperand[length]));
        }
        public static void setParam(Quad q, int i, RegisterOperand t) {
            ((ParamListOperand)q.getOp3()).set(i, t);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static MethodOperand getMethod(Quad q) { return (MethodOperand)q.getOp2(); }
        public static RegisterOperand getParam(Quad q, int i) { return ((ParamListOperand)q.getOp3()).get(i); }
        public static ParamListOperand getParamList(Quad q) { return (ParamListOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setMethod(Quad q, MethodOperand o) { q.setOp2(o); }
        public static void setParamList(Quad q, ParamListOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) {
            ParamListOperand plo = getParamList(q);
            List<RegisterOperand> a = new ArrayList<RegisterOperand>(plo.length());
            for (int i=0; i<plo.length(); ++i) a.add(plo.get(i));
            return a;
        }

        public abstract boolean isVirtual();
        public abstract byte getType();
        public abstract jq_Type getReturnType();
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitInvoke(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return anyexception;
        }
        public boolean hasSideEffects() { return true; }
        
        public abstract static class InvokeVirtual extends Invoke {
            public boolean isVirtual() { return true; }
            public byte getType() { return BytecodeVisitor.INVOKE_VIRTUAL; }
        }
        public abstract static class InvokeStatic extends Invoke {
            public boolean isVirtual() { return false; }
            public byte getType() { return BytecodeVisitor.INVOKE_STATIC; }
        }
        public abstract static class InvokeInterface extends Invoke {
            public boolean isVirtual() { return true; }
            public byte getType() { return BytecodeVisitor.INVOKE_INTERFACE; }
        }
        public static class INVOKEVIRTUAL_V extends InvokeVirtual {
            public static final INVOKEVIRTUAL_V INSTANCE = new INVOKEVIRTUAL_V();
            private INVOKEVIRTUAL_V() { }
            public String toString() { return "INVOKEVIRTUAL_V"; }
            public jq_Type getReturnType() { return jq_Primitive.VOID; }
        }
        public static class INVOKEVIRTUAL_I extends InvokeVirtual {
            public static final INVOKEVIRTUAL_I INSTANCE = new INVOKEVIRTUAL_I();
            private INVOKEVIRTUAL_I() { }
            public String toString() { return "INVOKEVIRTUAL_I"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.INT; }
        }
        public static class INVOKEVIRTUAL_F extends InvokeVirtual {
            public static final INVOKEVIRTUAL_F INSTANCE = new INVOKEVIRTUAL_F();
            private INVOKEVIRTUAL_F() { }
            public String toString() { return "INVOKEVIRTUAL_F"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.FLOAT; }
        }
        public static class INVOKEVIRTUAL_L extends InvokeVirtual {
            public static final INVOKEVIRTUAL_L INSTANCE = new INVOKEVIRTUAL_L();
            private INVOKEVIRTUAL_L() { }
            public String toString() { return "INVOKEVIRTUAL_L"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.LONG; }
        }
        public static class INVOKEVIRTUAL_D extends InvokeVirtual {
            public static final INVOKEVIRTUAL_D INSTANCE = new INVOKEVIRTUAL_D();
            private INVOKEVIRTUAL_D() { }
            public String toString() { return "INVOKEVIRTUAL_D"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.DOUBLE; }
        }
        public static class INVOKEVIRTUAL_A extends InvokeVirtual {
            public static final INVOKEVIRTUAL_A INSTANCE = new INVOKEVIRTUAL_A();
            private INVOKEVIRTUAL_A() { }
            public String toString() { return "INVOKEVIRTUAL_A"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return PrimordialClassLoader.getJavaLangObject(); }
        }
        public static class INVOKEVIRTUAL_P extends InvokeVirtual {
            public static final INVOKEVIRTUAL_P INSTANCE = new INVOKEVIRTUAL_P();
            private INVOKEVIRTUAL_P() { }
            public String toString() { return "INVOKEVIRTUAL_P"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return Address._class; }
        }
        public static class INVOKESTATIC_V extends InvokeStatic {
            public static final INVOKESTATIC_V INSTANCE = new INVOKESTATIC_V();
            private INVOKESTATIC_V() { }
            public String toString() { return "INVOKESTATIC_V"; }
            public jq_Type getReturnType() { return jq_Primitive.VOID; }
        }
        public static class INVOKESTATIC_I extends InvokeStatic {
            public static final INVOKESTATIC_I INSTANCE = new INVOKESTATIC_I();
            private INVOKESTATIC_I() { }
            public String toString() { return "INVOKESTATIC_I"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.INT; }
        }
        public static class INVOKESTATIC_F extends InvokeStatic {
            public static final INVOKESTATIC_F INSTANCE = new INVOKESTATIC_F();
            private INVOKESTATIC_F() { }
            public String toString() { return "INVOKESTATIC_F"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.FLOAT; }
        }
        public static class INVOKESTATIC_L extends InvokeStatic {
            public static final INVOKESTATIC_L INSTANCE = new INVOKESTATIC_L();
            private INVOKESTATIC_L() { }
            public String toString() { return "INVOKESTATIC_L"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.LONG; }
        }
        public static class INVOKESTATIC_D extends InvokeStatic {
            public static final INVOKESTATIC_D INSTANCE = new INVOKESTATIC_D();
            private INVOKESTATIC_D() { }
            public String toString() { return "INVOKESTATIC_D"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.DOUBLE; }
        }
        public static class INVOKESTATIC_A extends InvokeStatic {
            public static final INVOKESTATIC_A INSTANCE = new INVOKESTATIC_A();
            private INVOKESTATIC_A() { }
            public String toString() { return "INVOKESTATIC_A"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return PrimordialClassLoader.getJavaLangObject(); }
        }
        public static class INVOKESTATIC_P extends InvokeStatic {
            public static final INVOKESTATIC_P INSTANCE = new INVOKESTATIC_P();
            private INVOKESTATIC_P() { }
            public String toString() { return "INVOKESTATIC_P"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return Address._class; }
        }
        public static class INVOKEVIRTUAL_V_DYNLINK extends INVOKEVIRTUAL_V {
            public static final INVOKEVIRTUAL_V_DYNLINK INSTANCE = new INVOKEVIRTUAL_V_DYNLINK();
            private INVOKEVIRTUAL_V_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_V%"; }
        }
        public static class INVOKEVIRTUAL_I_DYNLINK extends INVOKEVIRTUAL_I {
            public static final INVOKEVIRTUAL_I_DYNLINK INSTANCE = new INVOKEVIRTUAL_I_DYNLINK();
            private INVOKEVIRTUAL_I_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_I%"; }
        }
        public static class INVOKEVIRTUAL_F_DYNLINK extends INVOKEVIRTUAL_F {
            public static final INVOKEVIRTUAL_F_DYNLINK INSTANCE = new INVOKEVIRTUAL_F_DYNLINK();
            private INVOKEVIRTUAL_F_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_F%"; }
        }
        public static class INVOKEVIRTUAL_L_DYNLINK extends INVOKEVIRTUAL_L {
            public static final INVOKEVIRTUAL_L_DYNLINK INSTANCE = new INVOKEVIRTUAL_L_DYNLINK();
            private INVOKEVIRTUAL_L_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_L%"; }
        }
        public static class INVOKEVIRTUAL_D_DYNLINK extends INVOKEVIRTUAL_D {
            public static final INVOKEVIRTUAL_D_DYNLINK INSTANCE = new INVOKEVIRTUAL_D_DYNLINK();
            private INVOKEVIRTUAL_D_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_D%"; }
        }
        public static class INVOKEVIRTUAL_A_DYNLINK extends INVOKEVIRTUAL_A {
            public static final INVOKEVIRTUAL_A_DYNLINK INSTANCE = new INVOKEVIRTUAL_A_DYNLINK();
            private INVOKEVIRTUAL_A_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_A%"; }
        }
        public static class INVOKEVIRTUAL_P_DYNLINK extends INVOKEVIRTUAL_P {
            public static final INVOKEVIRTUAL_P_DYNLINK INSTANCE = new INVOKEVIRTUAL_P_DYNLINK();
            private INVOKEVIRTUAL_P_DYNLINK() { }
            public String toString() { return "INVOKEVIRTUAL_P%"; }
        }
        public static class INVOKESTATIC_V_DYNLINK extends INVOKESTATIC_V {
            public static final INVOKESTATIC_V_DYNLINK INSTANCE = new INVOKESTATIC_V_DYNLINK();
            private INVOKESTATIC_V_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_V%"; }
        }
        public static class INVOKESTATIC_I_DYNLINK extends INVOKESTATIC_I {
            public static final INVOKESTATIC_I_DYNLINK INSTANCE = new INVOKESTATIC_I_DYNLINK();
            private INVOKESTATIC_I_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_I%"; }
        }
        public static class INVOKESTATIC_F_DYNLINK extends INVOKESTATIC_F {
            public static final INVOKESTATIC_F_DYNLINK INSTANCE = new INVOKESTATIC_F_DYNLINK();
            private INVOKESTATIC_F_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_F%"; }
        }
        public static class INVOKESTATIC_L_DYNLINK extends INVOKESTATIC_L {
            public static final INVOKESTATIC_L_DYNLINK INSTANCE = new INVOKESTATIC_L_DYNLINK();
            private INVOKESTATIC_L_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_L%"; }
        }
        public static class INVOKESTATIC_D_DYNLINK extends INVOKESTATIC_D {
            public static final INVOKESTATIC_D_DYNLINK INSTANCE = new INVOKESTATIC_D_DYNLINK();
            private INVOKESTATIC_D_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_D%"; }
        }
        public static class INVOKESTATIC_A_DYNLINK extends INVOKESTATIC_A {
            public static final INVOKESTATIC_A_DYNLINK INSTANCE = new INVOKESTATIC_A_DYNLINK();
            private INVOKESTATIC_A_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_A%"; }
        }
        public static class INVOKESTATIC_P_DYNLINK extends INVOKESTATIC_P {
            public static final INVOKESTATIC_P_DYNLINK INSTANCE = new INVOKESTATIC_P_DYNLINK();
            private INVOKESTATIC_P_DYNLINK() { }
            public String toString() { return "INVOKESTATIC_P%"; }
        }
        public static class INVOKESPECIAL_V_DYNLINK extends INVOKESTATIC_V {
            public static final INVOKESPECIAL_V_DYNLINK INSTANCE = new INVOKESPECIAL_V_DYNLINK();
            private INVOKESPECIAL_V_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_V%"; }
        }
        public static class INVOKESPECIAL_I_DYNLINK extends INVOKESTATIC_I {
            public static final INVOKESPECIAL_I_DYNLINK INSTANCE = new INVOKESPECIAL_I_DYNLINK();
            private INVOKESPECIAL_I_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_I%"; }
        }
        public static class INVOKESPECIAL_F_DYNLINK extends INVOKESTATIC_F {
            public static final INVOKESPECIAL_F_DYNLINK INSTANCE = new INVOKESPECIAL_F_DYNLINK();
            private INVOKESPECIAL_F_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_F%"; }
        }
        public static class INVOKESPECIAL_L_DYNLINK extends INVOKESTATIC_L {
            public static final INVOKESPECIAL_L_DYNLINK INSTANCE = new INVOKESPECIAL_L_DYNLINK();
            private INVOKESPECIAL_L_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_L%"; }
        }
        public static class INVOKESPECIAL_D_DYNLINK extends INVOKESTATIC_D {
            public static final INVOKESPECIAL_D_DYNLINK INSTANCE = new INVOKESPECIAL_D_DYNLINK();
            private INVOKESPECIAL_D_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_D%"; }
        }
        public static class INVOKESPECIAL_A_DYNLINK extends INVOKESTATIC_A {
            public static final INVOKESPECIAL_A_DYNLINK INSTANCE = new INVOKESPECIAL_A_DYNLINK();
            private INVOKESPECIAL_A_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_A%"; }
        }
        public static class INVOKESPECIAL_P_DYNLINK extends INVOKESTATIC_P {
            public static final INVOKESPECIAL_P_DYNLINK INSTANCE = new INVOKESPECIAL_P_DYNLINK();
            private INVOKESPECIAL_P_DYNLINK() { }
            public String toString() { return "INVOKESPECIAL_P%"; }
        }
        public static class INVOKEINTERFACE_V extends InvokeInterface {
            public static final INVOKEINTERFACE_V INSTANCE = new INVOKEINTERFACE_V();
            private INVOKEINTERFACE_V() { }
            public String toString() { return "INVOKEINTERFACE_V"; }
            public jq_Type getReturnType() { return jq_Primitive.VOID; }
        }
        public static class INVOKEINTERFACE_I extends InvokeInterface {
            public static final INVOKEINTERFACE_I INSTANCE = new INVOKEINTERFACE_I();
            private INVOKEINTERFACE_I() { }
            public String toString() { return "INVOKEINTERFACE_I"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.INT; }
        }
        public static class INVOKEINTERFACE_F extends InvokeInterface {
            public static final INVOKEINTERFACE_F INSTANCE = new INVOKEINTERFACE_F();
            private INVOKEINTERFACE_F() { }
            public String toString() { return "INVOKEINTERFACE_F"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.FLOAT; }
        }
        public static class INVOKEINTERFACE_L extends InvokeInterface {
            public static final INVOKEINTERFACE_L INSTANCE = new INVOKEINTERFACE_L();
            private INVOKEINTERFACE_L() { }
            public String toString() { return "INVOKEINTERFACE_L"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.LONG; }
        }
        public static class INVOKEINTERFACE_D extends InvokeInterface {
            public static final INVOKEINTERFACE_D INSTANCE = new INVOKEINTERFACE_D();
            private INVOKEINTERFACE_D() { }
            public String toString() { return "INVOKEINTERFACE_D"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return jq_Primitive.DOUBLE; }
        }
        public static class INVOKEINTERFACE_A extends InvokeInterface {
            public static final INVOKEINTERFACE_A INSTANCE = new INVOKEINTERFACE_A();
            private INVOKEINTERFACE_A() { }
            public String toString() { return "INVOKEINTERFACE_A"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return PrimordialClassLoader.getJavaLangObject(); }
        }
        public static class INVOKEINTERFACE_P extends InvokeInterface {
            public static final INVOKEINTERFACE_P INSTANCE = new INVOKEINTERFACE_P();
            private INVOKEINTERFACE_P() { }
            public String toString() { return "INVOKEINTERFACE_P"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public jq_Type getReturnType() { return Address._class; }
        }
    }
    
    public abstract static class New extends Operator {
        public static Quad create(int id, BasicBlock bb, New operator, RegisterOperand res, TypeOperand type) {
            return new Quad(id, bb, operator, res, type);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static TypeOperand getType(Quad q) { return (TypeOperand)q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setType(Quad q, TypeOperand o) { q.setOp2(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitNew(q);
            qv.visitAllocation(q);
            super.accept(q, qv);
        }
        
        public static class NEW extends New {
            public static final NEW INSTANCE = new NEW();
            private NEW() { }
            public String toString() { return "NEW"; }
        }
        public static class NEW_DYNLINK extends NEW {
            public static final NEW_DYNLINK INSTANCE = new NEW_DYNLINK();
            private NEW_DYNLINK() { }
            public String toString() { return "NEW%"; }
        }
    }
    
    public abstract static class NewArray extends Operator {
        public static Quad create(int id, BasicBlock bb, NewArray operator, RegisterOperand res, Operand size, TypeOperand type) {
            return new Quad(id, bb, operator, res, size, type);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSize(Quad q) { return q.getOp2(); }
        public static TypeOperand getType(Quad q) { return (TypeOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSize(Quad q, Operand o) { q.setOp2(o); }
        public static void setType(Quad q, TypeOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitNewArray(q);
            qv.visitAllocation(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return negativesizeexception;
        }
        
        public static class NEWARRAY extends NewArray {
            public static final NEWARRAY INSTANCE = new NEWARRAY();
            private NEWARRAY() { }
            public String toString() { return "NEWARRAY"; }
        }
    }

    public abstract static class MultiNewArray extends Operator {
        public static Quad create(int id, BasicBlock bb, MultiNewArray operator, RegisterOperand res, int dims, TypeOperand type) {
            return new Quad(id, bb, operator, res, new ParamListOperand(new RegisterOperand[dims]), type);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static ParamListOperand getParamList(Quad q) { return (ParamListOperand)q.getOp2(); }
        public static RegisterOperand getParam(Quad q, int i) { return ((ParamListOperand)q.getOp2()).get(i); }
        public static TypeOperand getType(Quad q) { return (TypeOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
		public static void setParam(Quad q, int i, RegisterOperand o) {
            ((ParamListOperand)q.getOp2()).set(i, o);
		}
        public static void setType(Quad q, TypeOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) {
            ParamListOperand plo = getParamList(q);
            List<RegisterOperand> a = new ArrayList<RegisterOperand>(plo.length());
            for (int i=0; i<plo.length(); ++i) a.add(plo.get(i));
            return a;
		}
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitMultiNewArray(q);
            qv.visitAllocation(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return negativesizeexception;
        }
        
        public static class MULTINEWARRAY extends MultiNewArray {
            public static final MULTINEWARRAY INSTANCE = new MULTINEWARRAY();
            private MULTINEWARRAY() { }
            public String toString() { return "MULTINEWARRAY"; }
        }
	}
    
    public abstract static class CheckCast extends Operator {
        public static Quad create(int id, BasicBlock bb, CheckCast operator, RegisterOperand res, Operand val, TypeOperand type) {
            return new Quad(id, bb, operator, res, val, type);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static TypeOperand getType(Quad q) { return (TypeOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public static void setType(Quad q, TypeOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitCheckCast(q);
            qv.visitTypeCheck(q);
            qv.visitCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return classcastexceptions;
        }
        
        public static class CHECKCAST extends CheckCast {
            public static final CHECKCAST INSTANCE = new CHECKCAST();
            private CHECKCAST() { }
            public String toString() { return "CHECKCAST"; }
        }
    }
    
    public abstract static class InstanceOf extends Operator {
        public static Quad create(int id, BasicBlock bb, InstanceOf operator, RegisterOperand res, Operand val, TypeOperand type) {
            return new Quad(id, bb, operator, res, val, type);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static TypeOperand getType(Quad q) { return (TypeOperand)q.getOp3(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public static void setType(Quad q, TypeOperand o) { q.setOp3(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitInstanceOf(q);
            qv.visitTypeCheck(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        public List<jq_Class> getThrownExceptions() {
            return resolutionexceptions;
        }
        
        public static class INSTANCEOF extends InstanceOf {
            public static final INSTANCEOF INSTANCE = new INSTANCEOF();
            private INSTANCEOF() { }
            public String toString() { return "INSTANCEOF"; }
        }
    }
    
    public abstract static class ALength extends Operator {
        public static Quad create(int id, BasicBlock bb, ALength operator, RegisterOperand res, Operand val) {
            return new Quad(id, bb, operator, res, val);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return false; }

        public void accept(Quad q, QuadVisitor qv) {
            qv.visitALength(q);
            qv.visitArray(q);
            super.accept(q, qv);
        }
        
        public static class ARRAYLENGTH extends ALength {
            public static final ARRAYLENGTH INSTANCE = new ARRAYLENGTH();
            private ARRAYLENGTH() { }
            public String toString() { return "ARRAYLENGTH"; }
        }
    }
    
    public abstract static class Monitor extends Operator {
        public static Quad create(int id, BasicBlock bb, Monitor operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Operand getSrc(Quad q) { return q.getOp2(); }
        public static void setSrc(Quad q, Operand o) { q.setOp2(o); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitMonitor(q);
            qv.visitExceptionThrower(q);
            super.accept(q, qv);
        }
        
        public static class MONITORENTER extends Monitor {
            public static final MONITORENTER INSTANCE = new MONITORENTER();
            private MONITORENTER() { }
            public String toString() { return "MONITORENTER"; }
            public List<jq_Class> getThrownExceptions() {
                return nullptrexception;
            }
        }
        public static class MONITOREXIT extends Monitor {
            public static final MONITOREXIT INSTANCE = new MONITOREXIT();
            private MONITOREXIT() { }
            public String toString() { return "MONITOREXIT"; }
            public List<jq_Class> getThrownExceptions() {
                return illegalmonitorstateexception;
            }
        }
    }
    
    public abstract static class MemLoad extends Operator {
        public static Quad create(int id, BasicBlock bb, MemLoad operator, RegisterOperand dst, Operand addr) {
            return new Quad(id, bb, operator, dst, addr);
        }
        public static RegisterOperand getDest(Quad q) { return (RegisterOperand)q.getOp1(); }
        public static Operand getAddress(Quad q) { return q.getOp2(); }
        public static void setDest(Quad q, RegisterOperand o) { q.setOp1(o); }
        public static void setAddress(Quad q, Operand o) { q.setOp2(o); }
        public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        public boolean hasSideEffects() { return false; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitMemLoad(q);
            qv.visitLoad(q);
            super.accept(q, qv);
        }
        
        public static class PEEK_P extends MemLoad {
            public static final PEEK_P INSTANCE = new PEEK_P();
            private PEEK_P() { }
            public String toString() { return "PEEK_P"; }
        }
        public static class PEEK_1 extends MemLoad {
            public static final PEEK_1 INSTANCE = new PEEK_1();
            private PEEK_1() { }
            public String toString() { return "PEEK_1"; }
        }
        public static class PEEK_2 extends MemLoad {
            public static final PEEK_2 INSTANCE = new PEEK_2();
            private PEEK_2() { }
            public String toString() { return "PEEK_2"; }
        }
        public static class PEEK_4 extends MemLoad {
            public static final PEEK_4 INSTANCE = new PEEK_4();
            private PEEK_4() { }
            public String toString() { return "PEEK_4"; }
        }
        public static class PEEK_8 extends MemLoad {
            public static final PEEK_8 INSTANCE = new PEEK_8();
            private PEEK_8() { }
            public String toString() { return "PEEK_8"; }
        }
    }

    public abstract static class MemStore extends Operator {
        public static Quad create(int id, BasicBlock bb, MemStore operator, Operand addr, Operand val) {
            return new Quad(id, bb, operator, null, addr, val);
        }
        public static Operand getAddress(Quad q) { return q.getOp2(); }
        public static Operand getValue(Quad q) { return q.getOp3(); }
        public static void setAddress(Quad q, Operand o) { q.setOp2(o); }
        public static void setValue(Quad q, Operand o) { q.setOp3(o); }
        public boolean hasSideEffects() { return true; }
        public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg23(q); }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitMemStore(q);
            qv.visitStore(q);
            super.accept(q, qv);
        }
        
        public static class POKE_P extends MemStore {
            public static final POKE_P INSTANCE = new POKE_P();
            private POKE_P() { }
            public String toString() { return "POKE_P"; }
        }
        public static class POKE_1 extends MemStore {
            public static final POKE_1 INSTANCE = new POKE_1();
            private POKE_1() { }
            public String toString() { return "POKE_1"; }
        }
        public static class POKE_2 extends MemStore {
            public static final POKE_2 INSTANCE = new POKE_2();
            private POKE_2() { }
            public String toString() { return "POKE_2"; }
        }
        public static class POKE_4 extends MemStore {
            public static final POKE_4 INSTANCE = new POKE_4();
            private POKE_4() { }
            public String toString() { return "POKE_4"; }
        }
        public static class POKE_8 extends MemStore {
            public static final POKE_8 INSTANCE = new POKE_8();
            private POKE_8() { }
            public String toString() { return "POKE_8"; }
        }
    }
    
    public abstract static class Special extends Operator {
        
        public static Quad create(int id, BasicBlock bb, GET_EXCEPTION operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, GET_BASE_POINTER operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, GET_STACK_POINTER operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, GET_THREAD_BLOCK operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, SET_THREAD_BLOCK operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Quad create(int id, BasicBlock bb, ALLOCA operator, RegisterOperand res, Operand val) {
            return new Quad(id, bb, operator, res, val);
        }
        public static Quad create(int id, BasicBlock bb, NOP operator) {
            return new Quad(id, bb, operator, null, null);
        }
        public static Quad create(int id, BasicBlock bb, ATOMICADD_I operator, Operand loc, Operand val) {
            return new Quad(id, bb, operator, null, loc, val);
        }
        public static Quad create(int id, BasicBlock bb, ATOMICSUB_I operator, Operand loc, Operand val) {
            return new Quad(id, bb, operator, null, loc, val);
        }
        public static Quad create(int id, BasicBlock bb, ATOMICAND_I operator, Operand loc, Operand val) {
            return new Quad(id, bb, operator, null, loc, val);
        }
        public static Quad create(int id, BasicBlock bb, ATOMICCAS4 operator, RegisterOperand res, Operand loc, Operand val1, Operand val2) {
            return new Quad(id, bb, operator, res, loc, val1, val2);
        }
        public static Quad create(int id, BasicBlock bb, ATOMICCAS8 operator, RegisterOperand res, Operand loc, Operand val1, Operand val2) {
            return new Quad(id, bb, operator, res, loc, val1, val2);
        }
        public static Quad create(int id, BasicBlock bb, LONG_JUMP operator, Operand ip, Operand fp, Operand sp, Operand eax) {
            return new Quad(id, bb, operator, ip, fp, sp, eax);
        }
        public static Quad create(int id, BasicBlock bb, POP_FP32 operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, POP_FP64 operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, PUSH_FP32 operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Quad create(int id, BasicBlock bb, PUSH_FP64 operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Quad create(int id, BasicBlock bb, GET_EAX operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, PUSHARG_I operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Quad create(int id, BasicBlock bb, PUSHARG_P operator, Operand val) {
            return new Quad(id, bb, operator, null, val);
        }
        public static Quad create(int id, BasicBlock bb, INVOKE_L operator, RegisterOperand res, Operand val) {
            return new Quad(id, bb, operator, res, val);
        }
        public static Quad create(int id, BasicBlock bb, INVOKE_P operator, RegisterOperand res, Operand val) {
            return new Quad(id, bb, operator, res, val);
        }
        public static Quad create(int id, BasicBlock bb, ISEQ operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Quad create(int id, BasicBlock bb, ISGE operator, RegisterOperand res) {
            return new Quad(id, bb, operator, res);
        }
        public static Operand getOp1(Quad q) { return q.getOp1(); }
        public static Operand getOp2(Quad q) { return q.getOp2(); }
        public static Operand getOp3(Quad q) { return q.getOp3(); }
        public static Operand getOp4(Quad q) { return q.getOp4(); }
        public static void setOp1(Quad q, Operand o) { q.setOp1(o); }
        public static void setOp2(Quad q, Operand o) { q.setOp2(o); }
        public static void setOp3(Quad q, Operand o) { q.setOp3(o); }
        public static void setOp4(Quad q, Operand o) { q.setOp4(o); }
        public boolean hasSideEffects() { return true; }
        
        public void accept(Quad q, QuadVisitor qv) {
            qv.visitSpecial(q);
            super.accept(q, qv);
        }
        
        public static class GET_EXCEPTION extends Special {
            public static final GET_EXCEPTION INSTANCE = new GET_EXCEPTION();
            private GET_EXCEPTION() { }
            public String toString() { return "GET_EXCEPTION"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class GET_BASE_POINTER extends Special {
            public static final GET_BASE_POINTER INSTANCE = new GET_BASE_POINTER();
            private GET_BASE_POINTER() { }
            public String toString() { return "GET_BASE_POINTER"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class GET_STACK_POINTER extends Special {
            public static final GET_STACK_POINTER INSTANCE = new GET_STACK_POINTER();
            private GET_STACK_POINTER() { }
            public String toString() { return "GET_STACK_POINTER"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class GET_THREAD_BLOCK extends Special {
            public static final GET_THREAD_BLOCK INSTANCE = new GET_THREAD_BLOCK();
            private GET_THREAD_BLOCK() { }
            public String toString() { return "GET_THREAD_BLOCK"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class SET_THREAD_BLOCK extends Special {
            public static final SET_THREAD_BLOCK INSTANCE = new SET_THREAD_BLOCK();
            private SET_THREAD_BLOCK() { }
            public String toString() { return "SET_THREAD_BLOCK"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class ATOMICADD_I extends Special {
            public static final ATOMICADD_I INSTANCE = new ATOMICADD_I();
            private ATOMICADD_I() { }
            public String toString() { return "ATOMICADD_I"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg23(q); }
        }
        public static class ATOMICSUB_I extends Special {
            public static final ATOMICSUB_I INSTANCE = new ATOMICSUB_I();
            private ATOMICSUB_I() { }
            public String toString() { return "ATOMICSUB_I"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg23(q); }
        }
        public static class ATOMICAND_I extends Special {
            public static final ATOMICAND_I INSTANCE = new ATOMICAND_I();
            private ATOMICAND_I() { }
            public String toString() { return "ATOMICAND_I"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg23(q); }
        }
        public static class ATOMICCAS4 extends Special {
            public static final ATOMICCAS4 INSTANCE = new ATOMICCAS4();
            private ATOMICCAS4() { }
            public String toString() { return "ATOMICCAS4"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg234(q); }
        }
        public static class ATOMICCAS8 extends Special {
            public static final ATOMICCAS8 INSTANCE = new ATOMICCAS8();
            private ATOMICCAS8() { }
            public String toString() { return "ATOMICCAS8"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg234(q); }
        }
        public static class ALLOCA extends Special {
            public static final ALLOCA INSTANCE = new ALLOCA();
            private ALLOCA() { }
            public String toString() { return "ALLOCA"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class LONG_JUMP extends Special {
            public static final LONG_JUMP INSTANCE = new LONG_JUMP();
            private LONG_JUMP() { }
            public String toString() { return "LONG_JUMP"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg1234(q); }
        }
        public static class POP_FP32 extends Special {
            public static final POP_FP32 INSTANCE = new POP_FP32();
            private POP_FP32() { }
            public String toString() { return "POP_FP32"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class POP_FP64 extends Special {
            public static final POP_FP64 INSTANCE = new POP_FP64();
            private POP_FP64() { }
            public String toString() { return "POP_FP64"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class PUSH_FP32 extends Special {
            public static final PUSH_FP32 INSTANCE = new PUSH_FP32();
            private PUSH_FP32() { }
            public String toString() { return "PUSH_FP32"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class PUSH_FP64 extends Special {
            public static final PUSH_FP64 INSTANCE = new PUSH_FP64();
            private PUSH_FP64() { }
            public String toString() { return "PUSH_FP64"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class GET_EAX extends Special {
            public static final GET_EAX INSTANCE = new GET_EAX();
            private GET_EAX() { }
            public String toString() { return "GET_EAX"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class PUSHARG_I extends Special {
            public static final PUSHARG_I INSTANCE = new PUSHARG_I();
            private PUSHARG_I() { }
            public String toString() { return "PUSHARG_I"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class PUSHARG_P extends Special {
            public static final PUSHARG_P INSTANCE = new PUSHARG_P();
            private PUSHARG_P() { }
            public String toString() { return "PUSHARG_P"; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class INVOKE_L extends Special {
            public static final INVOKE_L INSTANCE = new INVOKE_L();
            private INVOKE_L() { }
            public String toString() { return "INVOKE_L"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class INVOKE_P extends Special {
            public static final INVOKE_P INSTANCE = new INVOKE_P();
            private INVOKE_P() { }
            public String toString() { return "INVOKE_P"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return getReg2(q); }
        }
        public static class ISEQ extends Special {
            public static final ISEQ INSTANCE = new ISEQ();
            private ISEQ() { }
            public String toString() { return "ISEQ"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        public static class ISGE extends Special {
            public static final ISGE INSTANCE = new ISGE();
            private ISGE() { }
            public String toString() { return "ISGE"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return getReg1(q); }
        }
        
        public static class NOP extends Operator.Special {
            public static final NOP INSTANCE = new NOP();
            private NOP() {}
            public String toString() { return "NOP"; }
            public List<RegisterOperand> getDefinedRegisters(Quad q) { return null; }
            public List<RegisterOperand> getUsedRegisters(Quad q) { return null; }
        }
    }

	// MAYUR: copied from joeq.Runtime.MathSupport to prevent unnecessarily
	// loading that class during chord's scope construction
    public static boolean ucmp(/*unsigned*/int a, /*unsigned*/int b) {
        if ((b < 0) && (a >= 0)) return true;
        if ((b >= 0) && (a < 0)) return false;
        return a<b;
    }

	public boolean isHeapInst() {
        return this instanceof ALoad || this instanceof AStore ||
            this instanceof Getfield || this instanceof Putfield ||
            this instanceof Getstatic || this instanceof Putstatic;
	}
    public  boolean isWrHeapInst() {
        return this instanceof Putfield || this instanceof AStore ||
            this instanceof Putstatic;
    }
}
