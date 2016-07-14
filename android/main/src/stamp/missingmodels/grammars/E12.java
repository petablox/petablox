package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
# Field-sensitive, k-CFA-based, heap-flow-based, taint-flow-based taint
# analysis.

# Taint flow analysis (e.g. T2), except uses points-to from BDDBDDB

# Not handling:
# - Nested passthrough taint
# - ...

# Points-to rules
R1[f] :: cs_refStore[f] _flowsTo

# Basic taint propagation rules
Src2Obj :: cs_srcRefFlow _flowsTo

R4[f] :: cs_primStore[f] _flowsTo
R5[f] :: flowsTo cs_primLoad[f]
R6 :: R4[f] R5[f]

Src2Prim :: cs_srcPrimFlow
          | Src2Prim cs_primAssign
	  | Src2Prim cs_primAssignArg
	  | Src2Prim cs_primAssignRet
          | Src2Prim R6
	  | Src2Obj R5[*]

Obj2Sink :: flowsTo cs_refSinkFlow

# Make stub passthrough pass InstFldPt
PassThroughStub :: cs_passThroughStub
		 | _InstFldPt PassThroughStub

RefPrimFlowStub :: cs_refPrimFlowStub
		 | _InstFldPt RefPrimFlowStub

# Passthrough handling
Src2Ref :: Src2Obj flowsTo

Src2Obj :: Src2Ref cs_refRefFlow _flowsTo
	 | Src2Ref PassThroughStub _flowsTo
         | Src2Prim cs_primRefFlow _flowsTo
	 | Src2Prim cs_primRefFlowStub _flowsTo

Src2Prim :: Src2Prim cs_primPrimFlow
	  | Src2Prim cs_primPassThroughStub
          | Src2Ref cs_refPrimFlow
	  | Src2Ref RefPrimFlowStub

# Nested taint rules
# o tainted => o.* tainted
InstFldPt :: _R1[*] _flowsTo
Src2Obj :: Src2Obj InstFldPt

# o escapes => o.* escapes
Obj2Sink :: _InstFldPt Obj2Sink

Src2Sink :: Src2Prim R4[*] Obj2Sink
          | Src2Obj Obj2Sink
          | Src2Prim cs_primSinkFlow

.output Src2Sink
.weights cs_passThroughStub 1 cs_primRefFlowStub 1 cs_primPassThroughStub 1 cs_refPrimFlowStub 1
*/

/* Normalized Grammar:
R4:
	R4[i] :: cs_primStore[i] _flowsTo
R5:
	R5[i] :: flowsTo cs_primLoad[i]
R6:
	R6 :: R4[i] R5[i]
Src2Prim:
	Src2Prim :: cs_srcPrimFlow
	Src2Prim :: Src2Prim cs_primAssign
	Src2Prim :: Src2Prim cs_primAssignArg
	Src2Prim :: Src2Prim cs_primAssignRet
	Src2Prim :: Src2Prim R6
	Src2Prim :: Src2Obj R5
	Src2Prim :: Src2Prim cs_primPrimFlow
	Src2Prim :: Src2Prim cs_primPassThroughStub
	Src2Prim :: Src2Ref cs_refPrimFlow
	Src2Prim :: Src2Ref RefPrimFlowStub
R1:
	R1[i] :: cs_refStore[i] _flowsTo
%1:
	%1 :: Src2Ref PassThroughStub
Src2Ref:
	Src2Ref :: Src2Obj flowsTo
RefPrimFlowStub:
	RefPrimFlowStub :: cs_refPrimFlowStub
	RefPrimFlowStub :: _InstFldPt RefPrimFlowStub
PassThroughStub:
	PassThroughStub :: cs_passThroughStub
	PassThroughStub :: _InstFldPt PassThroughStub
Src2Sink:
	Src2Sink :: %4 Obj2Sink
	Src2Sink :: Src2Obj Obj2Sink
	Src2Sink :: Src2Prim cs_primSinkFlow
InstFldPt:
	InstFldPt :: _R1 _flowsTo
%3:
	%3 :: Src2Prim cs_primRefFlowStub
%4:
	%4 :: Src2Prim R4
Src2Obj:
	Src2Obj :: cs_srcRefFlow _flowsTo
	Src2Obj :: %0 _flowsTo
	Src2Obj :: %1 _flowsTo
	Src2Obj :: %2 _flowsTo
	Src2Obj :: %3 _flowsTo
	Src2Obj :: Src2Obj InstFldPt
%0:
	%0 :: Src2Ref cs_refRefFlow
Obj2Sink:
	Obj2Sink :: flowsTo cs_refSinkFlow
	Obj2Sink :: _InstFldPt Obj2Sink
%2:
	%2 :: Src2Prim cs_primRefFlow
*/

