#include "instruction.h"

using namespace llvm;

void translateReturn(unsigned long id, ReturnInst *ret_inst) {
    // Generate facts
    /*
     * TODO: Add rule for terminator_instruction(inst) :- ret_instruction(inst)
     * TODO: Well formed functions (see ret-instruction.logic)
     */
    errs() << "ret_instruction(" << id << ").\n";
    if (Value *ret_value = ret_inst->getReturnValue()) {
        if (dyn_cast<Constant>(ret_value)) {
            errs() << "constant(" << (unsigned long) ret_value << ", " << *ret_value << ").\n";
        }
        errs() << "ret_instruction_value(" << id << ", " << (unsigned long) ret_value << ").\n";
    }
    else {
        errs() << "ret_instruction_void(" << id << ").\n";
    }
    errs() << "\n";
}

void translateBr(unsigned long id, BranchInst *br_inst) {
    /*
     * TODO: Add terminator_instruction(inst) :- br_instruction(inst)
     * TODO: Facts for labels
     */
    // Generate facts
    errs() << "br_instruction(" << id << ").\n";

    // If the branch is conditional
    if (br_inst->isConditional()) {
        // Get the condition
        Value *condition = br_inst->getCondition();

        // Get next instructions based on condition
        BasicBlock *true_bb = br_inst->getSuccessor(0);
        BasicBlock *false_bb = br_inst->getSuccessor(1);
        auto true_iter = true_bb->begin();
        auto false_iter = false_bb->begin();
        Instruction *true_inst = &(*true_iter);
        Instruction *false_inst = &(*false_iter);

        errs() << "br_cond_instruction(" << id << ").\n";
        errs() << "br_cond_instruction_condition(" << id << ", " << (unsigned long) condition << ").\n";
        errs() << "br_cond_instruction_iftrue(" << id << ", " << (unsigned long) true_inst << ").\n";
        errs() << "br_cond_instruction_iffalse(" << id << ", " << (unsigned long) false_inst << ").\n";
    }
    else {
        // Get next instruction
        BasicBlock *next_bb = br_inst->getSuccessor(0);
        auto next_iter = next_bb->begin();
        Instruction *next_inst = &(*next_iter);

        errs() << "br_uncond_instruction(" << id << ").\n";
        errs() << "br_uncond_instruction_dest(" << id << ", " << (unsigned long) next_inst << ").\n";
    }
    errs() << "\n";
}

void translateUnreachable(unsigned long id) {
    errs() << "unreachable_instruction(" << id << ").\n";
    errs() << "\n";
}
