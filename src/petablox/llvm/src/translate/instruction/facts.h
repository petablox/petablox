#include "instruction.h"

using namespace llvm;
using namespace std;

/*
 * Constants
 */

/*
 * Terminator instructions
 * (ret, br, switch, indirectbr, invoke,
 * resume, catchswitch, catchret, cleanupret,
 * unreachable)
 */
static const string RETURN = "ret_instruction";
static const string RETURN_VALUE = "ret_instruction_value";
static const string RETURN_VOID = "ret_instruction_void";

static const string BRANCH = "br_instruction";
static const string COND_BR = "br_cond_instruction";
static const string COND_BR_CONDITION = "br_cond_instruction_condition";
static const string COND_BR_TRUE = "br_cond_instruction_iftrue";
static const string COND_BR_FALSE = "br_cond_instruction_iffalse";
static const string UNCOND_BR = "br_uncond_instruction"; 
static const string UNCOND_BR_DEST = "br_uncond_instruction_dest";

static const string UNREACHABLE = "unreachable_instruction";

/*
 * Binary operations
 * (add, fadd, sub, fsub, mul, fmul,
 * udiv, sdiv, fdiv, urem, srem, frem,
 * shl, lshl, ashr, and, or, xor)
 *
 * Create an enum to represent the binary 
 * operations. The enum can be used to 
 * create a mapping between the opcodes
 * and the relation constants.
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
 * Vector operations
 * (extractelement, insertelement, shufflevector)
 */
static const string EXTRACTELEMENT = "extractelement_instruction";
static const string EXTRACTELEMENT_BASE = "extractelement_instruction_base";
static const string EXTRACTELEMENT_INDEX = "extractelement_instruction_index";

static const string INSERTELEMENT = "insertelement_instruction";
static const string INSERTELEMENT_BASE = "insertelement_instruction_base";
static const string INSERTELEMENT_INDEX = "insertelement_instruction_index";
static const string INSERTELEMENT_VALUE = "insertelement_instruction_value";

/*
 * Aggregate operations
 * (extractvalue, insertvalue)
 */
static const string EXTRACTVALUE = "extractvalue_instruction";
static const string EXTRACTVALUE_BASE = "extractvalue_instruction_base";
static const string EXTRACTVALUE_NINDICES = "extractvalue_instruction_nindices";
static const string EXTRACTVALUE_INDEX = "extractvalue_instruction_index";

static const string INSERTVALUE = "insertvalue_instruction";
static const string INSERTVALUE_BASE = "insertvalue_instruction_base";
static const string INSERTVALUE_VALUE = "insertvalue_instruction_value";
static const string INSERTVALUE_NINDICES = "insertvalue_instruction_nindices";
static const string INSERTVALUE_INDEX = "insertvalue_instruction_index";

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

static const string GEP = "getelementptr_instruction";
static const string GEP_INBOUNDS = "getelementptr_instruction_inbounds";
static const string GEP_BASE = "getelementptr_instruction_base";
static const string GEP_NINDICES = "getelementptr_instruction_nindices";
static const string GEP_INDEX = "getelementptr_instruction_index";

/*
 * Conversion instructions
 * (trunc, zext, sext, fptrunc, fpext, fptoui, fptosi, sitofp,
 * uitofp, sitofp, ptrtoint, inttoptr, bitcast, addrspacecast)
 */
static const string TRUNC = "trunc_instruction";
static const string TRUNC_FROM = "trunc_instruction_from";
static const string TRUNC_FROM_TYPE = "trunc_instruction_from_type";
static const string TRUNC_TO_TYPE = "trunc_instruction_to_type";

static const string ZEXT = "zext_instruction";
static const string ZEXT_FROM = "zext_instruction_from";
static const string ZEXT_FROM_TYPE = "zext_instruction_from_type";
static const string ZEXT_TO_TYPE = "zext_instruction_to_type";

static const string SEXT = "sext_instruction";
static const string SEXT_FROM = "sext_instruction_from";
static const string SEXT_FROM_TYPE = "sext_instruction_from_type";
static const string SEXT_TO_TYPE = "sext_instruction_to_type";

static const string FPTRUNC = "fptrunc_instruction";
static const string FPTRUNC_FROM = "fptrunc_instruction_from";
static const string FPTRUNC_FROM_TYPE = "fptrunc_instruction_from_type";
static const string FPTRUNC_TO_TYPE = "fptrunc_instruction_to_type";

static const string FPEXT = "fpext_instruction";
static const string FPEXT_FROM = "fpext_instruction_from";
static const string FPEXT_FROM_TYPE = "fpext_instruction_from_type";
static const string FPEXT_TO_TYPE = "fpext_instruction_to_type";

static const string FPTOUI = "fptoui_instruction";
static const string FPTOUI_FROM = "fptoui_instruction_from";
static const string FPTOUI_FROM_TYPE = "fptoui_instruction_from_type";
static const string FPTOUI_TO_TYPE = "fptoui_instruction_to_type";

