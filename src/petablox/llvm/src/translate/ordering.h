#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include <sstream>
#include "translate/facts.h"

using namespace std;
using namespace llvm;

const string orderings[] = {
    "notatomic", "unordered", "monotonic", "consume", "acquire", "release", "acq_rel", "seq_cst"
};

inline string processOrder(AtomicOrdering id) {
    ostringstream val;
    val << "R" << id;
    print_fact("ordering", val.str());
    return val.str();
    //return orderings[id];
}
