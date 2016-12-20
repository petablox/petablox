#include "instruction.h"

using namespace llvm;

void translateBinOp(std::string prefix, Instruction &I, unsigned long id) {
    // Get data for relations
    Value *first = I.getOperand(0);
    Value *second = I.getOperand(1);

    if (dyn_cast<Constant>(first)) {
        errs() << prefix << "constant(" << (unsigned long) first << ", " << *first << ").\n";
    }

    if (dyn_cast<Constant>(second)) {
        errs() << prefix << "constant(" << (unsigned long) second << ", " << *second << ").\n";
    }
    
    // Generate facts
    errs() << prefix << "_instruction(" << id << ").\n";
    errs() << prefix << "_instruction_first_operand(" << id << ", " << (unsigned long) first << ").\n";
    errs() << prefix << "_instruction_second_operand(" << id << ", " << (unsigned long) second << ").\n";
    errs() << "\n";
}
