#include "translate/attribute.h"
#include "translate/calling_conv.h"
#include "translate/facts.h"
#include "translate/operand.h"
#include "translate/type.h"
#include "instruction_map.h"

using namespace llvm;
using namespace std;

/*
 * translateCmp
 *
 * Create the following relations:
 * (1) declare the instruction (icmp or fcmp)
 * (2) the condition
 * (3) the first operand
 * (4) the second operand
 *
 * 'icmp' and 'fcmp' instructions
 * (http://llvm.org/docs/LangRef.html#icmp-instruction)
 * (http://llvm.org/docs/LangRef.html#fcmp-instruction)
 */
void translateCmp(unsigned long id, CmpInst *cmp_inst) {
    const char *prefix = cmp_inst->getOpcodeName();
    bool icmp = strcmp("icmp", prefix) == 0;

    // Declare the instruction
    string INSTRUCTION = icmp ? ICMP : FCMP;
    print_fact(INSTRUCTION, id);

    // Condition
    const string conditions[] = {
        // fcmp predicates
        "false", "oeq", "ogt", "oge", "olt", "ole", "one", "ord", "uno", "ueq", "ugt", "uge", "ult", "ule", "une", "true",
        // icmp predicates
        "eq", "ne", "ugt", "uge", "ult", "ule", "sgt", "sge", "slt", "sle"};
    int condition_id = cmp_inst->getPredicate();
    // If the condition is greater than 15, it is an icmp, otherwise its an fcmp
    condition_id = condition_id > 15 ? condition_id - 16 : condition_id;
    string COND = icmp ? ICMP_COND : FCMP_COND;
    string condition = conditions[condition_id];
    print_fact(COND, id, condition);

    // First operand
    Value *first = cmp_inst->getOperand(0);
    string FIRST_OPER = icmp ? ICMP_FIRST : FCMP_FIRST;
    print_fact(FIRST_OPER, id, (unsigned long) first);

    // Second operand
    Value *second = cmp_inst->getOperand(1);
    string SECOND_OPER = icmp ? ICMP_SECOND : FCMP_SECOND;
    print_fact(SECOND_OPER, id, (unsigned long) second);

    print_new();
}

/*
 * translatePhi
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the instruction type
 * (3) the number of pairs
 * For each pair:
 * (4) the pair value
 * (5) the pair label
 *
 * 'phi' instruction
 * (http://llvm.org/docs/LangRef.html#phi-instruction)
 */
void translatePhi(unsigned long id, PHINode *phi) {
    // Declare the instruction
    print_fact(PHI, id); 

    // Type
    string type = processType(phi->getType());
    print_fact(PHI_TYPE, id, type);

    int num = 0;

    // For each pair
    for (auto it = phi->block_begin(); it != phi->block_end(); it++) {
        // Label 
        BasicBlock *bb = *it;
        int index = phi->getBasicBlockIndex(bb);
        //print_fact(PHI_PAIR_LABEL, id, index, basicblock_ids[(unsigned long) bb]);
        print_fact(PHI_PAIR_LABEL, id, index, (unsigned long) bb);

        // Value
        Value *val = phi->getIncomingValue(index);
        print_fact(PHI_PAIR_VAL, id, index, (unsigned long) val);

        ++num;
    }

    // Number of indices
    print_fact(PHI_NPAIRS, id, num);

    print_new();
}

/*
 * translateSelect
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the condition
 * (3) the true value
 * (4) the false value
 *
 * 'select' instruction
 * (http://llvm.org/docs/LangRef.html#select-instruction)
 */
void translateSelect(unsigned long id, SelectInst *select_inst) {
    // Declare the instruction
    print_fact(SELECT, id);

    // Condition
    Value *cond = select_inst->getCondition();
    print_fact(SELECT_COND, id, (unsigned long) cond);

    // True value
    Value *true_val = select_inst->getTrueValue();
    print_fact(SELECT_TRUE, id, (unsigned long) true_val);

    // False value
    Value *false_val = select_inst->getFalseValue();
    print_fact(SELECT_FALSE, id, (unsigned long) false_val);

    print_new();
}

