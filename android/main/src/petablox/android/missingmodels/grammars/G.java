package petablox.android.missingmodels.grammars;
import petablox.android.missingmodels.util.jcflsolver.*;

/* Original Grammar:
###################
# CONFIGURATION
###################

.weights ref2RefArgTStub 1
.weights ref2RefRetTStub 1

.weights prim2RefArgTStub 1
.weights prim2RefRetTStub 1

.weights ref2PrimTStub 1

.weights prim2PrimTStub 1

###################
# INPUTS
###################

# src and sink annotations: src2Label, sink2Label
# label annotations: label2RefT, label2PrimT
# sinkf annotations: sink2RefT, sink2PrimT, sinkF2RefF, sinkF2PrimF
# transfer annotations: ref2RefT, ref2PrimT, prim2RefT, prim2PrimT 
# flow annotations: ref2RefF, ref2PrimF, prim2RefF, prim2PrimF

# pt: pt, fptArr
# field: fpt
# helper: assignPrimCtxt, assignPrimCCtxt, loadPrimCtxtArr, storePrimCtxtArr
# field helper: loadPrimCtxt, loadStatPrimCtxt, storePrimCtxt, storeStatPrimCtxt

###################
# RULES: ANNOTATION CONVERSION
###################

# transfer annotations

Label2RefT :: label2RefT
Label2PrimT :: label2PrimT

# flow annotations

SinkF2RefF :: sinkF2RefF
SinkF2PrimF :: sinkF2PrimF

Ref2RefF :: ref2RefF
Ref2PrimF :: ref2PrimF
Prim2RefF :: prim2RefF
Prim2PrimF :: prim2PrimF

###################
# RULES: ANNOTATION CONVERSION
###################

Ref2RefT :: ref2RefT
Ref2PrimT :: ref2PrimT
Prim2RefT :: prim2RefT
Prim2PrimT :: prim2PrimT

###################
# RULES: STUB ANNOTATION CONVERSION
###################

Ref2RefT :: ref2RefArgTStub
Ref2RefT :: ref2RefRetTStub

Prim2RefT :: prim2RefArgTStub
Prim2RefT :: prim2RefRetTStub

Ref2PrimT :: ref2PrimTStub
Prim2PrimT :: prim2PrimTStub

###################
# RULES: PARTIAL FLOW PROPAGATION
###################

Fpt[f] :: _ptG Store[f] ptG
Fpt[f] :: fpt[f]
FptArr :: fptArr

###################
# RULES: OBJECT ANNOTATIONS
###################

Obj2RefT :: _ptG Ref2RefT
Obj2PrimT :: _ptG Ref2PrimT
Obj2RefT :: _FptArr Obj2RefT
Obj2PrimT :: _FptArr Obj2PrimT

Label2ObjT :: Label2RefT ptG
Label2ObjT :: Label2ObjT Fpt[*]

###################
# RULES: SINKF
###################

# Sink_full-obj flow

SinkF2Obj :: SinkF2RefF ptG
SinkF2Obj :: sink2Label Label2Obj _ptG _Ref2RefF ptG
SinkF2Obj :: sink2Label Label2Prim _Ref2PrimF ptG
SinkF2Obj :: SinkF2Obj Fpt[*]

# Sink_full-prim flow

SinkF2Prim :: SinkF2PrimF
SinkF2Prim :: sink2Label Label2Obj _ptG _Prim2RefF
SinkF2Prim :: sink2Label Label2Prim _Prim2PrimF

###################
# RULES: SRC-SINK FLOW
###################

Src2Sink :: src2Label Label2Obj _SinkF2Obj
Src2Sink :: src2Label Label2Prim _SinkF2Prim
Src2Sink :: src2Label Label2PrimFld[*] _SinkF2Obj

###################
# RULES: LABEL FLOW
###################

# Label-obj flow

Label2Obj :: Label2ObjT
Label2Obj :: Label2ObjX

Label2ObjX :: Label2Obj Obj2RefT ptG
Label2ObjX :: Label2Prim Prim2RefT ptG
Label2ObjX :: Label2PrimFldArr Obj2RefT ptG
Label2ObjX :: Label2ObjX FptArr

# Label-prim flow

Label2Prim :: Label2PrimT
Label2Prim :: Label2Prim _assignPrimCtxt
Label2Prim :: Label2Prim _assignPrimCCtxt

Label2Prim :: Label2Obj Obj2PrimT
Label2Prim :: Label2Prim Prim2PrimT

Label2Prim :: Label2ObjT _ptG _loadPrimCtxt[*]
Label2Prim :: Label2ObjX _ptG _loadPrimCtxtArr
Label2Prim :: Label2PrimFldArr Obj2PrimT

# cl Label2PrimFld[f] o _ptG v_c _loadPrimCtxt[f] u_c
Label2Prim :: Label2PrimFld[f] _ptG _loadPrimCtxt[f]
Label2Prim :: Label2PrimFldStat[f] _loadStatPrimCtxt[f]

# Label-prim_fld flow

Label2PrimFld[f] :: Label2Prim _storePrimCtxt[f] ptG
Label2PrimFldArr :: Label2Prim _storePrimCtxtArr ptG
Label2PrimFldStat[f] :: Label2Prim _storeStatPrimCtxt[f]
*/

