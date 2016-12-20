#include <iostream>
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/Module.h"

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

        errs() << "global_variable(" << global_id << ").\n";
        errs() << "global_variable_type(" << global_id << ", " << global_type << ").\n";
        errs() << "global_variable_name(" << global_id << ", " << global_name << ").\n";
        errs() << "global_variable_align(" << global_id << ", " << alignment << ").\n";
        errs() << "global_variable_linkage_type(" << global_id << ", " << linkage << ").\n";
        errs() << "global_variable_visibility(" << global_id << ", " << vis << ").\n";

        if (global.hasInitializer()) {
            Constant *global_init = global.getInitializer();
            errs() << "constant(" << (unsigned long) global_init << ", " << *global_init << ").\n";
            errs() << "global_variable_initializer(" << global_id << ", " << (unsigned long) global_init << ").\n";
        }

        if (global.hasSection()) {
            //std::string section = global.getSection().str();
            const char *section = global.getSection();
            errs() << "global_variable_section(" << global_id << ", " << section << ").\n";
        }

        if (global.isThreadLocal()) {
            GlobalValue::ThreadLocalMode mode = global.getThreadLocalMode();
            errs() << "global_variable_threadlocal_mode(" << global_id << ", " << mode << ").\n";
        }

        if (global.isConstant()) {
            errs() << "global_variable_constant(" << global_id << ").\n";
        }

        errs() << "\n";

    }
}

// TODO: aliases
