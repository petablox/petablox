package chord.analyses.alloc;

import java.util.List;

import soot.Type;
import soot.RefType;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;

import chord.program.Reflect;
import chord.analyses.alloc.DomH;
import chord.analyses.type.DomT;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.project.Messages;
import chord.util.tuple.object.Pair;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (h,t) such that object allocation unit h
 * allocates objects of type t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "HT",
    sign = "H0,T1:T1_H0"
)
public class RelHT extends ProgramRel {
    private DomH domH;
    private DomT domT;

    @Override
    public void fill() {
        domH = (DomH) doms[0];
        domT = (DomT) doms[1];
        int numA = domH.getLastA() + 1;
        for (int hIdx = 1; hIdx < numA; hIdx++) {
            Unit h = (Unit) domH.get(hIdx);
            Type t = null;
            if(h instanceof JAssignStmt){
            	JAssignStmt as = (JAssignStmt) h;
            	// do NOT merge handling of New and NewArray
                if (SootUtilities.isNewStmt(as)){
                	Value right=as.rightBox.getValue();
                	t=((JNewExpr)right).getType();
                } else if (SootUtilities.isNewArrayStmt(as)) {
                	Value right=as.rightBox.getValue();
                	t=((JNewArrayExpr)right).getType();                
                } else if (SootUtilities.isNewMultiArrayStmt(as)) {
                	Value right=as.rightBox.getValue();
                	t=((JNewMultiArrayExpr)right).getType();   
                } else if(SootUtilities.isInvoke(h)){
                	Value left = as.leftBox.getValue();
                	t = left.getType();
                }
            } else {
            	Messages.fatal("ERROR: RelHT: Unexpected quad kind %s in domain H", h);
            	t = null;
            }
            int tIdx = domT.indexOf(t);
            if (tIdx == -1) {
                Messages.log("WARN: RelHT: Cannot find type %s in domain T; " +
                    " referenced by quad %s in method %s", t, h, SootUtilities.getMethod(h));
                continue;
            }
            add(hIdx, tIdx);
        }
        Reflect reflect = Program.g().getReflect();
        processResolvedNewInstSites(reflect.getResolvedObjNewInstSites());
        processResolvedNewInstSites(reflect.getResolvedConNewInstSites());
        processResolvedNewInstSites(reflect.getResolvedAryNewInstSites());
    }

    private void processResolvedNewInstSites(List<Pair<Unit, List<RefType>>> l) {
        for (Pair<Unit, List<RefType>> p : l) {
            Unit q = p.val0;
            int hIdx = domH.indexOf(q);
            assert (hIdx >= 0);
            for (RefType t : p.val1) {
                int tIdx = domT.indexOf(t);
                assert (tIdx >= 0);
                add(hIdx, tIdx);
            }
        }
    }
}
