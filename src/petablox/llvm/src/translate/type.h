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
    ostringstream addr;
    addr << (unsigned long) type;
    return addr.str();
    /*
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
    }*/
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

inline string processPointer(Type *type) {
    if (PointerType *ptr = dyn_cast<PointerType>(type)) {
        // Declare the type
        unsigned long id = (unsigned long) ptr;
        print_fact(PTR_TY, id);

        // Component type
        string comp = processType(ptr->getElementType());
        print_fact(PTR_TY_COMP, id, comp);

        // Address space
        unsigned addr_space = ptr->getAddressSpace();
        print_fact(PTR_TY_ADDR_SPACE, id, addr_space);

        ostringstream addr;
        addr << (unsigned long) ptr;
        return addr.str();
    }
    else {
        return ERROR;
    }
}

inline string processVector(Type *type) {
    if (VectorType *vec = dyn_cast<VectorType>(type)) {
         unsigned long id = (unsigned long) vec;
        // Declare the type
        print_fact(VECTOR_TY, id);

        // Component type
        string comp = processType(vec->getElementType());
        print_fact(VECTOR_TY_COMP, id, comp);

        // Size
        uint64_t size = vec->getNumElements();
        print_fact(VECTOR_TY_SIZE, id, size);

        ostringstream addr;
        addr << id;
        return addr.str();
    }
    else {
        return ERROR;
    }
}

inline string processArray(Type *type) {
    if (ArrayType *array = dyn_cast<ArrayType>(type)) {
        unsigned long id = (unsigned long) array;
        // Declare the type
        print_fact(ARRAY_TY, id);

        // Component type
        string comp = processType(array->getElementType());
        print_fact(ARRAY_TY_COMP, id, comp);

        // Size
        uint64_t size = array->getNumElements();
        print_fact(ARRAY_TY_SIZE, id, size);

        ostringstream addr;
        addr << id;
        return addr.str();
    }
    else {
        return ERROR;
    }
}

inline string processStruct(Type *type) {
    if (StructType *struct_type = dyn_cast<StructType>(type)) {
        unsigned long id = (unsigned long) struct_type;
        // Declare the type
        print_fact(STRUCT_TY, id);

        // Struct name
        if (struct_type->hasName()) {
            string name = struct_type->getName().str();
            print_fact(STRUCT_TY_NAME, id, name);
        }

        // Is this an opaque struct?
        if (struct_type->isOpaque()) {
            print_fact(OPAQUE_STRUCT, id);
        }

        // Number of elements
        unsigned num_elements = struct_type->getNumElements();
        print_fact(STRUCT_TY_NFIELDS, id, num_elements);

        // For each element in the struct
        for (unsigned index = 0; index < num_elements; index++) {
            // Field type
            string field = processType(struct_type->getElementType(index));
            print_fact(STRUCT_TY_FIELD, id, index, field);

            // TODO: byte and bit offset per field
        }

        ostringstream addr;
        addr << (unsigned long) type;
        return addr.str();
    }
    else {
        return ERROR;
    }

}

inline string processType(Type *type) {
    // Primitive types (integer, fp, void, label, x86mmk)

    print_fact("type", (unsigned long) type);
    if (type->isIntegerTy()) {
        unsigned bits = type->getIntegerBitWidth();
        ostringstream width;
        width << bits;
        //return "i" + width.str();
        ostringstream addr;
        addr << (unsigned long) type;
        return addr.str();
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
    
    // Derived types (function, pointer, vector, array, struct)
    else if (type->isFunctionTy()) {
        print_fact("type", (unsigned long) type);
        return processFunction(type);
    }
    else if (type->isPointerTy()) {
        print_fact("type", (unsigned long) type);
        return processPointer(type);
    } 
    else if (type->isVectorTy()) {
        print_fact("type", (unsigned long) type);
        return processVector(type);
    }
    else if (type->isArrayTy()) {
        print_fact("type", (unsigned long) type);
        return processArray(type);
    }
    else if (type->isStructTy()) {
        print_fact("type", (unsigned long) type);
        return processStruct(type);
    }

    // Otherwise, something went wrong
    else {
        return ERROR;
    }
}
