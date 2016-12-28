#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "facts.h"

using namespace llvm;

void translateOperand(Value *operand) {
    unsigned long id = (unsigned long) operand;
    print_fact(OPERAND, id);

    Type::TypeID type = operand->getType()->getTypeID();

    if (Constant *constant = dyn_cast<Constant>(operand)) {
        print_fact(CONSTANT, id);
        print_fact(CONSTANT_TYPE, id, (unsigned long) type);

        if (ConstantInt *value = dyn_cast<ConstantInt>(constant)) {
            const APInt &val = value->getValue();
            print_fact(CONSTANT_VAL, id, val);
        }

        if (ConstantFP *value = dyn_cast<ConstantFP>(constant)) {
            const APFloat &val = value->getValueAPF();
            print_fact(CONSTANT_VAL, id, val.convertToFloat());
        }

    }
    else {
        print_fact(VARIABLE, id);
        print_fact(VARIABLE_TYPE, id, (unsigned long) type);
    }
}
