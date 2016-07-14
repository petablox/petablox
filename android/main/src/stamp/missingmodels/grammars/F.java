package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
# (backward) escape flow

# sink
# u_c Ref2Sink l <- u_c argSinkFlowCtxt l
Ref2Sink :: argSinkFlowCtxt
# u_c Ref2Sink l <- u_c argArgFlowCtxt v_c _Sinkl2Ref l
Ref2Sink :: argArgFlowCtxt _Sinkl2Ref
# u_c Ref2Sink l <- u_c argArgPrimFlowCtxt v_c _Sinkl2Prim l
Ref2Sink :: argArgPrimFlowCtxt _Sinkl2Prim

# sinkPrim
# u_c Prim2Sink l <- u_c argSinkPrimFlowCtxt l
Prim2Sink :: argSinkPrimFlowCtxt
# u_c Prim2Sink l <- u_c argPrimArgFlowCtxt v_c _Sinkl2Ref l
Prim2Sink :: argPrimArgFlowCtxt _Sinkl2Ref
# u_c Prim2Sink l <- u_c flowPrimPrimCtxt v_c _Sinkl2Prim l
Prim2Sink :: flowPrimPrimCtxt _Sinkl2Prim

# complete flow

# flow
# s1 Src2Sink s2 <- s1 Src2Ref u_c Sink s2
Src2Sink :: Src2Ref Sink
# s1 Src2Sink s2 <- s1 Src2Prim u_c SinkPrim s2
Src2Sink :: Src2Prim SinkPrim

# (forward) src label flow

# label
# l Src2Obj o_c <- l srcArgFlowCtxt v_c flowsTo o_c
Src2Obj :: srcArgFlowCtxt flowsTo
# l Src2Obj o_c <- l srcRetFlowCtxt v_c flowsTo o_c
Src2Obj :: srcRetFlowCtxt flowsTo
# l Src2Obj o_c <- l Src2Ref u_c argArgTransferCtxt v_c _flowsTo o_c
Src2Obj :: Src2Ref argArgTransferCtxt _flowsTo
# l Src2Obj o_c <- l Src2Ref u_c argRetTransferCtxt v_c _flowsTo o_c
Src2Obj :: Src2Ref argRetTransferCtxt _flowsTo
# l Src2Obj o_c <- l Src2Prim u_c argPrimArgTransferCtxt v_c _flowsTo o_c
Src2Obj :: Src2Prim argPrimArgTransferCtxt _flowsTo
# l Src2Obj o_c <- l Src2Prim u_c argPrimRetTransferCtxt v_c _flowsTo o_c
Src2Obj :: Src2Prim argPrimRetTransferCtxt _flowsTo

# labelRef
# l Src2Ref v_c <- l Src2Obj o_c flowsTo v_c
Src2Ref :: Src2Obj flowsTo

# labelPrim
# l Src2Prim v_c <- l srcRetPrimFlowCtxt v_c
Src2Prim :: srcRetPrimFlowCtxt
# l Src2Prim v_c <- l Src2Ref u_c argRetPrimTransferCtxt v_c
Src2Prim :: Src2Ref argRetPrimTransferCtxt
# l Src2Prim v_c <- l Src2Prim u_c argPrimRetPrimTransferCtxt v_c
Src2Prim :: Src2Prim argPrimRetPrimTransferCtxt

# labelPrim, labelPrimFld, labelPrimFldStat
# l Src2Prim v_c <- l Src2Prim u_c assignPrimCtxt v_c
Src2Prim :: Src2Prim assignPrimCtxt
# l Src2PrimFld[f] o_c <- l Src2Prim v_c storePrimCtxt[f] u_c _flowsTo o_c
Src2PrimFld[f] :: Src2Prim storePrimCtxt[f] _flowsTo
# l Src2Prim y_c <- l Src2PrimFld[f] o_c flowsTo x_c loadPrimCtxt[f] y_c
Src2Prim :: Src2PrimFld[f] flowsTo loadPrimCtxt[f]
# l Src2PrimStatFld f <- l Src2Prim v_c storeStatPrimCtxt f
Src2PrimStatFld :: Src2Prim storeStatPrimCtxt
# l Src2Prim y_c <- l Src2PrimstatFld f loadStatPrimCtxt y_c
Src2Prim :: Src2PrimstatFld loadStatPrimCtxt

# labelPrim param/return
# l Src2Prim u_d <- l Src2Prim v_c assignArgPrimCtxt u_d
Src2Prim :: Src2Prim assignArgPrimCtxt
# l Src2Prim u_c <- l Src2Prim v_d assignRetPrimCtxt u_c
Src2Prim :: Src2Prim assignRetPrimCtxt

# (forward) sinkl label flow

