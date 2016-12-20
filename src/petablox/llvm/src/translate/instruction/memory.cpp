#include "instruction.h"

using namespace llvm;

/*
 * 'alloca' instruction
 * (http://llvm.org/docs/LangRef.html#alloca-instruction)
 */
void translateAlloca(unsigned long id, AllocaInst *alloca_inst) {
    // Get data for relations
    unsigned alignment = alloca_inst->getAlignment();
    Value *size = alloca_inst->getArraySize();
    Type *type = alloca_inst->getAllocatedType();

    // Generate facts
    errs() << "alloca_instruction(" << id << ").\n";
    errs() << "alloca_instruction_alignment(" << id << ", " << alignment << ").\n";
    errs() << "alloca_instruction_size(" << id << ", " << *size << ").\n";
    errs() << "alloca_instruction_type(" << id << ". " << type->getTypeID() << ").\n";
    errs() << "\n";
}

/*
 * 'load' instruction
 * (http://llvm.org/docs/LangRef.html#load-instruction)
 */
void translateLoad(unsigned long id, LoadInst *load_inst) {
    // Get data for relations
    unsigned alignment = load_inst->getAlignment();
    AtomicOrdering order = load_inst->getOrdering();
    Value *addr = load_inst->getPointerOperand();

    // Generate facts
    errs() << "load_instruction(" << id << ").\n";
    errs() << "load_instruction_alignment(" << id << ", " << alignment << ").\n";
    errs() << "load_instruction_ordering(" << id << ", " << (int) order << ").\n";
    if (load_inst->isVolatile()) {
        errs() << "load_instruction_volatile(" << id << ").\n";
    }
    errs() << "load_instruction_address(" << id << ", " << (unsigned long) addr << ").\n";
    errs() << "\n";
}

/*
 * 'store' instruction
 * (http://llvm.org/docs/LangRef.html#store-instruction)
 */
void translateStore(unsigned long id, StoreInst *store_inst) {
    // Get data for relations
    unsigned alignment = store_inst->getAlignment();
    AtomicOrdering order = store_inst->getOrdering();
    Value *value = store_inst->getValueOperand();
    Value *addr = store_inst->getPointerOperand();


    if (dyn_cast<Constant>(value)) {
        errs() << "constant(" << (unsigned long) value << ", " << *value << ").\n";
    }

    // Generate facts
    errs() << "store_instruction(" << id << ").\n";
    errs() << "store_instruction_alignment(" << id << ", " << alignment << ").\n";
    errs() << "store_instruction_ordering(" << id << ", " << (int) order << ").\n";
    if (store_inst->isVolatile()) {
        errs() << "store_instruction_volatile(" << id << ").\n";
    }
    errs() << "store_instruction_value(" << id << ", " << (unsigned long) value << ").\n";
    errs() << "store_instruction_address(" << id << ", " << (unsigned long) addr << ").\n";
    errs() << "\n";
}

/*
 * 'fence' instruction
 * (http://llvm.org/docs/LangRef.html#fence-instruction)
 */
void translateFence(unsigned long id, FenceInst *fence_inst) {
    // Get data for relations
    AtomicOrdering order = fence_inst->getOrdering();

    // Generate facts
    errs() << "fence_instruction(" << id << ").\n";
    errs() << "fence_instruction_ordering(" << id << ", " << (int) order << ").\n";
    errs() << "\n";
}

/*
 * 'cmpxchg' instruction
 * (http://llvm.org/docs/LangRef.html#cmpxchg-instruction)
 */
void translateCmpXchg(unsigned long id, AtomicCmpXchgInst *cmpxchg_inst) {
    // Get data for relations
    AtomicOrdering order = cmpxchg_inst->getSuccessOrdering();
    // TODO: should success ordering or failure ordering be used?
    Value *addr = cmpxchg_inst->getPointerOperand();
    Value *compare = cmpxchg_inst->getCompareOperand();
    Value *newval = cmpxchg_inst->getNewValOperand();

    // Generate facts
    errs() << "cmpxchg_instruction(" << id << ").\n";
    errs() << "cmpxchg_instruction_ordering(" << id << ", " << (int) order << ").\n";
    if (cmpxchg_inst->isVolatile()) {
        errs() << "cmpxchg_instruction_volatile(" << id << ").\n";
    }

    // TODO: Auxiliary rules in cmpxchg-instruction.logic:
    errs() << "cmpxchg_instruction_address(" << id << ", " << (unsigned long) addr << ").\n";
    errs() << "cmpxchg_instruction_cmp(" << id << ", " << (unsigned long) compare << ").\n";
    errs() << "cmpxchg_instruction_new(" << id << ", " << (unsigned long) newval << ").\n";
    errs() << "\n";
}

/*
 * 'atomicrmw' instruction
 * (http://llvm.org/docs/LangRef.html#atomicrmw-instruction)
 */
void translateAtomicRmw(unsigned long id, AtomicRMWInst *rmw_inst) {
    // Get data for relations
    AtomicOrdering order = rmw_inst->getOrdering();
    int op = rmw_inst->getOperation();
    Value *addr = rmw_inst->getPointerOperand();
    Value *val = rmw_inst->getValOperand();

    // Generate facts
    errs() << "atomicrmw_instruction(" << id << ").\n";
    errs() << "atomicrmw_instruction_ordering(" << id << ", " << (int) order << ").\n";
    if (rmw_inst->isVolatile()) {
        errs() << "atomicrmw_instruction_volatile(" << id << ").\n";
    }

    // TODO: Map operands
    errs() << "atomicrmw_instruction_operation(" << id << ", " << (unsigned long) op << ").\n";
    errs() << "atomicrmw_instruction_address(" << id << ", " << (unsigned long) addr << ").\n";
    errs() << "atomicrmw_instruction_value(" << id << ", " << (unsigned long) val << ").\n";
    errs() << "\n";
}
