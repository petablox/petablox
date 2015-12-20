package petablox.util.soot;

import soot.Unit;

public interface UnitVisitor {

    /** A potentially excepting instruction.. */
    void visitExceptionThrower(Unit obj);
    /** An instruction that loads from memory. */
    void visitLoad(Unit obj);
    /** An instruction that stores into memory. */
    void visitStore(Unit obj);
    /** An instruction that may branch (not including exceptional control flow). */
    void visitBranch(Unit obj);
    /** A conditional branch instruction. */
    void visitCondBranch(Unit obj);
    /** An exception check instruction. */
    void visitCheck(Unit obj);
    /** An instruction.that accesses a static field. */
    void visitStaticField(Unit obj);
    /** An instruction.that accesses an instance field. */
    void visitInstanceField(Unit obj);
    /** An instruction.that accesses an array. */
    void visitArray(Unit obj);
    /** An instruction.that does an allocation. */
    void visitAllocation(Unit obj);
    /** An instruction.that does a type check. */
    void visitTypeCheck(Unit obj);
    
    /** An array load instruction. */
    void visitALoad(Unit obj);
    /** An array store instruction. */
    void visitAStore(Unit obj);
    /** An array length instruction. */
    void visitALength(Unit obj);
    /** A binary operation instruction. */
    void visitBinary(Unit obj);
    /** An array bounds check instruction. */
    void visitBoundsCheck(Unit obj);
    /** A type cast check instruction. */
    void visitCheckCast(Unit obj);
    /** A get instance field instruction. */
    void visitGetfield(Unit obj);
    /** A get static field instruction. */
    void visitGetstatic(Unit obj);
    /** A goto instruction. */
    void visitGoto(Unit obj);
    /** A type instance of instruction. */
    void visitInstanceOf(Unit obj);
    /** A compare and branch instruction. */
    void visitIntIfCmp(Unit obj);
    /** An invoke instruction. */
    void visitInvoke(Unit obj);
    /** A jump local subroutine instruction. */
    void visitJsr(Unit obj);
    /** A lookup switch instruction. */
    void visitLookupSwitch(Unit obj);
    /** A raw memory load instruction. */
    void visitMemLoad(Unit obj);
    /** A raw memory store instruction. */
    void visitMemStore(Unit obj);
    /** An object monitor lock/unlock instruction. */
    void visitMonitor(Unit obj);
    /** A register move instruction. */
    void visitMove(Unit obj);
    /** An object allocation instruction. */
    void visitNew(Unit obj);
    /** An array allocation instruction. */
    void visitNewArray(Unit obj);
	/** A multi-dimensional array allocation instruction */
	void visitMultiNewArray(Unit obj);
    /** A null pointer check instruction. */
    void visitNullCheck(Unit obj);
    /** A phi instruction. (For SSA.) */
    void visitPhi(Unit obj);
    /** A put instance field instruction. */
    void visitPutfield(Unit obj);
    /** A put static field instruction. */
    void visitPutstatic(Unit obj);
    /** A return from local subroutine instruction. */
    void visitRet(Unit obj);
    /** A return from method instruction. */
    void visitReturn(Unit obj);
    /** A special instruction. */
    void visitSpecial(Unit obj);
    /** An object array store type check instruction. */
    void visitStoreCheck(Unit obj);
    /** A jump table switch instruction. */
    void visitTableSwitch(Unit obj);
    /** A unary operation instruction. */
    void visitUnary(Unit obj);
    /** A divide-by-zero check instruction. */
    void visitZeroCheck(Unit obj);
    
    /** Any Unit. */
    void visitUnit(Unit obj);
    
    abstract class EmptyVisitor implements UnitVisitor {
        /** A potentially excepting instruction.. */
        public void visitExceptionThrower(Unit obj) {}
        /** An instruction that loads from memory. */
        public void visitLoad(Unit obj) {}
        /** An instruction that stores into memory. */
        public void visitStore(Unit obj) {}
        /** An instruction that may branch (not including exceptional control flow). */
        public void visitBranch(Unit obj) {}
        /** A conditional branch instruction. */
        public void visitCondBranch(Unit obj) {}
        /** An exception check instruction. */
        public void visitCheck(Unit obj) {}
        /** An instruction.that accesses a static field. */
        public void visitStaticField(Unit obj) {}
        /** An instruction.that accesses an instance field. */
        public void visitInstanceField(Unit obj) {}
        /** An instruction.that accesses an array. */
        public void visitArray(Unit obj) {}
        /** An instruction.that does an allocation. */
        public void visitAllocation(Unit obj) {}
        /** An instruction.that does a type check. */
        public void visitTypeCheck(Unit obj) {}

        /** An array load instruction. */
        public void visitALoad(Unit obj) {}
        /** An array store instruction. */
        public void visitAStore(Unit obj) {}
        /** An array length instruction. */
        public void visitALength(Unit obj) {}
        /** A binary operation instruction. */
        public void visitBinary(Unit obj) {}
        /** An array bounds check instruction. */
        public void visitBoundsCheck(Unit obj) {}
        /** A type cast check instruction. */
        public void visitCheckCast(Unit obj) {}
        /** A get instance field instruction. */
        public void visitGetfield(Unit obj) {}
        /** A get static field instruction. */
        public void visitGetstatic(Unit obj) {}
        /** A goto instruction. */
        public void visitGoto(Unit obj) {}
        /** A type instance of instruction. */
        public void visitInstanceOf(Unit obj) {}
        /** A compare and branch instruction. */
        public void visitIntIfCmp(Unit obj) {}
        /** An invoke instruction. */
        public void visitInvoke(Unit obj) {}
        /** A jump local subroutine instruction. */
        public void visitJsr(Unit obj) {}
        /** A lookup switch instruction. */
        public void visitLookupSwitch(Unit obj) {}
        /** A raw memory load instruction. */
        public void visitMemLoad(Unit obj) {}
        /** A raw memory store instruction. */
        public void visitMemStore(Unit obj) {}
        /** An object monitor lock/unlock instruction. */
        public void visitMonitor(Unit obj) {}
        /** A register move instruction. */
        public void visitMove(Unit obj) {}
        /** An object allocation instruction. */
        public void visitNew(Unit obj) {}
        /** An array allocation instruction. */
        public void visitNewArray(Unit obj) {}
        /** A multi-dimensional array allocation instruction. */
        public void visitMultiNewArray(Unit obj) {}
        /** A null pointer check instruction. */
        public void visitNullCheck(Unit obj) {}
        /** A phi instruction. (For SSA.) */
        public void visitPhi(Unit obj) {}
        /** A put instance field instruction. */
        public void visitPutfield(Unit obj) {}
        /** A put static field instruction. */
        public void visitPutstatic(Unit obj) {}
        /** A return from local subroutine instruction. */
        public void visitRet(Unit obj) {}
        /** A return from method instruction. */
        public void visitReturn(Unit obj) {}
        /** A special instruction. */
        public void visitSpecial(Unit obj) {}
        /** An object array store type check instruction. */
        public void visitStoreCheck(Unit obj) {}
        /** A jump table switch instruction. */
        public void visitTableSwitch(Unit obj) {}
        /** A unary operation instruction. */
        public void visitUnary(Unit obj) {}
        /** A divide-by-zero check instruction. */
        public void visitZeroCheck(Unit obj) {}
        
        /** Any Unit. */
        public void visitUnit(Unit obj) {}
    }
}