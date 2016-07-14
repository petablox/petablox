package stamp.analyses.string;

import soot.Local;
import soot.Immediate;
import soot.jimple.StringConstant;

import java.util.*;

public class Evaluator
{
	private static final String UNKNOWN = "$stamp$UNKNOWN$stamp$";
	private static final String UNKNOWN_UNKNOWN = UNKNOWN.concat(UNKNOWN);

	private final Map<Local,Map<Statement,Set<String>>> localToVals = new HashMap();
	private final Slicer slicer;
	private final StatementGraph sGraph;

	public Evaluator(Slicer slicer, StatementGraph sGraph)
	{
		this.slicer = slicer;
		this.sGraph = sGraph;
	}

	public Set<String> evaluate()
	{
		boolean changed = true;
		while(changed){
			changed = false;

			for(Statement stmt : sGraph.stmts()){
				//System.out.println("Evaluating "+slicer.stmtStr(stmt));
				if(stmt instanceof ToLower){
					changed |= toLower((ToLower) stmt);
				} else if(stmt instanceof Assign){
					changed |= assign((Assign) stmt);
				} else if(stmt instanceof Havoc){
					changed |= havoc((Havoc) stmt);
				} else if(stmt instanceof Concat){
					changed |= concat((Concat) stmt);
				}
			} 
			/*
			Set<String> newVals = localToVals.get(left).get(stmt);
			if(newVals == null)
				System.out.println("{  }");
			else {
				System.out.print("{ ");
				for(String val : newVals)
					System.out.print(val+" ");
				System.out.println(" }");
				}*/
		}

		for(Map.Entry<Local,Map<Statement,Set<String>>> e : localToVals.entrySet()){
			Local l = e.getKey();
			//System.out.println("Local: "+l);
			for(Map.Entry<Statement,Set<String>> f : e.getValue().entrySet()){
				System.out.println("stmt: "+slicer.stmtStr(f.getKey()));
				System.out.print("values: ");
				for(String s : f.getValue())
					System.out.print(s+" ");
				System.out.println("");
			}
		}

		for(Statement stmt : sGraph.stmts()){
			if(stmt instanceof Criterion){
				return getVals(((Criterion) stmt).local, stmt);
			}
		}

		throw new RuntimeException("unreachable");
	}

	private boolean havoc(Havoc stmt)
	{
		Local left = stmt.local;
		Set<String> newVals = new HashSet();
		newVals.add(UNKNOWN);
		return addVals(left, newVals, stmt);
	}

	private boolean assign(Assign stmt)
	{
		Immediate right = stmt.right;
		Local left = ((Assign) stmt).left;
		return addVals(left, getVals(right, stmt), stmt);
	}

	private boolean toLower(ToLower stmt)
	{
		Immediate right = ((ToLower) stmt).right;
		Local left = ((Assign) stmt).left;
		Set<String> newVals = new HashSet();
		for(String v : getVals(right, stmt)){
			newVals.add(v.toLowerCase());
		}		
		return addVals(left, newVals, stmt);
	}

	private boolean concat(Concat stmt)
	{
		Immediate right1 = stmt.right1;
		Immediate right2 = stmt.right2;
		Local left = stmt.left;
		
		Set<String> vals1 = getVals(right1, stmt);
		Set<String> vals2 = getVals(right2, stmt);
				
		Set<String> newVals = new HashSet();
		for(String v1 : vals1){
			for(String v2 : vals2){
				String newV;
				newV = v1.concat(v2);
				newV = newV.replace(UNKNOWN_UNKNOWN, UNKNOWN);
				//if(newV.length() < 50)
					newVals.add(newV);
			}
		}
		return addVals(left, newVals, stmt);
	}

	private Set<String> getVals(Immediate i, Statement stmt)
	{
		Set<String> ret = new HashSet();
		if(i instanceof StringConstant){
			ret.add(((StringConstant) i).value);
		} else {
			for(Statement defStmt : sGraph.predsOf(stmt)){
				Map<Statement,Set<String>> vs = localToVals.get((Local) i);
				if(vs == null)
					continue;
				Set<String> vals = vs.get(defStmt);
				if(vals == null)
					continue;
				ret.addAll(vals);
			}		
		}
		return ret;
	}

	private boolean addVals(Local local, Set<String> newVals, Statement s)
	{
		if(newVals == null)
			return false;
		Map<Statement,Set<String>> vs = localToVals.get(local);
		if(vs == null){
			vs = new HashMap();
			localToVals.put(local, vs);
		}
		Set<String> vals = vs.get(s);
		if(vals == null){
			vals = new HashSet();
			vs.put(s, vals);
		}
		if(vals.size() > 10){
			System.out.println("Warning: soundness is sacrificed for usefulness.");
			return false;
		}
		return vals.addAll(newVals);
	}
}