/* Reverse Productions:
cs_primPassThroughStub:
	cs_primPassThroughStub + (Src2Prim *) => Src2Prim
cs_primPrimFlow:
	cs_primPrimFlow + (Src2Prim *) => Src2Prim
cs_primRefFlow:
	cs_primRefFlow + (Src2Prim *) => %2
%4:
	%4 + (* Obj2Sink) => Src2Sink
%1:
	%1 + (* _flowsTo) => Src2Obj
%0:
	%0 + (* _flowsTo) => Src2Obj
%3:
	%3 + (* _flowsTo) => Src2Obj
cs_refSinkFlow:
	cs_refSinkFlow + (flowsTo *) => Obj2Sink
cs_primSinkFlow:
	cs_primSinkFlow + (Src2Prim *) => Src2Sink
cs_primAssign:
	cs_primAssign + (Src2Prim *) => Src2Prim
Obj2Sink:
	Obj2Sink + (_InstFldPt *) => Obj2Sink
	Obj2Sink + (%4 *) => Src2Sink
	Obj2Sink + (Src2Obj *) => Src2Sink
cs_refPrimFlowStub:
	cs_refPrimFlowStub => RefPrimFlowStub
cs_srcPrimFlow:
	cs_srcPrimFlow => Src2Prim
R4:
	R4[i] + (* R5[i]) => R6
	R4 + (Src2Prim *) => %4
R5:
	R5[i] + (R4[i] *) => R6
	R5 + (Src2Obj *) => Src2Prim
R6:
	R6 + (Src2Prim *) => Src2Prim
cs_primLoad:
	cs_primLoad[i] + (flowsTo *) => R5[i]
Src2Ref:
	Src2Ref + (* cs_refRefFlow) => %0
	Src2Ref + (* PassThroughStub) => %1
	Src2Ref + (* cs_refPrimFlow) => Src2Prim
	Src2Ref + (* RefPrimFlowStub) => Src2Prim
cs_primAssignRet:
	cs_primAssignRet + (Src2Prim *) => Src2Prim
RefPrimFlowStub:
	RefPrimFlowStub + (_InstFldPt *) => RefPrimFlowStub
	RefPrimFlowStub + (Src2Ref *) => Src2Prim
PassThroughStub:
	PassThroughStub + (_InstFldPt *) => PassThroughStub
	PassThroughStub + (Src2Ref *) => %1
cs_primStore:
	cs_primStore[i] + (* _flowsTo) => R4[i]
cs_primRefFlowStub:
	cs_primRefFlowStub + (Src2Prim *) => %3
cs_primAssignArg:
	cs_primAssignArg + (Src2Prim *) => Src2Prim
Src2Prim:
	Src2Prim + (* cs_primAssign) => Src2Prim
	Src2Prim + (* cs_primAssignArg) => Src2Prim
	Src2Prim + (* cs_primAssignRet) => Src2Prim
	Src2Prim + (* R6) => Src2Prim
	Src2Prim + (* cs_primRefFlow) => %2
	Src2Prim + (* cs_primRefFlowStub) => %3
	Src2Prim + (* cs_primPrimFlow) => Src2Prim
	Src2Prim + (* cs_primPassThroughStub) => Src2Prim
	Src2Prim + (* R4) => %4
	Src2Prim + (* cs_primSinkFlow) => Src2Sink
cs_srcRefFlow:
	cs_srcRefFlow + (* _flowsTo) => Src2Obj
cs_passThroughStub:
	cs_passThroughStub => PassThroughStub
cs_refPrimFlow:
	cs_refPrimFlow + (Src2Ref *) => Src2Prim
R1:
	_R1 + (* _flowsTo) => InstFldPt
InstFldPt:
	_InstFldPt + (* PassThroughStub) => PassThroughStub
	_InstFldPt + (* RefPrimFlowStub) => RefPrimFlowStub
	InstFldPt + (Src2Obj *) => Src2Obj
	_InstFldPt + (* Obj2Sink) => Obj2Sink
cs_refRefFlow:
	cs_refRefFlow + (Src2Ref *) => %0
flowsTo:
	_flowsTo + (cs_refStore[i] *) => R1[i]
	_flowsTo + (cs_srcRefFlow *) => Src2Obj
	_flowsTo + (cs_primStore[i] *) => R4[i]
	flowsTo + (* cs_primLoad[i]) => R5[i]
	flowsTo + (* cs_refSinkFlow) => Obj2Sink
	flowsTo + (Src2Obj *) => Src2Ref
	_flowsTo + (%0 *) => Src2Obj
	_flowsTo + (%1 *) => Src2Obj
	_flowsTo + (%2 *) => Src2Obj
	_flowsTo + (%3 *) => Src2Obj
	_flowsTo + (_R1 *) => InstFldPt
%2:
	%2 + (* _flowsTo) => Src2Obj
cs_refStore:
	cs_refStore[i] + (* _flowsTo) => R1[i]
Src2Obj:
	Src2Obj + (* R5) => Src2Prim
	Src2Obj + (* flowsTo) => Src2Ref
	Src2Obj + (* InstFldPt) => Src2Obj
	Src2Obj + (* Obj2Sink) => Src2Sink
*/

