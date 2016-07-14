package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
# Field-sensitive, k-CFA-based, heap-flow-based, taint-flow-based taint
# analysis.

# Combines points-to and taint flow analysis (e.g. G1 + T2)

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

R4[f] :: cs_primStore[f] _FlowsTo
R5[f] :: FlowsTo cs_primLoad[f]
R6 :: R4[f] R5[f]

Src2Prim :: cs_srcPrimFlow
          | Src2Prim cs_primAssign
	  | Src2Prim cs_primAssignArg
	  | Src2Prim cs_primAssignRet
          | Src2Prim R6
	  | Src2Obj R5[*]

Obj2Sink :: FlowsTo cs_refSinkFlow

# Passthrough handling
Src2Ref :: Src2Obj FlowsTo
Src2Obj :: Src2Ref cs_refRefFlow _FlowsTo
         | Src2Prim cs_primRefFlow _FlowsTo
Src2Prim :: Src2Prim cs_primPrimFlow
          | Src2Ref cs_refPrimFlow

# Nested taint rules
# o tainted => o.* tainted
InstFldPt :: _R1[*] _FlowsTo
Src2Obj :: Src2Obj InstFldPt

# o escapes => o.* escapes
Obj2Sink :: _InstFldPt Obj2Sink

Src2Sink :: Src2Prim R4[*] Obj2Sink
          | Src2Obj Obj2Sink
          | Src2Prim cs_primSinkFlow

.output Src2Sink

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
	Src2Prim :: Src2Prim cs_primAssign
	Src2Prim :: Src2Prim cs_primAssignArg
	Src2Prim :: Src2Prim cs_primAssignRet
	Src2Prim :: Src2Prim R6
	Src2Prim :: Src2Obj R5
	Src2Prim :: Src2Prim cs_primPrimFlow
	Src2Prim :: Src2Ref cs_refPrimFlow
R1:
	R1[i] :: cs_refStore[i] _FlowsTo
R2:
	R2[i] :: FlowsTo cs_refLoad[i]
R3:
	R3 :: R1[i] R2[i]
%1:
	%1 :: Src2Prim cs_primRefFlow
Src2Ref:
	Src2Ref :: Src2Obj FlowsTo
Src2Sink:
	Src2Sink :: %2 Obj2Sink
	Src2Sink :: Src2Obj Obj2Sink
	Src2Sink :: Src2Prim cs_primSinkFlow
InstFldPt:
	InstFldPt :: _R1 _FlowsTo
FlowsTo:
	FlowsTo :: cs_refAlloc
	FlowsTo :: FlowsTo cs_refAssign
	FlowsTo :: FlowsTo cs_refAssignArg
	FlowsTo :: FlowsTo cs_refAssignRet
	FlowsTo :: FlowsTo R3
Src2Obj:
	Src2Obj :: cs_srcRefFlow _FlowsTo
	Src2Obj :: %0 _FlowsTo
	Src2Obj :: %1 _FlowsTo
	Src2Obj :: Src2Obj InstFldPt
%0:
	%0 :: Src2Ref cs_refRefFlow
Obj2Sink:
	Obj2Sink :: FlowsTo cs_refSinkFlow
	Obj2Sink :: _InstFldPt Obj2Sink
%2:
	%2 :: Src2Prim R4
*/

