package chord.analyses.argret;

import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;
import chord.util.soot.SootUtilities;

/**
 * Domain of argument and return variable positions of methods and method invocation quads.
 * <p>
 * Let N be the largest number of arguments or return variables of any method or
 * method invocation quad.  Then, this domain contains elements 0, 1, ..., N-1 in order.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "Z")
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

    public void grow(int newSize) {
        int oldSize = maxArgs;
        for (int i = oldSize; i < newSize; i++)
            getOrAdd(new Integer(i));
        maxArgs = newSize;
    }
}
