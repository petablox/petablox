package stamp.missingmodels.grammars;
import stamp.missingmodels.util.jcflsolver.*;

/* Original Grammar:
# Field-sensitive, k-CFA-based, heap-flow-based, taint-flow-based taint
# analysis.

# Compared to T2.java.cfg, infers sources and sinks (e.g. G1 + T3)
# Compared to C13.java.cfg, labels flows from (method,arg/ret) pairs (using new relations)

# Not handling:
# - Nested passthrough taint
# - ...

# Points-to rules
R1[f] :: cs_refStore[f] _flowsTo

# Basic taint propagation rules
Src2Obj :: cs_srcRefFlowNew _flowsTo
	  | cs_srcFlowStub _flowsTo

R4[f] :: cs_primStore[f] _flowsTo
R5[f] :: flowsTo cs_primLoad[f]
R6 :: R4[f] R5[f]

Src2Prim :: cs_srcPrimFlowNew
	  | cs_primSrcFlowStub
          | Src2Prim cs_primAssign
          | Src2Prim cs_primAssignArg
          | Src2Prim cs_primAssignRet
          | Src2Prim R6
	  | Src2Obj R5[*]

Obj2Sink :: flowsTo cs_refSinkFlowNew
	  | flowsTo cs_sinkFlowStub

# Passthrough handling
Src2Ref :: Src2Obj flowsTo

Src2Obj :: Src2Ref cs_refRefFlow _flowsTo
         | Src2Prim cs_primRefFlow _flowsTo
#	 | Src2Ref cs_passThroughStub _flowsTo
#	 | Src2Prim cs_primRefFlowStub _flowsTo

Src2Prim :: Src2Prim cs_primPrimFlow
          | Src2Ref cs_refPrimFlow
#	  | Src2Prim cs_primPassThroughStub
#	  | Src2Ref cs_refPrimFlowStub

# Nested taint rules
# o tainted => o.* tainted
InstFldPt :: _R1[*] _flowsTo
Src2Obj :: Src2Obj InstFldPt

# o escapes => o.* escapes
Obj2Sink :: _InstFldPt Obj2Sink

Src2Sink :: Src2Prim R4[*] Obj2Sink
          | Src2Obj Obj2Sink
          | Src2Prim cs_primSinkFlowNew
	  | Src2Prim cs_primSinkFlowStub

.output Src2Sink
#.weights cs_passThroughStub 1 cs_primRefFlowStub 1 cs_primPassThroughStub 1 cs_refPrimFlowStub 1
.weights cs_srcFlowStub 3 cs_sinkFlowStub 6 cs_primSrcFlowStub 3 cs_primSinkFlowStub 6
.weights cs_srcRefFlowNew 1 cs_srcPrimFlowNew 1 cs_refSinkFlowNew 1 cs_primSinkFlowNew 1
*/

/* Normalized Grammar:
R4:
	R4[i] :: cs_primStore[i] _flowsTo
R5:
	R5[i] :: flowsTo cs_primLoad[i]
R6:
	R6 :: R4[i] R5[i]
Src2Prim:
	Src2Prim :: cs_srcPrimFlowNew
	Src2Prim :: cs_primSrcFlowStub
	Src2Prim :: Src2Prim cs_primAssign
	Src2Prim :: Src2Prim cs_primAssignArg
	Src2Prim :: Src2Prim cs_primAssignRet
	Src2Prim :: Src2Prim R6
	Src2Prim :: Src2Obj R5
	Src2Prim :: Src2Prim cs_primPrimFlow
	Src2Prim :: Src2Ref cs_refPrimFlow
R1:
	R1[i] :: cs_refStore[i] _flowsTo
%1:
	%1 :: Src2Prim cs_primRefFlow
Src2Ref:
	Src2Ref :: Src2Obj flowsTo
Src2Sink:
	Src2Sink :: %2 Obj2Sink
	Src2Sink :: Src2Obj Obj2Sink
	Src2Sink :: Src2Prim cs_primSinkFlowNew
	Src2Sink :: Src2Prim cs_primSinkFlowStub
InstFldPt:
	InstFldPt :: _R1 _flowsTo
Src2Obj:
	Src2Obj :: cs_srcRefFlowNew _flowsTo
	Src2Obj :: cs_srcFlowStub _flowsTo
	Src2Obj :: %0 _flowsTo
	Src2Obj :: %1 _flowsTo
	Src2Obj :: Src2Obj InstFldPt
%0:
	%0 :: Src2Ref cs_refRefFlow
Obj2Sink:
	Obj2Sink :: flowsTo cs_refSinkFlowNew
	Obj2Sink :: flowsTo cs_sinkFlowStub
	Obj2Sink :: _InstFldPt Obj2Sink
%2:
	%2 :: Src2Prim R4
*/

