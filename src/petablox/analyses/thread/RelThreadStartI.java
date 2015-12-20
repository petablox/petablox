package petablox.analyses.thread;

import soot.SootMethod;
import soot.Unit;
import soot.VoidType;
import soot.jimple.InvokeStmt;
import petablox.analyses.invk.DomI;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;

@Petablox(name="threadStartI", sign="I0:I0")
public class RelThreadStartI extends ProgramRel {
    @Override
    public void fill() {
        DomI domI = (DomI) doms[0];
        int numI = domI.size();
        for (int i = 0; i < numI; i++) {
            Unit q = domI.get(i);
            SootMethod m = ((InvokeStmt)q).getInvokeExpr().getMethod();
            if (m.getName().toString().equals("start") &&
                //m.getDesc().toString().equals("()V") &&
                m.getParameterCount() == 0 && (m.getReturnType() instanceof VoidType) && 
            	m.getDeclaringClass().getName().equals("java.lang.Thread")) {
                add(i);
            }
        }
    }
}
