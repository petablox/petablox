package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
###################
# CONFIGURATION
###################

.output LabelRef3
.output LabelPrim3
.output Flow3

.weights refArg2RefArgTStub 1
.weights refArg2RefRetTStub 1

.weights primArg2RefArgTStub 1
.weights primArg2RefRetTStub 1

.weights refArg2PrimRetTStub 1
.weights prim2PrimT 1 

###################
# INPUTS
###################

# source annotations: src2RefT, src2PrimT
# sink annotations: sink2RefT, sink2PrimT, sinkF2RefF, sinkF2PrimF
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

Src2RefT :: src2RefT
Src2PrimT :: src2PrimT
Sink2RefT :: sink2RefT
Sink2PrimT :: sink2PrimT

# flow annotations

SinkF2RefF :: sinkF2RefF
SinkF2PrimF :: sinkF2PrimF

Ref2RefF :: ref2RefF
Ref2PrimF :: ref2PrimF
Prim2RefF :: prim2RefF
Prim2PrimF :: prim2PrimF

###################
# RULES: STUB ANNOTATION CONVERSION
###################

#Ref2RefT :: refArg2RefArgTStub
Ref2RefTStub :: refArg2RefArgTStub
#Ref2RefT :: refArg2RefRetTStub
Ref2RefTStub :: refArg2RefRetTStub

#Prim2RefT :: primArg2RefArgTStub
Prim2RefTStub :: primArg2RefArgTStub
#Prim2RefT :: primArg2RefRetTStub
Prim2RefTStub :: primArg2RefRetTStub

Ref2PrimT :: refArg2PrimRetTStub
#Ref2PrimTStub :: refArg2PrimRetTStub
Prim2PrimT :: primArg2PrimRetTStub
#Prim2PrimTStub :: primArg2PrimRetTStub

###################
# RULES: PARTIAL FLOW PROPAGATION
###################

PreFlowsTo :: preFlowsTo
PostFlowsTo :: postFlowsTo
MidFlowsTo :: midFlowsTo

PreFlowsTo :: PreFlowsTo Ref2RefTStub MidFlowsTo

Pt :: pt
Pt :: _PostFlowsTo _Ref2RefTStub _PreFlowsTo

# TODO: make Fpt and FptArr sound
# TODO: handle Prim2RefTStub

Fpt[f] :: fpt[f]
FptArr :: fptArr

###################
# RULES: OBJECT ANNOTATIONS
###################

Obj2RefT :: _Pt Ref2RefT
Obj2PrimT :: _Pt Ref2PrimT
Obj2RefT :: _FptArr Obj2RefT
Obj2PrimT :: _FptArr Obj2PrimT

Src2ObjT :: Src2RefT Pt
Src2ObjT :: Src2ObjT Fpt[*]

Sink2ObjT :: Sink2RefT Pt
Sink2ObjT :: Sink2ObjT Fpt[*]

###################
# RULES: SINKF
###################

# Sink_full-obj flow

SinkF2Obj :: SinkF2RefF Pt
SinkF2Obj :: Sink2Obj _Pt _Ref2RefF Pt
SinkF2Obj :: Sink2Prim _Ref2PrimF Pt
SinkF2Obj :: SinkF2Obj Fpt[*]

# Sink_full-prim flow

SinkF2Prim :: SinkF2PrimF
SinkF2Prim :: Sink2Obj _Pt _Prim2RefF
SinkF2Prim :: Sink2Prim _Prim2PrimF

###################
# RULES: SRC-SINK FLOW
###################

Src2Sink :: Src2Obj _SinkF2Obj
Src2Sink :: Src2Prim _SinkF2Prim
Src2Sink :: Src2PrimFld[*] _SinkF2Obj

###################
# RULES: LABEL FLOW
###################

# Src-obj flow

Src2Obj :: Src2ObjT
Src2Obj :: Src2ObjX

Src2ObjX :: Src2Obj Obj2RefT Pt
Src2ObjX :: Src2Prim Prim2RefT Pt
Src2ObjX :: Src2PrimFldArr Obj2RefT Pt
Src2ObjX :: Src2ObjX FptArr

# Sink-obj flow

Sink2Obj :: Sink2ObjT
Sink2Obj :: Sink2ObjX

Sink2ObjX :: Sink2Obj Obj2RefT Pt
Sink2ObjX :: Sink2Prim Prim2RefT Pt
Sink2ObjX :: Sink2PrimFldArr Obj2RefT Pt
Sink2ObjX :: Sink2ObjX FptArr

# Src-prim flow

Src2Prim :: Src2PrimT
Src2Prim :: Src2Prim _assignPrimCtxt
Src2Prim :: Src2Prim _assignPrimCCtxt

Src2Prim :: Src2Obj Obj2PrimT
Src2Prim :: Src2Prim Prim2PrimT

Src2Prim :: Src2ObjT _Pt _loadPrimCtxt[*]
Src2Prim :: Src2ObjX _Pt _loadPrimCtxtArr
Src2Prim :: Src2PrimFldArr Obj2PrimT

# cl Src2PrimFld[f] o _Pt v_c _loadPrimCtxt[f] u_c
Src2Prim :: Src2PrimFld[f] _Pt _loadPrimCtxt[f]
Src2Prim :: Src2PrimFldStat[f] _loadStatPrimCtxt[f]

# Src-prim_fld flow

Src2PrimFld[f] :: Src2Prim _storePrimCtxt[f] Pt
Src2PrimFldArr :: Src2Prim _storePrimCtxtArr Pt
Src2PrimFldStat[f] :: Src2Prim _storeStatPrimCtxt[f]

# Sink-prim flow

Sink2Prim :: Sink2PrimT
Sink2Prim :: Sink2Prim _assignPrimCtxt
Sink2Prim :: Sink2Prim _assignPrimCCtxt

Sink2Prim :: Sink2Obj Obj2PrimT
Sink2Prim :: Sink2Prim Prim2PrimT

Sink2Prim :: Sink2ObjT _Pt _loadPrimCtxt[*]
Sink2Prim :: Sink2ObjX _Pt _loadPrimCtxtArr
Sink2Prim :: Sink2PrimFldArr Obj2PrimT

# cl Sink2PrimFld[f] o _Pt v_c _loadPrimCtxt[f] u_c
Sink2Prim :: Sink2PrimFld[f] _Pt _loadPrimCtxt[f]
Sink2Prim :: Sink2PrimFldStat[f] _loadStatPrimCtxt[f]

# Sink-prim_fld flow

Sink2PrimFld[f] :: Sink2Prim _storePrimCtxt[f] Pt
Sink2PrimFldArr :: Sink2Prim _storePrimCtxtArr Pt
Sink2PrimFldStat[f] :: Sink2Prim _storeStatPrimCtxt[f]

###################
# RULES: OUTPUT
###################

LabelRef3 :: Src2Obj _Pt
LabelRef3 :: Sink2Obj _Pt
LabelPrim3 :: Src2Prim
LabelPrim3 :: Sink2Prim
Flow3 :: Src2Sink
*/

