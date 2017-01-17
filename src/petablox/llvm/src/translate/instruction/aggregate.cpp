#include "translate/facts.h"

using namespace std;
using namespace llvm;

/*
 * translateExtractValue
 *
 * Creates the following facts for extracting a value 
 * from an aggregate data type:
 * (1) the aggregate structures base address
 * (2) the number of indices in the structure
 * (3) the index that the value is being extracted from
 */
void translateExtractValue(unsigned long id, ExtractValueInst *ev_inst) {
    print_fact(EXTRACTVALUE, id);

    // Get the aggregate structure
    Value *aggregate = ev_inst->getAggregateOperand();
    print_fact<unsigned long>(EXTRACTVALUE_BASE, id, (unsigned long) aggregate);

    // Get the number of indicies
    unsigned num = ev_inst->getNumIndices();
    print_fact<unsigned>(EXTRACTVALUE_NINDICES, id, num);

    // Iterate through all of the indices
    int index = 0;
    for (auto it = ev_inst->idx_begin(); it != ev_inst->idx_end(); ++it) {
        print_fact<unsigned long>(EXTRACTVALUE_INDEX, id, index, (unsigned long) *it);
        ++index;
    }

    print_new();
}

/* translateInsertValue
 *
 * Creates the following facts for inserting a value
 * into an aggregate data type:
 * (1) the base address of the aggregate structure
 * (2) the value being inserted
 * (3) the number of indices
 * (4) the index in which to insert the value
 */
void translateInsertValue(unsigned long id, InsertValueInst *iv_inst) {
    print_fact(INSERTVALUE, id);

    // Get the aggregate structure
    Value *aggregate = iv_inst->getAggregateOperand();
    print_fact<unsigned long>(INSERTVALUE_BASE, id, (unsigned long) aggregate);

    // Get the value being inserted
    Value *value = iv_inst->getInsertedValueOperand();
    print_fact<unsigned long>(INSERTVALUE_VALUE, id, (unsigned long) value);

    // Get the number of indices
    unsigned num = iv_inst->getNumIndices();
    print_fact<unsigned>(INSERTVALUE_NINDICES, id, num);

    // Iterate through all of the indices
    int index = 0;
    for (auto it = iv_inst->idx_begin(); it != iv_inst->idx_end(); ++it) {
        print_fact<unsigned long>(INSERTVALUE_INDEX, id, index, (unsigned long) *it);
        ++index;
    }

    print_new();
}
