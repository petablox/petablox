#include "translate/facts.h"
#include "translate/ordering.h"
#include "translate/type.h"

using namespace std;
using namespace llvm;

/*
 * translateAlloca
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the allocation allignment
 * (3) the allocation size
 * (4) the allocation type
 *
 * 'alloca' instruction
 * (http://llvm.org/docs/LangRef.html#alloca-instruction)
 */
void translateAlloca(unsigned long id, AllocaInst *alloca_inst) {
    // Declare the instruction
    print_fact(ALLOCA, id);
    
    // Alignment
    unsigned alignment = alloca_inst->getAlignment();
    print_fact("integer", alignment);
    print_fact(ALLOCA_ALIGN, id, alignment);

    // Size
    Value *size = alloca_inst->getArraySize();
    print_fact(ALLOCA_SIZE, id, (unsigned long) size);

    // Type
    //string type = processType(alloca_inst->getAllocatedType());
    unsigned long type = processType(alloca_inst->getAllocatedType());
    print_fact(ALLOCA_TYPE, id, type);

    print_new();
}

/*
 * translateLoad
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction alignment
 * (3) the instruction ordering
 * (4) if the instruction is volatile
 * (5) the address to load from
 *
 * 'load' instruction
 * (http://llvm.org/docs/LangRef.html#load-instruction)
 */
void translateLoad(unsigned long id, LoadInst *load_inst) {
    // Declare the instruction
    print_fact(LOAD, id);
    
    // Alignment
    unsigned alignment = load_inst->getAlignment();
    print_fact("integer", alignment);
    print_fact(LOAD_ALIGN, id, alignment);

    // Ordering
    AtomicOrdering order = load_inst->getOrdering();
    print_fact(LOAD_ORDER, id, processOrder(order));

    // Is the instruction volatile?
    if (load_inst->isVolatile()) {
        print_fact(LOAD_VOLATILE, id);
    }

    // Address
    Value *addr = load_inst->getPointerOperand();
    print_fact(LOAD_ADDR, id, (unsigned long) addr);

    print_new();
}

/*
 * translateStore
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction alignment
 * (3) the instruction ordering
 * (4) the value being stored
 * (5) the address to store the value at
 * (6) if the store is volatile
 *
 * 'store' instruction
 * (http://llvm.org/docs/LangRef.html#store-instruction)
 */
void translateStore(unsigned long id, StoreInst *store_inst) {
    // Declare the instruction
    print_fact(STORE, id);

    // Alignment
    unsigned alignment = store_inst->getAlignment();
    print_fact("integer", alignment);
    print_fact(STORE_ALIGN, id, alignment);

    // Ordering
    AtomicOrdering order = store_inst->getOrdering();
    print_fact(STORE_ORDER, id, processOrder(order));

    // Value
    Value *value = store_inst->getValueOperand();
    print_fact(STORE_VALUE, id, (unsigned long) value);

    // Address
    Value *addr = store_inst->getPointerOperand();
    print_fact(STORE_ADDR, id, (unsigned long) addr);

    // Is the instruction volatile?
    if (store_inst->isVolatile()) {
        print_fact(STORE_VOLATILE, id);
    }

    print_new();
}

/*
 * translateFence
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction ordering
 *
 * 'fence' instruction
 * (http://llvm.org/docs/LangRef.html#fence-instruction)
 */
void translateFence(unsigned long id, FenceInst *fence_inst) {
    // Declare the instruction
    print_fact(FENCE, id); 

    // Ordering
    AtomicOrdering order = fence_inst->getOrdering();
    print_fact(FENCE_ORDER, id, processOrder(order));  

    print_new();
}

/*
 * translateCmpXchg
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction ordering
 * (3) the address being analyzed
 * (4) the comparison operand
 * (5) the new value to store at the address
 * (6) if the instruction is volatile
 *
 * 'cmpxchg' instruction
 * (http://llvm.org/docs/LangRef.html#cmpxchg-instruction)
 */
