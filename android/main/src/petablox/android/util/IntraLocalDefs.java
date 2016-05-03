package petablox.android.util;

import java.util.ArrayList;
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
import soot.jimple.Stmt;
import soot.jimple.IdentityRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocal;
import soot.options.Options;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.LocalDefs;
import soot.util.Cons;


/**
 *  Slightly modify the original implementation of the SmartLocalDefs 
 *  in soot to support transitive closure.
 */
public class IntraLocalDefs implements LocalDefs
{
    private final Map<Cons, ArrayList<Unit>> answer;
    
    /** I use this map to keep track of the point-to value of a local var.*/
    private Map<Value, Set<Value>> actualValue;

    private final Map<Local, HashSet<Unit>> localToDefs; // for each local, set of units
                                   // where it's defined
    private final UnitGraph graph;
    private final LocalDefsAnalysis analysis;
    private final Map<Unit, HashSet> unitToMask;
    public IntraLocalDefs(UnitGraph g, LiveLocals live) {
        this.graph = g;

        if(Options.v().time())
            Timers.v().defsTimer.start();
        
        if(Options.v().verbose())
            G.v().out.println("[" + g.getBody().getMethod().getName() +
                               "]     Constructing IntraLocalDefs...");

        localToDefs = new HashMap<Local, HashSet<Unit>>();
        unitToMask = new HashMap<Unit, HashSet>();
        for( Iterator uIt = g.iterator(); uIt.hasNext(); ) {
            final Unit u = (Unit) uIt.next();
            Local l = localDef(u);
            if( l == null ) continue;
            HashSet<Unit> s = defsOf(l);
            s.add(u);
        }

        if(Options.v().verbose())
            G.v().out.println("[" + g.getBody().getMethod().getName() +
                               "]        done localToDefs map..." );

        for( Iterator uIt = g.iterator(); uIt.hasNext(); ) {
            final Unit u = (Unit) uIt.next();
            unitToMask.put(u, new HashSet(live.getLiveLocalsAfter(u)));
        }

        if(Options.v().verbose())
            G.v().out.println("[" + g.getBody().getMethod().getName() +
                               "]        done unitToMask map..." );

        analysis = new LocalDefsAnalysis(graph);

        answer = new HashMap<Cons, ArrayList<Unit>>();
        actualValue = new HashMap<Value, Set<Value>>();

        for( Iterator uIt = graph.iterator(); uIt.hasNext(); ) {
            final Unit u = (Unit) uIt.next();
            for( Iterator vbIt = u.getUseBoxes().iterator(); vbIt.hasNext(); ) {
                final ValueBox vb = (ValueBox) vbIt.next();
                Value v = vb.getValue();
                if( !(v instanceof Local) ) continue;
                HashSet analysisResult = (HashSet) analysis.getFlowBefore(u);
                ArrayList<Unit> al = new ArrayList<Unit>();
                for (Unit unit : defsOf((Local)v)) {
                	//JIdentityStmt, JAssignStmt and what else?
                    if(analysisResult.contains(unit)) {
                    	if (unit instanceof JAssignStmt) {
                        	Set<Value> valueList = new HashSet<Value>();
                        	Value rhs = ((JAssignStmt)unit).getRightOp();
                        	Value lhs = ((JAssignStmt)unit).getLeftOp();

                    		if (rhs instanceof JimpleLocal) {//this value can't not be used by reaching def. compute the transitive.
	                			Set<Value> ptList = actualValue.get(rhs);
								if (ptList == null) continue;
								
								//I need to create some new stmt for transitive closure.
                    			if (actualValue.get(lhs) != null ) valueList = actualValue.get(lhs);
								
                    			for(Value reg:ptList) {
									if (reg == null) continue;
									
                    				Stmt newStmt;
                    				if (reg instanceof IdentityRef) {
                    					newStmt = new JIdentityStmt(lhs, reg);
                    				} else{
                        				newStmt = new JAssignStmt(lhs, reg);
                    				}
                                	al.add(newStmt);
                                	//also add new point to info to the map.
                                	valueList.add(reg);        	
                    			}
                    			
                    		} else {//add to map directly.
                    			if (actualValue.get(lhs) != null) //old?
                    				valueList = actualValue.get(lhs);
                    			
                				valueList.add(rhs);
                        		al.add(unit);
                    		}
            				actualValue.put(lhs, valueList);
                    	} else if (unit instanceof JIdentityStmt) {//add to map directly.
                    		JIdentityStmt idStmt = (JIdentityStmt)unit;
                        	Set<Value> valueList = new HashSet<Value>();
                        	//old?
                        	if (actualValue.get(idStmt.getLeftOp()) != null) 
                        		valueList = actualValue.get(idStmt.getLeftOp());
                        	
                        	valueList.add(idStmt.getRightOp());
                        	actualValue.put(idStmt.getLeftOp(), valueList);
                    		al.add(unit);
                           
                    		
                    	} else {
                    		System.out.println("Can not figure out this value==> " + unit);
                        	al.add(unit);

                    	}
                    }
                }
                answer.put(new Cons(u, v), al);
            }
        }

        if(Options.v().time())
            Timers.v().defsTimer.end();

	if(Options.v().verbose())
	    G.v().out.println("[" + g.getBody().getMethod().getName() +
                               "]     IntraLocalDefs finished.");
    }
    private Local localDef(Unit u) {
        List defBoxes = u.getDefBoxes();
		int size = defBoxes.size();
        if( size == 0 ) return null;
        if( size != 1 ) throw new RuntimeException();
        ValueBox vb = (ValueBox) defBoxes.get(0);
        Value v = vb.getValue();
        if( !(v instanceof Local) ) return null;
        return (Local) v;
    }
    private HashSet<Unit> defsOf( Local l ) {
        HashSet<Unit> ret = localToDefs.get(l);
        if( ret == null ) localToDefs.put( l, ret = new HashSet<Unit>() );
        return ret;
    }

