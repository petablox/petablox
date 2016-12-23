using namespace llvm;

void translateCmp(unsigned long id, CmpInst *cmp_inst);
void translatePhi(unsigned long id, PHINode *phi);
void translateSelect(unsigned long id, SelectInst *select_inst);
