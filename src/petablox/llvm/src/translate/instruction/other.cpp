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

void translateSelect(unsigned long id, SelectInst *select_inst) {
    // Get data for relations
    Value *cond = select_inst->getCondition();
    Value *true_val = select_inst->getTrueValue();
    Value *false_val = select_inst->getFalseValue();

    if (dyn_cast<Constant>(cond)) {
        errs() << "constant(" << (unsigned long) cond << ", " << *cond << ").\n";
    }

    if (dyn_cast<Constant>(true_val)) {
        errs() << "constant(" << (unsigned long) true_val << ", " << *true_val << ").\n";
    }

    if (dyn_cast<Constant>(false_val)) {
        errs() << "constant(" << (unsigned long) false_val << ", " << *false_val << ").\n";
    }

    // Generate facts
    print_fact(SELECT, id);
    print_fact<unsigned long>(SELECT_COND, id, (unsigned long) cond);
    print_fact<unsigned long>(SELECT_TRUE, id, (unsigned long) true_val);
    print_fact<unsigned long>(SELECT_FALSE, id, (unsigned long) false_val);
    print_new();
}

void translateVAArg(unsigned long id, VAArgInst *va_arg) {
    Type::TypeID type = va_arg->getType()->getTypeID();

    print_fact(VAARG, id);
    print_fact<unsigned>(VAARG_TYPE, id, type);
    // TODO: list of arguments
}

void translateLandingpad(unsigned long id, LandingPadInst *lp_inst) {

    print_fact(LANDINGPAD, id);
    
    if (lp_inst->isCleanup()) {
        print_fact(LANDINGPAD_CLEANUP, id);
    }
    // TODO: Personality function

    Type::TypeID type = lp_inst->getType()->getTypeID();
    print_fact<unsigned>(LANDINGPAD_TYPE, id, type);

    unsigned nclauses = lp_inst->getNumClauses();
    print_fact<unsigned>(LANDINGPAD_NCLAUSES, id, nclauses);

    for (unsigned int index = 0; index < nclauses; index++) {
        Constant *clause = lp_inst->getClause(index);
        print_fact(CLAUSE, (unsigned long) clause);
        print_fact<unsigned long>(CLAUSE_BY_INDEX, id, index, (unsigned long) clause);
        print_fact<unsigned long>(LANDINGPAD_CLAUSE, id, index, (unsigned long) clause);
        if (lp_inst->isCatch(index)) {
            print_fact(CATCH_CLAUSE, (unsigned long) clause);
            // TODO: catch clause arg
        }

        if (lp_inst->isFilter(index)) {
            print_fact(FILTER_CLAUSE, (unsigned long) clause);
            // TODO: filter clause arg
        }
    }
}
