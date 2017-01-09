#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"
#include "translate/facts.h"
#include "operand.h"

using namespace llvm;

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
        Type::TypeID global_type = global.getValueType()->getTypeID();
        std::string global_name = global.getName().str();
        unsigned alignment = global.getAlignment();
        GlobalValue::LinkageTypes linkage = global.getLinkage();
        GlobalValue::VisibilityTypes vis = global.getVisibility();

        print_fact(GLOBAL_VAR, global_id);
        print_fact<int>(GLOBAL_TYPE, global_id, global_type);
        print_fact<std::string>(GLOBAL_NAME, global_id, global_name);
        print_fact<unsigned>(GLOBAL_ALIGN, global_id, alignment);
        print_fact<int>(GLOBAL_LINKAGE_TYPE, global_id, linkage);
        print_fact<int>(GLOBAL_VIS, global_id, vis);

        if (global.hasInitializer()) {
            Constant *global_init = global.getInitializer();
            print_fact<unsigned long>(GLOBAL_INIT, global_id, (unsigned long) global_init);
            translateOperand(global_init);
        }

        if (global.hasSection()) {
            const char *section = global.getSection();
            print_fact<const char *>(GLOBAL_SEC, global_id, section);
        }

        if (global.isThreadLocal()) {
            GlobalValue::ThreadLocalMode mode = global.getThreadLocalMode();
            print_fact<int>(GLOBAL_THREAD_LOCAL, global_id, mode); 
        }

        if (global.isConstant()) {
            print_fact(GLOBAL_CONSTANT, global_id);
        }

        print_new();
    }
}

// TODO: aliases
