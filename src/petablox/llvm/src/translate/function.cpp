#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "translate/attribute.h"
#include "translate/calling_conv.h"
#include "translate/facts.h"
#include "translate/linkage.h"
#include "translate/operand.h"
#include "type.h"
#include "translate/visibility.h"
//#include "instruction_map.h"

using namespace std;
using namespace llvm;

set<unsigned long> basicblocks;
map<unsigned long, int> basicblock_ids;

/*
 * buildCFG 
 * 
 * Creates a control flow graph for a function. A CFG consists of 
 * edges between basic blocks in the function and edges between
 * successive instructions in each basic block.
 * 
 * Create the following relations: 
 * (1) Basic block entry
 * (2) Basic block exit
 * (3) Basic block predecessors
 * (4) Which basic block an instruction belongs to
 * (5) Instruction predecessors
 *
 */
void buildCFG(Function &F) {
    outs() << "%% Constructing the CFG\n";

    int basicblock_id = 1;
    for (auto &B : F) {
        basicblock_ids[(unsigned long) &B] = basicblock_id++;
        basicblocks.insert((unsigned long) &B);

    }

    // Iterate through each basic block in the function
    for (auto &B : F) {
        //unsigned long bb_id = (unsigned long) &B;
        //int bb_id = basicblock_ids[(unsigned long) &B];
        unsigned long bb_id = (unsigned long) &B;
        outs() << "% Basic block " << bb_id << ":\n";

        // The address of each basic block is a label
        print_fact("type", bb_id);
        print_fact(LABEL_TY, bb_id);
        print_fact(OPERAND, bb_id);

        // Basic block entry
        Instruction *first_inst = &(*B.begin());
        print_fact(BB_ENTRY, bb_id, (unsigned long) first_inst);

        // Basic block exit
        const TerminatorInst *last_inst = B.getTerminator();
        print_fact(BB_EXIT, bb_id, (unsigned long) last_inst);

        // Basic block predecessor 
        for (unsigned i = 0; i < last_inst->getNumSuccessors(); i++){
            BasicBlock *succ = last_inst->getSuccessor(i);
            //print_fact(BB_PRED, basicblock_ids[(unsigned long) succ], bb_id); 
            print_fact(BB_PRED, (unsigned long) succ, bb_id); 
        }

        // Iterate through each function in this basic block 
        Instruction *prev = NULL;
        for (Instruction &I : B) {
            // Instruction belongs to this basic block
            print_fact(OPERAND, (unsigned long) &I);
            print_fact(INST_BB, (unsigned long) &I, bb_id);

            // Instruction predecessor
            if (!prev) {
                prev = &I;
            }
            else {
                print_fact(INST_NEXT, (unsigned long) prev, (unsigned long) &I);
                prev = &I;
            }
        }

        print_new();
    }
}


/*
 * translateFunction
 *
 * Extracts various relations from an LLVM function.
 * Features include name, type, parameters, etc.
 *
 * Create the following relations:
 * (1) Return type
 * (2) Function name
 * (3) Signature
 * (4) Linkage type
 * (5) Visibility
 * (6) Calling convention
 * (7) Unnamed address
 * (8) Alignment
 * (9) Garbage collector 
 * (10) Personality function
 * (11) Function attributes
 * (12) Section
 * (13) Parameters
 * (14) Parameter attributes
 *
 */
void translateFunction(Function &F, unsigned long id) {

    print_fact(FUNCTION, id);

    // Return type
    //string type = processType(F.getFunctionType());
    unsigned long type = processType(F.getFunctionType());
    print_fact(FUNCTION_TYPE, id, type);

    // Function name
    std::string name = F.getName().str();
    print_fact(FUNCTION_NAME, id, name);

    // TODO: function signature

    // Linkage type
    GlobalValue::LinkageTypes linkage = F.getLinkage();
    print_fact(FUNCTION_LINK, id, processLinkage(linkage));

    // Visibility
    GlobalValue::VisibilityTypes vis = F.getVisibility();
    print_fact(FUNCTION_VIS, id, processVis(vis));

    // Calling convention
    CallingConv::ID calling_conv = F.getCallingConv();
    print_fact(FUNCTION_CALL_CONV, id, processCallConv(calling_conv));

    if (F.hasUnnamedAddr()) {
        print_fact(FUNCTION_UNNAMED_ADDR, id);
    }

    // Alignment
    unsigned align = F.getAlignment();
    print_fact("integer", align);
    print_fact(FUNCTION_ALIGN, id, align);

    // Garbage collector
    if (F.hasGC()) {
        const std::string gc = F.getGC();
        print_fact(FUNCTION_GC, id, gc);
    }

    // Personality function
    if (F.hasPersonalityFn()) {
        Constant *personality = F.getPersonalityFn();
        print_fact(FUNCTION_PERS, id, (unsigned long) personality);
    }

    /*
     * Function attributes
     *
     * Generate facts about this function's attributes.
     * There are two kinds of attributes:
     * (1) Function attributes
     * (2) Return attributes
     */

    // Get a list of all attributes
    auto attributes = F.getAttributes();

    // Separate out the function and return attributes
    auto funcAttributes = attributes.getFnAttributes();
    auto retAttributes = attributes.getRetAttributes();

    // Create facts for function attributes
    for (unsigned i = 0; i < funcAttributes.getNumSlots(); i++) {
        for (auto attr = funcAttributes.begin(i); attr != funcAttributes.end(i); attr++) {
            print_fact(FUNCTION_ATTR, id, processAttr(attr));
        }
    }

    // Create facts for return attributes
    for (unsigned i = 0; i < retAttributes.getNumSlots(); i++) {
        for (auto attr = retAttributes.begin(i); attr != retAttributes.end(i); attr++) {
            print_fact(FUNCTION_RET_ATTR, id, processAttr(attr));
        }
    }

    // Section 
    if (F.hasSection()) {
        const char *section = F.getSection();
        print_fact(FUNCTION_SEC, id, section);
    }

    // Parameters/Parameter attributes
    int index = 0; 

    // Iterate through all of this function's parameters
    for (auto &param : F.args()) {
        print_fact(FUNCTION_PARAM, id, index, (unsigned long) &param);

        // translate the operand
        translateOperand(&param);

        // Iterate through all of the attributes for this parameter.
        auto paramAttributes = attributes.getParamAttributes(index+1);
        for (unsigned i = 0; i < paramAttributes.getNumSlots(); i++) {
            for (auto attr = paramAttributes.begin(i); attr != paramAttributes.end(i); attr++) {
                print_fact(FUNCTION_PARAM_ATTR, id, index, processAttr(attr));
            }
        }
        index++;
    }
    // Create a fact for how many parameters this function has
    print_fact(FUNCTION_NPARAMS, id, index);

    print_new();
}