/*
 * translateCall
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) the function called
 * (3) if the call is a direct call, inline assembly call
 *     or an indirect call
 * (4) if tail call optimizations can be used
 * (5) the calling convention used
 * (6) function attributes
 * (7) the call's arguments
 * (8) parameter attributes
 * (9) function signature
 * (10) return type
 *
 * 'call' instruction
 * (http://llvm.org/docs/LangRef.html#call-instruction)
 */
void translateCall(unsigned long id, CallInst *call) {
    // Declare the instruction
    print_fact(CALL, id);

    // Function called
    Value *function = call->getCalledValue();
    print_fact(CALL_FUNC, id, (unsigned long) function);

    // Is this a direct call? (i.e. a function is called)
    if (call->getCalledFunction()) {
        print_fact(DIRECT_CALL, id);
    }
    // Is this an inline assembly call?
    else if (call->isInlineAsm()) {
        print_fact(ASM_CALL, id);
    }
    // Otherwise its an indirect call
    else {
        print_fact(INDIRECT_CALL, id);
    }

    // Can tail call optimizations be applied?
    if (call->isTailCall()) {
        print_fact(CALL_TAIL, id);
    }

    // Calling convention
    CallingConv::ID conv = call->getCallingConv();
    print_fact(CALL_CONV, id, processCallConv(conv));

    /*
     * Attributes
     *
     * Generate facts about this call instruction's attributes.
     * There are two kinds of attributes:
     * (1) Function attributes
     * (2) Return attributes
     */
    
    // Get a list of all attributes
    auto attributes = call->getAttributes();

    // Separate out the function and return attributes
    auto funcAttributes = attributes.getFnAttributes();
    auto retAttributes = attributes.getRetAttributes();

    // Create facts for function attributes
    for (unsigned i = 0; i < funcAttributes.getNumSlots(); i++) {
        for (auto attr = funcAttributes.begin(i); attr != funcAttributes.end(i); attr++) {
            print_fact(CALL_ATTR, id, processAttr(attr)); 
        }
    }

    // Create facts for return attributes
    for (unsigned i = 0; i < retAttributes.getNumSlots(); i++) {
        for (auto attr = retAttributes.begin(i); attr != retAttributes.end(i); attr++) {
            print_fact(CALL_RET_ATTR, id, processAttr(attr)); 
        }
    }

    int index = 0;

    for (auto it = call->arg_begin(); it != call->arg_end(); ++it) {
        Value *arg = it->get();
        print_fact(CALL_ARG, id, index, (unsigned long) arg);
        translateOperand(arg);


        // Iterate through all of the attributes for this parameter.
        auto paramAttributes = attributes.getParamAttributes(index+1);
        for (unsigned i = 0; i < paramAttributes.getNumSlots(); i++) {
            for (auto attr = paramAttributes.begin(i); attr != paramAttributes.end(i); attr++) {
                print_fact(CALL_PARAM_ATTR, id, index, processAttr(attr));
            }
        }

        index++;
    }

    // Signature (function type)
    FunctionType *signature = call->getFunctionType();
    print_fact(CALL_SIG, id, (unsigned long) signature);

    // Return type
    string type = processType(signature->getReturnType());
    print_fact(CALL_RET, id, type);

    print_new();
}

/*
 * translateVAArg
 *
 * Create the following relations:
 * (1) declare the instruction
 * (2) argument type
 * (2) list of arguments
 *
 * 'va_arg' instruction
 * (http://llvm.org/docs/LangRef.html#va-arg-instruction)
 */
void translateVAArg(unsigned long id, VAArgInst *va_arg) {
    // Declare the instruction
    print_fact(VAARG, id);

    // Type
    string type = processType(va_arg->getType());
    print_fact(VAARG_TYPE, id, type);

    // List of arguments
    Value *args = va_arg->getOperand(0);
    print_fact(VAARG_LIST, id, (unsigned long) args);

    print_new();
}
