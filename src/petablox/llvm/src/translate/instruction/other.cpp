#include "instruction.h"

using namespace llvm;

void translateCmp(unsigned long id, CmpInst *cmp_inst) {
    // Get data for relations
    const char *prefix = cmp_inst->getOpcodeName();
    CmpInst::Predicate condition = cmp_inst->getPredicate();
    Value *first = cmp_inst->getOperand(0);
    Value *second = cmp_inst->getOperand(1);

    if (dyn_cast<Constant>(first)) {
        errs() << prefix << "constant(" << (unsigned long) first << ", " << *first << ").\n";
    }

    if (dyn_cast<Constant>(second)) {
        errs() << prefix << "constant(" << (unsigned long) second << ", " << *second << ").\n";
    }

    // Generate facts
    errs() << prefix << "_instruction(" << id << ").\n";
    errs() << prefix << "_instruction_condition(" << id << ", " << (unsigned long) condition << ").\n";
    errs() << prefix << "_instruction_first_operand(" << id << ", " << (unsigned long) first << ").\n";
    errs() << prefix << "_instruction_second_operand(" << id << ", " << (unsigned long) second << ").\n";
    errs() << "\n";
}

