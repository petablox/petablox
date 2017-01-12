#include "instruction.h"
#include "translate/facts.h"

using namespace std;
using namespace llvm;

/*
 * translateExtractElement
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the base address of the vector
 * (3) the index to extract at
 *
 */
void translateExtractElement(unsigned long id, ExtractElementInst *ee_inst) {
    // Declare the instruction
    print_fact(EXTRACTELEMENT, id);

    // Vector base
    Value *vector = ee_inst->getVectorOperand();
    print_fact(EXTRACTELEMENT_BASE, id, (unsigned long) vector);

    // Index
    Value *index = ee_inst->getIndexOperand();
    print_fact(EXTRACTELEMENT_INDEX, id, (unsigned long) index);

    print_new();
}

/*
 * translateInsertElement
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the base address of the vector
 * (3) the index to insert at
 * (4) the value to insert
 *
 */
void translateInsertElement(unsigned long id, InsertElementInst *ie_inst) {
    // Declare the instructions
    print_fact(INSERTELEMENT, id);

    // Vector base
    Value *vector = ie_inst->getOperand(0);
    print_fact(INSERTELEMENT_BASE, id, (unsigned long) vector);

    // Index
    Value *index = ie_inst->getOperand(2);
    print_fact(INSERTELEMENT_INDEX, id, (unsigned long) index);

    // Value
    Value *value = ie_inst->getOperand(1);
    print_fact(INSERTELEMENT_VALUE, id, (unsigned long) value);

    print_new();
}

/*
 * translateShuffleVector
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the first vector
 * (3) the second vector
 * (4) the mask
 *
 */
void translateShuffleVector(unsigned long id, ShuffleVectorInst *sv_inst) {
    // Declare the instruction
    print_fact(SHUFFLEVECTOR, id);

    // First vector
    Value *first = sv_inst->getOperand(0);
    print_fact(SHUFFLEVECTOR_FIRST, id, (unsigned long) first);

    // Second vector
    Value *second = sv_inst->getOperand(1);
    print_fact(SHUFFLEVECTOR_SECOND, id, (unsigned long) second);

    // Mask
    Constant *mask = sv_inst->getMask();
    print_fact(SHUFFLEVECTOR_MASK, id, (unsigned long) mask);

    print_new();
}
