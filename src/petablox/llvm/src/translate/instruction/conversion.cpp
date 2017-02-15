#include "translate/facts.h"
#include "translate/type.h"

using namespace llvm;
using namespace std;

/*
 * Create data structures to map
 * the instruction opcodes to the
 * appropriate relations
 */
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

/*
 * translateConversion
 *
 * Create the following facts for a conversion instruction:
 * (1) declare the operation
 * (2) the "from" value
 * (3) the "from" type
 * (4) the "to" type
 *
 */
void translateConversion(unsigned long id, CastInst *conv_inst) {
    populate_ops_map();
    populate_from_map();
    populate_from_type_map();
    populate_to_type_map();

    const char *prefix = conv_inst->getOpcodeName();

    // Declare the operation
    string INSTRUCTION = ops_map[prefix];
    print_fact(INSTRUCTION, id); 

    // "from" value
    Value *from = conv_inst->getOperand(0);
    string FROM = from_map[prefix];
    print_fact(FROM, id, (unsigned long) from);

    // "from" type
    //string from_type = processType(conv_inst->getSrcTy());
    unsigned long from_type = processType(conv_inst->getSrcTy());
    string FROM_TYPE = from_type_map[prefix];
    print_fact(FROM_TYPE, id, from_type);

    // "to" type
    //string to_type = processType(conv_inst->getDestTy());
    unsigned long to_type = processType(conv_inst->getDestTy());
    string TO_TYPE = to_type_map[prefix];
    print_fact(TO_TYPE, id, to_type);

    print_new();
}
