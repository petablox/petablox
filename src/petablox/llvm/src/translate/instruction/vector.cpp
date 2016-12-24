#include "instruction.h"
#include "facts.h"

using namespace llvm;

void translateExtractElement(unsigned long id, ExtractElementInst *ee_inst) {
    print_fact(EXTRACTELEMENT, id);

    Value *vector = ee_inst->getVectorOperand();
    print_fact<unsigned long>(EXTRACTELEMENT_BASE, id, (unsigned long) vector);

    Value *index = ee_inst->getIndexOperand();
    print_fact<unsigned long>(EXTRACTELEMENT_INDEX, id, (unsigned long) index);
    print_new();
}

void translateInsertElement(unsigned long id, InsertElementInst *ie_inst) {
    print_fact(INSERTELEMENT, id);

    Value *vector = ie_inst->getOperand(0);
    Value *index = ie_inst->getOperand(2);
    Value *value = ie_inst->getOperand(1);

    print_fact<unsigned long>(INSERTELEMENT_BASE, id, (unsigned long) vector);
    print_fact(INSERTELEMENT_INDEX, id, index);
    print_fact(INSERTELEMENT_VALUE, id, (unsigned long) value);
    print_new();
}

void translateShuffleVector(unsigned long id, ShuffleVectorInst *sv_inst) {
    print_fact(SHUFFLEVECTOR, id);

    Value *first = sv_inst->getOperand(0);
    Value *second = sv_inst->getOperand(1);
    print_fact<unsigned long>(SHUFFLEVECTOR_FIRST, id, (unsigned long) first);
    print_fact<unsigned long>(SHUFFLEVECTOR_SECOND, id, (unsigned long) second);

    Constant *mask = sv_inst->getMask();
    print_fact<unsigned long>(SHUFFLEVECTOR_MASK, id, (unsigned long) mask);

    print_new();
}
