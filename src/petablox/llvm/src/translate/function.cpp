#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
//#include "translate/facts.h"
#include "type.h"

using namespace std;
using namespace llvm;

/*
 * buildCFG 
 * 
 * Creates a control flow graph for a function. A CFG consists of 
 * edges between basic blocks in the function and edges between
 * successive instructions in each basic block.
 * 
 */
void buildCFG(Function &F) {
    /*
     * Generate facts for the following information:
     * (1) Basic block entry
     * (2) Basic block exit
     * (3) Basic block predecessors
     * (4) Which basic block an instruction belongs to
     * (5) Instruction predecessors
     */
    errs() << "%% Constructing the CFG\n";

    // Iterate through each basic block in the function
    for (auto &B : F) {
        unsigned long bb_id = (unsigned long) &B;
        errs() << "% Basic block " << bb_id << ":\n";

        // Basic block entry
        Instruction *first_inst = &(*B.begin());
        errs() << "basicblock_entry(" << bb_id << ", " << (unsigned long) first_inst << ").\n";

        // Basic block exit
        const TerminatorInst *last_inst = B.getTerminator();
        errs() << "basicblock_exit(" << bb_id << ", " << (unsigned long) last_inst << ").\n";

        // Basic block predecessor 
        for (unsigned i = 0; i < last_inst->getNumSuccessors(); i++){
            BasicBlock *Succ = last_inst->getSuccessor(i);
            errs() << "basicblock_pred(" << (unsigned long) Succ << ", " << bb_id<< ").\n";
        }

        // Iterate through each function in this basic block 
        Instruction *prev = NULL;
        for (Instruction &I : B) {
            // Instruction belongs to this basic block
            errs() << "instruction_basicblock(" << (unsigned long) &I << ", " << bb_id << ").\n";

            // Instruction predecessor
            if (prev) {
                errs() << "instruction_next(" << (unsigned long) prev << ", " << (unsigned long) &I << ").\n";
                prev = &I;
            }
        }

        errs() << "\n";
    }
}


/*
 * translateFunction
 *
 * Extracts various relations from an LLVM function.
 * Features include name, type, parameters, etc.
 *
 */
void translateFunction(Function &F, unsigned long id) {
    /*
     * Generate facts to represent the following information:
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

    errs() << "function(" << id << ").\n";

    // Return type
    //Type::TypeID ret_type = F.getReturnType()->getTypeID();
    string type = processType(F.getFunctionType());
    errs() << "function_type(" << id << ", " << type << ").\n";

    // Function name
    std::string name = F.getName().str();
    errs() << "function_name(" << id << ", " << name << ").\n";

    // TODO: function signature

    // Linkage type
    GlobalValue::LinkageTypes linkage = F.getLinkage();
    errs() << "function_linkage_type(" << id << ", " << (int) linkage << ").\n";

    // Visibility
    GlobalValue::VisibilityTypes vis = F.getVisibility();
    errs() << "function_visibility(" << id << ", " << (int) vis << ").\n";

    // Calling convention
    CallingConv::ID calling_conv = F.getCallingConv();
    errs() << "function_calling_convention(" << id << ", " << (int) calling_conv << ").\n";

    // TODO: unnamed_addr

    // Alignment
    unsigned align = F.getAlignment();
    errs() << "function_alignment(" << id << ", " << align << ").\n";

    // Garbage collector
    if (F.hasGC()) {
        const std::string gc = F.getGC();
        errs() << "function_gc(" << id << ", " << gc << ").\n";
    }

    // Personality function
    if (F.hasPersonalityFn()) {
        Constant *personality = F.getPersonalityFn();
        errs() << "function_pers_fn(" << id << ", " << (unsigned long) personality << ").\n";
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
            errs() << "function_attribute(" << id << ", " << (unsigned long) attr << ").\n";
        }
    }

    // Create facts for return attributes
    for (unsigned i = 0; i < retAttributes.getNumSlots(); i++) {
        for (auto attr = retAttributes.begin(i); attr != retAttributes.end(i); attr++) {
            errs() << "function_return_attribute(" << id << ", " << (unsigned long) attr << ").\n";
        }
    }

    // Section 
    if (F.hasSection()) {
        //std::string section = F.getSection().str();
        const char *section = F.getSection();
        errs() << "function_section(" << id << ", " << section << ").\n";
    }
    
    // Parameters/Parameter attributes
    int index = 0; 

    // Iterate through all of this function's parameters
    for (auto &param : F.args()) {
        errs() << "function_param(" << id << ", " << index << ", " << (unsigned long) &param << ").\n";

        // Iterate through all of the attributes for this parameter.
        auto paramAttributes = attributes.getParamAttributes(index);
        for (unsigned i = 0; i < paramAttributes.getNumSlots(); i++) {
            for (auto attr = paramAttributes.begin(i); attr != paramAttributes.end(i); attr++) {
                errs() << "function_param_attribute(" << id << ", " << index << ", " << (unsigned long) attr << ").\n";
            }
        }
        index++;
    }
    // Create a fact for how many parameters this function has
    errs() << "function_nparams(" << id << ", " << index << ").\n";

    errs() << "\n";
}
