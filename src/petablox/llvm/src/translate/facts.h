using namespace llvm;
using namespace std;

static const string GLOBAL_VAR = "global variable";
static const string GLOBAL_TYPE = "global_variable_type";
static const string GLOBAL_NAME = "global_variable_name";
static const string GLOBAL_ALIGN = "global_variable_alignment";
static const string GLOBAL_LINKAGE_TYPE = "global_variable_linkage_type";
static const string GLOBAL_VIS = "global_variable_visibility";
static const string GLOBAL_INIT = "global_variable_initializer";
static const string GLOBAL_SEC = "global_variable_section";
static const string GLOBAL_THREAD_LOCAL = "global_variable_threadlocal_mode";
static const string GLOBAL_CONSTANT = "global_variable_constant";

static const string OPERAND = "operand";
static const string CONSTANT = "constant";
static const string CONSTANT_TYPE = "constant_type";
static const string CONSTANT_VAL = "constant_value";
static const string VARIABLE = "variable";
static const string VARIABLE_TYPE = "variable_type";

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

template<typename T>
void print_fact(std::string name, T value)
{
    errs() << name << "(" << value << ").\n";
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
