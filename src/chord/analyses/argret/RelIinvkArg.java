package chord.analyses.argret;

import java.util.List;

import soot.Unit;
import soot.RefLikeType;
import soot.Local;
import soot.Value;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.soot.SootUtilities;

/**
 * Relation containing each tuple (i,z,v) such that local variable v
 * is the zth argument variable of method invocation quad i.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "IinvkArg",
    sign = "I0,Z0,V1:I0_V1_Z0"
)
public class RelIinvkArg extends ProgramRel {
    @Override
    public void fill() {
        DomI domI = (DomI) doms[0];
        DomV domV = (DomV) doms[2];
        int numI = domI.size();
        for (int iIdx = 0; iIdx < numI; iIdx++) {
            Unit u = (Unit) domI.get(iIdx);
            List<Value> l = SootUtilities.getInvokeArgs(u);
            if(SootUtilities.isInstanceInvoke(u)){
	            Value thisV = SootUtilities.getInstanceInvkBase(u);
	            l.add(0, thisV);
            }
            int numArgs = l.size();
            for (int zIdx = 0; zIdx < numArgs; zIdx++) {
                Value v=l.get(zIdx);
                if (v.getType() instanceof RefLikeType && v instanceof Local) {
                	Local r = (Local)v;
                    int vIdx = domV.indexOf(r);
                    assert (vIdx >= 0);
                    add(iIdx, zIdx, vIdx);
                }
            }
        }
    }
}