public class E12 extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 2:
  case 4:
  case 6:
  case 8:
  case 11:
  case 12:
  case 13:
  case 14:
  case 16:
  case 18:
  case 21:
  case 23:
  case 26:
  case 28:
  case 30:
  case 31:
  case 32:
  case 35:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 36;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("R1")) return 0;
  if (symbol.equals("cs_refStore")) return 1;
  if (symbol.equals("flowsTo")) return 2;
  if (symbol.equals("Src2Obj")) return 3;
  if (symbol.equals("cs_srcRefFlow")) return 4;
  if (symbol.equals("R4")) return 5;
  if (symbol.equals("cs_primStore")) return 6;
  if (symbol.equals("R5")) return 7;
  if (symbol.equals("cs_primLoad")) return 8;
  if (symbol.equals("R6")) return 9;
  if (symbol.equals("Src2Prim")) return 10;
  if (symbol.equals("cs_srcPrimFlow")) return 11;
  if (symbol.equals("cs_primAssign")) return 12;
  if (symbol.equals("cs_primAssignArg")) return 13;
  if (symbol.equals("cs_primAssignRet")) return 14;
  if (symbol.equals("Obj2Sink")) return 15;
  if (symbol.equals("cs_refSinkFlow")) return 16;
  if (symbol.equals("PassThroughStub")) return 17;
  if (symbol.equals("cs_passThroughStub")) return 18;
  if (symbol.equals("InstFldPt")) return 19;
  if (symbol.equals("RefPrimFlowStub")) return 20;
  if (symbol.equals("cs_refPrimFlowStub")) return 21;
  if (symbol.equals("Src2Ref")) return 22;
  if (symbol.equals("cs_refRefFlow")) return 23;
  if (symbol.equals("%0")) return 24;
  if (symbol.equals("%1")) return 25;
  if (symbol.equals("cs_primRefFlow")) return 26;
  if (symbol.equals("%2")) return 27;
  if (symbol.equals("cs_primRefFlowStub")) return 28;
  if (symbol.equals("%3")) return 29;
  if (symbol.equals("cs_primPrimFlow")) return 30;
  if (symbol.equals("cs_primPassThroughStub")) return 31;
  if (symbol.equals("cs_refPrimFlow")) return 32;
  if (symbol.equals("Src2Sink")) return 33;
  if (symbol.equals("%4")) return 34;
  if (symbol.equals("cs_primSinkFlow")) return 35;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "R1";
  case 1: return "cs_refStore";
  case 2: return "flowsTo";
  case 3: return "Src2Obj";
  case 4: return "cs_srcRefFlow";
  case 5: return "R4";
  case 6: return "cs_primStore";
  case 7: return "R5";
  case 8: return "cs_primLoad";
  case 9: return "R6";
  case 10: return "Src2Prim";
  case 11: return "cs_srcPrimFlow";
  case 12: return "cs_primAssign";
  case 13: return "cs_primAssignArg";
  case 14: return "cs_primAssignRet";
  case 15: return "Obj2Sink";
  case 16: return "cs_refSinkFlow";
  case 17: return "PassThroughStub";
  case 18: return "cs_passThroughStub";
  case 19: return "InstFldPt";
  case 20: return "RefPrimFlowStub";
  case 21: return "cs_refPrimFlowStub";
  case 22: return "Src2Ref";
  case 23: return "cs_refRefFlow";
  case 24: return "%0";
  case 25: return "%1";
  case 26: return "cs_primRefFlow";
  case 27: return "%2";
  case 28: return "cs_primRefFlowStub";
  case 29: return "%3";
  case 30: return "cs_primPrimFlow";
  case 31: return "cs_primPassThroughStub";
  case 32: return "cs_refPrimFlow";
  case 33: return "Src2Sink";
  case 34: return "%4";
  case 35: return "cs_primSinkFlow";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* R1 */
    /* _R1 + (* _flowsTo) => InstFldPt */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(base.to, other.from, 19, base, other, false);
    }
    break;
  case 1: /* cs_refStore */
    /* cs_refStore[i] + (* _flowsTo) => R1[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 0, base, other, true);
    }
    break;
  case 2: /* flowsTo */
    /* _flowsTo + (cs_refStore[i] *) => R1[i] */
    for(Edge other : base.to.getInEdges(1)){
      addEdge(other.from, base.from, 0, base, other, true);
    }
    /* _flowsTo + (cs_srcRefFlow *) => Src2Obj */
    for(Edge other : base.to.getInEdges(4)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (cs_primStore[i] *) => R4[i] */
    for(Edge other : base.to.getInEdges(6)){
      addEdge(other.from, base.from, 5, base, other, true);
    }
    /* flowsTo + (* cs_primLoad[i]) => R5[i] */
    for(Edge other : base.to.getOutEdges(8)){
      addEdge(base.from, other.to, 7, base, other, true);
    }
    /* flowsTo + (* cs_refSinkFlow) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(16)){
      addEdge(base.from, other.to, 15, base, other, false);
    }
    /* flowsTo + (Src2Obj *) => Src2Ref */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 22, base, other, false);
    }
    /* _flowsTo + (%0 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (%1 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(25)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (%2 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(27)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (%3 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(29)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (_R1 *) => InstFldPt */
    for(Edge other : base.to.getOutEdges(0)){
      addEdge(other.to, base.from, 19, base, other, false);
    }
    break;
  case 3: /* Src2Obj */
    /* Src2Obj + (* R5) => Src2Prim */
    for(Edge other : base.to.getOutEdges(7)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Obj + (* flowsTo) => Src2Ref */
    for(Edge other : base.to.getOutEdges(2)){
      addEdge(base.from, other.to, 22, base, other, false);
    }
    /* Src2Obj + (* InstFldPt) => Src2Obj */
    for(Edge other : base.to.getOutEdges(19)){
      addEdge(base.from, other.to, 3, base, other, false);
    }
    /* Src2Obj + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    break;
  case 4: /* cs_srcRefFlow */
    /* cs_srcRefFlow + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 5: /* R4 */
    /* R4[i] + (* R5[i]) => R6 */
    for(Edge other : base.to.getOutEdges(7)){
      addEdge(base.from, other.to, 9, base, other, false);
    }
    /* R4 + (Src2Prim *) => %4 */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 34, base, other, false);
    }
    break;
  case 6: /* cs_primStore */
    /* cs_primStore[i] + (* _flowsTo) => R4[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 5, base, other, true);
    }
    break;
  case 7: /* R5 */
    /* R5[i] + (R4[i] *) => R6 */
    for(Edge other : base.from.getInEdges(5)){
      addEdge(other.from, base.to, 9, base, other, false);
    }
    /* R5 + (Src2Obj *) => Src2Prim */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 8: /* cs_primLoad */
    /* cs_primLoad[i] + (flowsTo *) => R5[i] */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 7, base, other, true);
    }
    break;
  case 9: /* R6 */
    /* R6 + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 10: /* Src2Prim */
    /* Src2Prim + (* cs_primAssign) => Src2Prim */
    for(Edge other : base.to.getOutEdges(12)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignArg) => Src2Prim */
    for(Edge other : base.to.getOutEdges(13)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignRet) => Src2Prim */
    for(Edge other : base.to.getOutEdges(14)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* R6) => Src2Prim */
    for(Edge other : base.to.getOutEdges(9)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlow) => %2 */
    for(Edge other : base.to.getOutEdges(26)){
      addEdge(base.from, other.to, 27, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlowStub) => %3 */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 29, base, other, false);
    }
    /* Src2Prim + (* cs_primPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* cs_primPassThroughStub) => Src2Prim */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Prim + (* R4) => %4 */
    for(Edge other : base.to.getOutEdges(5)){
      addEdge(base.from, other.to, 34, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlow) => Src2Sink */
    for(Edge other : base.to.getOutEdges(35)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    break;
  case 11: /* cs_srcPrimFlow */
    /* cs_srcPrimFlow => Src2Prim */
    addEdge(base.from, base.to, 10, base, false);
    break;
  case 12: /* cs_primAssign */
    /* cs_primAssign + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 13: /* cs_primAssignArg */
    /* cs_primAssignArg + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 14: /* cs_primAssignRet */
    /* cs_primAssignRet + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 15: /* Obj2Sink */
    /* Obj2Sink + (_InstFldPt *) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(19)){
      addEdge(other.to, base.to, 15, base, other, false);
    }
    /* Obj2Sink + (%4 *) => Src2Sink */
    for(Edge other : base.from.getInEdges(34)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    /* Obj2Sink + (Src2Obj *) => Src2Sink */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    break;
  case 16: /* cs_refSinkFlow */
    /* cs_refSinkFlow + (flowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 15, base, other, false);
    }
    break;
  case 17: /* PassThroughStub */
    /* PassThroughStub + (_InstFldPt *) => PassThroughStub */
    for(Edge other : base.from.getOutEdges(19)){
      addEdge(other.to, base.to, 17, base, other, false);
    }
    /* PassThroughStub + (Src2Ref *) => %1 */
    for(Edge other : base.from.getInEdges(22)){
      addEdge(other.from, base.to, 25, base, other, false);
    }
    break;
  case 18: /* cs_passThroughStub */
    /* cs_passThroughStub => PassThroughStub */
    addEdge(base.from, base.to, 17, base, false);
    break;
  case 19: /* InstFldPt */
    /* _InstFldPt + (* PassThroughStub) => PassThroughStub */
    for(Edge other : base.from.getOutEdges(17)){
      addEdge(base.to, other.to, 17, base, other, false);
    }
    /* _InstFldPt + (* RefPrimFlowStub) => RefPrimFlowStub */
    for(Edge other : base.from.getOutEdges(20)){
      addEdge(base.to, other.to, 20, base, other, false);
    }
    /* InstFldPt + (Src2Obj *) => Src2Obj */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 3, base, other, false);
    }
    /* _InstFldPt + (* Obj2Sink) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(15)){
      addEdge(base.to, other.to, 15, base, other, false);
    }
    break;
  case 20: /* RefPrimFlowStub */
    /* RefPrimFlowStub + (_InstFldPt *) => RefPrimFlowStub */
    for(Edge other : base.from.getOutEdges(19)){
      addEdge(other.to, base.to, 20, base, other, false);
    }
    /* RefPrimFlowStub + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(22)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 21: /* cs_refPrimFlowStub */
    /* cs_refPrimFlowStub => RefPrimFlowStub */
    addEdge(base.from, base.to, 20, base, false);
    break;
  case 22: /* Src2Ref */
    /* Src2Ref + (* cs_refRefFlow) => %0 */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* Src2Ref + (* PassThroughStub) => %1 */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 25, base, other, false);
    }
    /* Src2Ref + (* cs_refPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(32)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Ref + (* RefPrimFlowStub) => Src2Prim */
    for(Edge other : base.to.getOutEdges(20)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    break;
  case 23: /* cs_refRefFlow */
    /* cs_refRefFlow + (Src2Ref *) => %0 */
    for(Edge other : base.from.getInEdges(22)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    break;
  case 24: /* %0 */
    /* %0 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 25: /* %1 */
    /* %1 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 26: /* cs_primRefFlow */
    /* cs_primRefFlow + (Src2Prim *) => %2 */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 27, base, other, false);
    }
    break;
  case 27: /* %2 */
    /* %2 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 28: /* cs_primRefFlowStub */
    /* cs_primRefFlowStub + (Src2Prim *) => %3 */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 29, base, other, false);
    }
    break;
  case 29: /* %3 */
    /* %3 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 30: /* cs_primPrimFlow */
    /* cs_primPrimFlow + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 31: /* cs_primPassThroughStub */
    /* cs_primPassThroughStub + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 32: /* cs_refPrimFlow */
    /* cs_refPrimFlow + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(22)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    break;
  case 34: /* %4 */
    /* %4 + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    break;
  case 35: /* cs_primSinkFlow */
    /* cs_primSinkFlow + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    break;
  }
}

public String[] outputRels() {
    String[] rels = {"Src2Sink"};
    return rels;
}

public short kindToWeight(int kind) {
  switch (kind) {
  case 18:
    return (short)1;
  case 21:
    return (short)1;
  case 28:
    return (short)1;
  case 31:
    return (short)1;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}