package stamp.analyses.string;

import soot.Unit;
import soot.Value;
import soot.Local;
import soot.Body;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;

import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.FlowUniverse;
import soot.toolkits.scalar.ArrayFlowUniverse;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.ArrayPackedSet;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.BriefUnitGraph;

import static stamp.analyses.string.Slicer.isStringType;

import java.util.*;

class ReachingDefsAnalysis extends ForwardFlowAnalysis<Unit,FlowSet>
{
    private FlowSet emptySet;
    final private Map<Stmt, FlowSet> stmtToKillSet = new HashMap();
	final private Map<Local,List<Stmt>> localToDefs = new HashMap();

    public ReachingDefsAnalysis(Body body)
    {		
        super(new BriefUnitGraph(body));

		final Map<Stmt,Set<Local>> stmtToDefSet = new HashMap();

		class DefProcessor {
			void addDef(Local local, Stmt def)
			{
				List<Stmt> defs = localToDefs.get(local);
				if(defs == null){
					defs = new ArrayList();
					localToDefs.put(local, defs);
				}
				defs.add(def);
				
				Set<Local> gens = stmtToDefSet.get(def);
				if(gens == null){
					gens = new HashSet();
					stmtToDefSet.put(def, gens);
				}
				gens.add(local);
			}
		}

		DefProcessor dp = new DefProcessor(); 

		for(Unit unit : body.getUnits()){
			Stmt stmt = (Stmt) unit;
			if(unit instanceof DefinitionStmt){
				DefinitionStmt ds = (DefinitionStmt) unit;
				Value leftOp = ds.getLeftOp();
				
				if(leftOp instanceof Local /*&& isStringType(leftOp.getType())*/){
					Local local = (Local) leftOp;
					dp.addDef(local, stmt);
				}
			}
			if(!stmt.containsInvokeExpr())
				continue;
			InvokeExpr ie = stmt.getInvokeExpr();
			String mSig = ie.getMethod().getSignature();
			if(mSig.equals("<java.lang.StringBuilder: void <init>(java.lang.String)>") ||
			   mSig.equals("<java.lang.StringBuffer: void <init>(java.lang.String)>") ||
			   mSig.equals("<java.lang.String: void <init>(java.lang.String)>") ||
			   mSig.equals("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>") ||
			   mSig.equals("<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>") ||
			   mSig.equals("<java.lang.StringBuilder: void <init>()>") ||
			   mSig.equals("<java.lang.StringBuffer: void <init>()>") ||
			   mSig.equals("<java.lang.String: void <init>()>")){
				Local base = (Local) ((InstanceInvokeExpr) ie).getBase();
				dp.addDef(base, stmt);
			}
		}

		
		FlowUniverse defUniverse = new ArrayFlowUniverse(stmtToDefSet.keySet().toArray(new Stmt[0]));	
        emptySet = new ArrayPackedSet(defUniverse);	

		for(Map.Entry<Stmt,Set<Local>> e : stmtToDefSet.entrySet()){
			Stmt stmt = e.getKey();
			Set<Local> defLocals = e.getValue();
			FlowSet killSet = new ArrayPackedSet(defUniverse);
			stmtToKillSet.put(stmt, killSet);
			for(Local l : defLocals){
				List<Stmt> lDefs = localToDefs.get(l);
				for(Stmt lDef : lDefs){
					killSet.add(lDef);
				}
			}
		}
		
		doAnalysis();
		
		System.out.println("Results of reaching definition analysis:");
		for(Map.Entry<Unit,FlowSet> e : unitToBeforeFlow.entrySet()){
			Unit s = e.getKey();
			FlowSet fs = e.getValue();
			System.out.println("Before stmt: "+s);
			for(Object o : fs.toList())
				System.out.println("+ "+o);
		}
		
	}

	public Iterable<Stmt> getDefsOf(Local local, Stmt stmt)
	{
		FlowSet fs = unitToBeforeFlow.get(stmt);
		List<Stmt> defs = new ArrayList();
		for(Stmt defStmt : localToDefs.get(local)){
			if(fs.contains(defStmt))
				defs.add(defStmt);
		}
		return defs;
	}
    
    protected FlowSet newInitialFlow()
    {
        return emptySet.clone();
    }

    protected FlowSet entryInitialFlow()
    {
        return emptySet.clone();
    }

    protected void flowThrough(FlowSet in, Unit stmt, FlowSet out)
    {
		if(out.size() > 10){
			return;
		}
			
		FlowSet killSet = stmtToKillSet.get(stmt);
		if(killSet != null){
			in.difference(killSet, out);
			out.add(stmt);
		} else
			in.copy(out);
    }

    protected void copy(FlowSet sourceSet, FlowSet destSet)
    {
        sourceSet.copy(destSet);
    }

    protected void merge(FlowSet inSet1, FlowSet inSet2, FlowSet outSet)
    {
        inSet1.union(inSet2, outSet);
    }
}
