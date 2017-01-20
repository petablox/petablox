#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"

using namespace std;
using namespace llvm;

const string linkages[] = { 
    "external", "available_externally", "linknonce", "linkonce_odr",
    "weak", "weak_odr", "appending", "internal", "private", "extern_weak", "common"
};

inline string processLinkage(GlobalValue::LinkageTypes id) {
    string linkage = linkages[id];
    print_fact("linkage", linkage);
    return linkage;
}
