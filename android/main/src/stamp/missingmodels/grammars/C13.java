package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
# Field-sensitive, k-CFA-based, heap-flow-based, taint-flow-based taint
# analysis.

# Compared to T2.java.cfg, infers sources and sinks (e.g. G1 + T3)

# Not handling:
# - Nested passthrough taint
# - ...

# Points-to rules
R1[f] :: cs_refStore[f] _FlowsTo
R2[f] :: FlowsTo cs_refLoad[f]
R3 :: R1[f] R2[f]

FlowsTo :: cs_refAlloc
         | FlowsTo cs_refAssign
         | FlowsTo cs_refAssignArg
         | FlowsTo cs_refAssignRet
         | FlowsTo R3

# Basic taint propagation rules
Src2Obj :: cs_srcRefFlow _FlowsTo
	  | cs_srcFlowStub _FlowsTo

R4[f] :: cs_primStore[f] _FlowsTo
R5[f] :: FlowsTo cs_primLoad[f]
R6 :: R4[f] R5[f]

Src2Prim :: cs_srcPrimFlow
	  | cs_primSrcFlowStub
          | Src2Prim cs_primAssign
          | Src2Prim cs_primAssignArg
          | Src2Prim cs_primAssignRet
          | Src2Prim R6
	  | Src2Obj R5[*]

Obj2Sink :: FlowsTo cs_refSinkFlow
	  | FlowsTo cs_sinkFlowStub

# Passthrough handling
Src2Ref :: Src2Obj FlowsTo

Src2Obj :: Src2Ref cs_refRefFlow _FlowsTo
	 | Src2Ref cs_passThroughStub _FlowsTo
         | Src2Prim cs_primRefFlow _FlowsTo
	 | Src2Prim cs_primRefFlowStub _FlowsTo

Src2Prim :: Src2Prim cs_primPrimFlow
	  | Src2Prim cs_primPassThroughStub
          | Src2Ref cs_refPrimFlow
	  | Src2Ref cs_refPrimFlowStub

# Nested taint rules
# o tainted => o.* tainted
InstFldPt :: _R1[*] _FlowsTo
Src2Obj :: Src2Obj InstFldPt

# o escapes => o.* escapes
Obj2Sink :: _InstFldPt Obj2Sink

Src2Sink :: Src2Prim R4[*] Obj2Sink
          | Src2Obj Obj2Sink
          | Src2Prim cs_primSinkFlow
	  | Src2Prim cs_primSinkFlowStub

.output Src2Sink
.weights cs_passThroughStub 1 cs_primRefFlowStub 1 cs_primPassThroughStub 1 cs_refPrimFlowStub 1
.weights cs_srcFlowStub 10 cs_sinkFlowStub 10 cs_primSrcFlowStub 10 cs_primSinkFlowStub 10
*/

/* Normalized Grammar:
R4:
	R4[i] :: cs_primStore[i] _FlowsTo
R5:
	R5[i] :: FlowsTo cs_primLoad[i]
R6:
	R6 :: R4[i] R5[i]
Src2Prim:
	Src2Prim :: cs_srcPrimFlow
	Src2Prim :: cs_primSrcFlowStub
	Src2Prim :: Src2Prim cs_primAssign
	Src2Prim :: Src2Prim cs_primAssignArg
	Src2Prim :: Src2Prim cs_primAssignRet
	Src2Prim :: Src2Prim R6
	Src2Prim :: Src2Obj R5
	Src2Prim :: Src2Prim cs_primPrimFlow
	Src2Prim :: Src2Prim cs_primPassThroughStub
	Src2Prim :: Src2Ref cs_refPrimFlow
	Src2Prim :: Src2Ref cs_refPrimFlowStub
R1:
	R1[i] :: cs_refStore[i] _FlowsTo
R2:
	R2[i] :: FlowsTo cs_refLoad[i]
R3:
	R3 :: R1[i] R2[i]
%1:
	%1 :: Src2Ref cs_passThroughStub
Src2Ref:
	Src2Ref :: Src2Obj FlowsTo
Src2Sink:
	Src2Sink :: %4 Obj2Sink
	Src2Sink :: Src2Obj Obj2Sink
	Src2Sink :: Src2Prim cs_primSinkFlow
	Src2Sink :: Src2Prim cs_primSinkFlowStub
InstFldPt:
	InstFldPt :: _R1 _FlowsTo
%3:
	%3 :: Src2Prim cs_primRefFlowStub
FlowsTo:
	FlowsTo :: cs_refAlloc
	FlowsTo :: FlowsTo cs_refAssign
	FlowsTo :: FlowsTo cs_refAssignArg
	FlowsTo :: FlowsTo cs_refAssignRet
	FlowsTo :: FlowsTo R3
%4:
	%4 :: Src2Prim R4
Src2Obj:
	Src2Obj :: cs_srcRefFlow _FlowsTo
	Src2Obj :: cs_srcFlowStub _FlowsTo
	Src2Obj :: %0 _FlowsTo
	Src2Obj :: %1 _FlowsTo
	Src2Obj :: %2 _FlowsTo
	Src2Obj :: %3 _FlowsTo
	Src2Obj :: Src2Obj InstFldPt
%0:
	%0 :: Src2Ref cs_refRefFlow
Obj2Sink:
	Obj2Sink :: FlowsTo cs_refSinkFlow
	Obj2Sink :: FlowsTo cs_sinkFlowStub
	Obj2Sink :: _InstFldPt Obj2Sink
%2:
	%2 :: Src2Prim cs_primRefFlow
*/

