package chord.analyses.invk;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing all method invocation quads whose target is an instance method.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "instI",
    sign = "I0"
)
public class RelInstI extends ProgramRel {
    public void fill() {
        DomI domI = (DomI) doms[0];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit q = (Unit) domI.get(iIdx);
            if(q instanceof Stmt){
            	Stmt s = (Stmt)q;
            	if(s.containsInvokeExpr()){
            		SootMethod m = s.getInvokeExpr().getMethod();
            		if(!m.isStatic())
            			add(iIdx);
            	}
            }
        }
    }
}