/* Reverse Productions:
cs_primPrimFlow:
	cs_primPrimFlow + (Src2Prim *) => Src2Prim
cs_srcFlowStub:
	cs_srcFlowStub + (* _flowsTo) => Src2Obj
cs_primSinkFlowNew:
	cs_primSinkFlowNew + (Src2Prim *) => Src2Sink
cs_primSinkFlowStub:
	cs_primSinkFlowStub + (Src2Prim *) => Src2Sink
%1:
	%1 + (* _flowsTo) => Src2Obj
%0:
	%0 + (* _flowsTo) => Src2Obj
Obj2Sink:
	Obj2Sink + (_InstFldPt *) => Obj2Sink
	Obj2Sink + (%2 *) => Src2Sink
	Obj2Sink + (Src2Obj *) => Src2Sink
R1:
	_R1 + (* _flowsTo) => InstFldPt
cs_srcPrimFlowNew:
	cs_srcPrimFlowNew => Src2Prim
cs_primAssign:
	cs_primAssign + (Src2Prim *) => Src2Prim
cs_srcRefFlowNew:
	cs_srcRefFlowNew + (* _flowsTo) => Src2Obj
%2:
	%2 + (* Obj2Sink) => Src2Sink
cs_sinkFlowStub:
	cs_sinkFlowStub + (flowsTo *) => Obj2Sink
R4:
	R4[i] + (* R5[i]) => R6
	R4 + (Src2Prim *) => %2
R5:
	R5[i] + (R4[i] *) => R6
	R5 + (Src2Obj *) => Src2Prim
R6:
	R6 + (Src2Prim *) => Src2Prim
cs_refRefFlow:
	cs_refRefFlow + (Src2Ref *) => %0
Src2Ref:
	Src2Ref + (* cs_refRefFlow) => %0
	Src2Ref + (* cs_refPrimFlow) => Src2Prim
cs_primAssignRet:
	cs_primAssignRet + (Src2Prim *) => Src2Prim
cs_refSinkFlowNew:
	cs_refSinkFlowNew + (flowsTo *) => Obj2Sink
cs_primStore:
	cs_primStore[i] + (* _flowsTo) => R4[i]
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
	Src2Prim + (* cs_primSinkFlowNew) => Src2Sink
	Src2Prim + (* cs_primSinkFlowStub) => Src2Sink
cs_primLoad:
	cs_primLoad[i] + (flowsTo *) => R5[i]
cs_refPrimFlow:
	cs_refPrimFlow + (Src2Ref *) => Src2Prim
InstFldPt:
	InstFldPt + (Src2Obj *) => Src2Obj
	_InstFldPt + (* Obj2Sink) => Obj2Sink
cs_primRefFlow:
	cs_primRefFlow + (Src2Prim *) => %1
flowsTo:
	_flowsTo + (cs_refStore[i] *) => R1[i]
	_flowsTo + (cs_srcRefFlowNew *) => Src2Obj
	_flowsTo + (cs_srcFlowStub *) => Src2Obj
	_flowsTo + (cs_primStore[i] *) => R4[i]
	flowsTo + (* cs_primLoad[i]) => R5[i]
	flowsTo + (* cs_refSinkFlowNew) => Obj2Sink
	flowsTo + (* cs_sinkFlowStub) => Obj2Sink
	flowsTo + (Src2Obj *) => Src2Ref
	_flowsTo + (%0 *) => Src2Obj
	_flowsTo + (%1 *) => Src2Obj
	_flowsTo + (_R1 *) => InstFldPt
cs_refStore:
	cs_refStore[i] + (* _flowsTo) => R1[i]
Src2Obj:
	Src2Obj + (* R5) => Src2Prim
	Src2Obj + (* flowsTo) => Src2Ref
	Src2Obj + (* InstFldPt) => Src2Obj
	Src2Obj + (* Obj2Sink) => Src2Sink
cs_primSrcFlowStub:
	cs_primSrcFlowStub => Src2Prim
*/

