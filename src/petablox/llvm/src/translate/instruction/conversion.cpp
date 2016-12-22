#include "instruction.h"
#include "facts.h"

using namespace llvm;
using namespace std;

map<string, string> ops_map;
map<string, string> from_map;
map<string, string> from_type_map;
map<string, string> to_type_map;

void populate_ops_map() {
    ops_map["trunc"] = TRUNC;
    ops_map["zext"] = ZEXT;
    ops_map["sext"] = SEXT;
    ops_map["fptrunc"] = FPTRUNC;
    ops_map["fpext"] = FPEXT;
    ops_map["fptoui"] = FPTOUI;
    ops_map["fptosi"] = FPTOSI;
    ops_map["uitofp"] = UITOFP;
    ops_map["sitofp"] = SITOFP;
    ops_map["ptrtoint"] = PTRTOINT;
    ops_map["inttoptr"] = INTTOPTR;
    ops_map["bitcast"] = BITCAST;
    ops_map["addrspacecast"] = ADDRSPACAECAST;
}

void populate_from_map() {
    from_map["trunc"] = TRUNC_FROM;
    from_map["zext"] = ZEXT_FROM;
    from_map["sext"] = SEXT_FROM;
    from_map["fptrunc"] = FPTRUNC_FROM;
    from_map["fpext"] = FPEXT_FROM;
    from_map["fptoui"] = FPTOUI_FROM;
    from_map["fptosi"] = FPTOSI_FROM;
    from_map["uitofp"] = UITOFP_FROM;
    from_map["sitofp"] = SITOFP_FROM;
    from_map["ptrtoint"] = PTRTOINT_FROM;
    from_map["inttoptr"] = INTTOPTR_FROM;
    from_map["bitcast"] = BITCAST_FROM;
    from_map["addrspacecast"] = ADDRSPACAECAST_FROM;
}

void populate_from_type_map() {
    from_type_map["trunc"] = TRUNC_FROM_TYPE;
    from_type_map["zext"] = ZEXT_FROM_TYPE;
    from_type_map["sext"] = SEXT_FROM_TYPE;
    from_type_map["fptrunc"] = FPTRUNC_FROM_TYPE;
    from_type_map["fpext"] = FPEXT_FROM_TYPE;
    from_type_map["fptoui"] = FPTOUI_FROM_TYPE;
    from_type_map["fptosi"] = FPTOSI_FROM_TYPE;
    from_type_map["uitofp"] = UITOFP_FROM_TYPE;
    from_type_map["sitofp"] = SITOFP_FROM_TYPE;
    from_type_map["ptrtoint"] = PTRTOINT_FROM_TYPE;
    from_type_map["inttoptr"] = INTTOPTR_FROM_TYPE;
    from_type_map["bitcast"] = BITCAST_FROM_TYPE;
    from_type_map["addrspacecast"] = ADDRSPACAECAST_FROM_TYPE;
}

void populate_to_type_map() {
    to_type_map["trunc"] = TRUNC_TO_TYPE;
    to_type_map["zext"] = ZEXT_TO_TYPE;
    to_type_map["sext"] = SEXT_TO_TYPE;
    to_type_map["fptrunc"] = FPTRUNC_TO_TYPE;
    to_type_map["fpext"] = FPEXT_TO_TYPE;
    to_type_map["fptoui"] = FPTOUI_TO_TYPE;
    to_type_map["fptosi"] = FPTOSI_TO_TYPE;
    to_type_map["uitofp"] = UITOFP_TO_TYPE;
    to_type_map["sitofp"] = SITOFP_TO_TYPE;
    to_type_map["ptrtoint"] = PTRTOINT_TO_TYPE;
    to_type_map["inttoptr"] = INTTOPTR_TO_TYPE;
    to_type_map["bitcast"] = BITCAST_TO_TYPE;
    to_type_map["addrspacecast"] = ADDRSPACAECAST_TO_TYPE;
}

void translateConversion(unsigned long id, CastInst *conv_inst) {
    populate_ops_map();
    populate_from_map();
    populate_from_type_map();
    populate_to_type_map();

    // Get data for relations
    const char *prefix = conv_inst->getOpcodeName();
    Value *from = conv_inst->getOperand(0);
    Type *from_type = conv_inst->getSrcTy();
    Type *to_type = conv_inst->getDestTy();
    Type::TypeID from_type_id = from_type->getTypeID();
    Type::TypeID to_type_id = to_type->getTypeID();

    string INSTRUCTION = ops_map[prefix];
    string FROM = from_map[prefix];
    string FROM_TYPE = from_type_map[prefix];
    string TO_TYPE = to_type_map[prefix];

    // Generate facts

    if (dyn_cast<Constant>(from)) {
        errs() << prefix << "constant(" << (unsigned long) from << ", " << *from << ").\n";
    }

    print_fact(INSTRUCTION, id); 
    print_fact<unsigned long>(FROM, id, (unsigned long) from);
    // TODO: do we need a rule for from_type like in cclyzer?
    print_fact<unsigned>(FROM_TYPE, id, from_type_id);
    print_fact<unsigned>(TO_TYPE, id, to_type_id);
    print_new();
}
