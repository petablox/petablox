package petablox.android.missingmodels.grammars;
import petablox.android.missingmodels.util.jcflsolver.*;

/* Original Grammar:
###################
# CONFIGURATION
###################

.weights transfer 1

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

Ref2RefT :: ref2RefT
Ref2PrimT :: ref2PrimT
Prim2RefT :: prim2RefT
Prim2PrimT :: prim2PrimT

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
# RULES: PARTIAL FLOW PROPAGATION
###################

PreFlowsTo :: preFlowsTo
PostFlowsTo :: postFlowsTo
MidFlowsTo :: midFlowsTo

PreFlowsTo :: PreFlowsTo transfer MidFlowsTo
PreFlowsTo :: PreFlowsTo transferSelf MidFlowsTo

Pt :: _PostFlowsTo _transfer _PreFlowsTo
Pt :: _PostFlowsTo _transferSelf _PreFlowsTo
FptArr :: _Pt StoreArr Pt

Pt :: ptH

Fpt[f] :: fpt[f]
FptArr :: fptArr

###################
# RULES: OBJECT ANNOTATIONS
###################

Obj2RefT :: _Pt Ref2RefT
Obj2PrimT :: _Pt Ref2PrimT
Obj2RefT :: _FptArr Obj2RefT
Obj2PrimT :: _FptArr Obj2PrimT

Label2ObjT :: Label2RefT Pt
Label2ObjT :: Label2ObjT Fpt[*]

###################
# RULES: SINKF
###################

# Sink_full-obj flow

SinkF2Obj :: SinkF2RefF Pt
SinkF2Obj :: sink2Label Label2Obj _Pt _Ref2RefF Pt
SinkF2Obj :: sink2Label Label2Prim _Ref2PrimF Pt
SinkF2Obj :: SinkF2Obj Fpt[*]

# Sink_full-prim flow

SinkF2Prim :: SinkF2PrimF
SinkF2Prim :: sink2Label Label2Obj _Pt _Prim2RefF
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

Label2ObjX :: Label2Obj Obj2RefT Pt
Label2ObjX :: Label2Prim Prim2RefT Pt
Label2ObjX :: Label2PrimFldArr Obj2RefT Pt
Label2ObjX :: Label2ObjX FptArr

# Label-prim flow

Label2Prim :: Label2PrimT
Label2Prim :: Label2Prim _assignPrimCtxt
Label2Prim :: Label2Prim _assignPrimCCtxt

Label2Prim :: Label2Obj Obj2PrimT
Label2Prim :: Label2Prim Prim2PrimT

Label2Prim :: Label2ObjT _Pt _loadPrimCtxt[*]
Label2Prim :: Label2ObjX _Pt _loadPrimCtxtArr
Label2Prim :: Label2PrimFldArr Obj2PrimT

# cl Label2PrimFld[f] o _Pt v_c _loadPrimCtxt[f] u_c
Label2Prim :: Label2PrimFld[f] _Pt _loadPrimCtxt[f]
Label2Prim :: Label2PrimFldStat[f] _loadStatPrimCtxt[f]

# Label-prim_fld flow

Label2PrimFld[f] :: Label2Prim _storePrimCtxt[f] Pt
Label2PrimFldArr :: Label2Prim _storePrimCtxtArr Pt
Label2PrimFldStat[f] :: Label2Prim _storeStatPrimCtxt[f]
*/

/* Normalized Grammar:
Label2Obj:
	Label2Obj :: Label2ObjT
	Label2Obj :: Label2ObjX
PostFlowsTo:
	PostFlowsTo :: postFlowsTo
Pt:
	Pt :: %2 _PreFlowsTo
	Pt :: %3 _PreFlowsTo
	Pt :: ptH
%14:
	%14 :: src2Label Label2Prim
Fpt:
	Fpt[i] :: fpt[i]
%9:
	%9 :: %8 _Ref2PrimF
%8:
	%8 :: sink2Label Label2Prim
%5:
	%5 :: sink2Label Label2Obj
%4:
	%4 :: _Pt StoreArr
%7:
	%7 :: %6 _Ref2RefF
%6:
	%6 :: %5 _Pt
%1:
	%1 :: PreFlowsTo transferSelf
%0:
	%0 :: PreFlowsTo transfer
%3:
	%3 :: _PostFlowsTo _transferSelf
%2:
	%2 :: _PostFlowsTo _transfer
%20:
	%20 :: Label2ObjX _Pt
%21:
	%21[i] :: Label2PrimFld[i] _Pt
%22:
	%22[i] :: Label2Prim _storePrimCtxt[i]
Label2RefT:
	Label2RefT :: label2RefT
FptArr:
	FptArr :: %4 Pt
	FptArr :: fptArr
Label2PrimFldStat:
	Label2PrimFldStat[i] :: Label2Prim _storeStatPrimCtxt[i]
SinkF2PrimF:
	SinkF2PrimF :: sinkF2PrimF
Label2ObjT:
	Label2ObjT :: Label2RefT Pt
	Label2ObjT :: Label2ObjT Fpt
PreFlowsTo:
	PreFlowsTo :: preFlowsTo
	PreFlowsTo :: %0 MidFlowsTo
	PreFlowsTo :: %1 MidFlowsTo
Label2ObjX:
	Label2ObjX :: %16 Pt
	Label2ObjX :: %17 Pt
	Label2ObjX :: %18 Pt
	Label2ObjX :: Label2ObjX FptArr
Label2PrimFld:
	Label2PrimFld[i] :: %22[i] Pt
Src2Sink:
	Src2Sink :: %13 _SinkF2Obj
	Src2Sink :: %14 _SinkF2Prim
	Src2Sink :: %15 _SinkF2Obj
Label2PrimFldArr:
	Label2PrimFldArr :: %23 Pt
Prim2PrimF:
	Prim2PrimF :: prim2PrimF
SinkF2RefF:
	SinkF2RefF :: sinkF2RefF
%19:
	%19 :: Label2ObjT _Pt
Obj2PrimT:
	Obj2PrimT :: _Pt Ref2PrimT
	Obj2PrimT :: _FptArr Obj2PrimT
Prim2PrimT:
	Prim2PrimT :: prim2PrimT
Label2Prim:
	Label2Prim :: Label2PrimT
	Label2Prim :: Label2Prim _assignPrimCtxt
	Label2Prim :: Label2Prim _assignPrimCCtxt
	Label2Prim :: Label2Obj Obj2PrimT
	Label2Prim :: Label2Prim Prim2PrimT
	Label2Prim :: %19 _loadPrimCtxt
	Label2Prim :: %20 _loadPrimCtxtArr
	Label2Prim :: Label2PrimFldArr Obj2PrimT
	Label2Prim :: %21[i] _loadPrimCtxt[i]
	Label2Prim :: Label2PrimFldStat[i] _loadStatPrimCtxt[i]
Obj2RefT:
	Obj2RefT :: _Pt Ref2RefT
	Obj2RefT :: _FptArr Obj2RefT
Ref2PrimT:
	Ref2PrimT :: ref2PrimT
%18:
	%18 :: Label2PrimFldArr Obj2RefT
SinkF2Prim:
	SinkF2Prim :: SinkF2PrimF
	SinkF2Prim :: %11 _Prim2RefF
	SinkF2Prim :: %12 _Prim2PrimF
Ref2RefF:
	Ref2RefF :: ref2RefF
%23:
	%23 :: Label2Prim _storePrimCtxtArr
%11:
	%11 :: %10 _Pt
%10:
	%10 :: sink2Label Label2Obj
Prim2RefT:
	Prim2RefT :: prim2RefT
%12:
	%12 :: sink2Label Label2Prim
%15:
	%15 :: src2Label Label2PrimFld
SinkF2Obj:
	SinkF2Obj :: SinkF2RefF Pt
	SinkF2Obj :: %7 Pt
	SinkF2Obj :: %9 Pt
	SinkF2Obj :: SinkF2Obj Fpt
%17:
	%17 :: Label2Prim Prim2RefT
%16:
	%16 :: Label2Obj Obj2RefT
Ref2PrimF:
	Ref2PrimF :: ref2PrimF
Label2PrimT:
	Label2PrimT :: label2PrimT
Ref2RefT:
	Ref2RefT :: ref2RefT
Prim2RefF:
	Prim2RefF :: prim2RefF
MidFlowsTo:
	MidFlowsTo :: midFlowsTo
%13:
	%13 :: src2Label Label2Obj
*/

