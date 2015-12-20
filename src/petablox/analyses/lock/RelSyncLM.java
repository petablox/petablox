package petablox.analyses.lock;

import soot.SootMethod;
import soot.Unit;
import petablox.analyses.lock.DomL;
import petablox.analyses.method.DomM;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;
import petablox.util.soot.SootUtilities;

/**
 * Relation containing each tuple (l,m) such that method m is synchronized on the lock it acquires at point l.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Petablox(
    name = "syncLM",
    sign = "L0,M0:L0_M0",
    consumes = { "entryP" }
)
public class RelSyncLM extends ProgramRel {
    public void fill() {
	    DomL domL = (DomL) doms[0];
	    DomM domM = (DomM) doms[1];
	    int numL = domL.size();
		ProgramRel relEntryP = (ProgramRel) ClassicProject.g().getTrgt("entryP");
		relEntryP.load();
		
	    for (int lIdx = 0; lIdx < numL; lIdx++) {
	        Unit i = domL.get(lIdx);
	        if (relEntryP.contains(lIdx)) {
	            SootMethod m = SootUtilities.getMethod(i);
	            int mIdx = domM.indexOf(m);
	            assert (mIdx >= 0);
	            add(lIdx, mIdx);
	        }
	    }
	    relEntryP.close();
	}
}