/* Reverse Productions:
cs_refAssignRet:
	cs_refAssignRet + (FlowsTo *) => FlowsTo
cs_primPassThroughStub:
	cs_primPassThroughStub + (Src2Prim *) => Src2Prim
cs_primPrimFlow:
	cs_primPrimFlow + (Src2Prim *) => Src2Prim
cs_primRefFlow:
	cs_primRefFlow + (Src2Prim *) => %2
cs_srcFlowStub:
	cs_srcFlowStub + (* _FlowsTo) => Src2Obj
FlowsTo:
	_FlowsTo + (cs_refStore[i] *) => R1[i]
	FlowsTo + (* cs_refLoad[i]) => R2[i]
	FlowsTo + (* cs_refAssign) => FlowsTo
	FlowsTo + (* cs_refAssignArg) => FlowsTo
	FlowsTo + (* cs_refAssignRet) => FlowsTo
	FlowsTo + (* R3) => FlowsTo
	_FlowsTo + (cs_srcRefFlow *) => Src2Obj
	_FlowsTo + (cs_srcFlowStub *) => Src2Obj
	_FlowsTo + (cs_primStore[i] *) => R4[i]
	FlowsTo + (* cs_primLoad[i]) => R5[i]
	FlowsTo + (* cs_refSinkFlow) => Obj2Sink
	FlowsTo + (* cs_sinkFlowStub) => Obj2Sink
	FlowsTo + (Src2Obj *) => Src2Ref
	_FlowsTo + (%0 *) => Src2Obj
	_FlowsTo + (%1 *) => Src2Obj
	_FlowsTo + (%2 *) => Src2Obj
	_FlowsTo + (%3 *) => Src2Obj
	_FlowsTo + (_R1 *) => InstFldPt
%4:
	%4 + (* Obj2Sink) => Src2Sink
cs_primSinkFlowStub:
	cs_primSinkFlowStub + (Src2Prim *) => Src2Sink
%1:
	%1 + (* _FlowsTo) => Src2Obj
%0:
	%0 + (* _FlowsTo) => Src2Obj
%3:
	%3 + (* _FlowsTo) => Src2Obj
cs_refSinkFlow:
	cs_refSinkFlow + (FlowsTo *) => Obj2Sink
cs_refAlloc:
	cs_refAlloc => FlowsTo
cs_primSinkFlow:
	cs_primSinkFlow + (Src2Prim *) => Src2Sink
cs_primAssign:
	cs_primAssign + (Src2Prim *) => Src2Prim
cs_refAssign:
	cs_refAssign + (FlowsTo *) => FlowsTo
Obj2Sink:
	Obj2Sink + (_InstFldPt *) => Obj2Sink
	Obj2Sink + (%4 *) => Src2Sink
	Obj2Sink + (Src2Obj *) => Src2Sink
cs_refLoad:
	cs_refLoad[i] + (FlowsTo *) => R2[i]
cs_srcPrimFlow:
	cs_srcPrimFlow => Src2Prim
cs_sinkFlowStub:
	cs_sinkFlowStub + (FlowsTo *) => Obj2Sink
R4:
	R4[i] + (* R5[i]) => R6
	R4 + (Src2Prim *) => %4
R5:
	R5[i] + (R4[i] *) => R6
	R5 + (Src2Obj *) => Src2Prim
R6:
	R6 + (Src2Prim *) => Src2Prim
R1:
	R1[i] + (* R2[i]) => R3
	_R1 + (* _FlowsTo) => InstFldPt
R2:
	R2[i] + (R1[i] *) => R3
R3:
	R3 + (FlowsTo *) => FlowsTo
Src2Ref:
	Src2Ref + (* cs_refRefFlow) => %0
	Src2Ref + (* cs_passThroughStub) => %1
	Src2Ref + (* cs_refPrimFlow) => Src2Prim
	Src2Ref + (* cs_refPrimFlowStub) => Src2Prim
cs_primAssignRet:
	cs_primAssignRet + (Src2Prim *) => Src2Prim
cs_refStore:
	cs_refStore[i] + (* _FlowsTo) => R1[i]
cs_refRefFlow:
	cs_refRefFlow + (Src2Ref *) => %0
cs_refAssignArg:
	cs_refAssignArg + (FlowsTo *) => FlowsTo
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
	Src2Prim + (* cs_primSinkFlowStub) => Src2Sink
cs_srcRefFlow:
	cs_srcRefFlow + (* _FlowsTo) => Src2Obj
cs_passThroughStub:
	cs_passThroughStub + (Src2Ref *) => %1
cs_refPrimFlow:
	cs_refPrimFlow + (Src2Ref *) => Src2Prim
cs_refPrimFlowStub:
	cs_refPrimFlowStub + (Src2Ref *) => Src2Prim
InstFldPt:
	InstFldPt + (Src2Obj *) => Src2Obj
	_InstFldPt + (* Obj2Sink) => Obj2Sink
cs_primLoad:
	cs_primLoad[i] + (FlowsTo *) => R5[i]
%2:
	%2 + (* _FlowsTo) => Src2Obj
cs_primStore:
	cs_primStore[i] + (* _FlowsTo) => R4[i]
Src2Obj:
	Src2Obj + (* R5) => Src2Prim
	Src2Obj + (* FlowsTo) => Src2Ref
	Src2Obj + (* InstFldPt) => Src2Obj
	Src2Obj + (* Obj2Sink) => Src2Sink
cs_primSrcFlowStub:
	cs_primSrcFlowStub => Src2Prim
*/

