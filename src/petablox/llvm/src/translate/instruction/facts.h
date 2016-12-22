#include "instruction.h"

using namespace llvm;
using namespace std;

/*
 * Constants
 */
enum BinOp {
    add, fadd, sub, fsub, mul, fmul,
    udiv, sdiv, fdiv, urem, srem, frem,

    shl, lshl, ashr, and_, or_, xor_
};

static const string ADD = "add_instruction";
static const string ADD_FIRST = "add_instruction_first_operand";
static const string ADD_SECOND = "add_instruction_second_operand";

static const string FADD = "fadd_instruction";
static const string FADD_FIRST = "fadd_instruction_first_operand";
static const string FADD_SECOND = "fadd_instruction_second_operand";

static const string SUB = "sub_instruction";
static const string SUB_FIRST = "sub_instruction_first_operand";
static const string SUB_SECOND = "sub_instruction_second_operand";

static const string FSUB = "fsub_instruction";
static const string FSUB_FIRST = "fsub_instruction_first_operand";
static const string FSUB_SECOND = "fsub_instruction_second_operand";

static const string MUL = "mul_instruction";
static const string MUL_FIRST = "mul_instruction_first_operand";
static const string MUL_SECOND = "mul_instruction_second_operand";

static const string FMUL = "fmul_instruction";
static const string FMUL_FIRST = "fmul_instruction_first_operand";
static const string FMUL_SECOND = "fmul_instruction_second_operand";

static const string UDIV = "udiv_instruction";
static const string UDIV_FIRST = "udiv_instruction_first_operand";
static const string UDIV_SECOND = "udiv_instruction_second_operand";

static const string SDIV = "sdiv_instruction";
static const string SDIV_FIRST = "sdiv_instruction_first_operand";
static const string SDIV_SECOND = "sdiv_instruction_second_operand";

static const string FDIV = "fdiv_instruction";
static const string FDIV_FIRST = "fdiv_instruction_first_operand";
static const string FDIV_SECOND = "fdiv_instruction_second_operand";

static const string UREM = "urem_instruction";
static const string UREM_FIRST = "urem_instruction_first_operand";
static const string UREM_SECOND = "urem_instruction_second_operand";

static const string SREM = "srem_instruction";
static const string SREM_FIRST = "srem_instruction_first_operand";
static const string SREM_SECOND = "srem_instruction_second_operand";

static const string FREM = "frem_instruction";
static const string FREM_FIRST = "frem_instruction_first_operand";
static const string FREM_SECOND = "frem_instruction_second_operand";

static const string SHL = "shl_instruction";
static const string SHL_FIRST = "shl_instruction_first_operand";
static const string SHL_SECOND = "shl_instruction_second_operand";

static const string LSHL = "lshl_instruction";
static const string LSHL_FIRST = "lshl_instruction_first_operand";
static const string LSHL_SECOND = "lshl_instruction_second_operand";

static const string ASHR = "ashr_instruction";
static const string ASHR_FIRST = "ashr_instruction_first_operand";
static const string ASHR_SECOND = "ashr_instruction_second_operand";

static const string AND = "and_instruction";
static const string AND_FIRST = "and_instruction_first_operand";
static const string AND_SECOND = "and_instruction_second_operand";

static const string OR = "or_instruction";
static const string OR_FIRST = "or_instruction_first_operand";
static const string OR_SECOND = "or_instruction_second_operand";

static const string XOR = "xor_instruction";
static const string XOR_FIRST = "xor_instruction_first_operand";
static const string XOR_SECOND = "xor_instruction_second_operand";

/*
 * Relations for memory operations 
 * (alloca, load, store, fence, cmpxchg and atomicrmw)
 */
static const string ALLOCA = "alloca_instruction";
static const string ALLOCA_ALIGN = "alloca_instruction_alignment";
static const string ALLOCA_SIZE = "alloca_instruction_size";
static const string ALLOCA_TYPE = "alloca_instruction_type";

static const string LOAD = "load_instruction";
static const string LOAD_ALIGN = "load_instruction_alignment";
static const string LOAD_ORDER = "load_instruction_ordering";
static const string LOAD_VOLATILE = "load_instruction_volatile";
static const string LOAD_ADDR = "load_instruction_address";

static const string STORE = "store_instruction";
static const string STORE_ALIGN = "store_instruction_alignment";
static const string STORE_ORDER = "store_instruction_ordering";
static const string STORE_VOLATILE = "store_instruction_volatile";
static const string STORE_VALUE = "store_instruction_value";
static const string STORE_ADDR = "store_instruction_address";

static const string FENCE = "fence_instruction";
static const string FENCE_ORDER = "fence_instruction_ordering";

static const string CMPXCHG = "cmpxchg_instruction";
static const string CMPXCHG_ORDER = "cmpxchg_instruction_ordering";  
static const string CMPXCHG_VOLATILE = "cmpxchg_instruction_volatile";
static const string CMPXCHG_ADDR = "cmpxchg_instruction_address";
static const string CMPXCHG_CMP = "cmpxchg_instruction_cmp";
static const string CMPXCHG_NEW = "cmpxchg_instruction_new";

static const string ATOMICRMW = "atomicrmw_instruction";
static const string ATOMICRMW_ORDER = "atomicrmw_instruction_ordering";
static const string ATOMICRMW_VOLATILE = "atomicrmw_instruction_volatile";
static const string ATOMICRMW_OP = "atomicrmw_instruction_operation";
static const string ATOMICRMW_ADDR = "atomicrmw_instruction_address";
static const string ATOMICRMW_VALUE = "atomicrmw_instruction_value";

template<typename T>
void print_fact(std::string name, unsigned long id, std::vector<T> args)
{
    errs() << name << "(" << id << ", ";
    for (auto it = args.begin(); it != args.end(); ++it) {
        errs() << *it;
        if (args.end() != it+1) {
            errs() << ", ";
        }
    }
    errs() << ").\n";
}

template<typename T>
void print_fact(std::string name, unsigned long id, T arg)
{
    errs() << name << "(" << id << ", " << arg << ").\n";
}

inline void print_fact(std::string name, unsigned long id)
{
    errs() << name << "(" << id << ").\n";
}

inline void print_fact(std::string name, unsigned long id, Value *arg)
{
    errs() << name << "(" << id << ", ";
    errs() << *arg;
    errs() << ").\n";
}

inline void print_new() {
    errs() << "\n";
}