static const string FPTOSI = "fptosi_instruction";
static const string FPTOSI_FROM = "fptosi_instruction_from";
static const string FPTOSI_FROM_TYPE = "fptosi_instruction_from_type";
static const string FPTOSI_TO_TYPE = "fptosi_instruction_to_type";

static const string UITOFP = "uitofp_instruction";
static const string UITOFP_FROM = "uitofp_instruction_from";
static const string UITOFP_FROM_TYPE = "uitofp_instruction_from_type";
static const string UITOFP_TO_TYPE = "uitofp_instruction_to_type";

static const string SITOFP = "sitofp_instruction";
static const string SITOFP_FROM = "sitofp_instruction_from";
static const string SITOFP_FROM_TYPE = "sitofp_instruction_from_type";
static const string SITOFP_TO_TYPE = "sitofp_instruction_to_type";

static const string PTRTOINT = "ptrtoint_instruction";
static const string PTRTOINT_FROM = "ptrtoint_instruction_from";
static const string PTRTOINT_FROM_TYPE = "ptrtoint_instruction_from_type";
static const string PTRTOINT_TO_TYPE = "ptrtoint_instruction_to_type";

static const string INTTOPTR = "inttoptr_instruction";
static const string INTTOPTR_FROM = "inttoptr_instruction_from";
static const string INTTOPTR_FROM_TYPE = "inttoptr_instruction_from_type";
static const string INTTOPTR_TO_TYPE = "inttoptr_instruction_to_type";

static const string BITCAST = "bitcast_instruction";
static const string BITCAST_FROM = "bitcast_instruction_from";
static const string BITCAST_FROM_TYPE = "bitcast_instruction_from_type";
static const string BITCAST_TO_TYPE = "bitcast_instruction_to_type";

static const string ADDRSPACAECAST = "addrspacecast_instruction";
static const string ADDRSPACAECAST_FROM = "addrspacecast_instruction_from";
static const string ADDRSPACAECAST_FROM_TYPE = "addrspacecast_instruction_from_type";
static const string ADDRSPACAECAST_TO_TYPE = "addrspacecast_instruction_to_type";

/*
 * Other instructions
 * (icmp, fcmp, phi)
 */
static const string ICMP = "icmp_instruction";
static const string ICMP_COND = "icmp_instruction_condition";
static const string ICMP_FIRST = "icmp_instruction_first_operand";
static const string ICMP_SECOND = "icmp_instruction_second_operand";

static const string FCMP = "fcmp_instruction";
static const string FCMP_COND = "fcmp_instruction_condition";
static const string FCMP_FIRST = "fcmp_instruction_first_operand";
static const string FCMP_SECOND = "fcmp_instruction_second_operand";

static const string PHI = "phi_instruction";
static const string PHI_TYPE = "phi_instruction_type";
static const string PHI_PAIR_VAL = "phi_instruction_pair_val";
static const string PHI_PAIR_LABEL = "phi_instruction_pair_label";
static const string PHI_NPAIRS = "phi_instruction_npairs";

static const string SELECT = "select_instruction";
static const string SELECT_COND = "select_instruction_condition";
static const string SELECT_TRUE = "select_instruction_true";
static const string SELECT_FALSE = "select_instruction_false";

static const string VAARG = "va_arg_instruction";
static const string VAARG_TYPE = "va_arg_instruction_type";

static const string LP = "landingpad";
static const string LANDINGPAD = "landingpad_instruction";
static const string LANDINGPAD_CLEANUP = "landingpad_instruction_cleanup";
static const string LANDINGPAD_TYPE = "landingpad_instruction_type";
static const string LANDINGPAD_PERSONALITY = "landingpad_instruction_pers_fn";
static const string LANDINGPAD_CLAUSE = "landingpad_instruction_clause";
static const string LANDINGPAD_NCLAUSES = "landingpad_instruction_nclauses";
static const string CLAUSE = "clause";
static const string CATCH_CLAUSE = "catch_clause";
static const string FILTER_CLAUSE = "filter_clause";
static const string CLAUSE_BY_INDEX = "clause_by_index";
static const string CATCH_CLAUSE_ARG = "catch_clause_arg";
static const string FILTER_CLAUSE_ARG = "filter_clause_arg";

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
    errs() << name << "(" << id << ", "; 
    errs() << arg; 
    errs() << ").\n";
}

template<typename T>
void print_fact(std::string name, unsigned long id, int index, T arg)
{
    errs() << name << "(" << id << ", "; 
    errs() << index << ", " ;
    errs() << arg;
    errs() << ").\n";
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

inline void print_fact(std::string name, unsigned long id, int index, Value *arg)
{
    errs() << name << "(" << id << ", ";
    errs() << index << ", ";
    errs() << *arg;
    errs() << ").\n";
}

inline void print_new() {
    errs() << "\n";
}
