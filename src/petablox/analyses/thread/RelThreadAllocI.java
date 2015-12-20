package petablox.analyses.thread;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import petablox.analyses.invk.DomI;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name="threadAllocI", sign="I0:I0")
public class RelThreadAllocI extends ProgramRel {
    @Override
    public void fill() {
        DomI domI = (DomI) doms[0];
        int numI = domI.size();
        for (int i = 0; i < numI; i++) {
            Unit q = domI.get(i);
            SootMethod m = ((InvokeStmt)q).getInvokeExpr().getMethod();
            if (m.getDeclaringClass().getName().equals("java.lang.Thread") &&
                    m.getName().toString().equals("<init>")) {
                add(i);
            }
        }
    }
}