# label
# l Sinkl2Obj o_c <- l sinklArgFlowCtxt v_c flowsTo o_c
Sinkl2Obj :: sinklArgFlowCtxt flowsTo
# l Sinkl2Obj o_c <- l sinklRetFlowCtxt v_c flowsTo o_c
Sinkl2Obj :: sinklRetFlowCtxt flowsTo
# l Sinkl2Obj o_c <- l Sinkl2Ref u_c argArgTransferCtxt v_c _flowsTo o_c
Sinkl2Obj :: Sinkl2Ref argArgTransferCtxt _flowsTo
# l Sinkl2Obj o_c <- l Sinkl2Ref u_c argRetTransferCtxt v_c _flowsTo o_c
Sinkl2Obj :: Sinkl2Ref argRetTransferCtxt _flowsTo
# l Sinkl2Obj o_c <- l Sinkl2Prim u_c argPrimArgTransferCtxt v_c _flowsTo o_c
Sinkl2Obj :: Sinkl2Prim argPrimArgTransferCtxt _flowsTo
# l Sinkl2Obj o_c <- l Sinkl2Prim u_c argPrimRetTransferCtxt v_c _flowsTo o_c
Sinkl2Obj :: Sinkl2Prim argPrimRetTransferCtxt _flowsTo

# labelRef
# l Sinkl2Ref v_c <- l Sinkl2Obj o_c flowsTo v_c
Sinkl2Ref :: Sinkl2Obj flowsTo

# labelPrim
# l Sinkl2Prim v_c <- l sinklRetPrimFlowCtxt v_c
Sinkl2Prim :: sinklRetPrimFlowCtxt
# l Sinkl2Prim v_c <- l Sinkl2Ref u_c argRetPrimTransferCtxt v_c
Sinkl2Prim :: Sinkl2Ref argRetPrimTransferCtxt
# l Sinkl2Prim v_c <- l Sinkl2Prim u_c argPrimRetPrimTransferCtxt v_c
Sinkl2Prim :: Sinkl2Prim argPrimRetPrimTransferCtxt

# labelPrim, labelPrimFld, labelPrimFldStat
# l Sinkl2Prim v_c <- l Sinkl2Prim u_c assignPrimCtxt v_c
Sinkl2Prim :: Sinkl2Prim assignPrimCtxt
# l Sinkl2PrimFld[f] o_c <- l Sinkl2Prim v_c storePrimCtxt[f] u_c _flowsTo o_c
Sinkl2PrimFld[f] :: Sinkl2Prim storePrimCtxt[f] _flowsTo
# l Sinkl2Prim y_c <- l Sinkl2PrimFld[f] o_c flowsTo x_c loadPrimCtxt[f] y_c
Sinkl2Prim :: Sinkl2PrimFld[f] flowsTo loadPrimCtxt[f]
# l Sinkl2PrimStatFld f <- l Sinkl2Prim v_c storeStatPrimCtxt f
Sinkl2PrimStatFld :: Sinkl2Prim storeStatPrimCtxt
# l Sinkl2Prim y_c <- l Sinkl2PrimstatFld f loadStatPrimCtxt y_c
Sinkl2Prim :: Sinkl2PrimstatFld loadStatPrimCtxt

# labelPrim param/return
# l Sinkl2Prim u_d <- l Sinkl2Prim v_c assignArgPrimCtxt u_d
Sinkl2Prim :: Sinkl2Prim assignArgPrimCtxt
# l Sinkl2Prim u_c <- l Sinkl2Prim v_d assignRetPrimCtxt u_c
Sinkl2Prim :: Sinkl2Prim assignRetPrimCtxt

*/

/* Normalized Grammar:
Ref2Sink:
	Ref2Sink :: argSinkFlowCtxt
	Ref2Sink :: argArgFlowCtxt _Sinkl2Ref
	Ref2Sink :: argArgPrimFlowCtxt _Sinkl2Prim
%9:
	%9 :: Sinkl2Prim argPrimRetTransferCtxt
%8:
	%8 :: Sinkl2Prim argPrimArgTransferCtxt
Prim2Sink:
	Prim2Sink :: argSinkPrimFlowCtxt
	Prim2Sink :: argPrimArgFlowCtxt _Sinkl2Ref
	Prim2Sink :: flowPrimPrimCtxt _Sinkl2Prim
%5:
	%5[i] :: Src2PrimFld[i] flowsTo
%4:
	%4[i] :: Src2Prim storePrimCtxt[i]
%7:
	%7 :: Sinkl2Ref argRetTransferCtxt
%6:
	%6 :: Sinkl2Ref argArgTransferCtxt
%1:
	%1 :: Src2Ref argRetTransferCtxt
%0:
	%0 :: Src2Ref argArgTransferCtxt
%3:
	%3 :: Src2Prim argPrimRetTransferCtxt
%2:
	%2 :: Src2Prim argPrimArgTransferCtxt
Sinkl2Obj:
	Sinkl2Obj :: sinklArgFlowCtxt flowsTo
	Sinkl2Obj :: sinklRetFlowCtxt flowsTo
	Sinkl2Obj :: %6 _flowsTo
	Sinkl2Obj :: %7 _flowsTo
	Sinkl2Obj :: %8 _flowsTo
	Sinkl2Obj :: %9 _flowsTo
Src2Sink:
	Src2Sink :: Src2Ref Sink
	Src2Sink :: Src2Prim SinkPrim
Src2Ref:
	Src2Ref :: Src2Obj flowsTo
Src2PrimStatFld:
	Src2PrimStatFld :: Src2Prim storeStatPrimCtxt
Sinkl2PrimStatFld:
	Sinkl2PrimStatFld :: Sinkl2Prim storeStatPrimCtxt
Src2PrimFld:
	Src2PrimFld[i] :: %4[i] _flowsTo
Sinkl2Prim:
	Sinkl2Prim :: sinklRetPrimFlowCtxt
	Sinkl2Prim :: Sinkl2Ref argRetPrimTransferCtxt
	Sinkl2Prim :: Sinkl2Prim argPrimRetPrimTransferCtxt
	Sinkl2Prim :: Sinkl2Prim assignPrimCtxt
	Sinkl2Prim :: %11[i] loadPrimCtxt[i]
	Sinkl2Prim :: Sinkl2PrimstatFld loadStatPrimCtxt
	Sinkl2Prim :: Sinkl2Prim assignArgPrimCtxt
	Sinkl2Prim :: Sinkl2Prim assignRetPrimCtxt
Sinkl2PrimFld:
	Sinkl2PrimFld[i] :: %10[i] _flowsTo
Src2Prim:
	Src2Prim :: srcRetPrimFlowCtxt
	Src2Prim :: Src2Ref argRetPrimTransferCtxt
	Src2Prim :: Src2Prim argPrimRetPrimTransferCtxt
	Src2Prim :: Src2Prim assignPrimCtxt
	Src2Prim :: %5[i] loadPrimCtxt[i]
	Src2Prim :: Src2PrimstatFld loadStatPrimCtxt
	Src2Prim :: Src2Prim assignArgPrimCtxt
	Src2Prim :: Src2Prim assignRetPrimCtxt
Sinkl2Ref:
	Sinkl2Ref :: Sinkl2Obj flowsTo
%11:
	%11[i] :: Sinkl2PrimFld[i] flowsTo
%10:
	%10[i] :: Sinkl2Prim storePrimCtxt[i]
Src2Obj:
	Src2Obj :: srcArgFlowCtxt flowsTo
	Src2Obj :: srcRetFlowCtxt flowsTo
	Src2Obj :: %0 _flowsTo
	Src2Obj :: %1 _flowsTo
	Src2Obj :: %2 _flowsTo
	Src2Obj :: %3 _flowsTo
*/

