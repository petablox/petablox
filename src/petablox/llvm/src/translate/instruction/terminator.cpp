#include "instruction.h"
#include "translate/facts.h"

using namespace llvm;

/*
 * translateReturn
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) if the instruction returns a value
 * (3) if the return value is void
 *
 * 'ret' instructions
 * (http://llvm.org/docs/LangRef.html#ret-instruction)
 */
void translateReturn(unsigned long id, ReturnInst *ret_inst) {
    // Declare the instruction
    print_fact(RETURN, id);

    // Is there a return value?
    if (Value *ret_value = ret_inst->getReturnValue()) {
        print_fact<unsigned long>(RETURN_VALUE, id, (unsigned long) ret_value);
    }
    // Otherwise the return is void
    else {
        print_fact(RETURN_VOID, id);
    }

    print_new();
}

/*
 * translateBr
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) if the branch is conditional
 * (3) if the branch is unconditional
 * If conditional:
 * (4) the condition
 * (5) the true label
 * (6) the false label
 * If unconditional:
 * (7) the destination
 *
 * 'br' instructions
 * (http://llvm.org/docs/LangRef.html#br-instruction)
 */
void translateBr(unsigned long id, BranchInst *br_inst) {
    // Declare the instruction
    print_fact(BRANCH, id);

    // If the branch is conditional
    if (br_inst->isConditional()) {
        print_fact(COND_BR, id);

        // Condition
        Value *condition = br_inst->getCondition();
        print_fact(COND_BR_CONDITION, id, (unsigned long) condition);

        // If true
        BasicBlock *true_bb = br_inst->getSuccessor(0);
        print_fact(COND_BR_TRUE, id, (unsigned long) true_bb);

        // If false
        BasicBlock *false_bb = br_inst->getSuccessor(1);
        print_fact(COND_BR_FALSE, id, (unsigned long) false_bb);
    }
    // If the branch is unconditional
    else {
        print_fact(UNCOND_BR, id);

        // Destination
        BasicBlock *next_bb = br_inst->getSuccessor(0);
        print_fact(UNCOND_BR_DEST, id, (unsigned long) next_bb);
    }

    print_new();
}

/*
 * translateSwitch
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the condition
 * (3) the default label
 * (4) number of cases
 * For each case:
 * (5) the value
 * (6) the label
 *
 */
void translateSwitch(unsigned long id, SwitchInst *switch_inst) {
    // Declare the isntruction
    print_fact(SWITCH, id);

    // Operand
    Value *cond = switch_inst->getCondition();
    print_fact(SWITCH_COND, id, (unsigned long) cond);

    // Default label
    BasicBlock *default_label = switch_inst->getDefaultDest();
    print_fact(SWITCH_DEFAULT, id, (unsigned long) default_label);

    // Number of cases
    unsigned num_cases = switch_inst->getNumCases();
    print_fact(SWITCH_NCASES, id, num_cases);

    // For each case
    unsigned index = 0;
    for (auto it = switch_inst->case_begin(); it != switch_inst->case_end(); it++) {
        // Label
        BasicBlock *label = switch_inst->getSuccessor(index+1);
        print_fact(SWITCH_CASE_LABEL, id, index, (unsigned long) label);

        // Value
        ConstantInt *value = it.getCaseValue();
        const APInt &val = value->getValue();
        print_fact(SWITCH_CASE_VALUE, id, index, val);
        index++;
    }

    print_new();
}

/*
 * translateIndirectBr
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the address of the label to jump to
 * (3) the number of labels
 * For each possible destination:
 * (4) the label
 *
 * 'indirectbr' instructions
 * (http://llvm.org/docs/LangRef.html#indirectbr-instruction)
 */
void translateIndirectBr(unsigned long id, IndirectBrInst *br_inst) {
    // Declare the instruction
    print_fact(INDIRECT_BR, id);

    // Address
    Value* addr = br_inst->getAddress();
    print_fact(INDIRECT_BR_ADDR, id, (unsigned long) addr);

    // Number of labels
    unsigned num_dests = br_inst->getNumDestinations();
    print_fact(INDIRECT_BR_NLABELS, id, (unsigned long) num_dests);

    // For each destination
    for (unsigned index = 0; index < num_dests; ++index) {
        // Label
        BasicBlock *label = br_inst->getDestination(index);
        print_fact(INDIRECT_BR_LABEL, id, index, (unsigned long) label);
    }

    print_new();
}

/*
 * translateUnreachable
 *
 * Declare an unreachable instruction
 */
void translateUnreachable(unsigned long id) {
    // Declare the instruction
    print_fact(UNREACHABLE, id);

    print_new();
}