public class D14 extends Graph {

public boolean isTerminal(int kind) {
  switch (kind) {
  case 1:
  case 2:
  case 4:
  case 5:
  case 7:
  case 9:
  case 12:
  case 13:
  case 14:
  case 15:
  case 16:
  case 18:
  case 19:
  case 21:
  case 23:
  case 25:
  case 26:
  case 30:
  case 31:
    return true;
  default:
    return false;
  }
}

public int numKinds() {
  return 32;
}

public int symbolToKind(String symbol) {
  if (symbol.equals("R1")) return 0;
  if (symbol.equals("cs_refStore")) return 1;
  if (symbol.equals("flowsTo")) return 2;
  if (symbol.equals("Src2Obj")) return 3;
  if (symbol.equals("cs_srcRefFlowNew")) return 4;
  if (symbol.equals("cs_srcFlowStub")) return 5;
  if (symbol.equals("R4")) return 6;
  if (symbol.equals("cs_primStore")) return 7;
  if (symbol.equals("R5")) return 8;
  if (symbol.equals("cs_primLoad")) return 9;
  if (symbol.equals("R6")) return 10;
  if (symbol.equals("Src2Prim")) return 11;
  if (symbol.equals("cs_srcPrimFlowNew")) return 12;
  if (symbol.equals("cs_primSrcFlowStub")) return 13;
  if (symbol.equals("cs_primAssign")) return 14;
  if (symbol.equals("cs_primAssignArg")) return 15;
  if (symbol.equals("cs_primAssignRet")) return 16;
  if (symbol.equals("Obj2Sink")) return 17;
  if (symbol.equals("cs_refSinkFlowNew")) return 18;
  if (symbol.equals("cs_sinkFlowStub")) return 19;
  if (symbol.equals("Src2Ref")) return 20;
  if (symbol.equals("cs_refRefFlow")) return 21;
  if (symbol.equals("%0")) return 22;
  if (symbol.equals("cs_primRefFlow")) return 23;
  if (symbol.equals("%1")) return 24;
  if (symbol.equals("cs_primPrimFlow")) return 25;
  if (symbol.equals("cs_refPrimFlow")) return 26;
  if (symbol.equals("InstFldPt")) return 27;
  if (symbol.equals("Src2Sink")) return 28;
  if (symbol.equals("%2")) return 29;
  if (symbol.equals("cs_primSinkFlowNew")) return 30;
  if (symbol.equals("cs_primSinkFlowStub")) return 31;
  throw new RuntimeException("Unknown symbol "+symbol);
}

public String kindToSymbol(int kind) {
  switch (kind) {
  case 0: return "R1";
  case 1: return "cs_refStore";
  case 2: return "flowsTo";
  case 3: return "Src2Obj";
  case 4: return "cs_srcRefFlowNew";
  case 5: return "cs_srcFlowStub";
  case 6: return "R4";
  case 7: return "cs_primStore";
  case 8: return "R5";
  case 9: return "cs_primLoad";
  case 10: return "R6";
  case 11: return "Src2Prim";
  case 12: return "cs_srcPrimFlowNew";
  case 13: return "cs_primSrcFlowStub";
  case 14: return "cs_primAssign";
  case 15: return "cs_primAssignArg";
  case 16: return "cs_primAssignRet";
  case 17: return "Obj2Sink";
  case 18: return "cs_refSinkFlowNew";
  case 19: return "cs_sinkFlowStub";
  case 20: return "Src2Ref";
  case 21: return "cs_refRefFlow";
  case 22: return "%0";
  case 23: return "cs_primRefFlow";
  case 24: return "%1";
  case 25: return "cs_primPrimFlow";
  case 26: return "cs_refPrimFlow";
  case 27: return "InstFldPt";
  case 28: return "Src2Sink";
  case 29: return "%2";
  case 30: return "cs_primSinkFlowNew";
  case 31: return "cs_primSinkFlowStub";
  default: throw new RuntimeException("Unknown kind "+kind);
  }
}

public void process(Edge base) {
  switch (base.kind) {
  case 0: /* R1 */
    /* _R1 + (* _flowsTo) => InstFldPt */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(base.to, other.from, 27, base, other, false);
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
    /* _flowsTo + (cs_srcRefFlowNew *) => Src2Obj */
    for(Edge other : base.to.getInEdges(4)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (cs_srcFlowStub *) => Src2Obj */
    for(Edge other : base.to.getInEdges(5)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (cs_primStore[i] *) => R4[i] */
    for(Edge other : base.to.getInEdges(7)){
      addEdge(other.from, base.from, 6, base, other, true);
    }
    /* flowsTo + (* cs_primLoad[i]) => R5[i] */
    for(Edge other : base.to.getOutEdges(9)){
      addEdge(base.from, other.to, 8, base, other, true);
    }
    /* flowsTo + (* cs_refSinkFlowNew) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(18)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* flowsTo + (* cs_sinkFlowStub) => Obj2Sink */
    for(Edge other : base.to.getOutEdges(19)){
      addEdge(base.from, other.to, 17, base, other, false);
    }
    /* flowsTo + (Src2Obj *) => Src2Ref */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 20, base, other, false);
    }
    /* _flowsTo + (%0 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(22)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (%1 *) => Src2Obj */
    for(Edge other : base.to.getInEdges(24)){
      addEdge(other.from, base.from, 3, base, other, false);
    }
    /* _flowsTo + (_R1 *) => InstFldPt */
    for(Edge other : base.to.getOutEdges(0)){
      addEdge(other.to, base.from, 27, base, other, false);
    }
    break;
  case 3: /* Src2Obj */
    /* Src2Obj + (* R5) => Src2Prim */
    for(Edge other : base.to.getOutEdges(8)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Obj + (* flowsTo) => Src2Ref */
    for(Edge other : base.to.getOutEdges(2)){
      addEdge(base.from, other.to, 20, base, other, false);
    }
    /* Src2Obj + (* InstFldPt) => Src2Obj */
    for(Edge other : base.to.getOutEdges(27)){
      addEdge(base.from, other.to, 3, base, other, false);
    }
    /* Src2Obj + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 28, base, other, false);
    }
    break;
  case 4: /* cs_srcRefFlowNew */
    /* cs_srcRefFlowNew + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 5: /* cs_srcFlowStub */
    /* cs_srcFlowStub + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 6: /* R4 */
    /* R4[i] + (* R5[i]) => R6 */
    for(Edge other : base.to.getOutEdges(8)){
      addEdge(base.from, other.to, 10, base, other, false);
    }
    /* R4 + (Src2Prim *) => %2 */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 29, base, other, false);
    }
    break;
  case 7: /* cs_primStore */
    /* cs_primStore[i] + (* _flowsTo) => R4[i] */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 6, base, other, true);
    }
    break;
  case 8: /* R5 */
    /* R5[i] + (R4[i] *) => R6 */
    for(Edge other : base.from.getInEdges(6)){
      addEdge(other.from, base.to, 10, base, other, false);
    }
    /* R5 + (Src2Obj *) => Src2Prim */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 9: /* cs_primLoad */
    /* cs_primLoad[i] + (flowsTo *) => R5[i] */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 8, base, other, true);
    }
    break;
  case 10: /* R6 */
    /* R6 + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 11: /* Src2Prim */
    /* Src2Prim + (* cs_primAssign) => Src2Prim */
    for(Edge other : base.to.getOutEdges(14)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignArg) => Src2Prim */
    for(Edge other : base.to.getOutEdges(15)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Prim + (* cs_primAssignRet) => Src2Prim */
    for(Edge other : base.to.getOutEdges(16)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Prim + (* R6) => Src2Prim */
    for(Edge other : base.to.getOutEdges(10)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Prim + (* cs_primRefFlow) => %1 */
    for(Edge other : base.to.getOutEdges(23)){
      addEdge(base.from, other.to, 24, base, other, false);
    }
    /* Src2Prim + (* cs_primPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(25)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    /* Src2Prim + (* R4) => %2 */
    for(Edge other : base.to.getOutEdges(6)){
      addEdge(base.from, other.to, 29, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlowNew) => Src2Sink */
    for(Edge other : base.to.getOutEdges(30)){
      addEdge(base.from, other.to, 28, base, other, false);
    }
    /* Src2Prim + (* cs_primSinkFlowStub) => Src2Sink */
    for(Edge other : base.to.getOutEdges(31)){
      addEdge(base.from, other.to, 28, base, other, false);
    }
    break;
  case 12: /* cs_srcPrimFlowNew */
    /* cs_srcPrimFlowNew => Src2Prim */
    addEdge(base.from, base.to, 11, base, false);
    break;
  case 13: /* cs_primSrcFlowStub */
    /* cs_primSrcFlowStub => Src2Prim */
    addEdge(base.from, base.to, 11, base, false);
    break;
  case 14: /* cs_primAssign */
    /* cs_primAssign + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 15: /* cs_primAssignArg */
    /* cs_primAssignArg + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 16: /* cs_primAssignRet */
    /* cs_primAssignRet + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 17: /* Obj2Sink */
    /* Obj2Sink + (_InstFldPt *) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(27)){
      addEdge(other.to, base.to, 17, base, other, false);
    }
    /* Obj2Sink + (%2 *) => Src2Sink */
    for(Edge other : base.from.getInEdges(29)){
      addEdge(other.from, base.to, 28, base, other, false);
    }
    /* Obj2Sink + (Src2Obj *) => Src2Sink */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 28, base, other, false);
    }
    break;
  case 18: /* cs_refSinkFlowNew */
    /* cs_refSinkFlowNew + (flowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 19: /* cs_sinkFlowStub */
    /* cs_sinkFlowStub + (flowsTo *) => Obj2Sink */
    for(Edge other : base.from.getInEdges(2)){
      addEdge(other.from, base.to, 17, base, other, false);
    }
    break;
  case 20: /* Src2Ref */
    /* Src2Ref + (* cs_refRefFlow) => %0 */
    for(Edge other : base.to.getOutEdges(21)){
      addEdge(base.from, other.to, 22, base, other, false);
    }
    /* Src2Ref + (* cs_refPrimFlow) => Src2Prim */
    for(Edge other : base.to.getOutEdges(26)){
      addEdge(base.from, other.to, 11, base, other, false);
    }
    break;
  case 21: /* cs_refRefFlow */
    /* cs_refRefFlow + (Src2Ref *) => %0 */
    for(Edge other : base.from.getInEdges(20)){
      addEdge(other.from, base.to, 22, base, other, false);
    }
    break;
  case 22: /* %0 */
    /* %0 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 23: /* cs_primRefFlow */
    /* cs_primRefFlow + (Src2Prim *) => %1 */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 24, base, other, false);
    }
    break;
  case 24: /* %1 */
    /* %1 + (* _flowsTo) => Src2Obj */
    for(Edge other : base.to.getInEdges(2)){
      addEdge(base.from, other.from, 3, base, other, false);
    }
    break;
  case 25: /* cs_primPrimFlow */
    /* cs_primPrimFlow + (Src2Prim *) => Src2Prim */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 26: /* cs_refPrimFlow */
    /* cs_refPrimFlow + (Src2Ref *) => Src2Prim */
    for(Edge other : base.from.getInEdges(20)){
      addEdge(other.from, base.to, 11, base, other, false);
    }
    break;
  case 27: /* InstFldPt */
    /* InstFldPt + (Src2Obj *) => Src2Obj */
    for(Edge other : base.from.getInEdges(3)){
      addEdge(other.from, base.to, 3, base, other, false);
    }
    /* _InstFldPt + (* Obj2Sink) => Obj2Sink */
    for(Edge other : base.from.getOutEdges(17)){
      addEdge(base.to, other.to, 17, base, other, false);
    }
    break;
  case 29: /* %2 */
    /* %2 + (* Obj2Sink) => Src2Sink */
    for(Edge other : base.to.getOutEdges(17)){
      addEdge(base.from, other.to, 28, base, other, false);
    }
    break;
  case 30: /* cs_primSinkFlowNew */
    /* cs_primSinkFlowNew + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 28, base, other, false);
    }
    break;
  case 31: /* cs_primSinkFlowStub */
    /* cs_primSinkFlowStub + (Src2Prim *) => Src2Sink */
    for(Edge other : base.from.getInEdges(11)){
      addEdge(other.from, base.to, 28, base, other, false);
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
  case 4:
    return (short)1;
  case 5:
    return (short)3;
  case 12:
    return (short)1;
  case 13:
    return (short)3;
  case 18:
    return (short)1;
  case 19:
    return (short)6;
  case 30:
    return (short)1;
  case 31:
    return (short)6;
  default:
    return (short)0;
  }
}

public boolean useReps() { return false; }

}