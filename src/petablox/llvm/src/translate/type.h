#include <sstream>

using namespace llvm;
using namespace std;

const string ERROR = "Invalid type";

inline string processInteger(Value *value) {
    if (ConstantInt *constant = dyn_cast<ConstantInt>(value)) {
        unsigned bits = constant->getBitWidth();
        ostringstream width;
        width << bits;
        return "i" + width.str();
    }
    else {
        return ERROR;
    }
}

inline string processType(int type, Value *value) {

    switch (type) {
        case 11:
            return processInteger(value);
        default:
            return ERROR;
    }

}

inline string processType(Type *type) {

    if (type->isIntegerTy()) {
        unsigned bits = type->getIntegerBitWidth();
        ostringstream width;
        width << bits;
        return "i" + width.str();
    }
    else {
        return ERROR;
    }
}
