#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/Module.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/Transforms/Utils/BasicBlockUtils.h"
#include "llvm/IR/DebugInfoMetadata.h"
#include <set>
#include <string>
#include <map>
#include <iostream>
#include <fstream>
//#include "translate/instruction/facts.h"
#include "translate/facts.h"
//#include "translate/instruction/instruction.h"
#include "translate/instruction/terminator.h"
#include "translate/instruction/binop.h"
#include "translate/instruction/vector.h"
#include "translate/instruction/aggregate.h"
#include "translate/instruction/memory.h"
#include "translate/instruction/conversion.h"
#include "translate/instruction/other.h"
#include "translate/function.h"
#include "translate/operand.h"
#include "translate/global.h"
#include "instruction_map.h"

using namespace std;
using namespace llvm;

set<unsigned long> instructions;
map<unsigned long, int> instruction_ids;
set<unsigned long> types;
map<unsigned long, int> type_ids;
int type_id = 17;

namespace {
    struct SkeletonPass : public FunctionPass {

        static char ID;
        SkeletonPass() : FunctionPass(ID) {}

        set<string> binops;
        map<string, BinOp> binops_map;

        int instruction_id = 1;

        void populate_binops() {
            // Add binary operations to set
            binops.insert("add");
            binops.insert("fadd");
            binops.insert("sub");
            binops.insert("fsub");
            binops.insert("mul");
            binops.insert("fmul");
            binops.insert("udiv");
            binops.insert("sdiv");
            binops.insert("fdiv");
            binops.insert("urem");
            binops.insert("srem");
            binops.insert("frem");

            // Add bitwise binary operations to set
            binops.insert("shl");
            binops.insert("lshr");
            binops.insert("ashr");
            binops.insert("and");
            binops.insert("or");
            binops.insert("xor");
        }

        void populate_binops_map() {
            // Add binary operations to set
            binops_map["add"] = add;
            binops_map["fadd"] = fadd;
            binops_map["sub"] = sub;
            binops_map["fsub"] = fsub;
            binops_map["mul"] = mul;
            binops_map["fmul"] = fmul;
            binops_map["udiv"] = udiv;
            binops_map["sdiv"] = sdiv;
            binops_map["fdiv"] = fdiv;
            binops_map["urem"] = urem;
            binops_map["srem"] = srem;
            binops_map["frem"] = frem;

            // Add bitwise binary operations to set
            binops_map["shl"] = shl;
            binops_map["lshr"] = lshr;
            binops_map["ashr"] = ashr;
            binops_map["and"] = and_;
            binops_map["or"] = or_;
            binops_map["xor"] = xor_;
        }

        

