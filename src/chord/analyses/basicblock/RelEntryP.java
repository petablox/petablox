package chord.analyses.basicblock;

import soot.SootMethod;
import soot.Unit;
import chord.analyses.method.DomM;
import chord.analyses.point.DomP;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.soot.CFG;
import chord.util.soot.SootUtilities;


/**
 * Relation containing each tuple (p) such that Unit p is the nop unit of the entry basic block of some method
 */
@Chord(
    name = "entryP",
    sign = "P0:P0",
    consumes = { "M", "P" }
)
public class RelEntryP extends ProgramRel {
	 public void fill() {
	 	DomM domM = (DomM) ClassicProject.g().getTrgt("M");
	 	DomP domP = (DomP) ClassicProject.g().getTrgt("P");
	 	for (int i = 0; i < domM.size(); i++) { 
	 		SootMethod m = (SootMethod) domM.get(i);
	 		CFG cfg = SootUtilities.getCFG(m);
	 		Unit hnop = cfg.getHeads().get(0).getHead();
	 		add(domP.indexOf(hnop));
        }
    }
}
