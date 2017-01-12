#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"

using namespace std;
using namespace llvm;
using namespace CallingConv;

inline string processCallConv(ID id) {
    // NOTE: This is easily extensible for other calling conventions
    switch (id) {
        case C:
            return "ccc";
        case Fast:
            return "fastcc";
        case Cold:
            return "coldcc";
        case GHC:
            return "cc 10";
        case HiPE:
            return "cc 11";
        case WebKit_JS:
            return "webkit_jscc";
        case AnyReg:
            return "anyregcc";
        case PreserveMost:
            return "preserve_mostcc";
        case PreserveAll:
            return "preserve_allcc";
        case Swift:
            return "swiftcc";
        case CXX_FAST_TLS:
            return "cxx_fast_tlscc";
        default:
            return "othercc";
    }
}