public class C13 extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 4:
  case 6:
  case 7:
  case 8:
  case 9:
  case 11:
  case 12:
  case 14:
  case 16:
  case 19:
  case 20:
  case 21:
  case 22:
  case 23:
  case 25:
  case 26:
  case 28:
  case 30:
  case 32:
  case 34:
  case 36:
  case 37:
  case 38:
  case 39:
  case 43:
  case 44:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 45;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("R1")) return 0;
  if (symbol.equals("cs_refStore")) return 1;
  if (symbol.equals("FlowsTo")) return 2;
  if (symbol.equals("R2")) return 3;
  if (symbol.equals("cs_refLoad")) return 4;
  if (symbol.equals("R3")) return 5;
  if (symbol.equals("cs_refAlloc")) return 6;
  if (symbol.equals("cs_refAssign")) return 7;
  if (symbol.equals("cs_refAssignArg")) return 8;
  if (symbol.equals("cs_refAssignRet")) return 9;
  if (symbol.equals("Src2Obj")) return 10;
  if (symbol.equals("cs_srcRefFlow")) return 11;
  if (symbol.equals("cs_srcFlowStub")) return 12;
  if (symbol.equals("R4")) return 13;
  if (symbol.equals("cs_primStore")) return 14;
  if (symbol.equals("R5")) return 15;
  if (symbol.equals("cs_primLoad")) return 16;
  if (symbol.equals("R6")) return 17;
  if (symbol.equals("Src2Prim")) return 18;
  if (symbol.equals("cs_srcPrimFlow")) return 19;
  if (symbol.equals("cs_primSrcFlowStub")) return 20;
  if (symbol.equals("cs_primAssign")) return 21;
  if (symbol.equals("cs_primAssignArg")) return 22;
  if (symbol.equals("cs_primAssignRet")) return 23;
  if (symbol.equals("Obj2Sink")) return 24;
  if (symbol.equals("cs_refSinkFlow")) return 25;
  if (symbol.equals("cs_sinkFlowStub")) return 26;
  if (symbol.equals("Src2Ref")) return 27;
  if (symbol.equals("cs_refRefFlow")) return 28;
  if (symbol.equals("%0")) return 29;
  if (symbol.equals("cs_passThroughStub")) return 30;
  if (symbol.equals("%1")) return 31;
  if (symbol.equals("cs_primRefFlow")) return 32;
  if (symbol.equals("%2")) return 33;
  if (symbol.equals("cs_primRefFlowStub")) return 34;
  if (symbol.equals("%3")) return 35;
  if (symbol.equals("cs_primPrimFlow")) return 36;
  if (symbol.equals("cs_primPassThroughStub")) return 37;
  if (symbol.equals("cs_refPrimFlow")) return 38;
  if (symbol.equals("cs_refPrimFlowStub")) return 39;
  if (symbol.equals("InstFldPt")) return 40;
  if (symbol.equals("Src2Sink")) return 41;
  if (symbol.equals("%4")) return 42;
  if (symbol.equals("cs_primSinkFlow")) return 43;
  if (symbol.equals("cs_primSinkFlowStub")) return 44;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "R1";
  case 1: return "cs_refStore";
  case 2: return "FlowsTo";
  case 3: return "R2";
  case 4: return "cs_refLoad";
  case 5: return "R3";
  case 6: return "cs_refAlloc";
  case 7: return "cs_refAssign";
  case 8: return "cs_refAssignArg";
  case 9: return "cs_refAssignRet";
  case 10: return "Src2Obj";
  case 11: return "cs_srcRefFlow";
  case 12: return "cs_srcFlowStub";
  case 13: return "R4";
  case 14: return "cs_primStore";
  case 15: return "R5";
  case 16: return "cs_primLoad";
  case 17: return "R6";
  case 18: return "Src2Prim";
  case 19: return "cs_srcPrimFlow";
  case 20: return "cs_primSrcFlowStub";
  case 21: return "cs_primAssign";
  case 22: return "cs_primAssignArg";
  case 23: return "cs_primAssignRet";
  case 24: return "Obj2Sink";
  case 25: return "cs_refSinkFlow";
  case 26: return "cs_sinkFlowStub";
  case 27: return "Src2Ref";
  case 28: return "cs_refRefFlow";
  case 29: return "%0";
  case 30: return "cs_passThroughStub";
  case 31: return "%1";
  case 32: return "cs_primRefFlow";
  case 33: return "%2";
  case 34: return "cs_primRefFlowStub";
  case 35: return "%3";
  case 36: return "cs_primPrimFlow";
  case 37: return "cs_primPassThroughStub";
  case 38: return "cs_refPrimFlow";
  case 39: return "cs_refPrimFlowStub";
  case 40: return "InstFldPt";
  case 41: return "Src2Sink";
  case 42: return "%4";
  case 43: return "cs_primSinkFlow";
  case 44: return "cs_primSinkFlowStub";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* R1 */
    /* R1[i] + (* R2[i]) => R3 */
    for(Edge other : base.to.getOutEdges(3)){
      addEdge(base.from, other.to, 5, base, other, false);
    }
    /* _R1 + (* _FlowsTo) => InstFldPt */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(base.to, other.from, 40, base, other, false);
    }
    break;
  case 1: /* cs_refStore */
    /* cs_refStore[i] + (* _FlowsTo) => R1[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 0, base, other, true);
    }
    break;
  case 2: /* FlowsTo */
    /* _FlowsTo + (cs_refStore[i] *) => R1[i] */
    for(Edge other : base.to.getInEdges(1)){
      addEdge(other.from, base.from, 0, base, other, true);
    }
    /* FlowsTo + (* cs_refLoad[i]) => R2[i] */
    for(Edge other : base.to.getOutEdges(4)){
      addEdge(base.from, other.to, 3, base, other, true);
    }
    /* FlowsTo + (* cs_refAssign) => FlowsTo */
    for(Edge other : base.to.getOutEdges(7)){
      addEdge(base.from, other.to, 2, base, other, false);
    }
    /* FlowsTo + (* cs_refAssignArg) => FlowsTo */
    for(Edge other : base.to.getOutEdges(8)){
      addEdge(base.from, other.to, 2, base, other, false);
    }
    /* FlowsTo + (* cs_refAssignRet) => FlowsTo */
    for(Edge other : base.to.getOutEdges(9)){
      addEdge(base.from, other.to, 2, base, other, false);
    }
    /* FlowsTo + (* R3) => FlowsTo */
    for(Edge other : base.to.getOutEdges(5)){
      addEdge(base.from, other.to, 2, base, other, false);
    }
    /* _FlowsTo + (cs_srcRefFlow *) => Src2Obj */
    for(Edge other : base.to.getInEdges(11)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (cs_srcFlowStub *) => Src2Obj */
    for(Edge other : base.to.getInEdges(12)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (cs_primStore[i] *) => R4[i] */
    for(Edge other : base.to.getInEdges(14)){
      addEdge(other.from, base.from, 13, base, other, true);
    }
    /* FlowsTo + (* cs_primLoad[i]) => R5[i] */
    for(Edge other : base.to.getOutEdges(16)){
      addEdge(base.from, other.to, 15, base, other, true);
    }
    /* FlowsTo + (* cs_refSinkFlow) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(25)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* FlowsTo + (* cs_sinkFlowStub) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(26)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* FlowsTo + (Src2Obj *) => Src2Ref */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 27, base, other, false);
    }
    /* _FlowsTo + (%0 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(29)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (%1 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(31)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (%2 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(33)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (%3 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(35)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (_R1 *) => InstFldPt */
    for(Edge other : base.to.getOutEdges(0)){
      addEdge(other.to, base.from, 40, base, other, false);
    }
    break;
  case 3: /* R2 */
    /* R2[i] + (R1[i] *) => R3 */
    for(Edge other : base.from.getInEdges(0)){
      addEdge(other.from, base.to, 5, base, other, false);
    }
    break;
  case 4: /* cs_refLoad */
    /* cs_refLoad[i] + (FlowsTo *) => R2[i] */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 3, base, other, true);
    }
    break;
  case 5: /* R3 */
    /* R3 + (FlowsTo *) => FlowsTo */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 2, base, other, false);
    }
    break;
  case 6: /* cs_refAlloc */
    /* cs_refAlloc => FlowsTo */
    addEdge(base.from, base.to, 2, base, false);
    break;
  case 7: /* cs_refAssign */
    /* cs_refAssign + (FlowsTo *) => FlowsTo */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 2, base, other, false);
    }
    break;
  case 8: /* cs_refAssignArg */
    /* cs_refAssignArg + (FlowsTo *) => FlowsTo */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 2, base, other, false);
    }
    break;
  case 9: /* cs_refAssignRet */
    /* cs_refAssignRet + (FlowsTo *) => FlowsTo */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 2, base, other, false);
    }
    break;
  case 10: /* Src2Obj */
    /* Src2Obj + (* R5) => Src2Prim */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Obj + (* FlowsTo) => Src2Ref */
    for(Edge other : base.to.getOutEdges(2)){
      addEdge(base.from, other.to, 27, base, other, false);
    }
    /* Src2Obj + (* InstFldPt) => Src2Obj */
    for(Edge other : base.to.getOutEdges(40)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Obj + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(24)){
      addEdge(base.from, other.to, 41, base, other, false);
    }
    break;
  case 11: /* cs_srcRefFlow */
    /* cs_srcRefFlow + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 12: /* cs_srcFlowStub */
    /* cs_srcFlowStub + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 13: /* R4 */
    /* R4[i] + (* R5[i]) => R6 */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* R4 + (Src2Prim *) => %4 */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 42, base, other, false);
    }
    break;
  case 14: /* cs_primStore */
    /* cs_primStore[i] + (* _FlowsTo) => R4[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 13, base, other, true);
    }
    break;
  case 15: /* R5 */
    /* R5[i] + (R4[i] *) => R6 */
    for(Edge other : base.from.getInEdges(13)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    /* R5 + (Src2Obj *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 16: /* cs_primLoad */
    /* cs_primLoad[i] + (FlowsTo *) => R5[i] */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 15, base, other, true);
    }
    break;
  case 17: /* R6 */
    /* R6 + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 18: /* Src2Prim */
    /* Src2Prim + (* cs_primAssign) => Src2Prim */
    for(Edge other : base.to.getOutEdges(21)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignArg) => Src2Prim */
    for(Edge other : base.to.getOutEdges(22)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignRet) => Src2Prim */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* R6) => Src2Prim */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlow) => %2 */
    for(Edge other : base.to.getOutEdges(32)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlowStub) => %3 */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 35, base, other, false);
    }
    /* Src2Prim + (* cs_primPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(36)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* cs_primPassThroughStub) => Src2Prim */
    for(Edge other : base.to.getOutEdges(37)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Prim + (* R4) => %4 */
    for(Edge other : base.to.getOutEdges(13)){
      addEdge(base.from, other.to, 42, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlow) => Src2Sink */
    for(Edge other : base.to.getOutEdges(43)){
      addEdge(base.from, other.to, 41, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlowStub) => Src2Sink */
    for(Edge other : base.to.getOutEdges(44)){
      addEdge(base.from, other.to, 41, base, other, false);
    }
    break;
  case 19: /* cs_srcPrimFlow */
    /* cs_srcPrimFlow => Src2Prim */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 20: /* cs_primSrcFlowStub */
    /* cs_primSrcFlowStub => Src2Prim */
    addEdge(base.from, base.to, 18, base, false);
    break;
  case 21: /* cs_primAssign */
    /* cs_primAssign + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 22: /* cs_primAssignArg */
    /* cs_primAssignArg + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 23: /* cs_primAssignRet */
    /* cs_primAssignRet + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 24: /* Obj2Sink */
    /* Obj2Sink + (_InstFldPt *) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(40)){
      addEdge(other.to, base.to, 24, base, other, false);
    }
    /* Obj2Sink + (%4 *) => Src2Sink */
    for(Edge other : base.from.getInEdges(42)){
      addEdge(other.from, base.to, 41, base, other, false);
    }
    /* Obj2Sink + (Src2Obj *) => Src2Sink */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 41, base, other, false);
    }
    break;
  case 25: /* cs_refSinkFlow */
    /* cs_refSinkFlow + (FlowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    break;
  case 26: /* cs_sinkFlowStub */
    /* cs_sinkFlowStub + (FlowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    break;
  case 27: /* Src2Ref */
    /* Src2Ref + (* cs_refRefFlow) => %0 */
    for(Edge other : base.to.getOutEdges(28)){
      addEdge(base.from, other.to, 29, base, other, false);
    }
    /* Src2Ref + (* cs_passThroughStub) => %1 */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 31, base, other, false);
    }
    /* Src2Ref + (* cs_refPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(38)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    /* Src2Ref + (* cs_refPrimFlowStub) => Src2Prim */
    for(Edge other : base.to.getOutEdges(39)){
      addEdge(base.from, other.to, 18, base, other, false);
    }
    break;
  case 28: /* cs_refRefFlow */
    /* cs_refRefFlow + (Src2Ref *) => %0 */
    for(Edge other : base.from.getInEdges(27)){
      addEdge(other.from, base.to, 29, base, other, false);
    }
    break;
  case 29: /* %0 */
    /* %0 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 30: /* cs_passThroughStub */
    /* cs_passThroughStub + (Src2Ref *) => %1 */
    for(Edge other : base.from.getInEdges(27)){
      addEdge(other.from, base.to, 31, base, other, false);
    }
    break;
  case 31: /* %1 */
    /* %1 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 32: /* cs_primRefFlow */
    /* cs_primRefFlow + (Src2Prim *) => %2 */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    break;
  case 33: /* %2 */
    /* %2 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 34: /* cs_primRefFlowStub */
    /* cs_primRefFlowStub + (Src2Prim *) => %3 */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 35, base, other, false);
    }
    break;
  case 35: /* %3 */
    /* %3 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 36: /* cs_primPrimFlow */
    /* cs_primPrimFlow + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 37: /* cs_primPassThroughStub */
    /* cs_primPassThroughStub + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 38: /* cs_refPrimFlow */
    /* cs_refPrimFlow + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(27)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 39: /* cs_refPrimFlowStub */
    /* cs_refPrimFlowStub + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(27)){
      addEdge(other.from, base.to, 18, base, other, false);
    }
    break;
  case 40: /* InstFldPt */
    /* InstFldPt + (Src2Obj *) => Src2Obj */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    /* _InstFldPt + (* Obj2Sink) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(24)){
      addEdge(base.to, other.to, 24, base, other, false);
    }
    break;
  case 42: /* %4 */
    /* %4 + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(24)){
      addEdge(base.from, other.to, 41, base, other, false);
    }
    break;
  case 43: /* cs_primSinkFlow */
    /* cs_primSinkFlow + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 41, base, other, false);
    }
    break;
  case 44: /* cs_primSinkFlowStub */
    /* cs_primSinkFlowStub + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(18)){
      addEdge(other.from, base.to, 41, base, other, false);
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
  case 12:
    return (short)10;
  case 20:
    return (short)10;
  case 26:
    return (short)10;
  case 30:
    return (short)1;
  case 34:
    return (short)1;
  case 37:
    return (short)1;
  case 39:
    return (short)1;
  case 44:
    return (short)10;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}