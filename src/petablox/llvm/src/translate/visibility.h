#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include <sstream>

using namespace std;
using namespace llvm;

const string visibilities[] = { 
    "default", "hidden", "protected"
};

inline string processVis(GlobalValue::VisibilityTypes id) {
    ostringstream val;
    val << "V" << id;
    print_fact("visibility", val.str());
    return val.str();
    //return visibilities[id];
}
