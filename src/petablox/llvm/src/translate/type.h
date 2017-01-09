#include <sstream>
#include <stdio.h>
#include <string.h>
#include "translate/facts.h"

using namespace llvm;
using namespace std;

// Floating point types
const string FLOAT = "float";
const string HALF = "half";
const string DOUBLE = "double";
const string FP128 = "fp128";
const string X86_FP80 = "x86_fp80";
const string PPC_FP128 = "ppc_fp128";

// "Other" primitives
const string VOID = "void";
const string LABEL = "label";
const string X86_MMX = "x86_mmx";

const string ERROR = "Invalid type";

string processType(Type *type);

inline string processInteger(Type *type) {
    unsigned bits = type->getIntegerBitWidth();
    ostringstream width;
    width << bits;
    return "i" + width.str();
}

inline string processFP(Type *type) {
    if (type->isFloatTy()) {
        return FLOAT;
    }
    else if (type->isHalfTy()) {
        return HALF;
    }
    else if (type->isDoubleTy()) {
        return DOUBLE;
    }
    else if (type->isFP128Ty()) {
        return FP128;
    }
    else if (type->isX86_FP80Ty()) {
        return X86_FP80;
    }
    else if (type->isPPC_FP128Ty()) {
        return PPC_FP128;
    }
    else {
        return ERROR;
    }
}

inline string processFunction(Type *type) {
    if (FunctionType *func = dyn_cast<FunctionType>(type)) {
        // Declare the type
        print_fact(FUNCTION_TY, (unsigned long) func);

        // Does this function accept variable arguments?
        if (func->isVarArg()) {
            print_fact(FUNCTION_TY_VARARGS, (unsigned long) func);
        }

        // Return type
        string ret_type = processType(func->getReturnType());
        print_fact(FUNCTION_TY_RET, (unsigned long) func, ret_type);

        // Number of parameters
        unsigned num_params = func->getNumParams();
        print_fact(FUNCTION_TY_NPARAMS, (unsigned long) func, num_params);

        // Declare param types
        for(unsigned i = 0; i < num_params; i++) {
            string param = processType(func->getParamType(i));
            print_fact(FUNCTION_TY_PARAM, (unsigned long) func, i, param);
        }

        ostringstream addr;
        addr << (unsigned long) func;
        return addr.str();
    }
    else {
        return ERROR;
    }
}

inline string processType(Type *type) {
    // Primitive types (integer, fp, void, label, x86mmk)
    if (type->isIntegerTy()) {
        unsigned bits = type->getIntegerBitWidth();
        ostringstream width;
        width << bits;
        return "i" + width.str();
    }
    else if (type->isFloatingPointTy()) {
        return processFP(type);
    }
    else if (type->isVoidTy()) {
        return VOID;
    }
    else if (type->isLabelTy()) {
        return LABEL;
    }
    else if (type->isX86_MMXTy()) {
        return X86_MMX;
    }
    
    // Derived types (function, pointer, vector (?), 
    // aggregate, array, struct, opaque struct)
    else if (type->isFunctionTy()) {
        return processFunction(type);
    }
    else {
        errs() << "=============================\n";
        errs() << *type;
        errs() << "=============================\n";
        return ERROR;
    }
}