/* Normalized Grammar:
%13:
	%13 :: Label2Prim Prim2RefT
%14:
	%14 :: Label2PrimFldArr Obj2RefT
Fpt:
	Fpt[i] :: %0[i] ptG
	Fpt[i] :: fpt[i]
%9:
	%9 :: src2Label Label2Obj
%8:
	%8 :: sink2Label Label2Prim
%5:
	%5 :: %4 _Ref2PrimF
%4:
	%4 :: sink2Label Label2Prim
%7:
	%7 :: %6 _ptG
%6:
	%6 :: sink2Label Label2Obj
%1:
	%1 :: sink2Label Label2Obj
%0:
	%0[i] :: _ptG Store[i]
%3:
	%3 :: %2 _Ref2RefF
%2:
	%2 :: %1 _ptG
Label2RefT:
	Label2RefT :: label2RefT
FptArr:
	FptArr :: fptArr
Label2PrimFldStat:
	Label2PrimFldStat[i] :: Label2Prim _storeStatPrimCtxt[i]
SinkF2PrimF:
	SinkF2PrimF :: sinkF2PrimF
Label2ObjT:
	Label2ObjT :: Label2RefT ptG
	Label2ObjT :: Label2ObjT Fpt
Label2ObjX:
	Label2ObjX :: %12 ptG
	Label2ObjX :: %13 ptG
	Label2ObjX :: %14 ptG
	Label2ObjX :: Label2ObjX FptArr
%19:
	%19 :: Label2Prim _storePrimCtxtArr
Label2PrimFld:
	Label2PrimFld[i] :: %18[i] ptG
Src2Sink:
	Src2Sink :: %9 _SinkF2Obj
	Src2Sink :: %10 _SinkF2Prim
	Src2Sink :: %11 _SinkF2Obj
Label2PrimFldArr:
	Label2PrimFldArr :: %19 ptG
Prim2PrimF:
	Prim2PrimF :: prim2PrimF
SinkF2RefF:
	SinkF2RefF :: sinkF2RefF
Obj2PrimT:
	Obj2PrimT :: _ptG Ref2PrimT
	Obj2PrimT :: _FptArr Obj2PrimT
Prim2PrimT:
	Prim2PrimT :: prim2PrimT
	Prim2PrimT :: prim2PrimTStub
Obj2RefT:
	Obj2RefT :: _ptG Ref2RefT
	Obj2RefT :: _FptArr Obj2RefT
Ref2PrimT:
	Ref2PrimT :: ref2PrimT
	Ref2PrimT :: ref2PrimTStub
%18:
	%18[i] :: Label2Prim _storePrimCtxt[i]
SinkF2Prim:
	SinkF2Prim :: SinkF2PrimF
	SinkF2Prim :: %7 _Prim2RefF
	SinkF2Prim :: %8 _Prim2PrimF
Ref2RefF:
	Ref2RefF :: ref2RefF
%11:
	%11 :: src2Label Label2PrimFld
%10:
	%10 :: src2Label Label2Prim
Prim2RefT:
	Prim2RefT :: prim2RefT
	Prim2RefT :: prim2RefArgTStub
	Prim2RefT :: prim2RefRetTStub
%12:
	%12 :: Label2Obj Obj2RefT
%15:
	%15 :: Label2ObjT _ptG
SinkF2Obj:
	SinkF2Obj :: SinkF2RefF ptG
	SinkF2Obj :: %3 ptG
	SinkF2Obj :: %5 ptG
	SinkF2Obj :: SinkF2Obj Fpt
Label2Prim:
	Label2Prim :: Label2PrimT
	Label2Prim :: Label2Prim _assignPrimCtxt
	Label2Prim :: Label2Prim _assignPrimCCtxt
	Label2Prim :: Label2Obj Obj2PrimT
	Label2Prim :: Label2Prim Prim2PrimT
	Label2Prim :: %15 _loadPrimCtxt
	Label2Prim :: %16 _loadPrimCtxtArr
	Label2Prim :: Label2PrimFldArr Obj2PrimT
	Label2Prim :: %17[i] _loadPrimCtxt[i]
	Label2Prim :: Label2PrimFldStat[i] _loadStatPrimCtxt[i]
%16:
	%16 :: Label2ObjX _ptG
Ref2PrimF:
	Ref2PrimF :: ref2PrimF
Label2PrimT:
	Label2PrimT :: label2PrimT
Ref2RefT:
	Ref2RefT :: ref2RefT
	Ref2RefT :: ref2RefArgTStub
	Ref2RefT :: ref2RefRetTStub
Prim2RefF:
	Prim2RefF :: prim2RefF
%17:
	%17[i] :: Label2PrimFld[i] _ptG
Label2Obj:
	Label2Obj :: Label2ObjT
	Label2Obj :: Label2ObjX
*/

