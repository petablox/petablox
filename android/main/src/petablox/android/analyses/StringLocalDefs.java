package petablox.android.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.G;
import soot.Local;
import soot.Timers;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.LiveLocals;
import soot.util.Cons;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.BasicAutomata;

public class StringLocalDefs {
	private final Map<Unit, Map<Value, Set<String>>> answer;


	/** I use this map to keep track of the point-to value of a local var. */
	private Map<Value, Automaton> actualValue;

	private final Map<Local, HashSet<Unit>> localToDefs; // for each local, set
															// of units
	// where it's defined
	private final UnitGraph graph;
	private final StringAnalysis analysis;
	private final Map<Unit, HashSet> unitToMask;

	public StringLocalDefs(UnitGraph g, LiveLocals live) {
		this.graph = g;

		if (Options.v().time())
			Timers.v().defsTimer.start();

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName()
					+ "]     Constructing IntraLocalDefs...");

		localToDefs = new HashMap<Local, HashSet<Unit>>();
		unitToMask = new HashMap<Unit, HashSet>();
		for (Iterator uIt = g.iterator(); uIt.hasNext();) {
			final Unit u = (Unit) uIt.next();
			Local l = localDef(u);
			if (l == null)
				continue;
			HashSet<Unit> s = defsOf(l);
			s.add(u);
		}

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName()
					+ "]        done localToDefs map...");

		for (Iterator uIt = g.iterator(); uIt.hasNext();) {
			final Unit u = (Unit) uIt.next();
			unitToMask.put(u, new HashSet(live.getLiveLocalsAfter(u)));
		}

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName()
					+ "]        done unitToMask map...");

		analysis = new StringAnalysis(graph);

		answer = new HashMap<Unit, Map<Value, Set<String>>>();
		actualValue = new HashMap<Value, Automaton>();

		for (Iterator uIt = graph.iterator(); uIt.hasNext();) {
			final Unit u = (Unit) uIt.next();
			Map<Value, Set<String>> al = new HashMap<Value, Set<String>>();
			for (Iterator vbIt = u.getUseBoxes().iterator(); vbIt.hasNext();) {
				//Automaton valueList = BasicAutomata.makeAnyString();
				Automaton valueList = BasicAutomata.makeString("*");

				final ValueBox vb = (ValueBox) vbIt.next();
				Value v = vb.getValue();
				
				if (!(v instanceof Local))
					continue;
				HashSet analysisResult = (HashSet) analysis.getFlowBefore(u);
				//System.out.println("check unit...." + u);
				//System.out.println("check defs...." + defsOf((Local) v));
				for (Unit unit : defsOf((Local) v)) {
					// JIdentityStmt, JAssignStmt and what else?
					if (analysisResult.contains(unit)) {
						if (unit instanceof JAssignStmt) {
							//valueList = BasicAutomata.makeAnyString();
							valueList = BasicAutomata.makeString("*");
							Value rhs = ((JAssignStmt) unit).getRightOp();
							Value lhs = ((JAssignStmt) unit).getLeftOp();

							if (rhs instanceof JimpleLocal) {
								// this value can't not be used by reaching def.
								// compute the transitive.
								Automaton ptList = actualValue.get(rhs);
								if (ptList == null) continue;

								valueList = ptList;
								al.put(lhs, valueList.getFiniteStrings());
								
							} else {// add to map directly.
								/*if (actualValue.get(lhs) != null) // old?
									valueList = valueList.union(actualValue
											.get(lhs));*/

								if (rhs instanceof StringConstant) {
									valueList = BasicAutomata
											.makeString(((StringConstant) rhs).value);
								} else if (rhs instanceof JVirtualInvokeExpr) {
									JVirtualInvokeExpr expr = (JVirtualInvokeExpr) rhs;
                                    String exprStr = expr.toString();
									if (exprStr.contains("java.lang.StringBuilder append(") ||
                                        exprStr.contains("java.lang.StringBuffer append(") ||
                                        exprStr.contains("java.lang.String concat(") ) {

										Value argStr = expr.getArg(0);
										ValueBox caller = (ValueBox)expr.getUseBoxes().get(0);
										Automaton fstAuto = actualValue.get(caller.getValue());
										Automaton secAuto = actualValue.get(argStr);
										if (caller instanceof StringConstant)
											fstAuto = BasicAutomata.makeString(((StringConstant) caller).value);
										if (argStr instanceof StringConstant)
											secAuto = BasicAutomata.makeString(((StringConstant) argStr).value);
									
                                        if (fstAuto != null && secAuto != null)
										    valueList = fstAuto.concatenate(secAuto);
                                        else if(fstAuto == null) 
										    valueList = secAuto;
                                        else if(secAuto == null) 
										    valueList = fstAuto;

									} 

								} else if (rhs instanceof JStaticInvokeExpr) {
									JStaticInvokeExpr expr = (JStaticInvokeExpr) rhs;
									//staticinvoke <java.lang.String: java.lang.String valueOf(java.lang.Object)>(r1)
									if (expr.toString().contains(
											"java.lang.String valueOf(")) {
										valueList = actualValue.get(expr.getArg(0));
									} 

								} else if (rhs instanceof JNewExpr) {
									JNewExpr expr = (JNewExpr) rhs;
                                    String exprStr = expr.toString();
									if ( exprStr.contains("new java.lang.StringBuilder") ||
									    exprStr.contains("new java.lang.StringBuffer") ) {
                                        if (actualValue.get(lhs)!=null)
										    valueList = actualValue.get(lhs);
									}

								} else {									
									valueList = BasicAutomata.makeString("*");
								}
								al.put(lhs, valueList.getFiniteStrings());
							}
							actualValue.put(lhs, valueList);
						} else if (unit instanceof JIdentityStmt) {// add to map
																	// directly.
							JIdentityStmt idStmt = (JIdentityStmt) unit;
							//valueList = BasicAutomata.makeAnyString();
							valueList = BasicAutomata.makeString("*");
							// old?
							if (actualValue.get(idStmt.getLeftOp()) != null)
								valueList = actualValue.get(idStmt.getLeftOp());

							// FIXME
							/*
							 * valueList.add(idStmt.getRightOp());
							 * actualValue.put(idStmt.getLeftOp(), valueList);
							 * al.add(unit);
							 */

						} else {
							System.out.println("unknown expr.......");
							// al.add(unit);
						}
					}
				}
				if (u instanceof JInvokeStmt) {
					JInvokeStmt specInvoke = (JInvokeStmt) u;
                    String invokeStr = specInvoke.toString();
					if (invokeStr.contains("<java.lang.StringBuilder: void <init>(java.lang.String") ||
					    invokeStr.contains("<java.lang.StringBuffer: void <init>(java.lang.String") ) {

						ValueBox vbox = (ValueBox) u.getUseBoxes().get(0);
						Value rhs_sp = specInvoke.getInvokeExpr().getArg(0);
						if (actualValue.get(rhs_sp) != null) {
							valueList = actualValue.get(rhs_sp);

							al.put(vbox.getValue(),
									valueList.getFiniteStrings());
							actualValue.put(vbox.getValue(), valueList);
							answer.put(u, al);
							continue;
						}

					}
				}
				answer.put(u, al);
			}
		}

		if (Options.v().time())
			Timers.v().defsTimer.end();

		if (Options.v().verbose())
			G.v().out.println("[" + g.getBody().getMethod().getName()
					+ "]     IntraLocalDefs finished.");
	}

	private Local localDef(Unit u) {
		List defBoxes = u.getDefBoxes();
		int size = defBoxes.size();
		if (size == 0)
			return null;
		if (size != 1)
			throw new RuntimeException();
		ValueBox vb = (ValueBox) defBoxes.get(0);
		Value v = vb.getValue();
		if (!(v instanceof Local))
			return null;
		return (Local) v;
	}

	private HashSet<Unit> defsOf(Local l) {
		HashSet<Unit> ret = localToDefs.get(l);
		if (ret == null)
			localToDefs.put(l, ret = new HashSet<Unit>());
		return ret;
	}

	class StringAnalysis extends ForwardFlowAnalysis {
		StringAnalysis(UnitGraph g) {
			super(g);
			doAnalysis();
		}

		protected void merge(Object inoutO, Object inO) {
			HashSet inout = (HashSet) inoutO;
			HashSet in = (HashSet) inO;

			inout.addAll(in);
		}

		protected void merge(Object in1, Object in2, Object out) {
			HashSet inSet1 = (HashSet) in1;
			HashSet inSet2 = (HashSet) in2;
			HashSet outSet = (HashSet) out;

			outSet.clear();
			outSet.addAll(inSet1);
			outSet.addAll(inSet2);
		}

		protected void flowThrough(Object inValue, Object unit, Object outValue) {
			Unit u = (Unit) unit;
			HashSet in = (HashSet) inValue;
			HashSet<Unit> out = (HashSet<Unit>) outValue;
			out.clear();
			Set mask = unitToMask.get(u);
			Local l = localDef(u);
			HashSet<Unit> allDefUnits = null;
			if (l == null) {// add all units contained in mask
				for (Iterator inUIt = in.iterator(); inUIt.hasNext();) {
					final Unit inU = (Unit) inUIt.next();
					if (mask.contains(localDef(inU))) {
						out.add(inU);
					}
				}
			} else {// check unit whether contained in allDefUnits before add
					// into out set.
				allDefUnits = defsOf(l);

				for (Iterator inUIt = in.iterator(); inUIt.hasNext();) {
					final Unit inU = (Unit) inUIt.next();
					if (mask.contains(localDef(inU))) {// only add unit not
														// contained in
														// allDefUnits
						if (allDefUnits.contains(inU)) {
							out.remove(inU);
						} else {
							out.add(inU);
						}
					}
				}
				out.removeAll(allDefUnits);
				if (mask.contains(l))
					out.add(u);
			}
		}

		protected void copy(Object source, Object dest) {
			HashSet sourceSet = (HashSet) source;
			HashSet<Object> destSet = (HashSet<Object>) dest;

			// retain all the elements contained by sourceSet
			if (destSet.size() > 0)
				destSet.retainAll(sourceSet);

			// add the elements not contained by destSet
			if (sourceSet.size() > 0) {
				for (Iterator its = sourceSet.iterator(); its.hasNext();) {
					Object o = its.next();
					if (!destSet.contains(o)) {// need add this element.
						destSet.add(o);
					}
				}
			}

		}

		protected Object newInitialFlow() {
			return new HashSet();
		}

		protected Object entryInitialFlow() {
			return new HashSet();
		}
	}

	public Map<Value, Set<String>> getDefsOfAt(Local l, Unit s) {
		// adding support for transitive close, e.g, x = 1; y=x; z=y;
//		return answer.get(new Cons(s, l));
		return answer.get(s);

	}

}