/* Reverse Productions:
cs_refAssignRet:
	cs_refAssignRet + (FlowsTo *) => FlowsTo
cs_primSinkFlow:
	cs_primSinkFlow + (Src2Prim *) => Src2Sink
cs_primPrimFlow:
	cs_primPrimFlow + (Src2Prim *) => Src2Prim
cs_primRefFlow:
	cs_primRefFlow + (Src2Prim *) => %1
FlowsTo:
	_FlowsTo + (cs_refStore[i] *) => R1[i]
	FlowsTo + (* cs_refLoad[i]) => R2[i]
	FlowsTo + (* cs_refAssign) => FlowsTo
	FlowsTo + (* cs_refAssignArg) => FlowsTo
	FlowsTo + (* cs_refAssignRet) => FlowsTo
	FlowsTo + (* R3) => FlowsTo
	_FlowsTo + (cs_srcRefFlow *) => Src2Obj
	_FlowsTo + (cs_primStore[i] *) => R4[i]
	FlowsTo + (* cs_primLoad[i]) => R5[i]
	FlowsTo + (* cs_refSinkFlow) => Obj2Sink
	FlowsTo + (Src2Obj *) => Src2Ref
	_FlowsTo + (%0 *) => Src2Obj
	_FlowsTo + (%1 *) => Src2Obj
	_FlowsTo + (_R1 *) => InstFldPt
%1:
	%1 + (* _FlowsTo) => Src2Obj
%0:
	%0 + (* _FlowsTo) => Src2Obj
Obj2Sink:
	Obj2Sink + (_InstFldPt *) => Obj2Sink
	Obj2Sink + (%2 *) => Src2Sink
	Obj2Sink + (Src2Obj *) => Src2Sink
cs_refSinkFlow:
	cs_refSinkFlow + (FlowsTo *) => Obj2Sink
cs_refAlloc:
	cs_refAlloc => FlowsTo
cs_primAssign:
	cs_primAssign + (Src2Prim *) => Src2Prim
cs_refAssign:
	cs_refAssign + (FlowsTo *) => FlowsTo
cs_refLoad:
	cs_refLoad[i] + (FlowsTo *) => R2[i]
cs_srcPrimFlow:
	cs_srcPrimFlow => Src2Prim
R4:
	R4[i] + (* R5[i]) => R6
	R4 + (Src2Prim *) => %2
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
	Src2Ref + (* cs_refPrimFlow) => Src2Prim
cs_primAssignRet:
	cs_primAssignRet + (Src2Prim *) => Src2Prim
cs_refStore:
	cs_refStore[i] + (* _FlowsTo) => R1[i]
cs_refRefFlow:
	cs_refRefFlow + (Src2Ref *) => %0
cs_refAssignArg:
	cs_refAssignArg + (FlowsTo *) => FlowsTo
cs_primAssignArg:
	cs_primAssignArg + (Src2Prim *) => Src2Prim
Src2Prim:
	Src2Prim + (* cs_primAssign) => Src2Prim
	Src2Prim + (* cs_primAssignArg) => Src2Prim
	Src2Prim + (* cs_primAssignRet) => Src2Prim
	Src2Prim + (* R6) => Src2Prim
	Src2Prim + (* cs_primRefFlow) => %1
	Src2Prim + (* cs_primPrimFlow) => Src2Prim
	Src2Prim + (* R4) => %2
	Src2Prim + (* cs_primSinkFlow) => Src2Sink
cs_srcRefFlow:
	cs_srcRefFlow + (* _FlowsTo) => Src2Obj
cs_refPrimFlow:
	cs_refPrimFlow + (Src2Ref *) => Src2Prim
InstFldPt:
	InstFldPt + (Src2Obj *) => Src2Obj
	_InstFldPt + (* Obj2Sink) => Obj2Sink
cs_primLoad:
	cs_primLoad[i] + (FlowsTo *) => R5[i]
%2:
	%2 + (* Obj2Sink) => Src2Sink
cs_primStore:
	cs_primStore[i] + (* _FlowsTo) => R4[i]
Src2Obj:
	Src2Obj + (* R5) => Src2Prim
	Src2Obj + (* FlowsTo) => Src2Ref
	Src2Obj + (* InstFldPt) => Src2Obj
	Src2Obj + (* Obj2Sink) => Src2Sink
*/

