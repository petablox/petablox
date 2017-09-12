package petablox.analyses.argret;

import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JInvokeStmt;

import petablox.program.visitors.IInvokeInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramDom;
import petablox.util.soot.SootUtilities;
import petablox.util.Utils;

/**
 * Domain of argument and return variable positions of methods and method invocation quads.
 * <p>
 * Let N be the largest number of arguments or return variables of any method or
 * method invocation quad.  Then, this domain contains elements 0, 1, ..., N-1 in order.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "Z")
public class DomZ extends ProgramDom<Integer> implements IInvokeInstVisitor {
    private int maxArgs;

    @Override
    public void init() {
        maxArgs = 0;
    }

    @Override
    public void visit(SootClass c) { }

    @Override
    public void visit(SootMethod m) {
        int numFormals = m.getParameterCount();
        if (numFormals > maxArgs)
            grow(numFormals);
    }

    @Override
    public void visitInvokeInst(Unit u) {
    	List<Value> l = SootUtilities.getInvokeArgs(u); 
    	if(SootUtilities.isInstanceInvoke(u)){
            Value thisV = SootUtilities.getInstanceInvkBase(u);
            l.add(0, thisV);
        }
        int numActuals = l.size();                 
        if (numActuals > maxArgs)
            grow(numActuals);
    }

    @Override
    public void visit(JInvokeStmt s) { }

    public void grow(int newSize) {
        int oldSize = maxArgs;
        for (int i = oldSize; i < newSize; i++)
            getOrAdd(new Integer(i));
        maxArgs = newSize;
    }
    
    @Override
    public String toFIString(Integer i) {	
    	StringBuilder sb = new StringBuilder();
    	boolean printId = Utils.buildBoolProperty("petablox.printrel.printID", false);
    	if(printId) sb.append("(" + indexOf(i) +")");
    	sb.append(i.toString());
    	return sb.toString();
    }
}