void translateCmpXchg(unsigned long id, AtomicCmpXchgInst *cmpxchg_inst) {
    // Declare the instruction
    print_fact(CMPXCHG, id);

    // Ordering
    AtomicOrdering succ_order = cmpxchg_inst->getSuccessOrdering();
    print_fact(CMPXCHG_SUCCESS_ORDER, id, processOrder(succ_order));

    AtomicOrdering fail_order = cmpxchg_inst->getFailureOrdering();
    print_fact(CMPXCHG_FAIL_ORDER, id, processOrder(fail_order));
 
    // Address
    Value *addr = cmpxchg_inst->getPointerOperand();
    print_fact(CMPXCHG_ADDR, id, (unsigned long) addr);

    // Comparison operand
    Value *compare = cmpxchg_inst->getCompareOperand();
    print_fact(CMPXCHG_CMP, id, (unsigned long) compare); 

    // New value
    Value *newval = cmpxchg_inst->getNewValOperand();
    print_fact(CMPXCHG_NEW, id, (unsigned long) newval); 

    // Is the instruction volatile?
    if (cmpxchg_inst->isVolatile()) {
        print_fact(CMPXCHG_VOLATILE, id);
    }

    print_new();
}

/*
 * translateAtomicRmw
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction ordering
 * (3) the address to be modified
 * (4) the operation to execute
 * (5) the operand
 * (6) if the instruction is volatile
 *
 * 'atomicrmw' instruction
 * (http://llvm.org/docs/LangRef.html#atomicrmw-instruction)
 */
void translateAtomicRmw(unsigned long id, AtomicRMWInst *rmw_inst) {
    // Declare the instruction
    print_fact(ATOMICRMW, id);

    // Ordering
    AtomicOrdering order = rmw_inst->getOrdering();
    print_fact(ATOMICRMW_ORDER, id, processOrder(order)); 

    // Address
    Value *addr = rmw_inst->getPointerOperand();
    print_fact(ATOMICRMW_ADDR, id, (unsigned long) addr); 

    // Operation

    // Operations map
    int op = rmw_inst->getOperation();
    const string opcodes[] = {"xchg", "add", "sub", "and", "nand", "or", "xor", "max", "min", "umax", "umin"};
    string opcode = opcodes[op];
    print_fact(ATOMICRMW_OP, id, opcode); 

    // Value
    Value *val = rmw_inst->getValOperand();
    print_fact(ATOMICRMW_VALUE, id, (unsigned long) val); 

    // Is the instruction volatile? 
    if (rmw_inst->isVolatile()) {
        print_fact(ATOMICRMW_VOLATILE, id);
    }

    print_new();
}

/*
 * translateGetElementPtr
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the base of the structure
 * (3) if the `inbounds` keyword is used
 * (4) the number of indices
 * (5) the index to "get" the element from
 *
 * 'getelementptr' instruction
 * (http://llvm.org/docs/LangRef.html#getelementptr-instruction)
 */
void translateGetElementPtr(unsigned long id, GetElementPtrInst *gep_inst) {
    // Declare the instruction
    print_fact(GEP, id);

    // Base address
    Value *pointer = gep_inst->getPointerOperand();
    print_fact(GEP_BASE, id, (unsigned long) pointer);

    // Is the instruction in bounds?
    if (gep_inst->isInBounds()) {
        print_fact(GEP_INBOUNDS, id);
    }

    // Number of indices
    unsigned num = gep_inst->getNumIndices();
    print_fact(GEP_NINDICES, id, num);

    // Iterate through each index
    int index = 0;
    for (auto it = gep_inst->idx_begin(); it != gep_inst->idx_end(); ++it) {
        Value *idx = *it;
        print_fact(GEP_INDEX, id, index, (unsigned long) idx);
        ++index;
    }

    print_new();

}
