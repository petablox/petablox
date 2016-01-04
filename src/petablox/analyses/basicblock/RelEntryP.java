package petablox.analyses.basicblock;

import soot.SootMethod;
import soot.Unit;
import petablox.analyses.method.DomM;
import petablox.analyses.point.DomP;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.CFG;
import petablox.util.soot.SootUtilities;


/**
 * Relation containing each tuple (p) such that Unit p is the nop unit of the entry basic block of some method
 */
@Petablox(
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
	 		if (!m.isAbstract()) {
		 		CFG cfg = SootUtilities.getCFG(m);
		 		Unit hnop = cfg.getHeads().get(0).getHead();
		 		add(domP.indexOf(hnop));
	 		}
        }
    }
}
