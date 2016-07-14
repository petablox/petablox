package stamp.analyses.inferaliasmodel;

import soot.Body;
import soot.Unit;
import soot.Local;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.NewExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

import chord.util.tuple.object.Pair;

import java.util.*;

public class MatchAllocToInitAnalysis
{
	private Map<Stmt,Set<Stmt>> newStmtToInvokeInitStmts = new HashMap();

	public MatchAllocToInitAnalysis(Body body)
	{
		SimpleLocalDefs ld = new SimpleLocalDefs(new ExceptionalUnitGraph(body));
		for(Unit unit : body.getUnits()){
			Stmt initInvokeStmt = (Stmt) unit;
			if(!initInvokeStmt.containsInvokeExpr())
				continue;
			InvokeExpr ie = initInvokeStmt.getInvokeExpr();
			if(!ie.getMethod().getName().equals("<init>"))
				continue;
			Local rcvr = (Local) ((InstanceInvokeExpr) ie).getBase();
			
			List<Pair<Local,Stmt>> workList = new LinkedList();
			workList.add(new Pair(rcvr, initInvokeStmt));
			Set<Pair<Local,Stmt>> visited = new HashSet();
			while(!workList.isEmpty()){
				Pair<Local,Stmt> p = workList.remove(0);
				if(visited.contains(p))
					continue;
				visited.add(p);

				Local local = p.val0;
				Stmt useStmt = p.val1;

				//System.out.println("Processing local:"+ local + " useStmt:"+useStmt);
				
				for(Unit stmt : ld.getDefsOfAt(local, useStmt)){
					if(stmt instanceof DefinitionStmt){
						DefinitionStmt ds = (DefinitionStmt) stmt;
						Value leftOp = ds.getLeftOp();
						Value rightOp = ds.getRightOp();
						assert local.equals(leftOp);
						if(rightOp instanceof NewExpr){
							Set<Stmt> invokeInitStmts = newStmtToInvokeInitStmts.get(ds);
							if(invokeInitStmts == null)
								newStmtToInvokeInitStmts.put(ds, (invokeInitStmts = new HashSet()));
							invokeInitStmts.add(initInvokeStmt);
						} else if(rightOp instanceof Local){
							workList.add(new Pair((Local) rightOp, ds));
						}
					}				
				}
			}
		}
	}
	
	public Set<Stmt> invokeInitStmtsFor(Stmt newStmt)
	{
		return newStmtToInvokeInitStmts.get(newStmt);
	}
}