/* Normalized Grammar:
%13:
	%13 :: Src2ObjX _Pt
PostFlowsTo:
	PostFlowsTo :: postFlowsTo
Pt:
	Pt :: pt
	Pt :: %1 _PreFlowsTo
Flow3:
	Flow3 :: Src2Sink
SinkF2Obj:
	SinkF2Obj :: SinkF2RefF Pt
	SinkF2Obj :: %3 Pt
	SinkF2Obj :: %4 Pt
	SinkF2Obj :: SinkF2Obj Fpt
Sink2RefT:
	Sink2RefT :: sink2RefT
Sink2PrimFldStat:
	Sink2PrimFldStat[i] :: Sink2Prim _storeStatPrimCtxt[i]
%9:
	%9 :: Sink2Obj Obj2RefT
%8:
	%8 :: Src2PrimFldArr Obj2RefT
%4:
	%4 :: Sink2Prim _Ref2PrimF
%5:
	%5 :: Sink2Obj _Pt
Src2PrimT:
	Src2PrimT :: src2PrimT
%7:
	%7 :: Src2Prim Prim2RefT
%6:
	%6 :: Src2Obj Obj2RefT
%1:
	%1 :: _PostFlowsTo _Ref2RefTStub
%0:
	%0 :: PreFlowsTo Ref2RefTStub
%3:
	%3 :: %2 _Ref2RefF
%2:
	%2 :: Sink2Obj _Pt
Sink2PrimT:
	Sink2PrimT :: sink2PrimT
Src2PrimFldStat:
	Src2PrimFldStat[i] :: Src2Prim _storeStatPrimCtxt[i]
%20:
	%20[i] :: Sink2Prim _storePrimCtxt[i]
%21:
	%21 :: Sink2Prim _storePrimCtxtArr
Sink2PrimFldArr:
	Sink2PrimFldArr :: %21 Pt
Sink2Prim:
	Sink2Prim :: Sink2PrimT
	Sink2Prim :: Sink2Prim _assignPrimCtxt
	Sink2Prim :: Sink2Prim _assignPrimCCtxt
	Sink2Prim :: Sink2Obj Obj2PrimT
	Sink2Prim :: Sink2Prim Prim2PrimT
	Sink2Prim :: %17 _loadPrimCtxt
	Sink2Prim :: %18 _loadPrimCtxtArr
	Sink2Prim :: Sink2PrimFldArr Obj2PrimT
	Sink2Prim :: %19[i] _loadPrimCtxt[i]
	Sink2Prim :: Sink2PrimFldStat[i] _loadStatPrimCtxt[i]
FptArr:
	FptArr :: fptArr
Obj2RefT:
	Obj2RefT :: _Pt Ref2RefT
	Obj2RefT :: _FptArr Obj2RefT
SinkF2PrimF:
	SinkF2PrimF :: sinkF2PrimF
PreFlowsTo:
	PreFlowsTo :: preFlowsTo
	PreFlowsTo :: %0 MidFlowsTo
Sink2PrimFld:
	Sink2PrimFld[i] :: %20[i] Pt
%19:
	%19[i] :: Sink2PrimFld[i] _Pt
Src2Sink:
	Src2Sink :: Src2Obj _SinkF2Obj
	Src2Sink :: Src2Prim _SinkF2Prim
	Src2Sink :: Src2PrimFld _SinkF2Obj
Src2PrimFldArr:
	Src2PrimFldArr :: %16 Pt
Src2Prim:
	Src2Prim :: Src2PrimT
	Src2Prim :: Src2Prim _assignPrimCtxt
	Src2Prim :: Src2Prim _assignPrimCCtxt
	Src2Prim :: Src2Obj Obj2PrimT
	Src2Prim :: Src2Prim Prim2PrimT
	Src2Prim :: %12 _loadPrimCtxt
	Src2Prim :: %13 _loadPrimCtxtArr
	Src2Prim :: Src2PrimFldArr Obj2PrimT
	Src2Prim :: %14[i] _loadPrimCtxt[i]
	Src2Prim :: Src2PrimFldStat[i] _loadStatPrimCtxt[i]
Prim2PrimF:
	Prim2PrimF :: prim2PrimF
Sink2Obj:
	Sink2Obj :: Sink2ObjT
	Sink2Obj :: Sink2ObjX
Src2PrimFld:
	Src2PrimFld[i] :: %15[i] Pt
LabelPrim3:
	LabelPrim3 :: Src2Prim
	LabelPrim3 :: Sink2Prim
MidFlowsTo:
	MidFlowsTo :: midFlowsTo
Sink2ObjT:
	Sink2ObjT :: Sink2RefT Pt
	Sink2ObjT :: Sink2ObjT Fpt
Obj2PrimT:
	Obj2PrimT :: _Pt Ref2PrimT
	Obj2PrimT :: _FptArr Obj2PrimT
Sink2ObjX:
	Sink2ObjX :: %9 Pt
	Sink2ObjX :: %10 Pt
	Sink2ObjX :: %11 Pt
	Sink2ObjX :: Sink2ObjX FptArr
%14:
	%14[i] :: Src2PrimFld[i] _Pt
Prim2PrimT:
	Prim2PrimT :: prim2PrimT
	Prim2PrimT :: primArg2PrimRetTStub
Prim2RefTStub:
	Prim2RefTStub :: primArg2RefArgTStub
	Prim2RefTStub :: primArg2RefRetTStub
Ref2PrimT:
	Ref2PrimT :: ref2PrimT
	Ref2PrimT :: refArg2PrimRetTStub
Src2ObjX:
	Src2ObjX :: %6 Pt
	Src2ObjX :: %7 Pt
	Src2ObjX :: %8 Pt
	Src2ObjX :: Src2ObjX FptArr
SinkF2Prim:
	SinkF2Prim :: SinkF2PrimF
	SinkF2Prim :: %5 _Prim2RefF
	SinkF2Prim :: Sink2Prim _Prim2PrimF
Ref2RefF:
	Ref2RefF :: ref2RefF
Ref2RefTStub:
	Ref2RefTStub :: refArg2RefArgTStub
	Ref2RefTStub :: refArg2RefRetTStub
Fpt:
	Fpt[i] :: fpt[i]
%11:
	%11 :: Sink2PrimFldArr Obj2RefT
%10:
	%10 :: Sink2Prim Prim2RefT
Prim2RefT:
	Prim2RefT :: prim2RefT
%12:
	%12 :: Src2ObjT _Pt
%15:
	%15[i] :: Src2Prim _storePrimCtxt[i]
Src2ObjT:
	Src2ObjT :: Src2RefT Pt
	Src2ObjT :: Src2ObjT Fpt
%17:
	%17 :: Sink2ObjT _Pt
%16:
	%16 :: Src2Prim _storePrimCtxtArr
Ref2PrimF:
	Ref2PrimF :: ref2PrimF
%18:
	%18 :: Sink2ObjX _Pt
Ref2RefT:
	Ref2RefT :: ref2RefT
Prim2RefF:
	Prim2RefF :: prim2RefF
SinkF2RefF:
	SinkF2RefF :: sinkF2RefF
Src2Obj:
	Src2Obj :: Src2ObjT
	Src2Obj :: Src2ObjX
Src2RefT:
	Src2RefT :: src2RefT
LabelRef3:
	LabelRef3 :: Src2Obj _Pt
	LabelRef3 :: Sink2Obj _Pt
*/

