#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"

using namespace std;
using namespace llvm;

const string attributes[] = { 
    "bogus", "align", "alwaysinline", "argmemonly", "builtin", "byval", "cold", "convergent", 
    "dereferenceable", "dereferenceable_or_null", "inalloca", "inreg", "inaccessiblememonly", 
    "inaccessiblemem_or_argmemonly", "inlinehint", "jumptable", "minsize", "naked", "nest", 
    "noalias", "nobuiltin", "nocapture", "noduplicate", "noimplicitfloat", "noinline", 
    "norecurse", "noredzone", "noreturn", "nounwind", "nonlazybind", "nonnull", "optnone", 
    "optsize", "readnone", "readonly", "returned", "returns_twice", "signext", "safestack", 
    "sanitize_address", "sanitize_memory", "sanitize_thread", "alignstack", "ssp", "sspreq", 
    "sspstrong", "sret", "uwtable", "zeroext" 
};

inline string processAttr(const Attribute *attr) {
    Attribute::AttrKind kind = attr->getKindAsEnum();
    return attributes[kind];
}
