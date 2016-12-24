using namespace llvm;

void translateExtractElement(unsigned long id, ExtractElementInst *ee_inst);
void translateInsertElement(unsigned long id, InsertElementInst *ie_inst);
void translateShuffleVector(unsigned long id, ShuffleVectorInst *sv_inst);

