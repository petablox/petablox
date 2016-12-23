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

void translatePhi(unsigned long id, PHINode *phi) {
    print_fact(PHI, id); 
    Type::TypeID type = phi->getType()->getTypeID();
    int num = 0;

    print_fact<unsigned long>(PHI_TYPE, id, (unsigned long) type);

    for (auto it = phi->block_begin(); it != phi->block_end(); it++) {
        BasicBlock *bb = *it;
        int index = phi->getBasicBlockIndex(bb);
        Value *val = phi->getIncomingValue(index);

        if (dyn_cast<Instruction>(val)) {
            print_fact<unsigned long>(PHI_PAIR_VAL, id, index, (unsigned long) val);
        }
        else {
            print_fact(PHI_PAIR_VAL, id, index, val);
        }
        print_fact<unsigned long>(PHI_PAIR_LABEL, id, index, (unsigned long) bb);
        ++num;
    }
    print_fact(PHI_NPAIRS, id, num);
    print_new();
}
