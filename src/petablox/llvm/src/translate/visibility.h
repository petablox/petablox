#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"

using namespace std;
using namespace llvm;

const string visibilities[] = { 
    "default", "hidden", "protected"
};

inline string processVis(GlobalValue::VisibilityTypes id) {
    return visibilities[id];
}
