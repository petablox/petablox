#include "instruction.h"
#include "facts.h"

using namespace llvm;

void translateReturn(unsigned long id, ReturnInst *ret_inst) {
    // Generate facts
    /*
     * TODO: Add rule for terminator_instruction(inst) :- ret_instruction(inst)
     * TODO: Well formed functions (see ret-instruction.logic)
     */
    print_fact(RETURN, id);
    if (Value *ret_value = ret_inst->getReturnValue()) {
        if (dyn_cast<Constant>(ret_value)) {
            errs() << "constant(" << (unsigned long) ret_value << ", " << *ret_value << ").\n";
        }
        print_fact<unsigned long>(RETURN_VALUE, id, (unsigned long) ret_value);
    }
    else {
        print_fact(RETURN_VOID, id);
    }
    print_new();
}

void translateBr(unsigned long id, BranchInst *br_inst) {
    /*
     * TODO: Add terminator_instruction(inst) :- br_instruction(inst)
     * TODO: Facts for labels
     */
    // Generate facts
    print_fact(BRANCH, id);

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

        print_fact(COND_BR, id);
        print_fact<unsigned long>(COND_BR_CONDITION, id, (unsigned long) condition);
        print_fact<unsigned long>(COND_BR_TRUE, id, (unsigned long) true_inst);
        print_fact<unsigned long>(COND_BR_FALSE, id, (unsigned long) false_inst);
    }
    else {
        // Get next instruction
        BasicBlock *next_bb = br_inst->getSuccessor(0);
        auto next_iter = next_bb->begin();
        Instruction *next_inst = &(*next_iter);

        print_fact(UNCOND_BR, id);
        print_fact(UNCOND_BR_DEST, id, (unsigned long) next_inst);
    }
    print_new();
}

void translateIndirectBr(unsigned long id, IndirectBrInst *br_inst) {
    print_fact(INDIRECT_BR, id);

    Value* addr = br_inst->getAddress();

    print_fact<unsigned long>(INDIRECT_BR_ADDR, id, (unsigned long) addr);

    unsigned num_dests = br_inst->getNumDestinations();
    print_fact<unsigned>(INDIRECT_BR_NLABELS, id, (unsigned long) num_dests);

    for (unsigned index = 0; index < num_dests; ++index) {
        BasicBlock *label = br_inst->getDestination(index);
        print_fact<unsigned long>(INDIRECT_BR_LABEL, id, index, (unsigned long) label);
    }
    print_new();
}

void translateInvoke(unsigned long id, InvokeInst *invoke_inst) {
    print_fact(INVOKE, id);

    if (invoke_inst->getCalledFunction()) {
        print_fact(DIRECTINVOKE, id);
    }
    else {
        print_fact(INDIRECTINVOKE, id);
    }

    Value *function = invoke_inst->getCalledValue();
    print_fact<unsigned long>(INVOKE_FUNCTION, id, (unsigned long) function);

    CallingConv::ID conv = invoke_inst->getCallingConv();
    print_fact<unsigned>(INVOKE_CALLING_CONV, id, conv);

    /*
     * Attributes
     *
     * Generate facts about this invoke instruction's attributes.
     * There are two kinds of attributes:
     * (1) Function attributes
     * (2) Return attributes
     */
    
    // Get a list of all attributes
    auto attributes = invoke_inst->getAttributes();

    // Separate out the function and return attributes
    auto funcAttributes = attributes.getFnAttributes();
    auto retAttributes = attributes.getRetAttributes();

    // Create facts for function attributes
    for (unsigned i = 0; i < funcAttributes.getNumSlots(); i++) {
        for (auto attr = funcAttributes.begin(i); attr != funcAttributes.end(i); attr++) {
            print_fact<unsigned long>(INVOKE_ATTR, id, (unsigned long) attr); 
        }
    }

    // Create facts for return attributes
    for (unsigned i = 0; i < retAttributes.getNumSlots(); i++) {
        for (auto attr = retAttributes.begin(i); attr != retAttributes.end(i); attr++) {
            print_fact<unsigned long>(INVOKE_RET_ATTR, id, (unsigned long) attr); 
        }
    }

    int index = 0;

    for (auto it = invoke_inst->arg_begin(); it != invoke_inst->arg_end(); ++it) {
        print_fact<unsigned long>(INVOKE_ARG, id, index, (unsigned long) it);
        
        // Iterate through all of the attributes for this parameter.
        auto paramAttributes = attributes.getParamAttributes(index);
        for (unsigned i = 0; i < paramAttributes.getNumSlots(); i++) {
            for (auto attr = paramAttributes.begin(i); attr != paramAttributes.end(i); attr++) {
                print_fact<unsigned long>(INVOKE_PARAM_ATTR, id, index, (unsigned long) attr);
            }
        }

        index++;
    }

    // TODO: normal/exception labels

    print_new();
}

void translateResume(unsigned long id, ResumeInst *resume_inst) {
    print_fact(RESUME, id);
    Value *oper = resume_inst->getValue();
    print_fact<unsigned long>(RESUME_OPER, id, (unsigned long) oper);
    print_new();
}

void translateUnreachable(unsigned long id) {
    print_fact(UNREACHABLE, id);
    print_new();
}