/* Reverse Productions:
ref2PrimT:
	ref2PrimT => Ref2PrimT
%13:
	%13 + (* ptG) => Label2ObjX
sink2Label:
	sink2Label + (* Label2Obj) => %1
	sink2Label + (* Label2Prim) => %4
	sink2Label + (* Label2Obj) => %6
	sink2Label + (* Label2Prim) => %8
ref2RefF:
	ref2RefF => Ref2RefF
sinkF2PrimF:
	sinkF2PrimF => SinkF2PrimF
assignPrimCtxt:
	_assignPrimCtxt + (Label2Prim *) => Label2Prim
prim2RefArgTStub:
	prim2RefArgTStub => Prim2RefT
prim2RefT:
	prim2RefT => Prim2RefT
%12:
	%12 + (* ptG) => Label2ObjX
%14:
	%14 + (* ptG) => Label2ObjX
fptArr:
	fptArr => FptArr
ref2PrimF:
	ref2PrimF => Ref2PrimF
label2PrimT:
	label2PrimT => Label2PrimT
%9:
	%9 + (* _SinkF2Obj) => Src2Sink
ref2RefArgTStub:
	ref2RefArgTStub => Ref2RefT
ref2RefT:
	ref2RefT => Ref2RefT
prim2RefF:
	prim2RefF => Prim2RefF
%4:
	%4 + (* _Ref2PrimF) => %5
%7:
	%7 + (* _Prim2RefF) => SinkF2Prim
%6:
	%6 + (* _ptG) => %7
%1:
	%1 + (* _ptG) => %2
%0:
	%0[i] + (* ptG) => Fpt[i]
Store:
	Store[i] + (_ptG *) => %0[i]
%2:
	%2 + (* _Ref2RefF) => %3
%5:
	%5 + (* ptG) => SinkF2Obj
FptArr:
	_FptArr + (* Obj2RefT) => Obj2RefT
	_FptArr + (* Obj2PrimT) => Obj2PrimT
	FptArr + (Label2ObjX *) => Label2ObjX
ptG:
	_ptG + (* Store[i]) => %0[i]
	ptG + (%0[i] *) => Fpt[i]
	_ptG + (* Ref2RefT) => Obj2RefT
	_ptG + (* Ref2PrimT) => Obj2PrimT
	ptG + (Label2RefT *) => Label2ObjT
	ptG + (SinkF2RefF *) => SinkF2Obj
	_ptG + (%1 *) => %2
	ptG + (%3 *) => SinkF2Obj
	ptG + (%5 *) => SinkF2Obj
	_ptG + (%6 *) => %7
	ptG + (%12 *) => Label2ObjX
	ptG + (%13 *) => Label2ObjX
	ptG + (%14 *) => Label2ObjX
	_ptG + (Label2ObjT *) => %15
	_ptG + (Label2ObjX *) => %16
	_ptG + (Label2PrimFld[i] *) => %17[i]
	ptG + (%18[i] *) => Label2PrimFld[i]
	ptG + (%19 *) => Label2PrimFldArr
src2Label:
	src2Label + (* Label2Obj) => %9
	src2Label + (* Label2Prim) => %10
	src2Label + (* Label2PrimFld) => %11
prim2PrimTStub:
	prim2PrimTStub => Prim2PrimT
storeStatPrimCtxt:
	_storeStatPrimCtxt[i] + (Label2Prim *) => Label2PrimFldStat[i]
fpt:
	fpt[i] => Fpt[i]
sinkF2RefF:
	sinkF2RefF => SinkF2RefF
%3:
	%3 + (* ptG) => SinkF2Obj
SinkF2PrimF:
	SinkF2PrimF => SinkF2Prim
Label2ObjT:
	Label2ObjT + (* Fpt) => Label2ObjT
	Label2ObjT => Label2Obj
	Label2ObjT + (* _ptG) => %15
Label2PrimFld:
	Label2PrimFld + (src2Label *) => %11
	Label2PrimFld[i] + (* _ptG) => %17[i]
ref2PrimTStub:
	ref2PrimTStub => Ref2PrimT
Label2PrimFldStat:
	Label2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Label2Prim
Label2ObjX:
	Label2ObjX => Label2Obj
	Label2ObjX + (* FptArr) => Label2ObjX
	Label2ObjX + (* _ptG) => %16
storePrimCtxt:
	_storePrimCtxt[i] + (Label2Prim *) => %18[i]
label2RefT:
	label2RefT => Label2RefT
ref2RefRetTStub:
	ref2RefRetTStub => Ref2RefT
loadStatPrimCtxt:
	_loadStatPrimCtxt[i] + (Label2PrimFldStat[i] *) => Label2Prim
Label2PrimFldArr:
	Label2PrimFldArr + (* Obj2RefT) => %14
	Label2PrimFldArr + (* Obj2PrimT) => Label2Prim
Prim2PrimF:
	_Prim2PrimF + (%8 *) => SinkF2Prim
storePrimCtxtArr:
	_storePrimCtxtArr + (Label2Prim *) => %19
SinkF2RefF:
	SinkF2RefF + (* ptG) => SinkF2Obj
%19:
	%19 + (* ptG) => Label2PrimFldArr
prim2RefRetTStub:
	prim2RefRetTStub => Prim2RefT
Obj2PrimT:
	Obj2PrimT + (_FptArr *) => Obj2PrimT
	Obj2PrimT + (Label2Obj *) => Label2Prim
	Obj2PrimT + (Label2PrimFldArr *) => Label2Prim
loadPrimCtxt:
	_loadPrimCtxt + (%15 *) => Label2Prim
	_loadPrimCtxt[i] + (%17[i] *) => Label2Prim
Prim2PrimT:
	Prim2PrimT + (Label2Prim *) => Label2Prim
Label2Prim:
	Label2Prim + (sink2Label *) => %4
	Label2Prim + (sink2Label *) => %8
	Label2Prim + (src2Label *) => %10
	Label2Prim + (* Prim2RefT) => %13
	Label2Prim + (* _assignPrimCtxt) => Label2Prim
	Label2Prim + (* _assignPrimCCtxt) => Label2Prim
	Label2Prim + (* Prim2PrimT) => Label2Prim
	Label2Prim + (* _storePrimCtxt[i]) => %18[i]
	Label2Prim + (* _storePrimCtxtArr) => %19
	Label2Prim + (* _storeStatPrimCtxt[i]) => Label2PrimFldStat[i]
Obj2RefT:
	Obj2RefT + (_FptArr *) => Obj2RefT
	Obj2RefT + (Label2Obj *) => %12
	Obj2RefT + (Label2PrimFldArr *) => %14
Ref2PrimT:
	Ref2PrimT + (_ptG *) => Obj2PrimT
%18:
	%18[i] + (* ptG) => Label2PrimFld[i]
SinkF2Prim:
	_SinkF2Prim + (%10 *) => Src2Sink
Ref2RefF:
	_Ref2RefF + (%2 *) => %3
loadPrimCtxtArr:
	_loadPrimCtxtArr + (%16 *) => Label2Prim
Fpt:
	Fpt + (Label2ObjT *) => Label2ObjT
	Fpt + (SinkF2Obj *) => SinkF2Obj
%11:
	%11 + (* _SinkF2Obj) => Src2Sink
%10:
	%10 + (* _SinkF2Prim) => Src2Sink
Label2Obj:
	Label2Obj + (sink2Label *) => %1
	Label2Obj + (sink2Label *) => %6
	Label2Obj + (src2Label *) => %9
	Label2Obj + (* Obj2RefT) => %12
	Label2Obj + (* Obj2PrimT) => Label2Prim
Label2RefT:
	Label2RefT + (* ptG) => Label2ObjT
%15:
	%15 + (* _loadPrimCtxt) => Label2Prim
SinkF2Obj:
	SinkF2Obj + (* Fpt) => SinkF2Obj
	_SinkF2Obj + (%9 *) => Src2Sink
	_SinkF2Obj + (%11 *) => Src2Sink
prim2PrimF:
	prim2PrimF => Prim2PrimF
%16:
	%16 + (* _loadPrimCtxtArr) => Label2Prim
assignPrimCCtxt:
	_assignPrimCCtxt + (Label2Prim *) => Label2Prim
Ref2PrimF:
	_Ref2PrimF + (%4 *) => %5
Label2PrimT:
	Label2PrimT => Label2Prim
Ref2RefT:
	Ref2RefT + (_ptG *) => Obj2RefT
Prim2RefF:
	_Prim2RefF + (%7 *) => SinkF2Prim
%17:
	%17[i] + (* _loadPrimCtxt[i]) => Label2Prim
prim2PrimT:
	prim2PrimT => Prim2PrimT
Prim2RefT:
	Prim2RefT + (Label2Prim *) => %13
%8:
	%8 + (* _Prim2PrimF) => SinkF2Prim
*/

