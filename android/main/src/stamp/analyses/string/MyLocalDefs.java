package stamp.analyses.string;

import soot.Unit;
import soot.Type;
import soot.RefType;
import soot.Value;
import soot.Local;
import soot.Body;
import soot.jimple.Stmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InstanceInvokeExpr;

import java.util.*;

public class MyLocalDefs
{
	Map<Local,List<Stmt>> localToDefs = new HashMap();

	private Type stringType = RefType.v("java.lang.String");
	private Type stringBuilderType = RefType.v("java.lang.StringBuilder");
	private Type stringBufferType = RefType.v("java.lang.StringBuffer");

	public MyLocalDefs(Body body)
	{
		for(Unit unit : body.getUnits()){
			Stmt stmt = (Stmt) unit;
			if(unit instanceof DefinitionStmt){
				DefinitionStmt ds = (DefinitionStmt) unit;
				Value leftOp = ds.getLeftOp();
				
				if(leftOp instanceof Local &&
				   isStringType(leftOp.getType())){
					Local local = (Local) leftOp;
					addDef(local, stmt);
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
				addDef(base, stmt);
			}
		}
	}

	private void addDef(Local local, Stmt def)
	{
		List<Stmt> defs = localToDefs.get(local);
		if(defs == null){
			defs = new ArrayList();
			localToDefs.put(local, defs);
		}
		defs.add(def);
	}

	private boolean isStringType(Type type)
	{
		return
			type.equals(stringType) ||
			type.equals(stringBuilderType) ||
			type.equals(stringBufferType);
	}

	public List<Stmt> getDefsOf(Local l)
	{
		return localToDefs.get(l);
	}
}