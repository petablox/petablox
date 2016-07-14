package stamp.analyses.ondemand;

import soot.SootMethod;
import soot.jimple.Stmt;
import chord.util.tuple.object.Pair;
import java.util.*;


public class CallStack implements Iterable<Pair<Stmt,SootMethod>>
{
	protected List<Pair<Stmt,SootMethod>> callStack;

	public CallStack(List<Pair<Stmt,SootMethod>> callStack)
	{
		this.callStack = callStack;
	}
	
	public CallStack()
	{
		this(new ArrayList());
	}
		
	public CallStack append(Stmt callStmt, SootMethod caller)
	{
		List<Pair<Stmt,SootMethod>> callStackCopy = new ArrayList();
		callStackCopy.addAll(this.callStack);
		callStackCopy.add(new Pair(callStmt, caller));
		
		return new CallStack(callStackCopy);
	}
	
	public Pair<Stmt,SootMethod> elemAt(int i)
	{
		return callStack.get(i);
	}
	
	public Iterator<Pair<Stmt,SootMethod>> iterator() 
	{
		return callStack.iterator();
	}
	
	public int size()
	{
		return callStack.size();
	}
}