#include "instruction.h"
#include "facts.h"

using namespace llvm;
using namespace std;

void translateCmp(unsigned long id, CmpInst *cmp_inst) {
    // Get data for relations
    const char *prefix = cmp_inst->getOpcodeName();
    CmpInst::Predicate condition = cmp_inst->getPredicate();
    Value *first = cmp_inst->getOperand(0);
    Value *second = cmp_inst->getOperand(1);

    bool icmp = strcmp("icmp", prefix) == 0;
    string INSTRUCTION = icmp ? ICMP : FCMP;
    string COND = icmp ? ICMP_COND : FCMP_COND;
    string FIRST_OPER = icmp ? ICMP_FIRST : FCMP_FIRST;
    string SECOND_OPER = icmp ? ICMP_SECOND : FCMP_SECOND;

    if (dyn_cast<Constant>(first)) {
        errs() << "constant(" << (unsigned long) first << ", " << *first << ").\n";
    }

    if (dyn_cast<Constant>(second)) {
        errs() << "constant(" << (unsigned long) second << ", " << *second << ").\n";
    }

    // Generate facts
    print_fact(INSTRUCTION, id);
    print_fact<unsigned long>(COND, id, (unsigned long) condition);
    print_fact<unsigned long>(FIRST_OPER, id, (unsigned long) first);
    print_fact<unsigned long>(SECOND_OPER, id, (unsigned long) second);
    print_new();
}