        void analyzeInstruction(Instruction &I, unsigned long id) {
            
            //outs() << "% (" << id << ")\n ";
            //errs() << "% (" << id << ")\n ";
            //errs() << *I.getType();
            //I.dump();
            //errs() << id << " is " << instruction_ids[id] << "\n";
            //errs() << id << " is " << operand_ids[id] << "\n";
            errs() << "(" << operand_ids[id] << ") ";
            errs() << *(&I) << "\n";

            //outs() << "instruction(" << instruction_ids[id] << ").\n";
            print_fact("instruction", id);
            if (DILocation *loc = I.getDebugLoc()) {
                string filename = loc->getFilename().str();
                unsigned line = loc->getLine();
                print_fact("instruction_line_number", id, line);
                print_fact("integer", line);
            }
            for (auto operands = I.value_op_begin(); operands != I.value_op_end(); operands++) {
                Value *oper = *operands;
                translateOperand(oper);
                if (!dyn_cast<BasicBlock>(oper)) {
                    print_fact("instruction_uses", id, (unsigned long) oper);
                }
            }
            /*
             * Terminator instructions:
             * ret, br, switch, indirectbr, invoke, resume, catchswitch,
             * catchret, cleanupret, unreachable
             *
             * (http://llvm.org/docs/LangRef.html#terminator-instructions)
             */
            if (ReturnInst *ret_inst = dyn_cast<ReturnInst>(&I)) {
                translateReturn(id, ret_inst);
            } 

            if (BranchInst *br_inst = dyn_cast<BranchInst>(&I)) {
                translateBr(id, br_inst);
            }

            if (SwitchInst *switch_inst = dyn_cast<SwitchInst>(&I)) {
                translateSwitch(id, switch_inst);
            }

            if (IndirectBrInst *br_inst = dyn_cast<IndirectBrInst>(&I)) {
                translateIndirectBr(id, br_inst);
            }

            if (InvokeInst *invoke_inst = dyn_cast<InvokeInst>(&I)) {
                translateInvoke(id, invoke_inst);
            }

            if (ResumeInst *resume_inst = dyn_cast<ResumeInst>(&I)) {
                translateResume(id, resume_inst);
            }

            if (dyn_cast<UnreachableInst>(&I)) {
                translateUnreachable(id);
            }
            
            /*
             * Binary operations:
             * add, fadd, sub, fsub, mul, fmul, udiv, sdiv, fdiv,
             * urem, srem, frem
             *
             * (http://llvm.org/docs/LangRef.html#binary-operations)
             *
             * Bitwise binary operations:
             * shl, lshr, ashr, and, or, xor
             *
             * (http://llvm.org/docs/LangRef.html#bitwise-binary-operations)
             */
            const char *opcode = I.getOpcodeName();

            if (binops.find(opcode) != binops.end()) {
                translateBinOp(binops_map[opcode], I, id);
            }

            /*
             * Vector Operations:
             * extractelement, insertelement, shufflevector
             *
             * (http://llvm.org/docs/LangRef.html#vector-operations)
             */
            if (ExtractElementInst *ee_inst = dyn_cast<ExtractElementInst>(&I)) {
                translateExtractElement(id, ee_inst);
            }

            if (InsertElementInst *ie_inst = dyn_cast<InsertElementInst>(&I)) {
                translateInsertElement(id, ie_inst);
            }

            if (ShuffleVectorInst *sv_inst = dyn_cast<ShuffleVectorInst>(&I)) {
                translateShuffleVector(id, sv_inst);
            }
            /*
             * Aggregate Operations:
             * extractvalue, insertvalue
             *
             * (http://llvm.org/docs/LangRef.html#aggregate-operations)
             */
            if (ExtractValueInst *ev_inst = dyn_cast<ExtractValueInst>(&I)) {
                translateExtractValue(id, ev_inst);
            }

            if (InsertValueInst *iv_inst = dyn_cast<InsertValueInst>(&I)) {
                translateInsertValue(id, iv_inst);
            }

            /*
             * Memory Access and Addressing Operations:
             * alloca, load, store, fence, cmpxchg, atomicrmw, getelementptr
             *
             * (http://llvm.org/docs/LangRef.html#memory-access-and-addressing-operations)
             */
            if (AllocaInst *alloca_inst = dyn_cast<AllocaInst>(&I)) {
                translateAlloca(id, alloca_inst);
            }

            if (LoadInst *load_inst = dyn_cast<LoadInst>(&I)) {
                translateLoad(id, load_inst);
            }

            if (StoreInst *store_inst = dyn_cast<StoreInst>(&I)) {
                translateStore(id, store_inst);
            }

            if (FenceInst *fence_inst = dyn_cast<FenceInst>(&I)) {
                translateFence(id, fence_inst);
            }

            if (AtomicCmpXchgInst *cmpxchg_inst = dyn_cast<AtomicCmpXchgInst>(&I)) {
                translateCmpXchg(id, cmpxchg_inst);
            }

            if (AtomicRMWInst *rmw_inst = dyn_cast<AtomicRMWInst>(&I)) {
                translateAtomicRmw(id, rmw_inst);
            }

            if (GetElementPtrInst *gep_inst = dyn_cast<GetElementPtrInst>(&I)) {
                translateGetElementPtr(id, gep_inst);
            }

            /*
             * Conversion instructions:
             * trunc, zext, sext, fptrunc, fpext, fptoui, fptosi,
             * uitofp, sitofp, ptrtoint, inttoptr, bitcast, addrspacecast
             *
             * (http://llvm.org/docs/LangRef.html#conversion-operations)
             */ 
            if (CastInst *conv_inst = dyn_cast<CastInst>(&I)) {
                translateConversion(id, conv_inst);
            }

            /*
             * Other instructions:
             * icmp, fcmp, phi, select, call, va_arg, landingpad, catchpad, cleanuppad
             *
             * (http://llvm.org/docs/LangRef.html#other-operations)
             */
            if (CmpInst *cmp_inst = dyn_cast<CmpInst>(&I)) {
                translateCmp(id, cmp_inst);
            }

            if (PHINode *phi = dyn_cast<PHINode>(&I)) {
                translatePhi(id, phi);
            }

            if (SelectInst *select_inst = dyn_cast<SelectInst>(&I)) {
                translateSelect(id, select_inst);
            }

            if (CallInst *call = dyn_cast<CallInst>(&I)) {
                translateCall(id, call);
            }

            if (VAArgInst *vaarg_inst = dyn_cast<VAArgInst>(&I)) {
                translateVAArg(id, vaarg_inst);
            }

            if (LandingPadInst *lp_inst = dyn_cast<LandingPadInst>(&I)) {
                translateLandingPad(id, lp_inst);
            }

        }

        // Runs analysis on all functions in the program
        virtual bool runOnFunction(Function &F) {
            outs() << "%% Analyzing function: " << F.getName().str() << "\n";
            errs() << F.getName().str() << ":\n";
            //F.dump();

            populate_binops();
            populate_binops_map();
            for (auto &B : F) {
                // For each instruction in a basic block
                for (Instruction &I : B) {
                    //instruction_ids[(unsigned long) &I] = instruction_id++;
                    //instructions.insert((unsigned long) &I);
                    operand_ids[(unsigned long) &I] = operand_id++;
                    operands.insert((unsigned long) &I);
                }
            }

            translateFunction(F, (unsigned long) &F);

            buildCFG(F);

            translateGlobals(F);

            // For each basic block in a function
            outs() << "\n%% Generating relations for each instruction\n";
            for (auto &B : F) {
                // For each instruction in a basic block

                //for (Instruction &I : B) {
                //    instruction_ids[(unsigned long) &I] = instruction_id++;
                //    instructions.insert((unsigned long) &I);
                //}

                for (Instruction &I : B) {
                    unsigned long inst_id = (unsigned long) &I;
                    analyzeInstruction(I, inst_id);
                }
            }

            errs() << "\n";

            return false;
        }
    };
}

char SkeletonPass::ID = 0;

// Automatically enable the pass.
// http://adriansampson.net/blog/clangpass.html
static void registerSkeletonPass(const PassManagerBuilder &,
        legacy::PassManagerBase &PM) {
    PM.add(new SkeletonPass());
}
static RegisterStandardPasses
RegisterMyPass(PassManagerBuilder::EP_EarlyAsPossible,
        registerSkeletonPass);