public class G extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 3:
  case 5:
  case 7:
  case 9:
  case 11:
  case 13:
  case 15:
  case 17:
  case 19:
  case 21:
  case 23:
  case 24:
  case 25:
  case 26:
  case 27:
  case 28:
  case 29:
  case 31:
  case 34:
  case 36:
  case 41:
  case 54:
  case 64:
  case 65:
  case 66:
  case 68:
  case 72:
  case 73:
  case 75:
  case 77:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 78;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("Label2RefT")) return 0;
  if (symbol.equals("label2RefT")) return 1;
  if (symbol.equals("Label2PrimT")) return 2;
  if (symbol.equals("label2PrimT")) return 3;
  if (symbol.equals("SinkF2RefF")) return 4;
  if (symbol.equals("sinkF2RefF")) return 5;
  if (symbol.equals("SinkF2PrimF")) return 6;
  if (symbol.equals("sinkF2PrimF")) return 7;
  if (symbol.equals("Ref2RefF")) return 8;
  if (symbol.equals("ref2RefF")) return 9;
  if (symbol.equals("Ref2PrimF")) return 10;
  if (symbol.equals("ref2PrimF")) return 11;
  if (symbol.equals("Prim2RefF")) return 12;
  if (symbol.equals("prim2RefF")) return 13;
  if (symbol.equals("Prim2PrimF")) return 14;
  if (symbol.equals("prim2PrimF")) return 15;
  if (symbol.equals("Ref2RefT")) return 16;
  if (symbol.equals("ref2RefT")) return 17;
  if (symbol.equals("Ref2PrimT")) return 18;
  if (symbol.equals("ref2PrimT")) return 19;
  if (symbol.equals("Prim2RefT")) return 20;
  if (symbol.equals("prim2RefT")) return 21;
  if (symbol.equals("Prim2PrimT")) return 22;
  if (symbol.equals("prim2PrimT")) return 23;
  if (symbol.equals("ref2RefArgTStub")) return 24;
  if (symbol.equals("ref2RefRetTStub")) return 25;
  if (symbol.equals("prim2RefArgTStub")) return 26;
  if (symbol.equals("prim2RefRetTStub")) return 27;
  if (symbol.equals("ref2PrimTStub")) return 28;
  if (symbol.equals("prim2PrimTStub")) return 29;
  if (symbol.equals("Fpt")) return 30;
  if (symbol.equals("ptG")) return 31;
  if (symbol.equals("Store")) return 32;
  if (symbol.equals("%0")) return 33;
  if (symbol.equals("fpt")) return 34;
  if (symbol.equals("FptArr")) return 35;
  if (symbol.equals("fptArr")) return 36;
  if (symbol.equals("Obj2RefT")) return 37;
  if (symbol.equals("Obj2PrimT")) return 38;
  if (symbol.equals("Label2ObjT")) return 39;
  if (symbol.equals("SinkF2Obj")) return 40;
  if (symbol.equals("sink2Label")) return 41;
  if (symbol.equals("Label2Obj")) return 42;
  if (symbol.equals("%1")) return 43;
  if (symbol.equals("%2")) return 44;
  if (symbol.equals("%3")) return 45;
  if (symbol.equals("Label2Prim")) return 46;
  if (symbol.equals("%4")) return 47;
  if (symbol.equals("%5")) return 48;
  if (symbol.equals("SinkF2Prim")) return 49;
  if (symbol.equals("%6")) return 50;
  if (symbol.equals("%7")) return 51;
  if (symbol.equals("%8")) return 52;
  if (symbol.equals("Src2Sink")) return 53;
  if (symbol.equals("src2Label")) return 54;
  if (symbol.equals("%9")) return 55;
  if (symbol.equals("%10")) return 56;
  if (symbol.equals("Label2PrimFld")) return 57;
  if (symbol.equals("%11")) return 58;
  if (symbol.equals("Label2ObjX")) return 59;
  if (symbol.equals("%12")) return 60;
  if (symbol.equals("%13")) return 61;
  if (symbol.equals("Label2PrimFldArr")) return 62;
  if (symbol.equals("%14")) return 63;
  if (symbol.equals("assignPrimCtxt")) return 64;
  if (symbol.equals("assignPrimCCtxt")) return 65;
  if (symbol.equals("loadPrimCtxt")) return 66;
  if (symbol.equals("%15")) return 67;
  if (symbol.equals("loadPrimCtxtArr")) return 68;
  if (symbol.equals("%16")) return 69;
  if (symbol.equals("%17")) return 70;
  if (symbol.equals("Label2PrimFldStat")) return 71;
  if (symbol.equals("loadStatPrimCtxt")) return 72;
  if (symbol.equals("storePrimCtxt")) return 73;
  if (symbol.equals("%18")) return 74;
  if (symbol.equals("storePrimCtxtArr")) return 75;
  if (symbol.equals("%19")) return 76;
  if (symbol.equals("storeStatPrimCtxt")) return 77;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "Label2RefT";
  case 1: return "label2RefT";
  case 2: return "Label2PrimT";
  case 3: return "label2PrimT";
  case 4: return "SinkF2RefF";
  case 5: return "sinkF2RefF";
  case 6: return "SinkF2PrimF";
  case 7: return "sinkF2PrimF";
  case 8: return "Ref2RefF";
  case 9: return "ref2RefF";
  case 10: return "Ref2PrimF";
  case 11: return "ref2PrimF";
  case 12: return "Prim2RefF";
  case 13: return "prim2RefF";
  case 14: return "Prim2PrimF";
  case 15: return "prim2PrimF";
  case 16: return "Ref2RefT";
  case 17: return "ref2RefT";
  case 18: return "Ref2PrimT";
  case 19: return "ref2PrimT";
  case 20: return "Prim2RefT";
  case 21: return "prim2RefT";
  case 22: return "Prim2PrimT";
  case 23: return "prim2PrimT";
  case 24: return "ref2RefArgTStub";
  case 25: return "ref2RefRetTStub";
  case 26: return "prim2RefArgTStub";
  case 27: return "prim2RefRetTStub";
  case 28: return "ref2PrimTStub";
  case 29: return "prim2PrimTStub";
  case 30: return "Fpt";
  case 31: return "ptG";
  case 32: return "Store";
  case 33: return "%0";
  case 34: return "fpt";
  case 35: return "FptArr";
  case 36: return "fptArr";
  case 37: return "Obj2RefT";
  case 38: return "Obj2PrimT";
  case 39: return "Label2ObjT";
  case 40: return "SinkF2Obj";
  case 41: return "sink2Label";
  case 42: return "Label2Obj";
  case 43: return "%1";
  case 44: return "%2";
  case 45: return "%3";
  case 46: return "Label2Prim";
  case 47: return "%4";
  case 48: return "%5";
  case 49: return "SinkF2Prim";
  case 50: return "%6";
  case 51: return "%7";
  case 52: return "%8";
  case 53: return "Src2Sink";
  case 54: return "src2Label";
  case 55: return "%9";
  case 56: return "%10";
  case 57: return "Label2PrimFld";
  case 58: return "%11";
  case 59: return "Label2ObjX";
  case 60: return "%12";
  case 61: return "%13";
  case 62: return "Label2PrimFldArr";
  case 63: return "%14";
  case 64: return "assignPrimCtxt";
  case 65: return "assignPrimCCtxt";
  case 66: return "loadPrimCtxt";
  case 67: return "%15";
  case 68: return "loadPrimCtxtArr";
  case 69: return "%16";
  case 70: return "%17";
  case 71: return "Label2PrimFldStat";
  case 72: return "loadStatPrimCtxt";
  case 73: return "storePrimCtxt";
  case 74: return "%18";
  case 75: return "storePrimCtxtArr";
  case 76: return "%19";
  case 77: return "storeStatPrimCtxt";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* Label2RefT */
    /* Label2RefT + (* ptG) => Label2ObjT */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 39, base, other, false);
    }
    break;
  case 1: /* label2RefT */
    /* label2RefT => Label2RefT */
    addEdge(base.from, base.to, 0, base, false);
    break;
  case 2: /* Label2PrimT */
    /* Label2PrimT => Label2Prim */
    addEdge(base.from, base.to, 46, base, false);
    break;
  case 3: /* label2PrimT */
    /* label2PrimT => Label2PrimT */
    addEdge(base.from, base.to, 2, base, false);
    break;
  case 4: /* SinkF2RefF */
    /* SinkF2RefF + (* ptG) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 40, base, other, false);
    }
    break;
  case 5: /* sinkF2RefF */
    /* sinkF2RefF => SinkF2RefF */
    addEdge(base.from, base.to, 4, base, false);
    break;
  case 6: /* SinkF2PrimF */
    /* SinkF2PrimF => SinkF2Prim */
    addEdge(base.from, base.to, 49, base, false);
    break;
  case 7: /* sinkF2PrimF */
    /* sinkF2PrimF => SinkF2PrimF */
    addEdge(base.from, base.to, 6, base, false);
    break;
  case 8: /* Ref2RefF */
    /* _Ref2RefF + (%2 *) => %3 */
    for(Edge other : base.to.getInEdges(44)){
      addEdge(other.from, base.from, 45, base, other, false);
    }
    break;
  case 9: /* ref2RefF */
    /* ref2RefF => Ref2RefF */
    addEdge(base.from, base.to, 8, base, false);
    break;
  case 10: /* Ref2PrimF */
    /* _Ref2PrimF + (%4 *) => %5 */
    for(Edge other : base.to.getInEdges(47)){
      addEdge(other.from, base.from, 48, base, other, false);
    }
    break;
  case 11: /* ref2PrimF */
    /* ref2PrimF => Ref2PrimF */
    addEdge(base.from, base.to, 10, base, false);
    break;
  case 12: /* Prim2RefF */
    /* _Prim2RefF + (%7 *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(51)){
      addEdge(other.from, base.from, 49, base, other, false);
    }
    break;
  case 13: /* prim2RefF */
    /* prim2RefF => Prim2RefF */
    addEdge(base.from, base.to, 12, base, false);
    break;
  case 14: /* Prim2PrimF */
    /* _Prim2PrimF + (%8 *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(52)){
      addEdge(other.from, base.from, 49, base, other, false);
    }
    break;
  case 15: /* prim2PrimF */
    /* prim2PrimF => Prim2PrimF */
    addEdge(base.from, base.to, 14, base, false);
    break;
  case 16: /* Ref2RefT */
    /* Ref2RefT + (_ptG *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(31)){
      addEdge(other.to, base.to, 37, base, other, false);
    }
    break;
  case 17: /* ref2RefT */
    /* ref2RefT => Ref2RefT */
    addEdge(base.from, base.to, 16, base, false);
    break;
  case 18: /* Ref2PrimT */
    /* Ref2PrimT + (_ptG *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(31)){
      addEdge(other.to, base.to, 38, base, other, false);
    }
    break;
  case 19: /* ref2PrimT */
    /* ref2PrimT => Ref2PrimT */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 20: /* Prim2RefT */
    /* Prim2RefT + (Label2Prim *) => %13 */
    for(Edge other : base.from.getInEdges(46)){
      addEdge(other.from, base.to, 61, base, other, false);
    }
    break;
  case 21: /* prim2RefT */
    /* prim2RefT => Prim2RefT */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 22: /* Prim2PrimT */
    /* Prim2PrimT + (Label2Prim *) => Label2Prim */
    for(Edge other : base.from.getInEdges(46)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    break;
  case 23: /* prim2PrimT */
    /* prim2PrimT => Prim2PrimT */
    addEdge(base.from, base.to, 22, base, false);
    break;
  case 24: /* ref2RefArgTStub */
    /* ref2RefArgTStub => Ref2RefT */
    addEdge(base.from, base.to, 16, base, false);
    break;
  case 25: /* ref2RefRetTStub */
    /* ref2RefRetTStub => Ref2RefT */
    addEdge(base.from, base.to, 16, base, false);
    break;
  case 26: /* prim2RefArgTStub */
    /* prim2RefArgTStub => Prim2RefT */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 27: /* prim2RefRetTStub */
    /* prim2RefRetTStub => Prim2RefT */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 28: /* ref2PrimTStub */
    /* ref2PrimTStub => Ref2PrimT */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 29: /* prim2PrimTStub */
    /* prim2PrimTStub => Prim2PrimT */
    addEdge(base.from, base.to, 22, base, false);
    break;
  case 30: /* Fpt */
    /* Fpt + (Label2ObjT *) => Label2ObjT */
    for(Edge other : base.from.getInEdges(39)){
      addEdge(other.from, base.to, 39, base, other, false);
    }
    /* Fpt + (SinkF2Obj *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(40)){
      addEdge(other.from, base.to, 40, base, other, false);
    }
    break;
  case 31: /* ptG */
    /* _ptG + (* Store[i]) => %0[i] */
    for(Edge other : base.from.getOutEdges(32)){
      addEdge(base.to, other.to, 33, base, other, true);
    }
    /* ptG + (%0[i] *) => Fpt[i] */
    for(Edge other : base.from.getInEdges(33)){
      addEdge(other.from, base.to, 30, base, other, true);
    }
    /* _ptG + (* Ref2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(16)){
      addEdge(base.to, other.to, 37, base, other, false);
    }
    /* _ptG + (* Ref2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(18)){
      addEdge(base.to, other.to, 38, base, other, false);
    }
    /* ptG + (Label2RefT *) => Label2ObjT */
    for(Edge other : base.from.getInEdges(0)){
      addEdge(other.from, base.to, 39, base, other, false);
    }
    /* ptG + (SinkF2RefF *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(4)){
      addEdge(other.from, base.to, 40, base, other, false);
    }
    /* _ptG + (%1 *) => %2 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(other.from, base.from, 44, base, other, false);
    }
    /* ptG + (%3 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(45)){
      addEdge(other.from, base.to, 40, base, other, false);
    }
    /* ptG + (%5 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(48)){
      addEdge(other.from, base.to, 40, base, other, false);
    }
    /* _ptG + (%6 *) => %7 */
    for(Edge other : base.to.getInEdges(50)){
      addEdge(other.from, base.from, 51, base, other, false);
    }
    /* ptG + (%12 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(60)){
      addEdge(other.from, base.to, 59, base, other, false);
    }
    /* ptG + (%13 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(61)){
      addEdge(other.from, base.to, 59, base, other, false);
    }
    /* ptG + (%14 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(63)){
      addEdge(other.from, base.to, 59, base, other, false);
    }
    /* _ptG + (Label2ObjT *) => %15 */
    for(Edge other : base.to.getInEdges(39)){
      addEdge(other.from, base.from, 67, base, other, false);
    }
    /* _ptG + (Label2ObjX *) => %16 */
    for(Edge other : base.to.getInEdges(59)){
      addEdge(other.from, base.from, 69, base, other, false);
    }
    /* _ptG + (Label2PrimFld[i] *) => %17[i] */
    for(Edge other : base.to.getInEdges(57)){
      addEdge(other.from, base.from, 70, base, other, true);
    }
    /* ptG + (%18[i] *) => Label2PrimFld[i] */
    for(Edge other : base.from.getInEdges(74)){
      addEdge(other.from, base.to, 57, base, other, true);
    }
    /* ptG + (%19 *) => Label2PrimFldArr */
    for(Edge other : base.from.getInEdges(76)){
      addEdge(other.from, base.to, 62, base, other, false);
    }
    break;
  case 32: /* Store */
    /* Store[i] + (_ptG *) => %0[i] */
    for(Edge other : base.from.getOutEdges(31)){
      addEdge(other.to, base.to, 33, base, other, true);
    }
    break;
  case 33: /* %0 */
    /* %0[i] + (* ptG) => Fpt[i] */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 30, base, other, true);
    }
    break;
  case 34: /* fpt */
    /* fpt[i] => Fpt[i] */
    addEdge(base.from, base.to, 30, base, true);
    break;
  case 35: /* FptArr */
    /* _FptArr + (* Obj2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(37)){
      addEdge(base.to, other.to, 37, base, other, false);
    }
    /* _FptArr + (* Obj2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(38)){
      addEdge(base.to, other.to, 38, base, other, false);
    }
    /* FptArr + (Label2ObjX *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(59)){
      addEdge(other.from, base.to, 59, base, other, false);
    }
    break;
  case 36: /* fptArr */
    /* fptArr => FptArr */
    addEdge(base.from, base.to, 35, base, false);
    break;
  case 37: /* Obj2RefT */
    /* Obj2RefT + (_FptArr *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(35)){
      addEdge(other.to, base.to, 37, base, other, false);
    }
    /* Obj2RefT + (Label2Obj *) => %12 */
    for(Edge other : base.from.getInEdges(42)){
      addEdge(other.from, base.to, 60, base, other, false);
    }
    /* Obj2RefT + (Label2PrimFldArr *) => %14 */
    for(Edge other : base.from.getInEdges(62)){
      addEdge(other.from, base.to, 63, base, other, false);
    }
    break;
  case 38: /* Obj2PrimT */
    /* Obj2PrimT + (_FptArr *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(35)){
      addEdge(other.to, base.to, 38, base, other, false);
    }
    /* Obj2PrimT + (Label2Obj *) => Label2Prim */
    for(Edge other : base.from.getInEdges(42)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    /* Obj2PrimT + (Label2PrimFldArr *) => Label2Prim */
    for(Edge other : base.from.getInEdges(62)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    break;
  case 39: /* Label2ObjT */
    /* Label2ObjT + (* Fpt) => Label2ObjT */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 39, base, other, false);
    }
    /* Label2ObjT => Label2Obj */
    addEdge(base.from, base.to, 42, base, false);
    /* Label2ObjT + (* _ptG) => %15 */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(base.from, other.from, 67, base, other, false);
    }
    break;
  case 40: /* SinkF2Obj */
    /* SinkF2Obj + (* Fpt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 40, base, other, false);
    }
    /* _SinkF2Obj + (%9 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(55)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    /* _SinkF2Obj + (%11 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 41: /* sink2Label */
    /* sink2Label + (* Label2Obj) => %1 */
    for(Edge other : base.to.getOutEdges(42)){
      addEdge(base.from, other.to, 43, base, other, false);
    }
    /* sink2Label + (* Label2Prim) => %4 */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    /* sink2Label + (* Label2Obj) => %6 */
    for(Edge other : base.to.getOutEdges(42)){
      addEdge(base.from, other.to, 50, base, other, false);
    }
    /* sink2Label + (* Label2Prim) => %8 */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 52, base, other, false);
    }
    break;
  case 42: /* Label2Obj */
    /* Label2Obj + (sink2Label *) => %1 */
    for(Edge other : base.from.getInEdges(41)){
      addEdge(other.from, base.to, 43, base, other, false);
    }
    /* Label2Obj + (sink2Label *) => %6 */
    for(Edge other : base.from.getInEdges(41)){
      addEdge(other.from, base.to, 50, base, other, false);
    }
    /* Label2Obj + (src2Label *) => %9 */
    for(Edge other : base.from.getInEdges(54)){
      addEdge(other.from, base.to, 55, base, other, false);
    }
    /* Label2Obj + (* Obj2RefT) => %12 */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 60, base, other, false);
    }
    /* Label2Obj + (* Obj2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(38)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    break;
  case 43: /* %1 */
    /* %1 + (* _ptG) => %2 */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(base.from, other.from, 44, base, other, false);
    }
    break;
  case 44: /* %2 */
    /* %2 + (* _Ref2RefF) => %3 */
    for(Edge other : base.to.getInEdges(8)){
      addEdge(base.from, other.from, 45, base, other, false);
    }
    break;
  case 45: /* %3 */
    /* %3 + (* ptG) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 40, base, other, false);
    }
    break;
  case 46: /* Label2Prim */
    /* Label2Prim + (sink2Label *) => %4 */
    for(Edge other : base.from.getInEdges(41)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    /* Label2Prim + (sink2Label *) => %8 */
    for(Edge other : base.from.getInEdges(41)){
      addEdge(other.from, base.to, 52, base, other, false);
    }
    /* Label2Prim + (src2Label *) => %10 */
    for(Edge other : base.from.getInEdges(54)){
      addEdge(other.from, base.to, 56, base, other, false);
    }
    /* Label2Prim + (* Prim2RefT) => %13 */
    for(Edge other : base.to.getOutEdges(20)){
      addEdge(base.from, other.to, 61, base, other, false);
    }
    /* Label2Prim + (* _assignPrimCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    /* Label2Prim + (* _assignPrimCCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(65)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    /* Label2Prim + (* Prim2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(22)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    /* Label2Prim + (* _storePrimCtxt[i]) => %18[i] */
    for(Edge other : base.to.getInEdges(73)){
      addEdge(base.from, other.from, 74, base, other, true);
    }
    /* Label2Prim + (* _storePrimCtxtArr) => %19 */
    for(Edge other : base.to.getInEdges(75)){
      addEdge(base.from, other.from, 76, base, other, false);
    }
    /* Label2Prim + (* _storeStatPrimCtxt[i]) => Label2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(77)){
      addEdge(base.from, other.from, 71, base, other, true);
    }
    break;
  case 47: /* %4 */
    /* %4 + (* _Ref2PrimF) => %5 */
    for(Edge other : base.to.getInEdges(10)){
      addEdge(base.from, other.from, 48, base, other, false);
    }
    break;
  case 48: /* %5 */
    /* %5 + (* ptG) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 40, base, other, false);
    }
    break;
  case 49: /* SinkF2Prim */
    /* _SinkF2Prim + (%10 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(56)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 50: /* %6 */
    /* %6 + (* _ptG) => %7 */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(base.from, other.from, 51, base, other, false);
    }
    break;
  case 51: /* %7 */
    /* %7 + (* _Prim2RefF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(12)){
      addEdge(base.from, other.from, 49, base, other, false);
    }
    break;
  case 52: /* %8 */
    /* %8 + (* _Prim2PrimF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(14)){
      addEdge(base.from, other.from, 49, base, other, false);
    }
    break;
  case 54: /* src2Label */
    /* src2Label + (* Label2Obj) => %9 */
    for(Edge other : base.to.getOutEdges(42)){
      addEdge(base.from, other.to, 55, base, other, false);
    }
    /* src2Label + (* Label2Prim) => %10 */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 56, base, other, false);
    }
    /* src2Label + (* Label2PrimFld) => %11 */
    for(Edge other : base.to.getOutEdges(57)){
      addEdge(base.from, other.to, 58, base, other, false);
    }
    break;
  case 55: /* %9 */
    /* %9 + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(40)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 56: /* %10 */
    /* %10 + (* _SinkF2Prim) => Src2Sink */
    for(Edge other : base.to.getInEdges(49)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 57: /* Label2PrimFld */
    /* Label2PrimFld + (src2Label *) => %11 */
    for(Edge other : base.from.getInEdges(54)){
      addEdge(other.from, base.to, 58, base, other, false);
    }
    /* Label2PrimFld[i] + (* _ptG) => %17[i] */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(base.from, other.from, 70, base, other, true);
    }
    break;
  case 58: /* %11 */
    /* %11 + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(40)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 59: /* Label2ObjX */
    /* Label2ObjX => Label2Obj */
    addEdge(base.from, base.to, 42, base, false);
    /* Label2ObjX + (* FptArr) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(35)){
      addEdge(base.from, other.to, 59, base, other, false);
    }
    /* Label2ObjX + (* _ptG) => %16 */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(base.from, other.from, 69, base, other, false);
    }
    break;
  case 60: /* %12 */
    /* %12 + (* ptG) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 59, base, other, false);
    }
    break;
  case 61: /* %13 */
    /* %13 + (* ptG) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 59, base, other, false);
    }
    break;
  case 62: /* Label2PrimFldArr */
    /* Label2PrimFldArr + (* Obj2RefT) => %14 */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 63, base, other, false);
    }
    /* Label2PrimFldArr + (* Obj2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(38)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    break;
  case 63: /* %14 */
    /* %14 + (* ptG) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 59, base, other, false);
    }
    break;
  case 64: /* assignPrimCtxt */
    /* _assignPrimCtxt + (Label2Prim *) => Label2Prim */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    break;
  case 65: /* assignPrimCCtxt */
    /* _assignPrimCCtxt + (Label2Prim *) => Label2Prim */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    break;
  case 66: /* loadPrimCtxt */
    /* _loadPrimCtxt + (%15 *) => Label2Prim */
    for(Edge other : base.to.getInEdges(67)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    /* _loadPrimCtxt[i] + (%17[i] *) => Label2Prim */
    for(Edge other : base.to.getInEdges(70)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    break;
  case 67: /* %15 */
    /* %15 + (* _loadPrimCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(66)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    break;
  case 68: /* loadPrimCtxtArr */
    /* _loadPrimCtxtArr + (%16 *) => Label2Prim */
    for(Edge other : base.to.getInEdges(69)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    break;
  case 69: /* %16 */
    /* %16 + (* _loadPrimCtxtArr) => Label2Prim */
    for(Edge other : base.to.getInEdges(68)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    break;
  case 70: /* %17 */
    /* %17[i] + (* _loadPrimCtxt[i]) => Label2Prim */
    for(Edge other : base.to.getInEdges(66)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    break;
  case 71: /* Label2PrimFldStat */
    /* Label2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Label2Prim */
    for(Edge other : base.to.getInEdges(72)){
      addEdge(base.from, other.from, 46, base, other, false);
    }
    break;
  case 72: /* loadStatPrimCtxt */
    /* _loadStatPrimCtxt[i] + (Label2PrimFldStat[i] *) => Label2Prim */
    for(Edge other : base.to.getInEdges(71)){
      addEdge(other.from, base.from, 46, base, other, false);
    }
    break;
  case 73: /* storePrimCtxt */
    /* _storePrimCtxt[i] + (Label2Prim *) => %18[i] */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 74, base, other, true);
    }
    break;
  case 74: /* %18 */
    /* %18[i] + (* ptG) => Label2PrimFld[i] */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 57, base, other, true);
    }
    break;
  case 75: /* storePrimCtxtArr */
    /* _storePrimCtxtArr + (Label2Prim *) => %19 */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 76, base, other, false);
    }
    break;
  case 76: /* %19 */
    /* %19 + (* ptG) => Label2PrimFldArr */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 62, base, other, false);
    }
    break;
  case 77: /* storeStatPrimCtxt */
    /* _storeStatPrimCtxt[i] + (Label2Prim *) => Label2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 71, base, other, true);
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
  case 24:
    return (short)1;
  case 25:
    return (short)1;
  case 26:
    return (short)1;
  case 27:
    return (short)1;
  case 28:
    return (short)1;
  case 29:
    return (short)1;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}
