#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"

using namespace std;
using namespace llvm;

const string orderings[] = {
    "notatomic", "unordered", "monotonic", "consume", "acquire", "release", "acq_rel", "seq_cst"
};

inline string processOrder(AtomicOrdering id) {
    return orderings[id];
}
