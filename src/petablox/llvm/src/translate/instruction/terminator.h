using namespace llvm;

void translateReturn(unsigned long id, ReturnInst *ret_inst);
void translateBr(unsigned long id, BranchInst *br_inst);
void translateUnreachable(unsigned long id);

