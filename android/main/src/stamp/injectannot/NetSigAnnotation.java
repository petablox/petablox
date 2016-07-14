package stamp.injectannot;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import java.util.*;

import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class NetSigAnnotation extends Visitor
{
	private SimpleLocalDefs sld;
	
	protected void visit(SootClass klass)
    {
		super.visit(klass);
		Collection<SootMethod> methodsCopy = new ArrayList(klass.getMethods());
		for(SootMethod method : methodsCopy)
			visit(method);
	}
	
	String getDef(Value i, Stmt u)
	{
		if(i instanceof StringConstant)
			return String.format("[%s]", ((StringConstant) i).value);
		
		if(i instanceof Constant)
			return "[]";

		if(sld == null)
			sld = new SimpleLocalDefs(new ExceptionalUnitGraph(body));
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Unit def : sld.getDefsOfAt((Local) i, u)){
			if(!(def instanceof AssignStmt))
				continue;
			Value rhs = ((AssignStmt) def).getRightOp();
			if(!(rhs instanceof StringConstant))
				continue;
			if(first)
				first = false;
			else
				sb.append(",");
			sb.append(String.format("\"%s\"", ((StringConstant) rhs).value));
		}
		return String.format("[%s]", sb.toString());
    }
	
	protected void visit(SootMethod method)
    {
		sld = null;
		if(!method.isConcrete())
			return;
		super.visit(method);

		Iterator<Unit> uit = units.snapshotIterator();
		while(uit.hasNext()){
			Stmt stmt = (Stmt) uit.next();
	    
			//invocation statements
			if(!stmt.containsInvokeExpr())
				continue;

			InvokeExpr ie = stmt.getInvokeExpr();
			SootMethod meth = ie.getMethod();
			if(!meth.getSignature().equals("<org.apache.http.message.BasicNameValuePair: void <init>(java.lang.String,java.lang.String)>"))
				continue;
			
			String label = getDef(ie.getArg(0), stmt) + " -> "+getDef(ie.getArg(1), stmt);
			
			Local base = (Local) ((InstanceInvokeExpr) ie).getBase();
			insertLabelIfNecessary(base, stmt, label, true, false, true);
		}
	}
}