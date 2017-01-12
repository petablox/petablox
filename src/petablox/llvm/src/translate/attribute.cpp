#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "translate/facts.h"
#include "type.h"

using namespace std;
using namespace llvm;

/*
 * translateOperand
 *
 * Extracts the following relations from an operand:
 * (1) declares the id as an operand
 * (2) declares the operand as either a constant or a 
 *     variable
 * (3) declares the type of the operand
 * (4) if the operand is a constant, declares the value
 */
string processAttr(const Attribute *attr) {
    string attributes[] = {"bogus", "align", "alwaysinline", "argmemonly", "builtin", "byval", "cold", "convergent", "dereferenceable", "dereferenceable_or_null", 
                           "inaccessiblememonly", "inaccessiblemem_or_argmemonly", "inalloca", "inlinehint", "inreg", "jumptable", "minsize", "naked", 
                           "nest", "noalias", "nobuiltin", "nocapture", "noduplicate", "noimplicitfloat", "noinline", "nonlazybind", "nonnull", "norecurse", 
                           "noredzone", "noreturn", "nounwind", "optsize", "optnone", "readnone", "readonly", "returned", "returns_twice", "safestack", 
                           "signext", "alignstack", "ssp", "sspreq", "sspstrong", "sret", "sanitize_address", "sanitize_thread", "sanitize_memory", "uwtable", 
                           "zeroext", "less-precise-fpmad", "no-infs-fp-math", "no-nans-fp-math", "unsafe-fp-math" };

    Attribute::AttrKind kind = attr->getKindAsEnum();
    return attributes[kind];
}
