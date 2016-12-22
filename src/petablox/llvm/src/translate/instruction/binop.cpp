#include "instruction.h"
#include "facts.h"
#include <stdio.h>
#include <string.h>

using namespace llvm;
using namespace std;

map<BinOp, string> binops_map;
map<BinOp, string> first_map;
map<BinOp, string> second_map;

void populate_binops_map() {
    // Add binary operations to set
    binops_map[add] = ADD;
    binops_map[fadd] = FADD;
    binops_map[sub] = SUB;
    binops_map[fsub] = FSUB;
    binops_map[mul] = MUL;
    binops_map[fmul] = FMUL;
    binops_map[udiv] = UDIV;
    binops_map[sdiv] = SDIV;
    binops_map[fdiv] = FDIV;
    binops_map[urem] = UREM;
    binops_map[srem] = SREM;
    binops_map[frem] = FREM;

    // Add bitwise binary operations to set
    binops_map[shl] = SHL;
    binops_map[lshl] = LSHL;
    binops_map[ashr] = ASHR;
    binops_map[and_] = AND;
    binops_map[or_] = OR;
    binops_map[xor_] = XOR;
}

void populate_first_map() {
    // Add binary operations to set
    first_map[add] = ADD_FIRST;
    first_map[fadd] = FADD_FIRST;
    first_map[sub] = SUB_FIRST;
    first_map[fsub] = FSUB_FIRST;
    first_map[mul] = MUL_FIRST;
    first_map[fmul] = FMUL_FIRST;
    first_map[udiv] = UDIV_FIRST;
    first_map[sdiv] = SDIV_FIRST;
    first_map[fdiv] = FDIV_FIRST;
    first_map[urem] = UREM_FIRST;
    first_map[srem] = SREM_FIRST;
    first_map[frem] = FREM_FIRST;

    // Add bitwise binary operations to set
    first_map[shl] = SHL_FIRST;
    first_map[lshl] = LSHL_FIRST;
    first_map[ashr] = ASHR_FIRST;
    first_map[and_] = AND_FIRST;
    first_map[or_] = OR_FIRST;
    first_map[xor_] = XOR_FIRST;
}

void populate_second_map() {
    // Add binary operations to set
    second_map[add] = ADD_SECOND;
    second_map[fadd] = FADD_SECOND;
    second_map[sub] = SUB_SECOND;
    second_map[fsub] = FSUB_SECOND;
    second_map[mul] = MUL_SECOND;
    second_map[fmul] = FMUL_SECOND;
    second_map[udiv] = UDIV_SECOND;
    second_map[sdiv] = SDIV_SECOND;
    second_map[fdiv] = FDIV_SECOND;
    second_map[urem] = UREM_SECOND;
    second_map[srem] = SREM_SECOND;
    second_map[frem] = FREM_SECOND;

    // Add bitwise binary operations to set
    second_map[shl] = SHL_SECOND;
    second_map[lshl] = LSHL_SECOND;
    second_map[ashr] = ASHR_SECOND;
    second_map[and_] = AND_SECOND;
    second_map[or_] = OR_SECOND;
    second_map[xor_] = XOR_SECOND;
}

void translateBinOp(BinOp op, Instruction &I, unsigned long id) {
    populate_binops_map();
    populate_first_map();
    populate_second_map();

    // Get data for relations
    Value *first = I.getOperand(0);
    Value *second = I.getOperand(1);

    string INSTRUCTION = binops_map[op];
    string FIRST_OPER = first_map[op];
    string SECOND_OPER = second_map[op];

    if (dyn_cast<Constant>(first)) {
        errs() << "constant(" << (unsigned long) first << ", " << *first << ").\n";
    }

    if (dyn_cast<Constant>(second)) {
        errs() << "constant(" << (unsigned long) second << ", " << *second << ").\n";
    }

    // Generate facts
    print_fact(INSTRUCTION, id);
    print_fact<unsigned long>(FIRST_OPER, id, (unsigned long) first);
    print_fact<unsigned long>(SECOND_OPER, id, (unsigned long) second);
    print_new();
}
