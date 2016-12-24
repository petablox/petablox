#include "instruction.h"
#include "facts.h"

using namespace llvm;

void translateExtractValue(unsigned long id, ExtractValueInst *ev_inst) {
    print_fact(EXTRACTVALUE, id);

    Value *aggregate = ev_inst->getAggregateOperand();
    print_fact<unsigned long>(EXTRACTVALUE_BASE, id, (unsigned long) aggregate);

    unsigned num = ev_inst->getNumIndices();
    print_fact<unsigned>(EXTRACTVALUE_NINDICES, id, num);

    int index = 0;
    for (auto it = ev_inst->idx_begin(); it != ev_inst->idx_end(); ++it) {
        print_fact<unsigned long>(EXTRACTVALUE_INDEX, id, index, (unsigned long) *it);
        ++index;
    }
    print_new();
}

void translateInsertValue(unsigned long id, InsertValueInst *iv_inst) {
    print_fact(INSERTVALUE, id);

    Value *aggregate = iv_inst->getAggregateOperand();
    print_fact<unsigned long>(INSERTVALUE_BASE, id, (unsigned long) aggregate);

    Value *value = iv_inst->getInsertedValueOperand();
    print_fact<unsigned long>(INSERTVALUE_VALUE, id, (unsigned long) value);

    unsigned num = iv_inst->getNumIndices();
    print_fact<unsigned>(INSERTVALUE_NINDICES, id, num);

    int index = 0;
    for (auto it = iv_inst->idx_begin(); it != iv_inst->idx_end(); ++it) {
        print_fact<unsigned long>(INSERTVALUE_INDEX, id, index, (unsigned long) *it);
        ++index;
    }
    print_new();
}
