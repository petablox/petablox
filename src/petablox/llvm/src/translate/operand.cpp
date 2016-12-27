#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "facts.h"

using namespace llvm;

void translateOperand(Value *operand) {
    print_fact(OPERAND, (unsigned long) operand);

    Type::TypeID type = operand->getType()->getTypeID();

    if (Constant *constant = dyn_cast<Constant>(operand)) {
        print_fact(CONSTANT, (unsigned long) operand);
        print_fact(CONSTANT_TYPE, (unsigned long) type);

        if (ConstantInt *value = dyn_cast<ConstantInt>(constant)) {
            const APInt &val = value->getValue();
            print_fact(CONSTANT_VAL, val);
        }

        if (ConstantFP *value = dyn_cast<ConstantFP>(constant)) {
            const APFloat &val = value->getValueAPF();
            print_fact(CONSTANT_VAL, val.convertToFloat());
        }

    }
    else {
        print_fact(VARIABLE, (unsigned long) operand);
        print_fact(VARIABLE_TYPE, (unsigned long) type);
    }
}