/* Reverse Productions:
sinklRetPrimFlowCtxt:
	sinklRetPrimFlowCtxt => Sinkl2Prim
argSinkFlowCtxt:
	argSinkFlowCtxt => Ref2Sink
assignPrimCtxt:
	assignPrimCtxt + (Src2Prim *) => Src2Prim
	assignPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim
argRetPrimTransferCtxt:
	argRetPrimTransferCtxt + (Src2Ref *) => Src2Prim
	argRetPrimTransferCtxt + (Sinkl2Ref *) => Sinkl2Prim
argArgTransferCtxt:
	argArgTransferCtxt + (Src2Ref *) => %0
	argArgTransferCtxt + (Sinkl2Ref *) => %6
%9:
	%9 + (* _flowsTo) => Sinkl2Obj
%8:
	%8 + (* _flowsTo) => Sinkl2Obj
argSinkPrimFlowCtxt:
	argSinkPrimFlowCtxt => Prim2Sink
%5:
	%5[i] + (* loadPrimCtxt[i]) => Src2Prim
%4:
	%4[i] + (* _flowsTo) => Src2PrimFld[i]
%7:
	%7 + (* _flowsTo) => Sinkl2Obj
%6:
	%6 + (* _flowsTo) => Sinkl2Obj
%1:
	%1 + (* _flowsTo) => Src2Obj
%0:
	%0 + (* _flowsTo) => Src2Obj
%3:
	%3 + (* _flowsTo) => Src2Obj
%2:
	%2 + (* _flowsTo) => Src2Obj
argArgFlowCtxt:
	argArgFlowCtxt + (* _Sinkl2Ref) => Ref2Sink
argRetTransferCtxt:
	argRetTransferCtxt + (Src2Ref *) => %1
	argRetTransferCtxt + (Sinkl2Ref *) => %7
SinkPrim:
	SinkPrim + (Src2Prim *) => Src2Sink
Src2PrimstatFld:
	Src2PrimstatFld + (* loadStatPrimCtxt) => Src2Prim
Sinkl2PrimstatFld:
	Sinkl2PrimstatFld + (* loadStatPrimCtxt) => Sinkl2Prim
Sink:
	Sink + (Src2Ref *) => Src2Sink
assignRetPrimCtxt:
	assignRetPrimCtxt + (Src2Prim *) => Src2Prim
	assignRetPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim
srcRetPrimFlowCtxt:
	srcRetPrimFlowCtxt => Src2Prim
flowPrimPrimCtxt:
	flowPrimPrimCtxt + (* _Sinkl2Prim) => Prim2Sink
storePrimCtxt:
	storePrimCtxt[i] + (Src2Prim *) => %4[i]
	storePrimCtxt[i] + (Sinkl2Prim *) => %10[i]
Src2Ref:
	Src2Ref + (* Sink) => Src2Sink
	Src2Ref + (* argArgTransferCtxt) => %0
	Src2Ref + (* argRetTransferCtxt) => %1
	Src2Ref + (* argRetPrimTransferCtxt) => Src2Prim
argPrimRetTransferCtxt:
	argPrimRetTransferCtxt + (Src2Prim *) => %3
	argPrimRetTransferCtxt + (Sinkl2Prim *) => %9
srcRetFlowCtxt:
	srcRetFlowCtxt + (* flowsTo) => Src2Obj
sinklArgFlowCtxt:
	sinklArgFlowCtxt + (* flowsTo) => Sinkl2Obj
Src2PrimFld:
	Src2PrimFld[i] + (* flowsTo) => %5[i]
Sinkl2Prim:
	_Sinkl2Prim + (argArgPrimFlowCtxt *) => Ref2Sink
	_Sinkl2Prim + (flowPrimPrimCtxt *) => Prim2Sink
	Sinkl2Prim + (* argPrimArgTransferCtxt) => %8
	Sinkl2Prim + (* argPrimRetTransferCtxt) => %9
	Sinkl2Prim + (* argPrimRetPrimTransferCtxt) => Sinkl2Prim
	Sinkl2Prim + (* assignPrimCtxt) => Sinkl2Prim
	Sinkl2Prim + (* storePrimCtxt[i]) => %10[i]
	Sinkl2Prim + (* storeStatPrimCtxt) => Sinkl2PrimStatFld
	Sinkl2Prim + (* assignArgPrimCtxt) => Sinkl2Prim
	Sinkl2Prim + (* assignRetPrimCtxt) => Sinkl2Prim
loadPrimCtxt:
	loadPrimCtxt[i] + (%5[i] *) => Src2Prim
	loadPrimCtxt[i] + (%11[i] *) => Sinkl2Prim
argArgPrimFlowCtxt:
	argArgPrimFlowCtxt + (* _Sinkl2Prim) => Ref2Sink
storeStatPrimCtxt:
	storeStatPrimCtxt + (Src2Prim *) => Src2PrimStatFld
	storeStatPrimCtxt + (Sinkl2Prim *) => Sinkl2PrimStatFld
Sinkl2PrimFld:
	Sinkl2PrimFld[i] + (* flowsTo) => %11[i]
argPrimRetPrimTransferCtxt:
	argPrimRetPrimTransferCtxt + (Src2Prim *) => Src2Prim
	argPrimRetPrimTransferCtxt + (Sinkl2Prim *) => Sinkl2Prim
Src2Prim:
	Src2Prim + (* SinkPrim) => Src2Sink
	Src2Prim + (* argPrimArgTransferCtxt) => %2
	Src2Prim + (* argPrimRetTransferCtxt) => %3
	Src2Prim + (* argPrimRetPrimTransferCtxt) => Src2Prim
	Src2Prim + (* assignPrimCtxt) => Src2Prim
	Src2Prim + (* storePrimCtxt[i]) => %4[i]
	Src2Prim + (* storeStatPrimCtxt) => Src2PrimStatFld
	Src2Prim + (* assignArgPrimCtxt) => Src2Prim
	Src2Prim + (* assignRetPrimCtxt) => Src2Prim
Sinkl2Ref:
	_Sinkl2Ref + (argArgFlowCtxt *) => Ref2Sink
	_Sinkl2Ref + (argPrimArgFlowCtxt *) => Prim2Sink
	Sinkl2Ref + (* argArgTransferCtxt) => %6
	Sinkl2Ref + (* argRetTransferCtxt) => %7
	Sinkl2Ref + (* argRetPrimTransferCtxt) => Sinkl2Prim
%11:
	%11[i] + (* loadPrimCtxt[i]) => Sinkl2Prim
%10:
	%10[i] + (* _flowsTo) => Sinkl2PrimFld[i]
assignArgPrimCtxt:
	assignArgPrimCtxt + (Src2Prim *) => Src2Prim
	assignArgPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim
sinklRetFlowCtxt:
	sinklRetFlowCtxt + (* flowsTo) => Sinkl2Obj
argPrimArgFlowCtxt:
	argPrimArgFlowCtxt + (* _Sinkl2Ref) => Prim2Sink
loadStatPrimCtxt:
	loadStatPrimCtxt + (Src2PrimstatFld *) => Src2Prim
	loadStatPrimCtxt + (Sinkl2PrimstatFld *) => Sinkl2Prim
flowsTo:
	flowsTo + (srcArgFlowCtxt *) => Src2Obj
	flowsTo + (srcRetFlowCtxt *) => Src2Obj
	_flowsTo + (%0 *) => Src2Obj
	_flowsTo + (%1 *) => Src2Obj
	_flowsTo + (%2 *) => Src2Obj
	_flowsTo + (%3 *) => Src2Obj
	flowsTo + (Src2Obj *) => Src2Ref
	_flowsTo + (%4[i] *) => Src2PrimFld[i]
	flowsTo + (Src2PrimFld[i] *) => %5[i]
	flowsTo + (sinklArgFlowCtxt *) => Sinkl2Obj
	flowsTo + (sinklRetFlowCtxt *) => Sinkl2Obj
	_flowsTo + (%6 *) => Sinkl2Obj
	_flowsTo + (%7 *) => Sinkl2Obj
	_flowsTo + (%8 *) => Sinkl2Obj
	_flowsTo + (%9 *) => Sinkl2Obj
	flowsTo + (Sinkl2Obj *) => Sinkl2Ref
	_flowsTo + (%10[i] *) => Sinkl2PrimFld[i]
	flowsTo + (Sinkl2PrimFld[i] *) => %11[i]
Sinkl2Obj:
	Sinkl2Obj + (* flowsTo) => Sinkl2Ref
Src2Obj:
	Src2Obj + (* flowsTo) => Src2Ref
srcArgFlowCtxt:
	srcArgFlowCtxt + (* flowsTo) => Src2Obj
argPrimArgTransferCtxt:
	argPrimArgTransferCtxt + (Src2Prim *) => %2
	argPrimArgTransferCtxt + (Sinkl2Prim *) => %8
*/

