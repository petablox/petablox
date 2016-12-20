using namespace llvm;

void translateAlloca(unsigned long id, AllocaInst *alloca_inst);
void translateLoad(unsigned long id, LoadInst *load_inst);
void translateStore(unsigned long id, StoreInst *store_inst);
void translateFence(unsigned long id, FenceInst *fence_inst);
void translateCmpXchg(unsigned long id, AtomicCmpXchgInst *cmpxchg_inst);
void translateAtomicRmw(unsigned long id, AtomicRMWInst *rmw_inst);
