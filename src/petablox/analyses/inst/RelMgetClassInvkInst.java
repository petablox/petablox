package petablox.analyses.inst;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JInvokeStmt;

import petablox.analyses.invk.DomI;
import petablox.analyses.method.DomM;
import petablox.program.visitors.IInvokeInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (m,i) such that method m contains a statement i invoking instance
 * method getClass() defined in class java.lang.Object.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(name = "MgetClassInvkInst", sign = "M0,I0:M0xI0")
public class RelMgetClassInvkInst extends ProgramRel implements IInvokeInstVisitor {
    private DomM domM;
    private DomI domI;
    private SootMethod ctnrMethod;

    public void init() {
        domM = (DomM) doms[0];
        domI = (DomI) doms[1];
    }

    public void visit(SootClass c) {}

    public void visit(SootMethod m) {
        ctnrMethod = m;
    }

    public void visit(JInvokeStmt s) {}

    public void visitInvokeInst(Unit q) {
        if (SootUtilities.isInvoke(q)) {
            SootMethod meth = SootUtilities.getInvokeExpr(q).getMethod();
            if (meth.getName().equals("getClass")
                    && meth.getBytecodeSignature().contains("()Ljava/lang/Class;")
                    && meth.getDeclaringClass().getName().equals("java.lang.Object")) {
                int mIdx = domM.indexOf(ctnrMethod);
                assert (mIdx >= 0);
                int iIdx = domI.indexOf(q);
                assert (iIdx >= 0);
                add(mIdx, iIdx);
                    }
        }
    }
}
