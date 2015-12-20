package petablox.analyses.inst;

import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import petablox.analyses.field.DomF;
import petablox.analyses.point.DomP;
import petablox.analyses.var.DomV;
import petablox.program.visitors.IHeapInstVisitor;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (p,v,f) such that the quad
 * at program point p is of the form <tt>v = f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "PgetStatFldInst",
    sign = "P0,V0,F0:F0_P0_V0"
)
public class RelPgetStatFldInst extends ProgramRel implements IHeapInstVisitor {
    private DomP domP;
    private DomV domV;
    private DomF domF;
    public void init() {
        domP = (DomP) doms[0];
        domV = (DomV) doms[1];
        domF = (DomF) doms[2];
    }
    public void visit(SootClass c) { }
    public void visit(SootMethod m) { }
    public void visitHeapInst(Unit q) {
        if(q instanceof JAssignStmt){
        	JAssignStmt j = (JAssignStmt)q;
        	if(SootUtilities.isStaticGet(j) &&
        			j.rightBox.getValue().getType() instanceof RefLikeType){
        		SootField f = j.getFieldRef().getField();
        		Local l = (Local)j.leftBox.getValue();
        		int pIdx = domP.indexOf(q);
                assert (pIdx >= 0);
                int lIdx = domV.indexOf(l);
                assert (lIdx >= 0);
                int fIdx = domF.indexOf(f);
                assert (fIdx >= 0);
                add(pIdx, lIdx, fIdx);
        	}
        }
    }
}