public class C11 extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 4:
  case 6:
  case 7:
  case 8:
  case 9:
  case 11:
  case 13:
  case 15:
  case 18:
  case 19:
  case 20:
  case 21:
  case 23:
  case 25:
  case 27:
  case 29:
  case 30:
  case 34:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 35;
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
  if (symbol.equals("R4")) return 12;
  if (symbol.equals("cs_primStore")) return 13;
  if (symbol.equals("R5")) return 14;
  if (symbol.equals("cs_primLoad")) return 15;
  if (symbol.equals("R6")) return 16;
  if (symbol.equals("Src2Prim")) return 17;
  if (symbol.equals("cs_srcPrimFlow")) return 18;
  if (symbol.equals("cs_primAssign")) return 19;
  if (symbol.equals("cs_primAssignArg")) return 20;
  if (symbol.equals("cs_primAssignRet")) return 21;
  if (symbol.equals("Obj2Sink")) return 22;
  if (symbol.equals("cs_refSinkFlow")) return 23;
  if (symbol.equals("Src2Ref")) return 24;
  if (symbol.equals("cs_refRefFlow")) return 25;
  if (symbol.equals("%0")) return 26;
  if (symbol.equals("cs_primRefFlow")) return 27;
  if (symbol.equals("%1")) return 28;
  if (symbol.equals("cs_primPrimFlow")) return 29;
  if (symbol.equals("cs_refPrimFlow")) return 30;
  if (symbol.equals("InstFldPt")) return 31;
  if (symbol.equals("Src2Sink")) return 32;
  if (symbol.equals("%2")) return 33;
  if (symbol.equals("cs_primSinkFlow")) return 34;
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
  case 12: return "R4";
  case 13: return "cs_primStore";
  case 14: return "R5";
  case 15: return "cs_primLoad";
  case 16: return "R6";
  case 17: return "Src2Prim";
  case 18: return "cs_srcPrimFlow";
  case 19: return "cs_primAssign";
  case 20: return "cs_primAssignArg";
  case 21: return "cs_primAssignRet";
  case 22: return "Obj2Sink";
  case 23: return "cs_refSinkFlow";
  case 24: return "Src2Ref";
  case 25: return "cs_refRefFlow";
  case 26: return "%0";
  case 27: return "cs_primRefFlow";
  case 28: return "%1";
  case 29: return "cs_primPrimFlow";
  case 30: return "cs_refPrimFlow";
  case 31: return "InstFldPt";
  case 32: return "Src2Sink";
  case 33: return "%2";
  case 34: return "cs_primSinkFlow";
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
      addEdge(base.to, other.from, 31, base, other, false);
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
    /* _FlowsTo + (cs_primStore[i] *) => R4[i] */
    for(Edge other : base.to.getInEdges(13)){
      addEdge(other.from, base.from, 12, base, other, true);
    }
    /* FlowsTo + (* cs_primLoad[i]) => R5[i] */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 14, base, other, true);
    }
    /* FlowsTo + (* cs_refSinkFlow) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 22, base, other, false);
    }
    /* FlowsTo + (Src2Obj *) => Src2Ref */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    /* _FlowsTo + (%0 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(26)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (%1 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(28)){
      addEdge(other.from, base.from, 10, base, other, false);
    }
    /* _FlowsTo + (_R1 *) => InstFldPt */
    for(Edge other : base.to.getOutEdges(0)){
      addEdge(other.to, base.from, 31, base, other, false);
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
    for(Edge other : base.to.getOutEdges(14)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Obj + (* FlowsTo) => Src2Ref */
    for(Edge other : base.to.getOutEdges(2)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* Src2Obj + (* InstFldPt) => Src2Obj */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* Src2Obj + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(22)){
      addEdge(base.from, other.to, 32, base, other, false);
    }
    break;
  case 11: /* cs_srcRefFlow */
    /* cs_srcRefFlow + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 12: /* R4 */
    /* R4[i] + (* R5[i]) => R6 */
    for(Edge other : base.to.getOutEdges(14)){
      addEdge(base.from, other.to, 16, base, other, false);
    }
    /* R4 + (Src2Prim *) => %2 */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 33, base, other, false);
    }
    break;
  case 13: /* cs_primStore */
    /* cs_primStore[i] + (* _FlowsTo) => R4[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 12, base, other, true);
    }
    break;
  case 14: /* R5 */
    /* R5[i] + (R4[i] *) => R6 */
    for(Edge other : base.from.getInEdges(12)){
      addEdge(other.from, base.to, 16, base, other, false);
    }
    /* R5 + (Src2Obj *) => Src2Prim */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 15: /* cs_primLoad */
    /* cs_primLoad[i] + (FlowsTo *) => R5[i] */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 14, base, other, true);
    }
    break;
  case 16: /* R6 */
    /* R6 + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 17: /* Src2Prim */
    /* Src2Prim + (* cs_primAssign) => Src2Prim */
    for(Edge other : base.to.getOutEdges(19)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignArg) => Src2Prim */
    for(Edge other : base.to.getOutEdges(20)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignRet) => Src2Prim */
    for(Edge other : base.to.getOutEdges(21)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Prim + (* R6) => Src2Prim */
    for(Edge other : base.to.getOutEdges(16)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlow) => %1 */
    for(Edge other : base.to.getOutEdges(27)){
      addEdge(base.from, other.to, 28, base, other, false);
    }
    /* Src2Prim + (* cs_primPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(29)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* Src2Prim + (* R4) => %2 */
    for(Edge other : base.to.getOutEdges(12)){
      addEdge(base.from, other.to, 33, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlow) => Src2Sink */
    for(Edge other : base.to.getOutEdges(34)){
      addEdge(base.from, other.to, 32, base, other, false);
    }
    break;
  case 18: /* cs_srcPrimFlow */
    /* cs_srcPrimFlow => Src2Prim */
    addEdge(base.from, base.to, 17, base, false);
    break;
  case 19: /* cs_primAssign */
    /* cs_primAssign + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 20: /* cs_primAssignArg */
    /* cs_primAssignArg + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 21: /* cs_primAssignRet */
    /* cs_primAssignRet + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 22: /* Obj2Sink */
    /* Obj2Sink + (_InstFldPt *) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(31)){
      addEdge(other.to, base.to, 22, base, other, false);
    }
    /* Obj2Sink + (%2 *) => Src2Sink */
    for(Edge other : base.from.getInEdges(33)){
      addEdge(other.from, base.to, 32, base, other, false);
    }
    /* Obj2Sink + (Src2Obj *) => Src2Sink */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 32, base, other, false);
    }
    break;
  case 23: /* cs_refSinkFlow */
    /* cs_refSinkFlow + (FlowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 22, base, other, false);
    }
    break;
  case 24: /* Src2Ref */
    /* Src2Ref + (* cs_refRefFlow) => %0 */
    for(Edge other : base.to.getOutEdges(25)){
      addEdge(base.from, other.to, 26, base, other, false);
    }
    /* Src2Ref + (* cs_refPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    break;
  case 25: /* cs_refRefFlow */
    /* cs_refRefFlow + (Src2Ref *) => %0 */
    for(Edge other : base.from.getInEdges(24)){
      addEdge(other.from, base.to, 26, base, other, false);
    }
    break;
  case 26: /* %0 */
    /* %0 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 27: /* cs_primRefFlow */
    /* cs_primRefFlow + (Src2Prim *) => %1 */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 28, base, other, false);
    }
    break;
  case 28: /* %1 */
    /* %1 + (* _FlowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 10, base, other, false);
    }
    break;
  case 29: /* cs_primPrimFlow */
    /* cs_primPrimFlow + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 30: /* cs_refPrimFlow */
    /* cs_refPrimFlow + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(24)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 31: /* InstFldPt */
    /* InstFldPt + (Src2Obj *) => Src2Obj */
    for(Edge other : base.from.getInEdges(10)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    /* _InstFldPt + (* Obj2Sink) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(22)){
      addEdge(base.to, other.to, 22, base, other, false);
    }
    break;
  case 33: /* %2 */
    /* %2 + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(22)){
      addEdge(base.from, other.to, 32, base, other, false);
    }
    break;
  case 34: /* cs_primSinkFlow */
    /* cs_primSinkFlow + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(17)){
      addEdge(other.from, base.to, 32, base, other, false);
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
  default:
    return (short)0;
  }
}

public boolean useReps() { return true; }

}