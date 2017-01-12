#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "translate/facts.h"
#include "translate/type.h"
#include "translate/visibility.h"
#include "operand.h"

using namespace std;
using namespace llvm;

string thread_local_modes[] = { "notthreadlocal", "dynamic", "localdynamic", "initialexec", "localexec" };

/*
 * translateGlobals
 *
 * For each global variable in a module, create the following relations:
 * (1) declare the variable
 * (2) the variable type
 * (3) the variable name
 * (4) the variable's initializer (if any)
 * (5) the variable section
 * (6) the variable alignment
 * (7) the variable's linkage type
 * (8) the variable visibility
 * (9) if the variable is threadlocal mode
 * (10) if the variable is constant
 */
void translateGlobals(Function &F) {

    // Get this function's first basic block
    BasicBlock &B = F.getEntryBlock();
    Module *module = B.getModule();

    // Get the list of global variables
    auto &global_list = module->getGlobalList();

    for (auto &global : global_list) {
        errs() << "% ";
        global.dump();

        unsigned long global_id = (unsigned long) &global;

        // Declare the variable
        print_fact(GLOBAL_VAR, global_id);

        // Type
        string global_type = processType(global.getValueType());
        print_fact(GLOBAL_TYPE, global_id, global_type);

        // Name
        string global_name = global.getName().str();
        print_fact(GLOBAL_NAME, global_id, global_name);

        // Alignment
        unsigned alignment = global.getAlignment();
        print_fact(GLOBAL_ALIGN, global_id, alignment);

        // Linkage type
        GlobalValue::LinkageTypes linkage = global.getLinkage();
        print_fact(GLOBAL_LINKAGE_TYPE, global_id, linkage);

        // Visibility
        GlobalValue::VisibilityTypes vis = global.getVisibility();
        print_fact(GLOBAL_VIS, global_id, processVis(vis));

        // Initializer
        if (global.hasInitializer()) {
            Constant *global_init = global.getInitializer();
            print_fact(GLOBAL_INIT, global_id, (unsigned long) global_init);
            translateOperand(global_init);
        }

        // Section
        if (global.hasSection()) {
            const char *section = global.getSection();
            print_fact(GLOBAL_SEC, global_id, section);
        }

        // Thread local
        if (global.isThreadLocal()) {
            GlobalValue::ThreadLocalMode mode = global.getThreadLocalMode();
            print_fact(GLOBAL_THREAD_LOCAL, global_id, thread_local_modes[mode]); 
        }

        // Is this a constant?
        if (global.isConstant()) {
            print_fact(GLOBAL_CONSTANT, global_id);
        }

        print_new();
    }
}

// TODO: aliases