public class F extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 2:
  case 4:
  case 7:
  case 8:
  case 9:
  case 16:
  case 17:
  case 18:
  case 19:
  case 21:
  case 23:
  case 25:
  case 27:
  case 28:
  case 29:
  case 30:
  case 32:
  case 34:
  case 37:
  case 39:
  case 40:
  case 41:
  case 43:
  case 44:
  case 49:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 55;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("Ref2Sink")) return 0;
  if (symbol.equals("argSinkFlowCtxt")) return 1;
  if (symbol.equals("argArgFlowCtxt")) return 2;
  if (symbol.equals("Sinkl2Ref")) return 3;
  if (symbol.equals("argArgPrimFlowCtxt")) return 4;
  if (symbol.equals("Sinkl2Prim")) return 5;
  if (symbol.equals("Prim2Sink")) return 6;
  if (symbol.equals("argSinkPrimFlowCtxt")) return 7;
  if (symbol.equals("argPrimArgFlowCtxt")) return 8;
  if (symbol.equals("flowPrimPrimCtxt")) return 9;
  if (symbol.equals("Src2Sink")) return 10;
  if (symbol.equals("Src2Ref")) return 11;
  if (symbol.equals("Sink")) return 12;
  if (symbol.equals("Src2Prim")) return 13;
  if (symbol.equals("SinkPrim")) return 14;
  if (symbol.equals("Src2Obj")) return 15;
  if (symbol.equals("srcArgFlowCtxt")) return 16;
  if (symbol.equals("flowsTo")) return 17;
  if (symbol.equals("srcRetFlowCtxt")) return 18;
  if (symbol.equals("argArgTransferCtxt")) return 19;
  if (symbol.equals("%0")) return 20;
  if (symbol.equals("argRetTransferCtxt")) return 21;
  if (symbol.equals("%1")) return 22;
  if (symbol.equals("argPrimArgTransferCtxt")) return 23;
  if (symbol.equals("%2")) return 24;
  if (symbol.equals("argPrimRetTransferCtxt")) return 25;
  if (symbol.equals("%3")) return 26;
  if (symbol.equals("srcRetPrimFlowCtxt")) return 27;
  if (symbol.equals("argRetPrimTransferCtxt")) return 28;
  if (symbol.equals("argPrimRetPrimTransferCtxt")) return 29;
  if (symbol.equals("assignPrimCtxt")) return 30;
  if (symbol.equals("Src2PrimFld")) return 31;
  if (symbol.equals("storePrimCtxt")) return 32;
  if (symbol.equals("%4")) return 33;
  if (symbol.equals("loadPrimCtxt")) return 34;
  if (symbol.equals("%5")) return 35;
  if (symbol.equals("Src2PrimStatFld")) return 36;
  if (symbol.equals("storeStatPrimCtxt")) return 37;
  if (symbol.equals("Src2PrimstatFld")) return 38;
  if (symbol.equals("loadStatPrimCtxt")) return 39;
  if (symbol.equals("assignArgPrimCtxt")) return 40;
  if (symbol.equals("assignRetPrimCtxt")) return 41;
  if (symbol.equals("Sinkl2Obj")) return 42;
  if (symbol.equals("sinklArgFlowCtxt")) return 43;
  if (symbol.equals("sinklRetFlowCtxt")) return 44;
  if (symbol.equals("%6")) return 45;
  if (symbol.equals("%7")) return 46;
  if (symbol.equals("%8")) return 47;
  if (symbol.equals("%9")) return 48;
  if (symbol.equals("sinklRetPrimFlowCtxt")) return 49;
  if (symbol.equals("Sinkl2PrimFld")) return 50;
  if (symbol.equals("%10")) return 51;
  if (symbol.equals("%11")) return 52;
  if (symbol.equals("Sinkl2PrimStatFld")) return 53;
  if (symbol.equals("Sinkl2PrimstatFld")) return 54;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "Ref2Sink";
  case 1: return "argSinkFlowCtxt";
  case 2: return "argArgFlowCtxt";
  case 3: return "Sinkl2Ref";
  case 4: return "argArgPrimFlowCtxt";
  case 5: return "Sinkl2Prim";
  case 6: return "Prim2Sink";
  case 7: return "argSinkPrimFlowCtxt";
  case 8: return "argPrimArgFlowCtxt";
  case 9: return "flowPrimPrimCtxt";
  case 10: return "Src2Sink";
  case 11: return "Src2Ref";
  case 12: return "Sink";
  case 13: return "Src2Prim";
  case 14: return "SinkPrim";
  case 15: return "Src2Obj";
  case 16: return "srcArgFlowCtxt";
  case 17: return "flowsTo";
  case 18: return "srcRetFlowCtxt";
  case 19: return "argArgTransferCtxt";
  case 20: return "%0";
  case 21: return "argRetTransferCtxt";
  case 22: return "%1";
  case 23: return "argPrimArgTransferCtxt";
  case 24: return "%2";
  case 25: return "argPrimRetTransferCtxt";
  case 26: return "%3";
  case 27: return "srcRetPrimFlowCtxt";
  case 28: return "argRetPrimTransferCtxt";
  case 29: return "argPrimRetPrimTransferCtxt";
  case 30: return "assignPrimCtxt";
  case 31: return "Src2PrimFld";
  case 32: return "storePrimCtxt";
  case 33: return "%4";
  case 34: return "loadPrimCtxt";
  case 35: return "%5";
  case 36: return "Src2PrimStatFld";
  case 37: return "storeStatPrimCtxt";
  case 38: return "Src2PrimstatFld";
  case 39: return "loadStatPrimCtxt";
  case 40: return "assignArgPrimCtxt";
  case 41: return "assignRetPrimCtxt";
  case 42: return "Sinkl2Obj";
  case 43: return "sinklArgFlowCtxt";
  case 44: return "sinklRetFlowCtxt";
  case 45: return "%6";
  case 46: return "%7";
  case 47: return "%8";
  case 48: return "%9";
  case 49: return "sinklRetPrimFlowCtxt";
  case 50: return "Sinkl2PrimFld";
  case 51: return "%10";
  case 52: return "%11";
  case 53: return "Sinkl2PrimStatFld";
  case 54: return "Sinkl2PrimstatFld";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 1: /* argSinkFlowCtxt */
    /* argSinkFlowCtxt => Ref2Sink */
    addEdge(base.from, base.to, 0, base, false);
    break;
  case 2: /* argArgFlowCtxt */
    /* argArgFlowCtxt + (* _Sinkl2Ref) => Ref2Sink */
    for(Edge other : base.to.getInEdges(3)){
      addEdge(base.from, other.from, 0, base, other, false);
    }
    break;
  case 3: /* Sinkl2Ref */
    /* _Sinkl2Ref + (argArgFlowCtxt *) => Ref2Sink */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(other.from, base.from, 0, base, other, false);
    }
    /* _Sinkl2Ref + (argPrimArgFlowCtxt *) => Prim2Sink */
    for(Edge other : base.to.getInEdges(8)){
      addEdge(other.from, base.from, 6, base, other, false);
    }
    /* Sinkl2Ref + (* argArgTransferCtxt) => %6 */
    for(Edge other : base.to.getOutEdges(19)){
      addEdge(base.from, other.to, 45, base, other, false);
    }
    /* Sinkl2Ref + (* argRetTransferCtxt) => %7 */
    for(Edge other : base.to.getOutEdges(21)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    /* Sinkl2Ref + (* argRetPrimTransferCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    break;
  case 4: /* argArgPrimFlowCtxt */
    /* argArgPrimFlowCtxt + (* _Sinkl2Prim) => Ref2Sink */
    for(Edge other : base.to.getInEdges(5)){
      addEdge(base.from, other.from, 0, base, other, false);
    }
    break;
  case 5: /* Sinkl2Prim */
    /* _Sinkl2Prim + (argArgPrimFlowCtxt *) => Ref2Sink */
    for(Edge other : base.to.getInEdges(4)){
      addEdge(other.from, base.from, 0, base, other, false);
    }
    /* _Sinkl2Prim + (flowPrimPrimCtxt *) => Prim2Sink */
    for(Edge other : base.to.getInEdges(9)){
      addEdge(other.from, base.from, 6, base, other, false);
    }
    /* Sinkl2Prim + (* argPrimArgTransferCtxt) => %8 */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    /* Sinkl2Prim + (* argPrimRetTransferCtxt) => %9 */
    for(Edge other : base.to.getOutEdges(25)){
      addEdge(base.from, other.to, 48, base, other, false);
    }
    /* Sinkl2Prim + (* argPrimRetPrimTransferCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(29)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    /* Sinkl2Prim + (* assignPrimCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    /* Sinkl2Prim + (* storePrimCtxt[i]) => %10[i] */
    for(Edge other : base.to.getOutEdges(32)){
      addEdge(base.from, other.to, 51, base, other, true);
    }
    /* Sinkl2Prim + (* storeStatPrimCtxt) => Sinkl2PrimStatFld */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    /* Sinkl2Prim + (* assignArgPrimCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(40)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    /* Sinkl2Prim + (* assignRetPrimCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(41)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    break;
  case 7: /* argSinkPrimFlowCtxt */
    /* argSinkPrimFlowCtxt => Prim2Sink */
    addEdge(base.from, base.to, 6, base, false);
    break;
  case 8: /* argPrimArgFlowCtxt */
    /* argPrimArgFlowCtxt + (* _Sinkl2Ref) => Prim2Sink */
    for(Edge other : base.to.getInEdges(3)){
      addEdge(base.from, other.from, 6, base, other, false);
    }
    break;
  case 9: /* flowPrimPrimCtxt */
    /* flowPrimPrimCtxt + (* _Sinkl2Prim) => Prim2Sink */
    for(Edge other : base.to.getInEdges(5)){
      addEdge(base.from, other.from, 6, base, other, false);
    }
    break;
  case 11: /* Src2Ref */
    /* Src2Ref + (* Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(12)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Ref + (* argArgTransferCtxt) => %0 */
    for(Edge other : base.to.getOutEdges(19)){
      addEdge(base.from, other.to, 20, base, other, false);
    }
    /* Src2Ref + (* argRetTransferCtxt) => %1 */
    for(Edge other : base.to.getOutEdges(21)){
      addEdge(base.from, other.to, 22, base, other, false);
    }
    /* Src2Ref + (* argRetPrimTransferCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    break;
  case 12: /* Sink */
    /* Sink + (Src2Ref *) => Src2Sink */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 13: /* Src2Prim */
    /* Src2Prim + (* SinkPrim) => Src2Sink */
    for(Edge other : base.to.getOutEdges(14)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* argPrimArgTransferCtxt) => %2 */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* Src2Prim + (* argPrimRetTransferCtxt) => %3 */
    for(Edge other : base.to.getOutEdges(25)){
      addEdge(base.from, other.to, 26, base, other, false);
    }
    /* Src2Prim + (* argPrimRetPrimTransferCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(29)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    /* Src2Prim + (* assignPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    /* Src2Prim + (* storePrimCtxt[i]) => %4[i] */
    for(Edge other : base.to.getOutEdges(32)){
      addEdge(base.from, other.to, 33, base, other, true);
    }
    /* Src2Prim + (* storeStatPrimCtxt) => Src2PrimStatFld */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 36, base, other, false);
    }
    /* Src2Prim + (* assignArgPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(40)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    /* Src2Prim + (* assignRetPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(41)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    break;
  case 14: /* SinkPrim */
    /* SinkPrim + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 15: /* Src2Obj */
    /* Src2Obj + (* flowsTo) => Src2Ref */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    break;
  case 16: /* srcArgFlowCtxt */
    /* srcArgFlowCtxt + (* flowsTo) => Src2Obj */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 15, base, other, false);
    }
    break;
  case 17: /* flowsTo */
    /* flowsTo + (srcArgFlowCtxt *) => Src2Obj */
    for(Edge other : base.from.getInEdges(16)){
      addEdge(other.from, base.to, 15, base, other, false);
    }
    /* flowsTo + (srcRetFlowCtxt *) => Src2Obj */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 15, base, other, false);
    }
    /* _flowsTo + (%0 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(20)){
      addEdge(other.from, base.from, 15, base, other, false);
    }
    /* _flowsTo + (%1 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(22)){
      addEdge(other.from, base.from, 15, base, other, false);
    }
    /* _flowsTo + (%2 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(other.from, base.from, 15, base, other, false);
    }
    /* _flowsTo + (%3 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(26)){
      addEdge(other.from, base.from, 15, base, other, false);
    }
    /* flowsTo + (Src2Obj *) => Src2Ref */
    for(Edge other : base.from.getInEdges(15)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    /* _flowsTo + (%4[i] *) => Src2PrimFld[i] */
    for(Edge other : base.to.getInEdges(33)){
      addEdge(other.from, base.from, 31, base, other, true);
    }
    /* flowsTo + (Src2PrimFld[i] *) => %5[i] */
    for(Edge other : base.from.getInEdges(31)){
      addEdge(other.from, base.to, 35, base, other, true);
    }
    /* flowsTo + (sinklArgFlowCtxt *) => Sinkl2Obj */
    for(Edge other : base.from.getInEdges(43)){
      addEdge(other.from, base.to, 42, base, other, false);
    }
    /* flowsTo + (sinklRetFlowCtxt *) => Sinkl2Obj */
    for(Edge other : base.from.getInEdges(44)){
      addEdge(other.from, base.to, 42, base, other, false);
    }
    /* _flowsTo + (%6 *) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(45)){
      addEdge(other.from, base.from, 42, base, other, false);
    }
    /* _flowsTo + (%7 *) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 42, base, other, false);
    }
    /* _flowsTo + (%8 *) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(47)){
      addEdge(other.from, base.from, 42, base, other, false);
    }
    /* _flowsTo + (%9 *) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(48)){
      addEdge(other.from, base.from, 42, base, other, false);
    }
    /* flowsTo + (Sinkl2Obj *) => Sinkl2Ref */
    for(Edge other : base.from.getInEdges(42)){
      addEdge(other.from, base.to, 3, base, other, false);
    }
    /* _flowsTo + (%10[i] *) => Sinkl2PrimFld[i] */
    for(Edge other : base.to.getInEdges(51)){
      addEdge(other.from, base.from, 50, base, other, true);
    }
    /* flowsTo + (Sinkl2PrimFld[i] *) => %11[i] */
    for(Edge other : base.from.getInEdges(50)){
      addEdge(other.from, base.to, 52, base, other, true);
    }
    break;
  case 18: /* srcRetFlowCtxt */
    /* srcRetFlowCtxt + (* flowsTo) => Src2Obj */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 15, base, other, false);
    }
    break;
  case 19: /* argArgTransferCtxt */
    /* argArgTransferCtxt + (Src2Ref *) => %0 */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 20, base, other, false);
    }
    /* argArgTransferCtxt + (Sinkl2Ref *) => %6 */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 45, base, other, false);
    }
    break;
  case 20: /* %0 */
    /* %0 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 15, base, other, false);
    }
    break;
  case 21: /* argRetTransferCtxt */
    /* argRetTransferCtxt + (Src2Ref *) => %1 */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 22, base, other, false);
    }
    /* argRetTransferCtxt + (Sinkl2Ref *) => %7 */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    break;
  case 22: /* %1 */
    /* %1 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 15, base, other, false);
    }
    break;
  case 23: /* argPrimArgTransferCtxt */
    /* argPrimArgTransferCtxt + (Src2Prim *) => %2 */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    /* argPrimArgTransferCtxt + (Sinkl2Prim *) => %8 */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    break;
  case 24: /* %2 */
    /* %2 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 15, base, other, false);
    }
    break;
  case 25: /* argPrimRetTransferCtxt */
    /* argPrimRetTransferCtxt + (Src2Prim *) => %3 */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 26, base, other, false);
    }
    /* argPrimRetTransferCtxt + (Sinkl2Prim *) => %9 */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 48, base, other, false);
    }
    break;
  case 26: /* %3 */
    /* %3 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 15, base, other, false);
    }
    break;
  case 27: /* srcRetPrimFlowCtxt */
    /* srcRetPrimFlowCtxt => Src2Prim */
    addEdge(base.from, base.to, 13, base, false);
    break;
  case 28: /* argRetPrimTransferCtxt */
    /* argRetPrimTransferCtxt + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* argRetPrimTransferCtxt + (Sinkl2Ref *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 29: /* argPrimRetPrimTransferCtxt */
    /* argPrimRetPrimTransferCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* argPrimRetPrimTransferCtxt + (Sinkl2Prim *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 30: /* assignPrimCtxt */
    /* assignPrimCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* assignPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 31: /* Src2PrimFld */
    /* Src2PrimFld[i] + (* flowsTo) => %5[i] */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 35, base, other, true);
    }
    break;
  case 32: /* storePrimCtxt */
    /* storePrimCtxt[i] + (Src2Prim *) => %4[i] */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 33, base, other, true);
    }
    /* storePrimCtxt[i] + (Sinkl2Prim *) => %10[i] */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 51, base, other, true);
    }
    break;
  case 33: /* %4 */
    /* %4[i] + (* _flowsTo) => Src2PrimFld[i] */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 31, base, other, true);
    }
    break;
  case 34: /* loadPrimCtxt */
    /* loadPrimCtxt[i] + (%5[i] *) => Src2Prim */
    for(Edge other : base.from.getInEdges(35)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* loadPrimCtxt[i] + (%11[i] *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(52)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 35: /* %5 */
    /* %5[i] + (* loadPrimCtxt[i]) => Src2Prim */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    break;
  case 37: /* storeStatPrimCtxt */
    /* storeStatPrimCtxt + (Src2Prim *) => Src2PrimStatFld */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 36, base, other, false);
    }
    /* storeStatPrimCtxt + (Sinkl2Prim *) => Sinkl2PrimStatFld */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    break;
  case 38: /* Src2PrimstatFld */
    /* Src2PrimstatFld + (* loadStatPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getOutEdges(39)){
      addEdge(base.from, other.to, 13, base, other, false);
    }
    break;
  case 39: /* loadStatPrimCtxt */
    /* loadStatPrimCtxt + (Src2PrimstatFld *) => Src2Prim */
    for(Edge other : base.from.getInEdges(38)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* loadStatPrimCtxt + (Sinkl2PrimstatFld *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(54)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 40: /* assignArgPrimCtxt */
    /* assignArgPrimCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* assignArgPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 41: /* assignRetPrimCtxt */
    /* assignRetPrimCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 13, base, other, false);
    }
    /* assignRetPrimCtxt + (Sinkl2Prim *) => Sinkl2Prim */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 42: /* Sinkl2Obj */
    /* Sinkl2Obj + (* flowsTo) => Sinkl2Ref */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 3, base, other, false);
    }
    break;
  case 43: /* sinklArgFlowCtxt */
    /* sinklArgFlowCtxt + (* flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 42, base, other, false);
    }
    break;
  case 44: /* sinklRetFlowCtxt */
    /* sinklRetFlowCtxt + (* flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 42, base, other, false);
    }
    break;
  case 45: /* %6 */
    /* %6 + (* _flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 42, base, other, false);
    }
    break;
  case 46: /* %7 */
    /* %7 + (* _flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 42, base, other, false);
    }
    break;
  case 47: /* %8 */
    /* %8 + (* _flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 42, base, other, false);
    }
    break;
  case 48: /* %9 */
    /* %9 + (* _flowsTo) => Sinkl2Obj */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 42, base, other, false);
    }
    break;
  case 49: /* sinklRetPrimFlowCtxt */
    /* sinklRetPrimFlowCtxt => Sinkl2Prim */
    addEdge(base.from, base.to, 5, base, false);
    break;
  case 50: /* Sinkl2PrimFld */
    /* Sinkl2PrimFld[i] + (* flowsTo) => %11[i] */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 52, base, other, true);
    }
    break;
  case 51: /* %10 */
    /* %10[i] + (* _flowsTo) => Sinkl2PrimFld[i] */
    for(Edge other : base.to.getInEdges(17)){
      addEdge(base.from, other.from, 50, base, other, true);
    }
    break;
  case 52: /* %11 */
    /* %11[i] + (* loadPrimCtxt[i]) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    break;
  case 54: /* Sinkl2PrimstatFld */
    /* Sinkl2PrimstatFld + (* loadStatPrimCtxt) => Sinkl2Prim */
    for(Edge other : base.to.getOutEdges(39)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    break;
  }
}

public String[] outputRels() {
    String[] rels = {};
    return rels;
}

public short kindToWeight(int kind) {
  switch (kind) {
  default:
    return (short)0;
  }
}

public boolean useReps() { return true; }

}