#include "instruction.h"

using namespace llvm;

void translateConversion(unsigned long id, CastInst *conv_inst) {
    // Get data for relations
    const char *prefix = conv_inst->getOpcodeName();
    Value *from = conv_inst->getOperand(0);
    Type *from_type = conv_inst->getSrcTy();
    Type *to_type = conv_inst->getDestTy();
    Type::TypeID from_type_id = from_type->getTypeID();
    Type::TypeID to_type_id = to_type->getTypeID();

    // Generate facts

    if (dyn_cast<Constant>(from)) {
        errs() << prefix << "constant(" << (unsigned long) from << ", " << *from << ").\n";
    }

    errs() << prefix << "_instruction(" << id << ").\n";
    errs() << prefix << "_instruction_from(" << id << ", " << (unsigned long) from << ").\n";
    // TODO: do we need a rule for from_type like in cclyzer?
    errs() << prefix << "_instruction_from_type(" << id << ", " << from_type_id << ").\n";
    errs() << prefix << "_instruction_to_type(" << id << ", " << to_type_id << ").\n";
    errs() << "\n";
}
