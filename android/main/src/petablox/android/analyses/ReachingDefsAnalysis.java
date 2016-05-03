package petablox.android.analyses;

import soot.*;
import soot.jimple.*;
import soot.util.*;
import soot.tagkit.LinkTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import java.util.*;
import petablox.android.util.IntraLocalDefs;

/*
 * An analysis that runs intra-proc reachingDef.
 */

public class ReachingDefsAnalysis {

	public static void runReachingDef(Body body) {

        UnitGraph g = new ExceptionalUnitGraph(body);
        //support transitive closure.
        LocalDefs sld = new IntraLocalDefs(g, new SimpleLiveLocals(g));

        Iterator it = body.getUnits().iterator();
        while (it.hasNext()){
            Stmt s = (Stmt)it.next();
            //System.out.println("stmt: "+s);
            Iterator usesIt = s.getUseBoxes().iterator();
            while (usesIt.hasNext()){
                ValueBox vbox = (ValueBox)usesIt.next();
                if (vbox.getValue() instanceof Local) {
                    Local l = (Local)vbox.getValue();
                    //System.out.println("local: "+l);
                    Iterator<Unit> rDefsIt = sld.getDefsOfAt(l, s).iterator();
                    while (rDefsIt.hasNext()){
                        Stmt next = (Stmt)rDefsIt.next();
                        String info = l+" has reaching def: "+next.toString();
                        s.addTag(new LinkTag(info, next, body.getMethod().getDeclaringClass().getName(), 
                        "Reaching Defs"));
                    }
                }
            }
        }

	}
}
