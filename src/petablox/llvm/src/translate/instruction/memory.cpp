#include "instruction.h"
#include "facts.h"

using namespace llvm;

/*
 * 'alloca' instruction
 * (http://llvm.org/docs/LangRef.html#alloca-instruction)
 */
void translateAlloca(unsigned long id, AllocaInst *alloca_inst) {
    // Get data for relations
    unsigned alignment = alloca_inst->getAlignment();
    Value *size_val = alloca_inst->getArraySize();
    const APInt &size = dyn_cast<ConstantInt>(size_val)->getValue();
    Type *type = alloca_inst->getAllocatedType();

    // Generate facts
    print_fact(ALLOCA, id);
    print_fact<unsigned>(ALLOCA_ALIGN, id, alignment);
    print_fact<unsigned>(ALLOCA_SIZE, id, (unsigned) size.roundToDouble());
    print_fact<unsigned long>(ALLOCA_TYPE, id, type->getTypeID());
    print_new();
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
    print_fact(LOAD, id);
    print_fact<unsigned>(LOAD_ALIGN, id, alignment);
    print_fact<int>(LOAD_ORDER, id, order);
    if (load_inst->isVolatile()) {
        print_fact(LOAD_VOLATILE, id);
    }
    print_fact(LOAD_ADDR, id, (unsigned long) addr);
    print_new();
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
    print_fact(STORE, id);
    print_fact<unsigned>(STORE_ALIGN, id, alignment);
    print_fact<int>(STORE_ORDER, id, order);
    if (store_inst->isVolatile()) {
        print_fact(STORE_VOLATILE, id);
    }
    print_fact<unsigned long>(STORE_VALUE, id, (unsigned long) value);
    print_fact<unsigned long>(STORE_ADDR, id, (unsigned long) addr);
    print_new();
}

/*
 * 'fence' instruction
 * (http://llvm.org/docs/LangRef.html#fence-instruction)
 */
void translateFence(unsigned long id, FenceInst *fence_inst) {
    // Get data for relations
    AtomicOrdering order = fence_inst->getOrdering();

    // Generate facts
    print_fact(FENCE, id); 
    print_fact(FENCE_ORDER, id, order);  
    print_new();
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
    print_fact(CMPXCHG, id);
    print_fact<unsigned>(CMPXCHG_ORDER, id, order);
    if (cmpxchg_inst->isVolatile()) {
        print_fact(CMPXCHG_VOLATILE, id);
    }

    // TODO: Auxiliary rules in cmpxchg-instruction.logic:
    print_fact<unsigned long>(CMPXCHG_ADDR, id, (unsigned long) addr);
    print_fact<unsigned long>(CMPXCHG_CMP, id, (unsigned long) compare); 
    print_fact<unsigned long>(CMPXCHG_NEW, id, (unsigned long) newval); 
    print_new();
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
    print_fact(ATOMICRMW, id);
    print_fact<unsigned>(ATOMICRMW_ORDER, id, order); 
    if (rmw_inst->isVolatile()) {
        print_fact(ATOMICRMW_VOLATILE, id);
    }

    // TODO: Map operands
    print_fact<unsigned>(ATOMICRMW_OP, id, op); 
    print_fact<unsigned long>(ATOMICRMW_ADDR, id, (unsigned long) addr); 
    print_fact<unsigned long>(ATOMICRMW_VALUE, id, (unsigned long) val); 
    print_new();
}