/* Reverse Productions:
storePrimCtxtArr:
	_storePrimCtxtArr + (Src2Prim *) => %16
	_storePrimCtxtArr + (Sink2Prim *) => %21
prim2RefT:
	prim2RefT => Prim2RefT
%9:
	%9 + (* Pt) => Sink2ObjX
%8:
	%8 + (* Pt) => Src2ObjX
prim2RefF:
	prim2RefF => Prim2RefF
%4:
	%4 + (* Pt) => SinkF2Obj
%7:
	%7 + (* Pt) => Src2ObjX
%6:
	%6 + (* Pt) => Src2ObjX
%1:
	%1 + (* _PreFlowsTo) => Pt
%0:
	%0 + (* MidFlowsTo) => PreFlowsTo
%3:
	%3 + (* Pt) => SinkF2Obj
%2:
	%2 + (* _Ref2RefF) => %3
Src2PrimFldStat:
	Src2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Src2Prim
postFlowsTo:
	postFlowsTo => PostFlowsTo
sink2RefT:
	sink2RefT => Sink2RefT
refArg2RefRetTStub:
	refArg2RefRetTStub => Ref2RefTStub
sink2PrimT:
	sink2PrimT => Sink2PrimT
storePrimCtxt:
	_storePrimCtxt[i] + (Src2Prim *) => %15[i]
	_storePrimCtxt[i] + (Sink2Prim *) => %20[i]
primArg2RefArgTStub:
	primArg2RefArgTStub => Prim2RefTStub
Src2PrimFld:
	Src2PrimFld + (* _SinkF2Obj) => Src2Sink
	Src2PrimFld[i] + (* _Pt) => %14[i]
MidFlowsTo:
	MidFlowsTo + (%0 *) => PreFlowsTo
preFlowsTo:
	preFlowsTo => PreFlowsTo
Src2ObjX:
	Src2ObjX => Src2Obj
	Src2ObjX + (* FptArr) => Src2ObjX
	Src2ObjX + (* _Pt) => %13
Ref2RefF:
	_Ref2RefF + (%2 *) => %3
assignPrimCCtxt:
	_assignPrimCCtxt + (Src2Prim *) => Src2Prim
	_assignPrimCCtxt + (Sink2Prim *) => Sink2Prim
SinkF2Obj:
	SinkF2Obj + (* Fpt) => SinkF2Obj
	_SinkF2Obj + (Src2Obj *) => Src2Sink
	_SinkF2Obj + (Src2PrimFld *) => Src2Sink
Ref2RefT:
	Ref2RefT + (_Pt *) => Obj2RefT
refArg2PrimRetTStub:
	refArg2PrimRetTStub => Ref2PrimT
Src2Obj:
	Src2Obj + (* _SinkF2Obj) => Src2Sink
	Src2Obj + (* Obj2RefT) => %6
	Src2Obj + (* Obj2PrimT) => Src2Prim
	Src2Obj + (* _Pt) => LabelRef3
ref2PrimT:
	ref2PrimT => Ref2PrimT
Pt:
	_Pt + (* Ref2RefT) => Obj2RefT
	_Pt + (* Ref2PrimT) => Obj2PrimT
	Pt + (Src2RefT *) => Src2ObjT
	Pt + (Sink2RefT *) => Sink2ObjT
	Pt + (SinkF2RefF *) => SinkF2Obj
	_Pt + (Sink2Obj *) => %2
	Pt + (%3 *) => SinkF2Obj
	Pt + (%4 *) => SinkF2Obj
	_Pt + (Sink2Obj *) => %5
	Pt + (%6 *) => Src2ObjX
	Pt + (%7 *) => Src2ObjX
	Pt + (%8 *) => Src2ObjX
	Pt + (%9 *) => Sink2ObjX
	Pt + (%10 *) => Sink2ObjX
	Pt + (%11 *) => Sink2ObjX
	_Pt + (Src2ObjT *) => %12
	_Pt + (Src2ObjX *) => %13
	_Pt + (Src2PrimFld[i] *) => %14[i]
	Pt + (%15[i] *) => Src2PrimFld[i]
	Pt + (%16 *) => Src2PrimFldArr
	_Pt + (Sink2ObjT *) => %17
	_Pt + (Sink2ObjX *) => %18
	_Pt + (Sink2PrimFld[i] *) => %19[i]
	Pt + (%20[i] *) => Sink2PrimFld[i]
	Pt + (%21 *) => Sink2PrimFldArr
	_Pt + (Src2Obj *) => LabelRef3
	_Pt + (Sink2Obj *) => LabelRef3
Src2ObjT:
	Src2ObjT + (* Fpt) => Src2ObjT
	Src2ObjT => Src2Obj
	Src2ObjT + (* _Pt) => %12
%5:
	%5 + (* _Prim2RefF) => SinkF2Prim
ref2PrimF:
	ref2PrimF => Ref2PrimF
refArg2RefArgTStub:
	refArg2RefArgTStub => Ref2RefTStub
FptArr:
	_FptArr + (* Obj2RefT) => Obj2RefT
	_FptArr + (* Obj2PrimT) => Obj2PrimT
	FptArr + (Src2ObjX *) => Src2ObjX
	FptArr + (Sink2ObjX *) => Sink2ObjX
SinkF2PrimF:
	SinkF2PrimF => SinkF2Prim
Sink2PrimFld:
	Sink2PrimFld[i] + (* _Pt) => %19[i]
Src2Sink:
	Src2Sink => Flow3
loadStatPrimCtxt:
	_loadStatPrimCtxt[i] + (Src2PrimFldStat[i] *) => Src2Prim
	_loadStatPrimCtxt[i] + (Sink2PrimFldStat[i] *) => Sink2Prim
Prim2PrimF:
	_Prim2PrimF + (Sink2Prim *) => SinkF2Prim
Prim2PrimT:
	Prim2PrimT + (Src2Prim *) => Src2Prim
	Prim2PrimT + (Sink2Prim *) => Sink2Prim
storeStatPrimCtxt:
	_storeStatPrimCtxt[i] + (Src2Prim *) => Src2PrimFldStat[i]
	_storeStatPrimCtxt[i] + (Sink2Prim *) => Sink2PrimFldStat[i]
Ref2PrimT:
	Ref2PrimT + (_Pt *) => Obj2PrimT
SinkF2Prim:
	_SinkF2Prim + (Src2Prim *) => Src2Sink
loadPrimCtxtArr:
	_loadPrimCtxtArr + (%13 *) => Src2Prim
	_loadPrimCtxtArr + (%18 *) => Sink2Prim
Ref2RefTStub:
	Ref2RefTStub + (PreFlowsTo *) => %0
	_Ref2RefTStub + (_PostFlowsTo *) => %1
Ref2PrimF:
	_Ref2PrimF + (Sink2Prim *) => %4
PostFlowsTo:
	_PostFlowsTo + (* _Ref2RefTStub) => %1
assignPrimCtxt:
	_assignPrimCtxt + (Src2Prim *) => Src2Prim
	_assignPrimCtxt + (Sink2Prim *) => Sink2Prim
fptArr:
	fptArr => FptArr
sinkF2PrimF:
	sinkF2PrimF => SinkF2PrimF
Src2PrimT:
	Src2PrimT => Src2Prim
pt:
	pt => Pt
Sink2PrimFldArr:
	Sink2PrimFldArr + (* Obj2RefT) => %11
	Sink2PrimFldArr + (* Obj2PrimT) => Sink2Prim
sinkF2RefF:
	sinkF2RefF => SinkF2RefF
Sink2RefT:
	Sink2RefT + (* Pt) => Sink2ObjT
src2PrimT:
	src2PrimT => Src2PrimT
Obj2PrimT:
	Obj2PrimT + (_FptArr *) => Obj2PrimT
	Obj2PrimT + (Src2Obj *) => Src2Prim
	Obj2PrimT + (Src2PrimFldArr *) => Src2Prim
	Obj2PrimT + (Sink2Obj *) => Sink2Prim
	Obj2PrimT + (Sink2PrimFldArr *) => Sink2Prim
loadPrimCtxt:
	_loadPrimCtxt + (%12 *) => Src2Prim
	_loadPrimCtxt[i] + (%14[i] *) => Src2Prim
	_loadPrimCtxt + (%17 *) => Sink2Prim
	_loadPrimCtxt[i] + (%19[i] *) => Sink2Prim
Obj2RefT:
	Obj2RefT + (_FptArr *) => Obj2RefT
	Obj2RefT + (Src2Obj *) => %6
	Obj2RefT + (Src2PrimFldArr *) => %8
	Obj2RefT + (Sink2Obj *) => %9
	Obj2RefT + (Sink2PrimFldArr *) => %11
Src2Prim:
	Src2Prim + (* _SinkF2Prim) => Src2Sink
	Src2Prim + (* Prim2RefT) => %7
	Src2Prim + (* _assignPrimCtxt) => Src2Prim
	Src2Prim + (* _assignPrimCCtxt) => Src2Prim
	Src2Prim + (* Prim2PrimT) => Src2Prim
	Src2Prim + (* _storePrimCtxt[i]) => %15[i]
	Src2Prim + (* _storePrimCtxtArr) => %16
	Src2Prim + (* _storeStatPrimCtxt[i]) => Src2PrimFldStat[i]
	Src2Prim => LabelPrim3
Fpt:
	Fpt + (Src2ObjT *) => Src2ObjT
	Fpt + (Sink2ObjT *) => Sink2ObjT
	Fpt + (SinkF2Obj *) => SinkF2Obj
Prim2RefT:
	Prim2RefT + (Src2Prim *) => %7
	Prim2RefT + (Sink2Prim *) => %10
prim2PrimF:
	prim2PrimF => Prim2PrimF
midFlowsTo:
	midFlowsTo => MidFlowsTo
Prim2RefF:
	_Prim2RefF + (%5 *) => SinkF2Prim
prim2PrimT:
	prim2PrimT => Prim2PrimT
Src2RefT:
	Src2RefT + (* Pt) => Src2ObjT
ref2RefF:
	ref2RefF => Ref2RefF
primArg2RefRetTStub:
	primArg2RefRetTStub => Prim2RefTStub
Sink2PrimFldStat:
	Sink2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Sink2Prim
ref2RefT:
	ref2RefT => Ref2RefT
Sink2Prim:
	Sink2Prim + (* _Ref2PrimF) => %4
	Sink2Prim + (* _Prim2PrimF) => SinkF2Prim
	Sink2Prim + (* Prim2RefT) => %10
	Sink2Prim + (* _assignPrimCtxt) => Sink2Prim
	Sink2Prim + (* _assignPrimCCtxt) => Sink2Prim
	Sink2Prim + (* Prim2PrimT) => Sink2Prim
	Sink2Prim + (* _storePrimCtxt[i]) => %20[i]
	Sink2Prim + (* _storePrimCtxtArr) => %21
	Sink2Prim + (* _storeStatPrimCtxt[i]) => Sink2PrimFldStat[i]
	Sink2Prim => LabelPrim3
primArg2PrimRetTStub:
	primArg2PrimRetTStub => Prim2PrimT
Src2PrimFldArr:
	Src2PrimFldArr + (* Obj2RefT) => %8
	Src2PrimFldArr + (* Obj2PrimT) => Src2Prim
%20:
	%20[i] + (* Pt) => Sink2PrimFld[i]
%21:
	%21 + (* Pt) => Sink2PrimFldArr
src2RefT:
	src2RefT => Src2RefT
fpt:
	fpt[i] => Fpt[i]
PreFlowsTo:
	PreFlowsTo + (* Ref2RefTStub) => %0
	_PreFlowsTo + (%1 *) => Pt
Sink2PrimT:
	Sink2PrimT => Sink2Prim
Sink2Obj:
	Sink2Obj + (* _Pt) => %2
	Sink2Obj + (* _Pt) => %5
	Sink2Obj + (* Obj2RefT) => %9
	Sink2Obj + (* Obj2PrimT) => Sink2Prim
	Sink2Obj + (* _Pt) => LabelRef3
SinkF2RefF:
	SinkF2RefF + (* Pt) => SinkF2Obj
Sink2ObjT:
	Sink2ObjT + (* Fpt) => Sink2ObjT
	Sink2ObjT => Sink2Obj
	Sink2ObjT + (* _Pt) => %17
Sink2ObjX:
	Sink2ObjX => Sink2Obj
	Sink2ObjX + (* FptArr) => Sink2ObjX
	Sink2ObjX + (* _Pt) => %18
%19:
	%19[i] + (* _loadPrimCtxt[i]) => Sink2Prim
%18:
	%18 + (* _loadPrimCtxtArr) => Sink2Prim
%11:
	%11 + (* Pt) => Sink2ObjX
%10:
	%10 + (* Pt) => Sink2ObjX
%13:
	%13 + (* _loadPrimCtxtArr) => Src2Prim
%12:
	%12 + (* _loadPrimCtxt) => Src2Prim
%15:
	%15[i] + (* Pt) => Src2PrimFld[i]
%14:
	%14[i] + (* _loadPrimCtxt[i]) => Src2Prim
%17:
	%17 + (* _loadPrimCtxt) => Sink2Prim
%16:
	%16 + (* Pt) => Src2PrimFldArr
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
  case 25:
  case 27:
  case 29:
  case 30:
  case 32:
  case 33:
  case 34:
  case 35:
  case 37:
  case 39:
  case 41:
  case 44:
  case 47:
  case 49:
  case 76:
  case 77:
  case 78:
  case 80:
  case 84:
  case 85:
  case 87:
  case 89:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 100;
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
  if (symbol.equals("Src2RefT")) return 8;
  if (symbol.equals("src2RefT")) return 9;
  if (symbol.equals("Src2PrimT")) return 10;
  if (symbol.equals("src2PrimT")) return 11;
  if (symbol.equals("Sink2RefT")) return 12;
  if (symbol.equals("sink2RefT")) return 13;
  if (symbol.equals("Sink2PrimT")) return 14;
  if (symbol.equals("sink2PrimT")) return 15;
  if (symbol.equals("SinkF2RefF")) return 16;
  if (symbol.equals("sinkF2RefF")) return 17;
  if (symbol.equals("SinkF2PrimF")) return 18;
  if (symbol.equals("sinkF2PrimF")) return 19;
  if (symbol.equals("Ref2RefF")) return 20;
  if (symbol.equals("ref2RefF")) return 21;
  if (symbol.equals("Ref2PrimF")) return 22;
  if (symbol.equals("ref2PrimF")) return 23;
  if (symbol.equals("Prim2RefF")) return 24;
  if (symbol.equals("prim2RefF")) return 25;
  if (symbol.equals("Prim2PrimF")) return 26;
  if (symbol.equals("prim2PrimF")) return 27;
  if (symbol.equals("Ref2RefTStub")) return 28;
  if (symbol.equals("refArg2RefArgTStub")) return 29;
  if (symbol.equals("refArg2RefRetTStub")) return 30;
  if (symbol.equals("Prim2RefTStub")) return 31;
  if (symbol.equals("primArg2RefArgTStub")) return 32;
  if (symbol.equals("primArg2RefRetTStub")) return 33;
  if (symbol.equals("refArg2PrimRetTStub")) return 34;
  if (symbol.equals("primArg2PrimRetTStub")) return 35;
  if (symbol.equals("PreFlowsTo")) return 36;
  if (symbol.equals("preFlowsTo")) return 37;
  if (symbol.equals("PostFlowsTo")) return 38;
  if (symbol.equals("postFlowsTo")) return 39;
  if (symbol.equals("MidFlowsTo")) return 40;
  if (symbol.equals("midFlowsTo")) return 41;
  if (symbol.equals("%0")) return 42;
  if (symbol.equals("Pt")) return 43;
  if (symbol.equals("pt")) return 44;
  if (symbol.equals("%1")) return 45;
  if (symbol.equals("Fpt")) return 46;
  if (symbol.equals("fpt")) return 47;
  if (symbol.equals("FptArr")) return 48;
  if (symbol.equals("fptArr")) return 49;
  if (symbol.equals("Obj2RefT")) return 50;
  if (symbol.equals("Obj2PrimT")) return 51;
  if (symbol.equals("Src2ObjT")) return 52;
  if (symbol.equals("Sink2ObjT")) return 53;
  if (symbol.equals("SinkF2Obj")) return 54;
  if (symbol.equals("Sink2Obj")) return 55;
  if (symbol.equals("%2")) return 56;
  if (symbol.equals("%3")) return 57;
  if (symbol.equals("Sink2Prim")) return 58;
  if (symbol.equals("%4")) return 59;
  if (symbol.equals("SinkF2Prim")) return 60;
  if (symbol.equals("%5")) return 61;
  if (symbol.equals("Src2Sink")) return 62;
  if (symbol.equals("Src2Obj")) return 63;
  if (symbol.equals("Src2Prim")) return 64;
  if (symbol.equals("Src2PrimFld")) return 65;
  if (symbol.equals("Src2ObjX")) return 66;
  if (symbol.equals("%6")) return 67;
  if (symbol.equals("%7")) return 68;
  if (symbol.equals("Src2PrimFldArr")) return 69;
  if (symbol.equals("%8")) return 70;
  if (symbol.equals("Sink2ObjX")) return 71;
  if (symbol.equals("%9")) return 72;
  if (symbol.equals("%10")) return 73;
  if (symbol.equals("Sink2PrimFldArr")) return 74;
  if (symbol.equals("%11")) return 75;
  if (symbol.equals("assignPrimCtxt")) return 76;
  if (symbol.equals("assignPrimCCtxt")) return 77;
  if (symbol.equals("loadPrimCtxt")) return 78;
  if (symbol.equals("%12")) return 79;
  if (symbol.equals("loadPrimCtxtArr")) return 80;
  if (symbol.equals("%13")) return 81;
  if (symbol.equals("%14")) return 82;
  if (symbol.equals("Src2PrimFldStat")) return 83;
  if (symbol.equals("loadStatPrimCtxt")) return 84;
  if (symbol.equals("storePrimCtxt")) return 85;
  if (symbol.equals("%15")) return 86;
  if (symbol.equals("storePrimCtxtArr")) return 87;
  if (symbol.equals("%16")) return 88;
  if (symbol.equals("storeStatPrimCtxt")) return 89;
  if (symbol.equals("%17")) return 90;
  if (symbol.equals("%18")) return 91;
  if (symbol.equals("Sink2PrimFld")) return 92;
  if (symbol.equals("%19")) return 93;
  if (symbol.equals("Sink2PrimFldStat")) return 94;
  if (symbol.equals("%20")) return 95;
  if (symbol.equals("%21")) return 96;
  if (symbol.equals("LabelRef3")) return 97;
  if (symbol.equals("LabelPrim3")) return 98;
  if (symbol.equals("Flow3")) return 99;
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
  case 8: return "Src2RefT";
  case 9: return "src2RefT";
  case 10: return "Src2PrimT";
  case 11: return "src2PrimT";
  case 12: return "Sink2RefT";
  case 13: return "sink2RefT";
  case 14: return "Sink2PrimT";
  case 15: return "sink2PrimT";
  case 16: return "SinkF2RefF";
  case 17: return "sinkF2RefF";
  case 18: return "SinkF2PrimF";
  case 19: return "sinkF2PrimF";
  case 20: return "Ref2RefF";
  case 21: return "ref2RefF";
  case 22: return "Ref2PrimF";
  case 23: return "ref2PrimF";
  case 24: return "Prim2RefF";
  case 25: return "prim2RefF";
  case 26: return "Prim2PrimF";
  case 27: return "prim2PrimF";
  case 28: return "Ref2RefTStub";
  case 29: return "refArg2RefArgTStub";
  case 30: return "refArg2RefRetTStub";
  case 31: return "Prim2RefTStub";
  case 32: return "primArg2RefArgTStub";
  case 33: return "primArg2RefRetTStub";
  case 34: return "refArg2PrimRetTStub";
  case 35: return "primArg2PrimRetTStub";
  case 36: return "PreFlowsTo";
  case 37: return "preFlowsTo";
  case 38: return "PostFlowsTo";
  case 39: return "postFlowsTo";
  case 40: return "MidFlowsTo";
  case 41: return "midFlowsTo";
  case 42: return "%0";
  case 43: return "Pt";
  case 44: return "pt";
  case 45: return "%1";
  case 46: return "Fpt";
  case 47: return "fpt";
  case 48: return "FptArr";
  case 49: return "fptArr";
  case 50: return "Obj2RefT";
  case 51: return "Obj2PrimT";
  case 52: return "Src2ObjT";
  case 53: return "Sink2ObjT";
  case 54: return "SinkF2Obj";
  case 55: return "Sink2Obj";
  case 56: return "%2";
  case 57: return "%3";
  case 58: return "Sink2Prim";
  case 59: return "%4";
  case 60: return "SinkF2Prim";
  case 61: return "%5";
  case 62: return "Src2Sink";
  case 63: return "Src2Obj";
  case 64: return "Src2Prim";
  case 65: return "Src2PrimFld";
  case 66: return "Src2ObjX";
  case 67: return "%6";
  case 68: return "%7";
  case 69: return "Src2PrimFldArr";
  case 70: return "%8";
  case 71: return "Sink2ObjX";
  case 72: return "%9";
  case 73: return "%10";
  case 74: return "Sink2PrimFldArr";
  case 75: return "%11";
  case 76: return "assignPrimCtxt";
  case 77: return "assignPrimCCtxt";
  case 78: return "loadPrimCtxt";
  case 79: return "%12";
  case 80: return "loadPrimCtxtArr";
  case 81: return "%13";
  case 82: return "%14";
  case 83: return "Src2PrimFldStat";
  case 84: return "loadStatPrimCtxt";
  case 85: return "storePrimCtxt";
  case 86: return "%15";
  case 87: return "storePrimCtxtArr";
  case 88: return "%16";
  case 89: return "storeStatPrimCtxt";
  case 90: return "%17";
  case 91: return "%18";
  case 92: return "Sink2PrimFld";
  case 93: return "%19";
  case 94: return "Sink2PrimFldStat";
  case 95: return "%20";
  case 96: return "%21";
  case 97: return "LabelRef3";
  case 98: return "LabelPrim3";
  case 99: return "Flow3";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* Ref2RefT */
    /* Ref2RefT + (_Pt *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(43)){
      addEdge(other.to, base.to, 50, base, other, false);
    }
    break;
  case 1: /* ref2RefT */
    /* ref2RefT => Ref2RefT */
    addEdge(base.from, base.to, 0, base, false);
    break;
  case 2: /* Ref2PrimT */
    /* Ref2PrimT + (_Pt *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(43)){
      addEdge(other.to, base.to, 51, base, other, false);
    }
    break;
  case 3: /* ref2PrimT */
    /* ref2PrimT => Ref2PrimT */
    addEdge(base.from, base.to, 2, base, false);
    break;
  case 4: /* Prim2RefT */
    /* Prim2RefT + (Src2Prim *) => %7 */
    for(Edge other : base.from.getInEdges(64)){
      addEdge(other.from, base.to, 68, base, other, false);
    }
    /* Prim2RefT + (Sink2Prim *) => %10 */
    for(Edge other : base.from.getInEdges(58)){
      addEdge(other.from, base.to, 73, base, other, false);
    }
    break;
  case 5: /* prim2RefT */
    /* prim2RefT => Prim2RefT */
    addEdge(base.from, base.to, 4, base, false);
    break;
  case 6: /* Prim2PrimT */
    /* Prim2PrimT + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(64)){
      addEdge(other.from, base.to, 64, base, other, false);
    }
    /* Prim2PrimT + (Sink2Prim *) => Sink2Prim */
    for(Edge other : base.from.getInEdges(58)){
      addEdge(other.from, base.to, 58, base, other, false);
    }
    break;
  case 7: /* prim2PrimT */
    /* prim2PrimT => Prim2PrimT */
    addEdge(base.from, base.to, 6, base, false);
    break;
  case 8: /* Src2RefT */
    /* Src2RefT + (* Pt) => Src2ObjT */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 52, base, other, false);
    }
    break;
  case 9: /* src2RefT */
    /* src2RefT => Src2RefT */
    addEdge(base.from, base.to, 8, base, false);
    break;
  case 10: /* Src2PrimT */
    /* Src2PrimT => Src2Prim */
    addEdge(base.from, base.to, 64, base, false);
    break;
  case 11: /* src2PrimT */
    /* src2PrimT => Src2PrimT */
    addEdge(base.from, base.to, 10, base, false);
    break;
  case 12: /* Sink2RefT */
    /* Sink2RefT + (* Pt) => Sink2ObjT */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    break;
  case 13: /* sink2RefT */
    /* sink2RefT => Sink2RefT */
    addEdge(base.from, base.to, 12, base, false);
    break;
  case 14: /* Sink2PrimT */
    /* Sink2PrimT => Sink2Prim */
    addEdge(base.from, base.to, 58, base, false);
    break;
  case 15: /* sink2PrimT */
    /* sink2PrimT => Sink2PrimT */
    addEdge(base.from, base.to, 14, base, false);
    break;
  case 16: /* SinkF2RefF */
    /* SinkF2RefF + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 54, base, other, false);
    }
    break;
  case 17: /* sinkF2RefF */
    /* sinkF2RefF => SinkF2RefF */
    addEdge(base.from, base.to, 16, base, false);
    break;
  case 18: /* SinkF2PrimF */
    /* SinkF2PrimF => SinkF2Prim */
    addEdge(base.from, base.to, 60, base, false);
    break;
  case 19: /* sinkF2PrimF */
    /* sinkF2PrimF => SinkF2PrimF */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 20: /* Ref2RefF */
    /* _Ref2RefF + (%2 *) => %3 */
    for(Edge other : base.to.getInEdges(56)){
      addEdge(other.from, base.from, 57, base, other, false);
    }
    break;
  case 21: /* ref2RefF */
    /* ref2RefF => Ref2RefF */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 22: /* Ref2PrimF */
    /* _Ref2PrimF + (Sink2Prim *) => %4 */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 59, base, other, false);
    }
    break;
  case 23: /* ref2PrimF */
    /* ref2PrimF => Ref2PrimF */
    addEdge(base.from, base.to, 22, base, false);
    break;
  case 24: /* Prim2RefF */
    /* _Prim2RefF + (%5 *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(61)){
      addEdge(other.from, base.from, 60, base, other, false);
    }
    break;
  case 25: /* prim2RefF */
    /* prim2RefF => Prim2RefF */
    addEdge(base.from, base.to, 24, base, false);
    break;
  case 26: /* Prim2PrimF */
    /* _Prim2PrimF + (Sink2Prim *) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 60, base, other, false);
    }
    break;
  case 27: /* prim2PrimF */
    /* prim2PrimF => Prim2PrimF */
    addEdge(base.from, base.to, 26, base, false);
    break;
  case 28: /* Ref2RefTStub */
    /* Ref2RefTStub + (PreFlowsTo *) => %0 */
    for(Edge other : base.from.getInEdges(36)){
      addEdge(other.from, base.to, 42, base, other, false);
    }
    /* _Ref2RefTStub + (_PostFlowsTo *) => %1 */
    for(Edge other : base.to.getOutEdges(38)){
      addEdge(other.to, base.from, 45, base, other, false);
    }
    break;
  case 29: /* refArg2RefArgTStub */
    /* refArg2RefArgTStub => Ref2RefTStub */
    addEdge(base.from, base.to, 28, base, false);
    break;
  case 30: /* refArg2RefRetTStub */
    /* refArg2RefRetTStub => Ref2RefTStub */
    addEdge(base.from, base.to, 28, base, false);
    break;
  case 32: /* primArg2RefArgTStub */
    /* primArg2RefArgTStub => Prim2RefTStub */
    addEdge(base.from, base.to, 31, base, false);
    break;
  case 33: /* primArg2RefRetTStub */
    /* primArg2RefRetTStub => Prim2RefTStub */
    addEdge(base.from, base.to, 31, base, false);
    break;
  case 34: /* refArg2PrimRetTStub */
    /* refArg2PrimRetTStub => Ref2PrimT */
    addEdge(base.from, base.to, 2, base, false);
    break;
  case 35: /* primArg2PrimRetTStub */
    /* primArg2PrimRetTStub => Prim2PrimT */
    addEdge(base.from, base.to, 6, base, false);
    break;
  case 36: /* PreFlowsTo */
    /* PreFlowsTo + (* Ref2RefTStub) => %0 */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 42, base, other, false);
    }
    /* _PreFlowsTo + (%1 *) => Pt */
    for(Edge other : base.to.getInEdges(45)){
      addEdge(other.from, base.from, 43, base, other, false);
    }
    break;
  case 37: /* preFlowsTo */
    /* preFlowsTo => PreFlowsTo */
    addEdge(base.from, base.to, 36, base, false);
    break;
  case 38: /* PostFlowsTo */
    /* _PostFlowsTo + (* _Ref2RefTStub) => %1 */
    for(Edge other : base.from.getInEdges(28)){
      addEdge(base.to, other.from, 45, base, other, false);
    }
    break;
  case 39: /* postFlowsTo */
    /* postFlowsTo => PostFlowsTo */
    addEdge(base.from, base.to, 38, base, false);
    break;
  case 40: /* MidFlowsTo */
    /* MidFlowsTo + (%0 *) => PreFlowsTo */
    for(Edge other : base.from.getInEdges(42)){
      addEdge(other.from, base.to, 36, base, other, false);
    }
    break;
  case 41: /* midFlowsTo */
    /* midFlowsTo => MidFlowsTo */
    addEdge(base.from, base.to, 40, base, false);
    break;
  case 42: /* %0 */
    /* %0 + (* MidFlowsTo) => PreFlowsTo */
    for(Edge other : base.to.getOutEdges(40)){
      addEdge(base.from, other.to, 36, base, other, false);
    }
    break;
  case 43: /* Pt */
    /* _Pt + (* Ref2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(0)){
      addEdge(base.to, other.to, 50, base, other, false);
    }
    /* _Pt + (* Ref2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(2)){
      addEdge(base.to, other.to, 51, base, other, false);
    }
    /* Pt + (Src2RefT *) => Src2ObjT */
    for(Edge other : base.from.getInEdges(8)){
      addEdge(other.from, base.to, 52, base, other, false);
    }
    /* Pt + (Sink2RefT *) => Sink2ObjT */
    for(Edge other : base.from.getInEdges(12)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    /* Pt + (SinkF2RefF *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(16)){
      addEdge(other.from, base.to, 54, base, other, false);
    }
    /* _Pt + (Sink2Obj *) => %2 */
    for(Edge other : base.to.getInEdges(55)){
      addEdge(other.from, base.from, 56, base, other, false);
    }
    /* Pt + (%3 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(57)){
      addEdge(other.from, base.to, 54, base, other, false);
    }
    /* Pt + (%4 *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(59)){
      addEdge(other.from, base.to, 54, base, other, false);
    }
    /* _Pt + (Sink2Obj *) => %5 */
    for(Edge other : base.to.getInEdges(55)){
      addEdge(other.from, base.from, 61, base, other, false);
    }
    /* Pt + (%6 *) => Src2ObjX */
    for(Edge other : base.from.getInEdges(67)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* Pt + (%7 *) => Src2ObjX */
    for(Edge other : base.from.getInEdges(68)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* Pt + (%8 *) => Src2ObjX */
    for(Edge other : base.from.getInEdges(70)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* Pt + (%9 *) => Sink2ObjX */
    for(Edge other : base.from.getInEdges(72)){
      addEdge(other.from, base.to, 71, base, other, false);
    }
    /* Pt + (%10 *) => Sink2ObjX */
    for(Edge other : base.from.getInEdges(73)){
      addEdge(other.from, base.to, 71, base, other, false);
    }
    /* Pt + (%11 *) => Sink2ObjX */
    for(Edge other : base.from.getInEdges(75)){
      addEdge(other.from, base.to, 71, base, other, false);
    }
    /* _Pt + (Src2ObjT *) => %12 */
    for(Edge other : base.to.getInEdges(52)){
      addEdge(other.from, base.from, 79, base, other, false);
    }
    /* _Pt + (Src2ObjX *) => %13 */
    for(Edge other : base.to.getInEdges(66)){
      addEdge(other.from, base.from, 81, base, other, false);
    }
    /* _Pt + (Src2PrimFld[i] *) => %14[i] */
    for(Edge other : base.to.getInEdges(65)){
      addEdge(other.from, base.from, 82, base, other, true);
    }
    /* Pt + (%15[i] *) => Src2PrimFld[i] */
    for(Edge other : base.from.getInEdges(86)){
      addEdge(other.from, base.to, 65, base, other, true);
    }
    /* Pt + (%16 *) => Src2PrimFldArr */
    for(Edge other : base.from.getInEdges(88)){
      addEdge(other.from, base.to, 69, base, other, false);
    }
    /* _Pt + (Sink2ObjT *) => %17 */
    for(Edge other : base.to.getInEdges(53)){
      addEdge(other.from, base.from, 90, base, other, false);
    }
    /* _Pt + (Sink2ObjX *) => %18 */
    for(Edge other : base.to.getInEdges(71)){
      addEdge(other.from, base.from, 91, base, other, false);
    }
    /* _Pt + (Sink2PrimFld[i] *) => %19[i] */
    for(Edge other : base.to.getInEdges(92)){
      addEdge(other.from, base.from, 93, base, other, true);
    }
    /* Pt + (%20[i] *) => Sink2PrimFld[i] */
    for(Edge other : base.from.getInEdges(95)){
      addEdge(other.from, base.to, 92, base, other, true);
    }
    /* Pt + (%21 *) => Sink2PrimFldArr */
    for(Edge other : base.from.getInEdges(96)){
      addEdge(other.from, base.to, 74, base, other, false);
    }
    /* _Pt + (Src2Obj *) => LabelRef3 */
    for(Edge other : base.to.getInEdges(63)){
      addEdge(other.from, base.from, 97, base, other, false);
    }
    /* _Pt + (Sink2Obj *) => LabelRef3 */
    for(Edge other : base.to.getInEdges(55)){
      addEdge(other.from, base.from, 97, base, other, false);
    }
    break;
  case 44: /* pt */
    /* pt => Pt */
    addEdge(base.from, base.to, 43, base, false);
    break;
  case 45: /* %1 */
    /* %1 + (* _PreFlowsTo) => Pt */
    for(Edge other : base.to.getInEdges(36)){
      addEdge(base.from, other.from, 43, base, other, false);
    }
    break;
  case 46: /* Fpt */
    /* Fpt + (Src2ObjT *) => Src2ObjT */
    for(Edge other : base.from.getInEdges(52)){
      addEdge(other.from, base.to, 52, base, other, false);
    }
    /* Fpt + (Sink2ObjT *) => Sink2ObjT */
    for(Edge other : base.from.getInEdges(53)){
      addEdge(other.from, base.to, 53, base, other, false);
    }
    /* Fpt + (SinkF2Obj *) => SinkF2Obj */
    for(Edge other : base.from.getInEdges(54)){
      addEdge(other.from, base.to, 54, base, other, false);
    }
    break;
  case 47: /* fpt */
    /* fpt[i] => Fpt[i] */
    addEdge(base.from, base.to, 46, base, true);
    break;
  case 48: /* FptArr */
    /* _FptArr + (* Obj2RefT) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(50)){
      addEdge(base.to, other.to, 50, base, other, false);
    }
    /* _FptArr + (* Obj2PrimT) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(51)){
      addEdge(base.to, other.to, 51, base, other, false);
    }
    /* FptArr + (Src2ObjX *) => Src2ObjX */
    for(Edge other : base.from.getInEdges(66)){
      addEdge(other.from, base.to, 66, base, other, false);
    }
    /* FptArr + (Sink2ObjX *) => Sink2ObjX */
    for(Edge other : base.from.getInEdges(71)){
      addEdge(other.from, base.to, 71, base, other, false);
    }
    break;
  case 49: /* fptArr */
    /* fptArr => FptArr */
    addEdge(base.from, base.to, 48, base, false);
    break;
  case 50: /* Obj2RefT */
    /* Obj2RefT + (_FptArr *) => Obj2RefT */
    for(Edge other : base.from.getOutEdges(48)){
      addEdge(other.to, base.to, 50, base, other, false);
    }
    /* Obj2RefT + (Src2Obj *) => %6 */
    for(Edge other : base.from.getInEdges(63)){
      addEdge(other.from, base.to, 67, base, other, false);
    }
    /* Obj2RefT + (Src2PrimFldArr *) => %8 */
    for(Edge other : base.from.getInEdges(69)){
      addEdge(other.from, base.to, 70, base, other, false);
    }
    /* Obj2RefT + (Sink2Obj *) => %9 */
    for(Edge other : base.from.getInEdges(55)){
      addEdge(other.from, base.to, 72, base, other, false);
    }
    /* Obj2RefT + (Sink2PrimFldArr *) => %11 */
    for(Edge other : base.from.getInEdges(74)){
      addEdge(other.from, base.to, 75, base, other, false);
    }
    break;
  case 51: /* Obj2PrimT */
    /* Obj2PrimT + (_FptArr *) => Obj2PrimT */
    for(Edge other : base.from.getOutEdges(48)){
      addEdge(other.to, base.to, 51, base, other, false);
    }
    /* Obj2PrimT + (Src2Obj *) => Src2Prim */
    for(Edge other : base.from.getInEdges(63)){
      addEdge(other.from, base.to, 64, base, other, false);
    }
    /* Obj2PrimT + (Src2PrimFldArr *) => Src2Prim */
    for(Edge other : base.from.getInEdges(69)){
      addEdge(other.from, base.to, 64, base, other, false);
    }
    /* Obj2PrimT + (Sink2Obj *) => Sink2Prim */
    for(Edge other : base.from.getInEdges(55)){
      addEdge(other.from, base.to, 58, base, other, false);
    }
    /* Obj2PrimT + (Sink2PrimFldArr *) => Sink2Prim */
    for(Edge other : base.from.getInEdges(74)){
      addEdge(other.from, base.to, 58, base, other, false);
    }
    break;
  case 52: /* Src2ObjT */
    /* Src2ObjT + (* Fpt) => Src2ObjT */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 52, base, other, false);
    }
    /* Src2ObjT => Src2Obj */
    addEdge(base.from, base.to, 63, base, false);
    /* Src2ObjT + (* _Pt) => %12 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 79, base, other, false);
    }
    break;
  case 53: /* Sink2ObjT */
    /* Sink2ObjT + (* Fpt) => Sink2ObjT */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 53, base, other, false);
    }
    /* Sink2ObjT => Sink2Obj */
    addEdge(base.from, base.to, 55, base, false);
    /* Sink2ObjT + (* _Pt) => %17 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 90, base, other, false);
    }
    break;
  case 54: /* SinkF2Obj */
    /* SinkF2Obj + (* Fpt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(46)){
      addEdge(base.from, other.to, 54, base, other, false);
    }
    /* _SinkF2Obj + (Src2Obj *) => Src2Sink */
    for(Edge other : base.to.getInEdges(63)){
      addEdge(other.from, base.from, 62, base, other, false);
    }
    /* _SinkF2Obj + (Src2PrimFld *) => Src2Sink */
    for(Edge other : base.to.getInEdges(65)){
      addEdge(other.from, base.from, 62, base, other, false);
    }
    break;
  case 55: /* Sink2Obj */
    /* Sink2Obj + (* _Pt) => %2 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 56, base, other, false);
    }
    /* Sink2Obj + (* _Pt) => %5 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 61, base, other, false);
    }
    /* Sink2Obj + (* Obj2RefT) => %9 */
    for(Edge other : base.to.getOutEdges(50)){
      addEdge(base.from, other.to, 72, base, other, false);
    }
    /* Sink2Obj + (* Obj2PrimT) => Sink2Prim */
    for(Edge other : base.to.getOutEdges(51)){
      addEdge(base.from, other.to, 58, base, other, false);
    }
    /* Sink2Obj + (* _Pt) => LabelRef3 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 97, base, other, false);
    }
    break;
  case 56: /* %2 */
    /* %2 + (* _Ref2RefF) => %3 */
    for(Edge other : base.to.getInEdges(20)){
      addEdge(base.from, other.from, 57, base, other, false);
    }
    break;
  case 57: /* %3 */
    /* %3 + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 54, base, other, false);
    }
    break;
  case 58: /* Sink2Prim */
    /* Sink2Prim + (* _Ref2PrimF) => %4 */
    for(Edge other : base.to.getInEdges(22)){
      addEdge(base.from, other.from, 59, base, other, false);
    }
    /* Sink2Prim + (* _Prim2PrimF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(26)){
      addEdge(base.from, other.from, 60, base, other, false);
    }
    /* Sink2Prim + (* Prim2RefT) => %10 */
    for(Edge other : base.to.getOutEdges(4)){
      addEdge(base.from, other.to, 73, base, other, false);
    }
    /* Sink2Prim + (* _assignPrimCtxt) => Sink2Prim */
    for(Edge other : base.to.getInEdges(76)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    /* Sink2Prim + (* _assignPrimCCtxt) => Sink2Prim */
    for(Edge other : base.to.getInEdges(77)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    /* Sink2Prim + (* Prim2PrimT) => Sink2Prim */
    for(Edge other : base.to.getOutEdges(6)){
      addEdge(base.from, other.to, 58, base, other, false);
    }
    /* Sink2Prim + (* _storePrimCtxt[i]) => %20[i] */
    for(Edge other : base.to.getInEdges(85)){
      addEdge(base.from, other.from, 95, base, other, true);
    }
    /* Sink2Prim + (* _storePrimCtxtArr) => %21 */
    for(Edge other : base.to.getInEdges(87)){
      addEdge(base.from, other.from, 96, base, other, false);
    }
    /* Sink2Prim + (* _storeStatPrimCtxt[i]) => Sink2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(89)){
      addEdge(base.from, other.from, 94, base, other, true);
    }
    /* Sink2Prim => LabelPrim3 */
    addEdge(base.from, base.to, 98, base, false);
    break;
  case 59: /* %4 */
    /* %4 + (* Pt) => SinkF2Obj */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 54, base, other, false);
    }
    break;
  case 60: /* SinkF2Prim */
    /* _SinkF2Prim + (Src2Prim *) => Src2Sink */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 62, base, other, false);
    }
    break;
  case 61: /* %5 */
    /* %5 + (* _Prim2RefF) => SinkF2Prim */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(base.from, other.from, 60, base, other, false);
    }
    break;
  case 62: /* Src2Sink */
    /* Src2Sink => Flow3 */
    addEdge(base.from, base.to, 99, base, false);
    break;
  case 63: /* Src2Obj */
    /* Src2Obj + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(54)){
      addEdge(base.from, other.from, 62, base, other, false);
    }
    /* Src2Obj + (* Obj2RefT) => %6 */
    for(Edge other : base.to.getOutEdges(50)){
      addEdge(base.from, other.to, 67, base, other, false);
    }
    /* Src2Obj + (* Obj2PrimT) => Src2Prim */
    for(Edge other : base.to.getOutEdges(51)){
      addEdge(base.from, other.to, 64, base, other, false);
    }
    /* Src2Obj + (* _Pt) => LabelRef3 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 97, base, other, false);
    }
    break;
  case 64: /* Src2Prim */
    /* Src2Prim + (* _SinkF2Prim) => Src2Sink */
    for(Edge other : base.to.getInEdges(60)){
      addEdge(base.from, other.from, 62, base, other, false);
    }
    /* Src2Prim + (* Prim2RefT) => %7 */
    for(Edge other : base.to.getOutEdges(4)){
      addEdge(base.from, other.to, 68, base, other, false);
    }
    /* Src2Prim + (* _assignPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getInEdges(76)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    /* Src2Prim + (* _assignPrimCCtxt) => Src2Prim */
    for(Edge other : base.to.getInEdges(77)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    /* Src2Prim + (* Prim2PrimT) => Src2Prim */
    for(Edge other : base.to.getOutEdges(6)){
      addEdge(base.from, other.to, 64, base, other, false);
    }
    /* Src2Prim + (* _storePrimCtxt[i]) => %15[i] */
    for(Edge other : base.to.getInEdges(85)){
      addEdge(base.from, other.from, 86, base, other, true);
    }
    /* Src2Prim + (* _storePrimCtxtArr) => %16 */
    for(Edge other : base.to.getInEdges(87)){
      addEdge(base.from, other.from, 88, base, other, false);
    }
    /* Src2Prim + (* _storeStatPrimCtxt[i]) => Src2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(89)){
      addEdge(base.from, other.from, 83, base, other, true);
    }
    /* Src2Prim => LabelPrim3 */
    addEdge(base.from, base.to, 98, base, false);
    break;
  case 65: /* Src2PrimFld */
    /* Src2PrimFld + (* _SinkF2Obj) => Src2Sink */
    for(Edge other : base.to.getInEdges(54)){
      addEdge(base.from, other.from, 62, base, other, false);
    }
    /* Src2PrimFld[i] + (* _Pt) => %14[i] */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 82, base, other, true);
    }
    break;
  case 66: /* Src2ObjX */
    /* Src2ObjX => Src2Obj */
    addEdge(base.from, base.to, 63, base, false);
    /* Src2ObjX + (* FptArr) => Src2ObjX */
    for(Edge other : base.to.getOutEdges(48)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    /* Src2ObjX + (* _Pt) => %13 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 81, base, other, false);
    }
    break;
  case 67: /* %6 */
    /* %6 + (* Pt) => Src2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 68: /* %7 */
    /* %7 + (* Pt) => Src2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 69: /* Src2PrimFldArr */
    /* Src2PrimFldArr + (* Obj2RefT) => %8 */
    for(Edge other : base.to.getOutEdges(50)){
      addEdge(base.from, other.to, 70, base, other, false);
    }
    /* Src2PrimFldArr + (* Obj2PrimT) => Src2Prim */
    for(Edge other : base.to.getOutEdges(51)){
      addEdge(base.from, other.to, 64, base, other, false);
    }
    break;
  case 70: /* %8 */
    /* %8 + (* Pt) => Src2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 66, base, other, false);
    }
    break;
  case 71: /* Sink2ObjX */
    /* Sink2ObjX => Sink2Obj */
    addEdge(base.from, base.to, 55, base, false);
    /* Sink2ObjX + (* FptArr) => Sink2ObjX */
    for(Edge other : base.to.getOutEdges(48)){
      addEdge(base.from, other.to, 71, base, other, false);
    }
    /* Sink2ObjX + (* _Pt) => %18 */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 91, base, other, false);
    }
    break;
  case 72: /* %9 */
    /* %9 + (* Pt) => Sink2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 71, base, other, false);
    }
    break;
  case 73: /* %10 */
    /* %10 + (* Pt) => Sink2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 71, base, other, false);
    }
    break;
  case 74: /* Sink2PrimFldArr */
    /* Sink2PrimFldArr + (* Obj2RefT) => %11 */
    for(Edge other : base.to.getOutEdges(50)){
      addEdge(base.from, other.to, 75, base, other, false);
    }
    /* Sink2PrimFldArr + (* Obj2PrimT) => Sink2Prim */
    for(Edge other : base.to.getOutEdges(51)){
      addEdge(base.from, other.to, 58, base, other, false);
    }
    break;
  case 75: /* %11 */
    /* %11 + (* Pt) => Sink2ObjX */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 71, base, other, false);
    }
    break;
  case 76: /* assignPrimCtxt */
    /* _assignPrimCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _assignPrimCtxt + (Sink2Prim *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    break;
  case 77: /* assignPrimCCtxt */
    /* _assignPrimCCtxt + (Src2Prim *) => Src2Prim */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _assignPrimCCtxt + (Sink2Prim *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    break;
  case 78: /* loadPrimCtxt */
    /* _loadPrimCtxt + (%12 *) => Src2Prim */
    for(Edge other : base.to.getInEdges(79)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _loadPrimCtxt[i] + (%14[i] *) => Src2Prim */
    for(Edge other : base.to.getInEdges(82)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _loadPrimCtxt + (%17 *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(90)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    /* _loadPrimCtxt[i] + (%19[i] *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(93)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    break;
  case 79: /* %12 */
    /* %12 + (* _loadPrimCtxt) => Src2Prim */
    for(Edge other : base.to.getInEdges(78)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    break;
  case 80: /* loadPrimCtxtArr */
    /* _loadPrimCtxtArr + (%13 *) => Src2Prim */
    for(Edge other : base.to.getInEdges(81)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _loadPrimCtxtArr + (%18 *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(91)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    break;
  case 81: /* %13 */
    /* %13 + (* _loadPrimCtxtArr) => Src2Prim */
    for(Edge other : base.to.getInEdges(80)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    break;
  case 82: /* %14 */
    /* %14[i] + (* _loadPrimCtxt[i]) => Src2Prim */
    for(Edge other : base.to.getInEdges(78)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    break;
  case 83: /* Src2PrimFldStat */
    /* Src2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Src2Prim */
    for(Edge other : base.to.getInEdges(84)){
      addEdge(base.from, other.from, 64, base, other, false);
    }
    break;
  case 84: /* loadStatPrimCtxt */
    /* _loadStatPrimCtxt[i] + (Src2PrimFldStat[i] *) => Src2Prim */
    for(Edge other : base.to.getInEdges(83)){
      addEdge(other.from, base.from, 64, base, other, false);
    }
    /* _loadStatPrimCtxt[i] + (Sink2PrimFldStat[i] *) => Sink2Prim */
    for(Edge other : base.to.getInEdges(94)){
      addEdge(other.from, base.from, 58, base, other, false);
    }
    break;
  case 85: /* storePrimCtxt */
    /* _storePrimCtxt[i] + (Src2Prim *) => %15[i] */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 86, base, other, true);
    }
    /* _storePrimCtxt[i] + (Sink2Prim *) => %20[i] */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 95, base, other, true);
    }
    break;
  case 86: /* %15 */
    /* %15[i] + (* Pt) => Src2PrimFld[i] */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 65, base, other, true);
    }
    break;
  case 87: /* storePrimCtxtArr */
    /* _storePrimCtxtArr + (Src2Prim *) => %16 */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 88, base, other, false);
    }
    /* _storePrimCtxtArr + (Sink2Prim *) => %21 */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 96, base, other, false);
    }
    break;
  case 88: /* %16 */
    /* %16 + (* Pt) => Src2PrimFldArr */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 69, base, other, false);
    }
    break;
  case 89: /* storeStatPrimCtxt */
    /* _storeStatPrimCtxt[i] + (Src2Prim *) => Src2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(64)){
      addEdge(other.from, base.from, 83, base, other, true);
    }
    /* _storeStatPrimCtxt[i] + (Sink2Prim *) => Sink2PrimFldStat[i] */
    for(Edge other : base.to.getInEdges(58)){
      addEdge(other.from, base.from, 94, base, other, true);
    }
    break;
  case 90: /* %17 */
    /* %17 + (* _loadPrimCtxt) => Sink2Prim */
    for(Edge other : base.to.getInEdges(78)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    break;
  case 91: /* %18 */
    /* %18 + (* _loadPrimCtxtArr) => Sink2Prim */
    for(Edge other : base.to.getInEdges(80)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    break;
  case 92: /* Sink2PrimFld */
    /* Sink2PrimFld[i] + (* _Pt) => %19[i] */
    for(Edge other : base.to.getInEdges(43)){
      addEdge(base.from, other.from, 93, base, other, true);
    }
    break;
  case 93: /* %19 */
    /* %19[i] + (* _loadPrimCtxt[i]) => Sink2Prim */
    for(Edge other : base.to.getInEdges(78)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    break;
  case 94: /* Sink2PrimFldStat */
    /* Sink2PrimFldStat[i] + (* _loadStatPrimCtxt[i]) => Sink2Prim */
    for(Edge other : base.to.getInEdges(84)){
      addEdge(base.from, other.from, 58, base, other, false);
    }
    break;
  case 95: /* %20 */
    /* %20[i] + (* Pt) => Sink2PrimFld[i] */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 92, base, other, true);
    }
    break;
  case 96: /* %21 */
    /* %21 + (* Pt) => Sink2PrimFldArr */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 74, base, other, false);
    }
    break;
  }
}

public String[] outputRels() {
    String[] rels = {"LabelRef3", "LabelPrim3", "Flow3"};
    return rels;
}

public short kindToWeight(int kind) {
  switch (kind) {
  case 7:
    return (short)1;
  case 29:
    return (short)1;
  case 30:
    return (short)1;
  case 32:
    return (short)1;
  case 33:
    return (short)1;
  case 34:
    return (short)1;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}