package petablox.analyses.inst;

import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;

import java.util.HashMap;

import petablox.analyses.alloc.DomH;
import petablox.analyses.method.DomM;
import petablox.analyses.var.DomV;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.Messages;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;
import petablox.util.tuple.object.Pair;

/**
 * Relation containing each tuple (m,v,h) such that method m contains
 * object allocation statement h which assigns to local variable v.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "MobjValAsgnInst",
    sign = "M0,V0,H0:M0_V0_H0"
)
public class RelMobjValAsgnInst extends ProgramRel {
    public void fill() {
        DomM domM = (DomM) doms[0];
        DomV domV = (DomV) doms[1];
        DomH domH = (DomH) doms[2];
        int numA = domH.getLastA() + 1;
        for (int hIdx = 1; hIdx < numA; hIdx++) {
            Unit q = (Unit) domH.get(hIdx);
            SootMethod m = SootUtilities.getMethod(q);
            int mIdx = domM.indexOf(m);
            assert (mIdx >= 0);
            Local vo=null;
            if(q instanceof JAssignStmt){
            	JAssignStmt j = (JAssignStmt)q;
            	Value left = j.leftBox.getValue();
            	if(SootUtilities.isNew(j))
            		vo = (Local)left;
            	else if(SootUtilities.isNewArray(j))
            		vo = (Local)left;
            	else if(SootUtilities.isNewMultiArrayStmt(j))
            		vo = (Local)left;
            	else if(j.containsInvokeExpr())
            		vo = (Local)left;
            	else{
            		Messages.fatal("Unknown quad in domain H: " + q);
                    vo = null;
            	}
            }
            int vIdx = domV.indexOf(vo);
            if (vIdx >= 0)
                add(mIdx, vIdx, hIdx);
        }
    }
}
