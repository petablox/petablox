#include <sstream>
#include <stdio.h>
#include <string.h>

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

inline string processType(Type *type) {
    errs() << "=============================\n";

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
    else {
        return ERROR;
    }
    errs() << "=============================\n";
}