    class LocalDefsAnalysis extends ForwardFlowAnalysis {
        LocalDefsAnalysis(UnitGraph g) {
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
			if (l == null)
			{//add all units contained in mask
	            for( Iterator inUIt = in.iterator(); inUIt.hasNext(); ) {
	                final Unit inU = (Unit) inUIt.next();
	                if( mask.contains(localDef(inU)) )
					{
						out.add(inU);
					}
	            }
			}
			else
			{//check unit whether contained in allDefUnits before add into out set.
				allDefUnits = defsOf(l);
				
	            for( Iterator inUIt = in.iterator(); inUIt.hasNext(); ) {
	                final Unit inU = (Unit) inUIt.next();
    	            if( mask.contains(localDef(inU)) )
					{//only add unit not contained in allDefUnits
						if ( allDefUnits.contains(inU)){
							out.remove(inU);
						} else {
							out.add(inU);
						}
					}
    	        }
   	            out.removeAll(allDefUnits);
   	            if(mask.contains(l)) out.add(u);
			}
        }

    
        protected void copy(Object source, Object dest) {
            HashSet sourceSet = (HashSet) source;
            HashSet<Object> destSet   = (HashSet<Object>) dest;
              
			//retain all the elements contained by sourceSet
			if (destSet.size() > 0)
				destSet.retainAll(sourceSet);
			
			//add the elements not contained by destSet
			if (sourceSet.size() > 0)
			{
				for( Iterator its = sourceSet.iterator(); its.hasNext(); ) {
					Object o = its.next();
					if (!destSet.contains(o))
					{//need add this element.
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

    public List<Unit> getDefsOfAt(Local l, Unit s)
    {
    	//adding support for transitive close, e.g, x = 1; y=x; z=y; 	
        return answer.get(new Cons(s, l));
    }

}