/* Reverse Productions:
ref2PrimT:
	ref2PrimT => Ref2PrimT
sink2Label:
	sink2Label + (* Label2Obj) => %5
	sink2Label + (* Label2Prim) => %8
	sink2Label + (* Label2Obj) => %10
	sink2Label + (* Label2Prim) => %12
ref2RefF:
	ref2RefF => Ref2RefF
PostFlowsTo:
	_PostFlowsTo + (* _transfer) => %2
	_PostFlowsTo + (* _transferSelf) => %3
ref2RefT:
	ref2RefT => Ref2RefT
assignPrimCtxt:
	_assignPrimCtxt + (Label2Prim *) => Label2Prim
prim2RefT:
	prim2RefT => Prim2RefT
%12:
	%12 + (* _Prim2PrimF) => SinkF2Prim
%14:
	%14 + (* _SinkF2Prim) => Src2Sink
fptArr:
	fptArr => FptArr
ref2PrimF:
	ref2PrimF => Ref2PrimF
label2PrimT:
	label2PrimT => Label2PrimT
%9:
	%9 + (* Pt) => SinkF2Obj
Label2Prim:
	Label2Prim + (sink2Label *) => %8
	Label2Prim + (sink2Label *) => %12
	Label2Prim + (src2Label *) => %14
	Label2Prim + (* Prim2RefT) => %17
	Label2Prim + (* _assignPrimCtxt) => Label2Prim
	Label2Prim + (* _assignPrimCCtxt) => Label2Prim
	Label2Prim + (* Prim2PrimT) => Label2Prim
	Label2Prim + (* _storePrimCtxt[i]) => %22[i]
	Label2Prim + (* _storePrimCtxtArr) => %23
	Label2Prim + (* _storeStatPrimCtxt[i]) => Label2PrimFldStat[i]
sinkF2PrimF:
	sinkF2PrimF => SinkF2PrimF
loadPrimCtxt:
	_loadPrimCtxt + (%19 *) => Label2Prim
	_loadPrimCtxt[i] + (%21[i] *) => Label2Prim
prim2RefF:
	prim2RefF => Prim2RefF
%4:
	%4 + (* Pt) => FptArr
%7:
	%7 + (* Pt) => SinkF2Obj
%6:
	%6 + (* _Ref2RefF) => %7
%1:
	%1 + (* MidFlowsTo) => PreFlowsTo
%0:
	%0 + (* MidFlowsTo) => PreFlowsTo
%3:
	%3 + (* _PreFlowsTo) => Pt
%2:
	%2 + (* _PreFlowsTo) => Pt
%20:
	%20 + (* _loadPrimCtxtArr) => Label2Prim
postFlowsTo:
	postFlowsTo => PostFlowsTo
%5:
	%5 + (* _Pt) => %6
ptH:
	ptH => Pt
transfer:
	transfer + (PreFlowsTo *) => %0
	_transfer + (_PostFlowsTo *) => %2
%17:
	%17 + (* Pt) => Label2ObjX
src2Label:
	src2Label + (* Label2Obj) => %13
	src2Label + (* Label2Prim) => %14
	src2Label + (* Label2PrimFld) => %15
storeStatPrimCtxt:
	_storeStatPrimCtxt[i] + (Label2Prim *) => Label2PrimFldStat[i]
fpt:
	fpt[i] => Fpt[i]
transferSelf:
	transferSelf + (PreFlowsTo *) => %1
	_transferSelf + (_PostFlowsTo *) => %3
sinkF2RefF:
	sinkF2RefF => SinkF2RefF
Label2PrimFldStat:
	Label2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Label2Prim
SinkF2PrimF:
	SinkF2PrimF => SinkF2Prim
Label2ObjT:
	Label2ObjT + (* Fpt) => Label2ObjT
	Label2ObjT => Label2Obj
	Label2ObjT + (* _Pt) => %19
%21:
	%21[i] + (* _loadPrimCtxt[i]) => Label2Prim
PreFlowsTo:
	PreFlowsTo + (* transfer) => %0
	PreFlowsTo + (* transferSelf) => %1
	_PreFlowsTo + (%2 *) => Pt
	_PreFlowsTo + (%3 *) => Pt
Label2ObjX:
	Label2ObjX => Label2Obj
	Label2ObjX + (* FptArr) => Label2ObjX
	Label2ObjX + (* _Pt) => %20
storePrimCtxt:
	_storePrimCtxt[i] + (Label2Prim *) => %22[i]
Label2PrimFld:
	Label2PrimFld + (src2Label *) => %15
	Label2PrimFld[i] + (* _Pt) => %21[i]
label2RefT:
	label2RefT => Label2RefT
loadStatPrimCtxt:
	_loadStatPrimCtxt[i] + (Label2PrimFldStat[i] *) => Label2Prim
Label2PrimFldArr:
	Label2PrimFldArr + (* Obj2RefT) => %18
	Label2PrimFldArr + (* Obj2PrimT) => Label2Prim
Prim2PrimF:
	_Prim2PrimF + (%12 *) => SinkF2Prim
%22:
	%22[i] + (* Pt) => Label2PrimFld[i]
storePrimCtxtArr:
	_storePrimCtxtArr + (Label2Prim *) => %23
MidFlowsTo:
	MidFlowsTo + (%0 *) => PreFlowsTo
	MidFlowsTo + (%1 *) => PreFlowsTo
%19:
	%19 + (* _loadPrimCtxt) => Label2Prim
Obj2PrimT:
	Obj2PrimT + (_FptArr *) => Obj2PrimT
	Obj2PrimT + (Label2Obj *) => Label2Prim
	Obj2PrimT + (Label2PrimFldArr *) => Label2Prim
FptArr:
	_FptArr + (* Obj2RefT) => Obj2RefT
	_FptArr + (* Obj2PrimT) => Obj2PrimT
	FptArr + (Label2ObjX *) => Label2ObjX
Pt:
	_Pt + (* StoreArr) => %4
	Pt + (%4 *) => FptArr
	_Pt + (* Ref2RefT) => Obj2RefT
	_Pt + (* Ref2PrimT) => Obj2PrimT
	Pt + (Label2RefT *) => Label2ObjT
	Pt + (SinkF2RefF *) => SinkF2Obj
	_Pt + (%5 *) => %6
	Pt + (%7 *) => SinkF2Obj
	Pt + (%9 *) => SinkF2Obj
	_Pt + (%10 *) => %11
	Pt + (%16 *) => Label2ObjX
	Pt + (%17 *) => Label2ObjX
	Pt + (%18 *) => Label2ObjX
	_Pt + (Label2ObjT *) => %19
	_Pt + (Label2ObjX *) => %20
	_Pt + (Label2PrimFld[i] *) => %21[i]
	Pt + (%22[i] *) => Label2PrimFld[i]
	Pt + (%23 *) => Label2PrimFldArr
preFlowsTo:
	preFlowsTo => PreFlowsTo
Prim2PrimT:
	Prim2PrimT + (Label2Prim *) => Label2Prim
%23:
	%23 + (* Pt) => Label2PrimFldArr
Obj2RefT:
	Obj2RefT + (_FptArr *) => Obj2RefT
	Obj2RefT + (Label2Obj *) => %16
	Obj2RefT + (Label2PrimFldArr *) => %18
Prim2RefT:
	Prim2RefT + (Label2Prim *) => %17
Ref2PrimT:
	Ref2PrimT + (_Pt *) => Obj2PrimT
%18:
	%18 + (* Pt) => Label2ObjX
SinkF2Prim:
	_SinkF2Prim + (%14 *) => Src2Sink
Ref2RefF:
	_Ref2RefF + (%6 *) => %7
loadPrimCtxtArr:
	_loadPrimCtxtArr + (%20 *) => Label2Prim
Fpt:
	Fpt + (Label2ObjT *) => Label2ObjT
	Fpt + (SinkF2Obj *) => SinkF2Obj
%11:
	%11 + (* _Prim2RefF) => SinkF2Prim
StoreArr:
	StoreArr + (_Pt *) => %4
Label2Obj:
	Label2Obj + (sink2Label *) => %5
	Label2Obj + (sink2Label *) => %10
	Label2Obj + (src2Label *) => %13
	Label2Obj + (* Obj2RefT) => %16
	Label2Obj + (* Obj2PrimT) => Label2Prim
Label2RefT:
	Label2RefT + (* Pt) => Label2ObjT
%15:
	%15 + (* _SinkF2Obj) => Src2Sink
SinkF2Obj:
	SinkF2Obj + (* Fpt) => SinkF2Obj
	_SinkF2Obj + (%13 *) => Src2Sink
	_SinkF2Obj + (%15 *) => Src2Sink
prim2PrimF:
	prim2PrimF => Prim2PrimF
%16:
	%16 + (* Pt) => Label2ObjX
assignPrimCCtxt:
	_assignPrimCCtxt + (Label2Prim *) => Label2Prim
Ref2PrimF:
	_Ref2PrimF + (%8 *) => %9
Label2PrimT:
	Label2PrimT => Label2Prim
midFlowsTo:
	midFlowsTo => MidFlowsTo
Ref2RefT:
	Ref2RefT + (_Pt *) => Obj2RefT
%10:
	%10 + (* _Pt) => %11
Prim2RefF:
	_Prim2RefF + (%11 *) => SinkF2Prim
SinkF2RefF:
	SinkF2RefF + (* Pt) => SinkF2Obj
prim2PrimT:
	prim2PrimT => Prim2PrimT
%13:
	%13 + (* _SinkF2Obj) => Src2Sink
%8:
	%8 + (* _Ref2PrimF) => %9
*/

public class H extends Graph {

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
  case 25:
  case 27:
  case 29:
  case 30:
  case 32:
  case 40:
  case 42:
  case 43:
  case 48:
  case 61:
  case 71:
  case 72:
  case 73:
  case 75:
  case 79:
  case 80:
  case 82:
  case 84:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 85;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("Ref2RefT")) return 0;
  if (symbol.equals("ref2RefT")) return 1;
  if (symbol.equals("Ref2PrimT")) return 2;
  if (symbol.equals("ref2PrimT")) return 3;
  if (symbol.equals("Prim2RefT")) return 4;
  if (symbol.equals("prim2RefT")) return 5;
  if (symbol.equals("Prim2PrimT")) return 6;
  if (symbol.equals("prim2PrimT")) return 7;
  if (symbol.equals("Label2RefT")) return 8;
  if (symbol.equals("label2RefT")) return 9;
  if (symbol.equals("Label2PrimT")) return 10;
  if (symbol.equals("label2PrimT")) return 11;
  if (symbol.equals("SinkF2RefF")) return 12;
  if (symbol.equals("sinkF2RefF")) return 13;
  if (symbol.equals("SinkF2PrimF")) return 14;
  if (symbol.equals("sinkF2PrimF")) return 15;
  if (symbol.equals("Ref2RefF")) return 16;
  if (symbol.equals("ref2RefF")) return 17;
  if (symbol.equals("Ref2PrimF")) return 18;
  if (symbol.equals("ref2PrimF")) return 19;
  if (symbol.equals("Prim2RefF")) return 20;
  if (symbol.equals("prim2RefF")) return 21;
  if (symbol.equals("Prim2PrimF")) return 22;
  if (symbol.equals("prim2PrimF")) return 23;
  if (symbol.equals("PreFlowsTo")) return 24;
  if (symbol.equals("preFlowsTo")) return 25;
  if (symbol.equals("PostFlowsTo")) return 26;
  if (symbol.equals("postFlowsTo")) return 27;
  if (symbol.equals("MidFlowsTo")) return 28;
  if (symbol.equals("midFlowsTo")) return 29;
  if (symbol.equals("transfer")) return 30;
  if (symbol.equals("%0")) return 31;
  if (symbol.equals("transferSelf")) return 32;
  if (symbol.equals("%1")) return 33;
  if (symbol.equals("Pt")) return 34;
  if (symbol.equals("%2")) return 35;
  if (symbol.equals("%3")) return 36;
  if (symbol.equals("FptArr")) return 37;
  if (symbol.equals("StoreArr")) return 38;
  if (symbol.equals("%4")) return 39;
  if (symbol.equals("ptH")) return 40;
  if (symbol.equals("Fpt")) return 41;
  if (symbol.equals("fpt")) return 42;
  if (symbol.equals("fptArr")) return 43;
  if (symbol.equals("Obj2RefT")) return 44;
  if (symbol.equals("Obj2PrimT")) return 45;
  if (symbol.equals("Label2ObjT")) return 46;
  if (symbol.equals("SinkF2Obj")) return 47;
  if (symbol.equals("sink2Label")) return 48;
  if (symbol.equals("Label2Obj")) return 49;
  if (symbol.equals("%5")) return 50;
  if (symbol.equals("%6")) return 51;
  if (symbol.equals("%7")) return 52;
  if (symbol.equals("Label2Prim")) return 53;
  if (symbol.equals("%8")) return 54;
  if (symbol.equals("%9")) return 55;
  if (symbol.equals("SinkF2Prim")) return 56;
  if (symbol.equals("%10")) return 57;
  if (symbol.equals("%11")) return 58;
  if (symbol.equals("%12")) return 59;
  if (symbol.equals("Src2Sink")) return 60;
  if (symbol.equals("src2Label")) return 61;
  if (symbol.equals("%13")) return 62;
  if (symbol.equals("%14")) return 63;
  if (symbol.equals("Label2PrimFld")) return 64;
  if (symbol.equals("%15")) return 65;
  if (symbol.equals("Label2ObjX")) return 66;
  if (symbol.equals("%16")) return 67;
  if (symbol.equals("%17")) return 68;
  if (symbol.equals("Label2PrimFldArr")) return 69;
  if (symbol.equals("%18")) return 70;
  if (symbol.equals("assignPrimCtxt")) return 71;
  if (symbol.equals("assignPrimCCtxt")) return 72;
  if (symbol.equals("loadPrimCtxt")) return 73;
  if (symbol.equals("%19")) return 74;
  if (symbol.equals("loadPrimCtxtArr")) return 75;
  if (symbol.equals("%20")) return 76;
  if (symbol.equals("%21")) return 77;
  if (symbol.equals("Label2PrimFldStat")) return 78;
  if (symbol.equals("loadStatPrimCtxt")) return 79;
  if (symbol.equals("storePrimCtxt")) return 80;
  if (symbol.equals("%22")) return 81;
  if (symbol.equals("storePrimCtxtArr")) return 82;
  if (symbol.equals("%23")) return 83;
  if (symbol.equals("storeStatPrimCtxt")) return 84;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "Ref2RefT";
  case 1: return "ref2RefT";
  case 2: return "Ref2PrimT";
  case 3: return "ref2PrimT";
  case 4: return "Prim2RefT";
  case 5: return "prim2RefT";
  case 6: return "Prim2PrimT";
  case 7: return "prim2PrimT";
  case 8: return "Label2RefT";
  case 9: return "label2RefT";
  case 10: return "Label2PrimT";
  case 11: return "label2PrimT";
  case 12: return "SinkF2RefF";
  case 13: return "sinkF2RefF";
  case 14: return "SinkF2PrimF";
  case 15: return "sinkF2PrimF";
  case 16: return "Ref2RefF";
  case 17: return "ref2RefF";
  case 18: return "Ref2PrimF";
  case 19: return "ref2PrimF";
  case 20: return "Prim2RefF";
  case 21: return "prim2RefF";
  case 22: return "Prim2PrimF";
  case 23: return "prim2PrimF";
  case 24: return "PreFlowsTo";
  case 25: return "preFlowsTo";
  case 26: return "PostFlowsTo";
  case 27: return "postFlowsTo";
  case 28: return "MidFlowsTo";
  case 29: return "midFlowsTo";
  case 30: return "transfer";
  case 31: return "%0";
  case 32: return "transferSelf";
  case 33: return "%1";
  case 34: return "Pt";
  case 35: return "%2";
  case 36: return "%3";
  case 37: return "FptArr";
  case 38: return "StoreArr";
  case 39: return "%4";
  case 40: return "ptH";
  case 41: return "Fpt";
  case 42: return "fpt";
  case 43: return "fptArr";
  case 44: return "Obj2RefT";
  case 45: return "Obj2PrimT";
  case 46: return "Label2ObjT";
  case 47: return "SinkF2Obj";
  case 48: return "sink2Label";
  case 49: return "Label2Obj";
  case 50: return "%5";
  case 51: return "%6";
  case 52: return "%7";
  case 53: return "Label2Prim";
  case 54: return "%8";
  case 55: return "%9";
  case 56: return "SinkF2Prim";
  case 57: return "%10";
  case 58: return "%11";
  case 59: return "%12";
  case 60: return "Src2Sink";
  case 61: return "src2Label";
  case 62: return "%13";
  case 63: return "%14";
  case 64: return "Label2PrimFld";
  case 65: return "%15";
  case 66: return "Label2ObjX";
  case 67: return "%16";
  case 68: return "%17";
  case 69: return "Label2PrimFldArr";
  case 70: return "%18";
  case 71: return "assignPrimCtxt";
  case 72: return "assignPrimCCtxt";
  case 73: return "loadPrimCtxt";
  case 74: return "%19";
  case 75: return "loadPrimCtxtArr";
  case 76: return "%20";
  case 77: return "%21";
  case 78: return "Label2PrimFldStat";
  case 79: return "loadStatPrimCtxt";
  case 80: return "storePrimCtxt";
  case 81: return "%22";
  case 82: return "storePrimCtxtArr";
  case 83: return "%23";
  case 84: return "storeStatPrimCtxt";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* Ref2RefT */
    /* Ref2RefT + (_Pt *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(34)){
      addEdge(other.to, base.to, 44, base, other, false);
    }
    break;
  case 1: /* ref2RefT */
    /* ref2RefT => Ref2RefT */
    addEdge(base.from, base.to, 0, base, false);
    break;
  case 2: /* Ref2PrimT */
    /* Ref2PrimT + (_Pt *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(34)){
      addEdge(other.to, base.to, 45, base, other, false);
    }
    break;
  case 3: /* ref2PrimT */
    /* ref2PrimT => Ref2PrimT */
    addEdge(base.from, base.to, 2, base, false);
    break;
  case 4: /* Prim2RefT */
    /* Prim2RefT + (Label2Prim *) => %17 */
    for(Edge other : base.from.getInEdges(53)){
      addEdge(other.from, base.to, 68, base, other, false);
    }
    break;
  case 5: /* prim2RefT */
    /* prim2RefT => Prim2RefT */
    addEdge(base.from, base.to, 4, base, false);
    break;
  case 6: /* Prim2PrimT */
    /* Prim2PrimT + (Label2Prim *) => Label2Prim */
    for(Edge other : base.from.getInEdges(53)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    break;
  case 7: /* prim2PrimT */
    /* prim2PrimT => Prim2PrimT */
    addEdge(base.from, base.to, 6, base, false);
    break;
  case 8: /* Label2RefT */
    /* Label2RefT + (* Pt) => Label2ObjT */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    break;
  case 9: /* label2RefT */
    /* label2RefT => Label2RefT */
    addEdge(base.from, base.to, 8, base, false);
    break;
  case 10: /* Label2PrimT */
    /* Label2PrimT => Label2Prim */
    addEdge(base.from, base.to, 53, base, false);
    break;
  case 11: /* label2PrimT */
    /* label2PrimT => Label2PrimT */
    addEdge(base.from, base.to, 10, base, false);
    break;
  case 12: /* SinkF2RefF */
    /* SinkF2RefF + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    break;
  case 13: /* sinkF2RefF */
    /* sinkF2RefF => SinkF2RefF */
    addEdge(base.from, base.to, 12, base, false);
    break;
  case 14: /* SinkF2PrimF */
    /* SinkF2PrimF => SinkF2Prim */
    addEdge(base.from, base.to, 56, base, false);
    break;
  case 15: /* sinkF2PrimF */
    /* sinkF2PrimF => SinkF2PrimF */
    addEdge(base.from, base.to, 14, base, false);
    break;
  case 16: /* Ref2RefF */
    /* _Ref2RefF + (%6 *) => %7 */
    for(Edge other : base.to.getInEdges(51)){
      addEdge(other.from, base.from, 52, base, other, false);
    }
    break;
  case 17: /* ref2RefF */
    /* ref2RefF => Ref2RefF */
    addEdge(base.from, base.to, 16, base, false);
    break;
  case 18: /* Ref2PrimF */
    /* _Ref2PrimF + (%8 *) => %9 */
    for(Edge other : base.to.getInEdges(54)){
      addEdge(other.from, base.from, 55, base, other, false);
    }
    break;
  case 19: /* ref2PrimF */
    /* ref2PrimF => Ref2PrimF */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 20: /* Prim2RefF */
    /* _Prim2RefF + (%11 *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 56, base, other, false);
    }
    break;
  case 21: /* prim2RefF */
    /* prim2RefF => Prim2RefF */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 22: /* Prim2PrimF */
    /* _Prim2PrimF + (%12 *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(59)){
      addEdge(other.from, base.from, 56, base, other, false);
    }
    break;
  case 23: /* prim2PrimF */
    /* prim2PrimF => Prim2PrimF */
    addEdge(base.from, base.to, 22, base, false);
    break;
  case 24: /* PreFlowsTo */
    /* PreFlowsTo + (* transfer) => %0 */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 31, base, other, false);
    }
    /* PreFlowsTo + (* transferSelf) => %1 */
    for(Edge other : base.to.getOutEdges(32)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    /* _PreFlowsTo + (%2 *) => Pt */
    for(Edge other : base.to.getInEdges(35)){
      addEdge(other.from, base.from, 34, base, other, false);
    }
    /* _PreFlowsTo + (%3 *) => Pt */
    for(Edge other : base.to.getInEdges(36)){
      addEdge(other.from, base.from, 34, base, other, false);
    }
    break;
  case 25: /* preFlowsTo */
    /* preFlowsTo => PreFlowsTo */
    addEdge(base.from, base.to, 24, base, false);
    break;
  case 26: /* PostFlowsTo */
    /* _PostFlowsTo + (* _transfer) => %2 */
    for(Edge other : base.from.getInEdges(30)){
      addEdge(base.to, other.from, 35, base, other, false);
    }
    /* _PostFlowsTo + (* _transferSelf) => %3 */
    for(Edge other : base.from.getInEdges(32)){
      addEdge(base.to, other.from, 36, base, other, false);
    }
    break;
  case 27: /* postFlowsTo */
    /* postFlowsTo => PostFlowsTo */
    addEdge(base.from, base.to, 26, base, false);
    break;
  case 28: /* MidFlowsTo */
    /* MidFlowsTo + (%0 *) => PreFlowsTo */
    for(Edge other : base.from.getInEdges(31)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    /* MidFlowsTo + (%1 *) => PreFlowsTo */
    for(Edge other : base.from.getInEdges(33)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    break;
  case 29: /* midFlowsTo */
    /* midFlowsTo => MidFlowsTo */
    addEdge(base.from, base.to, 28, base, false);
    break;
  case 30: /* transfer */
    /* transfer + (PreFlowsTo *) => %0 */
    for(Edge other : base.from.getInEdges(24)){
      addEdge(other.from, base.to, 31, base, other, false);
    }
    /* _transfer + (_PostFlowsTo *) => %2 */
    for(Edge other : base.to.getOutEdges(26)){
      addEdge(other.to, base.from, 35, base, other, false);
    }
    break;
  case 31: /* %0 */
    /* %0 + (* MidFlowsTo) => PreFlowsTo */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    break;
  case 32: /* transferSelf */
    /* transferSelf + (PreFlowsTo *) => %1 */
    for(Edge other : base.from.getInEdges(24)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    /* _transferSelf + (_PostFlowsTo *) => %3 */
    for(Edge other : base.to.getOutEdges(26)){
      addEdge(other.to, base.from, 36, base, other, false);
    }
    break;
  case 33: /* %1 */
    /* %1 + (* MidFlowsTo) => PreFlowsTo */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    break;
  case 34: /* Pt */
    /* _Pt + (* StoreArr) => %4 */
    for(Edge other : base.from.getOutEdges(38)){
      addEdge(base.to, other.to, 39, base, other, false);
    }
    /* Pt + (%4 *) => FptArr */
    for(Edge other : base.from.getInEdges(39)){
      addEdge(other.from, base.to, 37, base, other, false);
    }
    /* _Pt + (* Ref2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(0)){
      addEdge(base.to, other.to, 44, base, other, false);
    }
    /* _Pt + (* Ref2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(2)){
      addEdge(base.to, other.to, 45, base, other, false);
    }
    /* Pt + (Label2RefT *) => Label2ObjT */
    for(Edge other : base.from.getInEdges(8)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    /* Pt + (SinkF2RefF *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(12)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    /* _Pt + (%5 *) => %6 */
    for(Edge other : base.to.getInEdges(50)){
      addEdge(other.from, base.from, 51, base, other, false);
    }
    /* Pt + (%7 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(52)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    /* Pt + (%9 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(55)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    /* _Pt + (%10 *) => %11 */
    for(Edge other : base.to.getInEdges(57)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    /* Pt + (%16 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(67)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* Pt + (%17 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(68)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* Pt + (%18 *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(70)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* _Pt + (Label2ObjT *) => %19 */
    for(Edge other : base.to.getInEdges(46)){
      addEdge(other.from, base.from, 74, base, other, false);
    }
    /* _Pt + (Label2ObjX *) => %20 */
    for(Edge other : base.to.getInEdges(66)){
      addEdge(other.from, base.from, 76, base, other, false);
    }
    /* _Pt + (Label2PrimFld[i] *) => %21[i] */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 77, base, other, true);
    }
    /* Pt + (%22[i] *) => Label2PrimFld[i] */
    for(Edge other : base.from.getInEdges(81)){
      addEdge(other.from, base.to, 64, base, other, true);
    }
    /* Pt + (%23 *) => Label2PrimFldArr */
    for(Edge other : base.from.getInEdges(83)){
      addEdge(other.from, base.to, 69, base, other, false);
    }
    break;
  case 35: /* %2 */
    /* %2 + (* _PreFlowsTo) => Pt */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(base.from, other.from, 34, base, other, false);
    }
    break;
  case 36: /* %3 */
    /* %3 + (* _PreFlowsTo) => Pt */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(base.from, other.from, 34, base, other, false);
    }
    break;
  case 37: /* FptArr */
    /* _FptArr + (* Obj2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(44)){
      addEdge(base.to, other.to, 44, base, other, false);
    }
    /* _FptArr + (* Obj2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(45)){
      addEdge(base.to, other.to, 45, base, other, false);
    }
    /* FptArr + (Label2ObjX *) => Label2ObjX */
    for(Edge other : base.from.getInEdges(66)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    break;
  case 38: /* StoreArr */
    /* StoreArr + (_Pt *) => %4 */
    for(Edge other : base.from.getOutEdges(34)){
      addEdge(other.to, base.to, 39, base, other, false);
    }
    break;
  case 39: /* %4 */
    /* %4 + (* Pt) => FptArr */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 37, base, other, false);
    }
    break;
  case 40: /* ptH */
    /* ptH => Pt */
    addEdge(base.from, base.to, 34, base, false);
    break;
  case 41: /* Fpt */
    /* Fpt + (Label2ObjT *) => Label2ObjT */
    for(Edge other : base.from.getInEdges(46)){
      addEdge(other.from, base.to, 46, base, other, false);
    }
    /* Fpt + (SinkF2Obj *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(47)){
      addEdge(other.from, base.to, 47, base, other, false);
    }
    break;
  case 42: /* fpt */
    /* fpt[i] => Fpt[i] */
    addEdge(base.from, base.to, 41, base, true);
    break;
  case 43: /* fptArr */
    /* fptArr => FptArr */
    addEdge(base.from, base.to, 37, base, false);
    break;
  case 44: /* Obj2RefT */
    /* Obj2RefT + (_FptArr *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(37)){
      addEdge(other.to, base.to, 44, base, other, false);
    }
    /* Obj2RefT + (Label2Obj *) => %16 */
    for(Edge other : base.from.getInEdges(49)){
      addEdge(other.from, base.to, 67, base, other, false);
    }
    /* Obj2RefT + (Label2PrimFldArr *) => %18 */
    for(Edge other : base.from.getInEdges(69)){
      addEdge(other.from, base.to, 70, base, other, false);
    }
    break;
  case 45: /* Obj2PrimT */
    /* Obj2PrimT + (_FptArr *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(37)){
      addEdge(other.to, base.to, 45, base, other, false);
    }
    /* Obj2PrimT + (Label2Obj *) => Label2Prim */
    for(Edge other : base.from.getInEdges(49)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    /* Obj2PrimT + (Label2PrimFldArr *) => Label2Prim */
    for(Edge other : base.from.getInEdges(69)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    break;
  case 46: /* Label2ObjT */
    /* Label2ObjT + (* Fpt) => Label2ObjT */
    for(Edge other : base.to.getOutEdges(41)){
      addEdge(base.from, other.to, 46, base, other, false);
    }
    /* Label2ObjT => Label2Obj */
    addEdge(base.from, base.to, 49, base, false);
    /* Label2ObjT + (* _Pt) => %19 */
    for(Edge other : base.to.getInEdges(34)){
      addEdge(base.from, other.from, 74, base, other, false);
    }
    break;
  case 47: /* SinkF2Obj */
    /* SinkF2Obj + (* Fpt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(41)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    /* _SinkF2Obj + (%13 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(62)){
      addEdge(other.from, base.from, 60, base, other, false);
    }
    /* _SinkF2Obj + (%15 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(65)){
      addEdge(other.from, base.from, 60, base, other, false);
    }
    break;
  case 48: /* sink2Label */
    /* sink2Label + (* Label2Obj) => %5 */
    for(Edge other : base.to.getOutEdges(49)){
      addEdge(base.from, other.to, 50, base, other, false);
    }
    /* sink2Label + (* Label2Prim) => %8 */
    for(Edge other : base.to.getOutEdges(53)){
      addEdge(base.from, other.to, 54, base, other, false);
    }
    /* sink2Label + (* Label2Obj) => %10 */
    for(Edge other : base.to.getOutEdges(49)){
      addEdge(base.from, other.to, 57, base, other, false);
    }
    /* sink2Label + (* Label2Prim) => %12 */
    for(Edge other : base.to.getOutEdges(53)){
      addEdge(base.from, other.to, 59, base, other, false);
    }
    break;
  case 49: /* Label2Obj */
    /* Label2Obj + (sink2Label *) => %5 */
    for(Edge other : base.from.getInEdges(48)){
      addEdge(other.from, base.to, 50, base, other, false);
    }
    /* Label2Obj + (sink2Label *) => %10 */
    for(Edge other : base.from.getInEdges(48)){
      addEdge(other.from, base.to, 57, base, other, false);
    }
    /* Label2Obj + (src2Label *) => %13 */
    for(Edge other : base.from.getInEdges(61)){
      addEdge(other.from, base.to, 62, base, other, false);
    }
    /* Label2Obj + (* Obj2RefT) => %16 */
    for(Edge other : base.to.getOutEdges(44)){
      addEdge(base.from, other.to, 67, base, other, false);
    }
    /* Label2Obj + (* Obj2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(45)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    break;
  case 50: /* %5 */
    /* %5 + (* _Pt) => %6 */
    for(Edge other : base.to.getInEdges(34)){
      addEdge(base.from, other.from, 51, base, other, false);
    }
    break;
  case 51: /* %6 */
    /* %6 + (* _Ref2RefF) => %7 */
    for(Edge other : base.to.getInEdges(16)){
      addEdge(base.from, other.from, 52, base, other, false);
    }
    break;
  case 52: /* %7 */
    /* %7 + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    break;
  case 53: /* Label2Prim */
    /* Label2Prim + (sink2Label *) => %8 */
    for(Edge other : base.from.getInEdges(48)){
      addEdge(other.from, base.to, 54, base, other, false);
    }
    /* Label2Prim + (sink2Label *) => %12 */
    for(Edge other : base.from.getInEdges(48)){
      addEdge(other.from, base.to, 59, base, other, false);
    }
    /* Label2Prim + (src2Label *) => %14 */
    for(Edge other : base.from.getInEdges(61)){
      addEdge(other.from, base.to, 63, base, other, false);
    }
    /* Label2Prim + (* Prim2RefT) => %17 */
    for(Edge other : base.to.getOutEdges(4)){
      addEdge(base.from, other.to, 68, base, other, false);
    }
    /* Label2Prim + (* _assignPrimCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(71)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    /* Label2Prim + (* _assignPrimCCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(72)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    /* Label2Prim + (* Prim2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(6)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    /* Label2Prim + (* _storePrimCtxt[i]) => %22[i] */
    for(Edge other : base.to.getInEdges(80)){
      addEdge(base.from, other.from, 81, base, other, true);
    }
    /* Label2Prim + (* _storePrimCtxtArr) => %23 */
    for(Edge other : base.to.getInEdges(82)){
      addEdge(base.from, other.from, 83, base, other, false);
    }
    /* Label2Prim + (* _storeStatPrimCtxt[i]) => Label2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(84)){
      addEdge(base.from, other.from, 78, base, other, true);
    }
    break;
  case 54: /* %8 */
    /* %8 + (* _Ref2PrimF) => %9 */
    for(Edge other : base.to.getInEdges(18)){
      addEdge(base.from, other.from, 55, base, other, false);
    }
    break;
  case 55: /* %9 */
    /* %9 + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 47, base, other, false);
    }
    break;
  case 56: /* SinkF2Prim */
    /* _SinkF2Prim + (%14 *) => Src2Sink */
    for(Edge other : base.to.getInEdges(63)){
      addEdge(other.from, base.from, 60, base, other, false);
    }
    break;
  case 57: /* %10 */
    /* %10 + (* _Pt) => %11 */
    for(Edge other : base.to.getInEdges(34)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    break;
  case 58: /* %11 */
    /* %11 + (* _Prim2RefF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(20)){
      addEdge(base.from, other.from, 56, base, other, false);
    }
    break;
  case 59: /* %12 */
    /* %12 + (* _Prim2PrimF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(22)){
      addEdge(base.from, other.from, 56, base, other, false);
    }
    break;
  case 61: /* src2Label */
    /* src2Label + (* Label2Obj) => %13 */
    for(Edge other : base.to.getOutEdges(49)){
      addEdge(base.from, other.to, 62, base, other, false);
    }
    /* src2Label + (* Label2Prim) => %14 */
    for(Edge other : base.to.getOutEdges(53)){
      addEdge(base.from, other.to, 63, base, other, false);
    }
    /* src2Label + (* Label2PrimFld) => %15 */
    for(Edge other : base.to.getOutEdges(64)){
      addEdge(base.from, other.to, 65, base, other, false);
    }
    break;
  case 62: /* %13 */
    /* %13 + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(47)){
      addEdge(base.from, other.from, 60, base, other, false);
    }
    break;
  case 63: /* %14 */
    /* %14 + (* _SinkF2Prim) => Src2Sink */
    for(Edge other : base.to.getInEdges(56)){
      addEdge(base.from, other.from, 60, base, other, false);
    }
    break;
  case 64: /* Label2PrimFld */
    /* Label2PrimFld + (src2Label *) => %15 */
    for(Edge other : base.from.getInEdges(61)){
      addEdge(other.from, base.to, 65, base, other, false);
    }
    /* Label2PrimFld[i] + (* _Pt) => %21[i] */
    for(Edge other : base.to.getInEdges(34)){
      addEdge(base.from, other.from, 77, base, other, true);
    }
    break;
  case 65: /* %15 */
    /* %15 + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(47)){
      addEdge(base.from, other.from, 60, base, other, false);
    }
    break;
  case 66: /* Label2ObjX */
    /* Label2ObjX => Label2Obj */
    addEdge(base.from, base.to, 49, base, false);
    /* Label2ObjX + (* FptArr) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    /* Label2ObjX + (* _Pt) => %20 */
    for(Edge other : base.to.getInEdges(34)){
      addEdge(base.from, other.from, 76, base, other, false);
    }
    break;
  case 67: /* %16 */
    /* %16 + (* Pt) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 68: /* %17 */
    /* %17 + (* Pt) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 69: /* Label2PrimFldArr */
    /* Label2PrimFldArr + (* Obj2RefT) => %18 */
    for(Edge other : base.to.getOutEdges(44)){
      addEdge(base.from, other.to, 70, base, other, false);
    }
    /* Label2PrimFldArr + (* Obj2PrimT) => Label2Prim */
    for(Edge other : base.to.getOutEdges(45)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    break;
  case 70: /* %18 */
    /* %18 + (* Pt) => Label2ObjX */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 71: /* assignPrimCtxt */
    /* _assignPrimCtxt + (Label2Prim *) => Label2Prim */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 72: /* assignPrimCCtxt */
    /* _assignPrimCCtxt + (Label2Prim *) => Label2Prim */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 73: /* loadPrimCtxt */
    /* _loadPrimCtxt + (%19 *) => Label2Prim */
    for(Edge other : base.to.getInEdges(74)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    /* _loadPrimCtxt[i] + (%21[i] *) => Label2Prim */
    for(Edge other : base.to.getInEdges(77)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 74: /* %19 */
    /* %19 + (* _loadPrimCtxt) => Label2Prim */
    for(Edge other : base.to.getInEdges(73)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 75: /* loadPrimCtxtArr */
    /* _loadPrimCtxtArr + (%20 *) => Label2Prim */
    for(Edge other : base.to.getInEdges(76)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 76: /* %20 */
    /* %20 + (* _loadPrimCtxtArr) => Label2Prim */
    for(Edge other : base.to.getInEdges(75)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 77: /* %21 */
    /* %21[i] + (* _loadPrimCtxt[i]) => Label2Prim */
    for(Edge other : base.to.getInEdges(73)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 78: /* Label2PrimFldStat */
    /* Label2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Label2Prim */
    for(Edge other : base.to.getInEdges(79)){
      addEdge(base.from, other.from, 53, base, other, false);
    }
    break;
  case 79: /* loadStatPrimCtxt */
    /* _loadStatPrimCtxt[i] + (Label2PrimFldStat[i] *) => Label2Prim */
    for(Edge other : base.to.getInEdges(78)){
      addEdge(other.from, base.from, 53, base, other, false);
    }
    break;
  case 80: /* storePrimCtxt */
    /* _storePrimCtxt[i] + (Label2Prim *) => %22[i] */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 81, base, other, true);
    }
    break;
  case 81: /* %22 */
    /* %22[i] + (* Pt) => Label2PrimFld[i] */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 64, base, other, true);
    }
    break;
  case 82: /* storePrimCtxtArr */
    /* _storePrimCtxtArr + (Label2Prim *) => %23 */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 83, base, other, false);
    }
    break;
  case 83: /* %23 */
    /* %23 + (* Pt) => Label2PrimFldArr */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 69, base, other, false);
    }
    break;
  case 84: /* storeStatPrimCtxt */
    /* _storeStatPrimCtxt[i] + (Label2Prim *) => Label2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 78, base, other, true);
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
  case 30:
    return (short)1;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}
