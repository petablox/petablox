package petablox.android.analyses;

import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.ProgramRel;
import petablox.util.tuple.integer.IntTrio;

/**
 * Relation containing each tuple (m1,m2) such that
 * method m2 overrides method m1
 *
 * @author Ravi Mangal
 */
@Petablox(
	name = "overrideM",
	sign = "M0,M1:M0_M1",
	consumes = {"cha"}
)
public class RelOverrideM extends ProgramRel {
 private ProgramRel relCHA;

	public void fill() {
 	relCHA = (ProgramRel) ClassicProject.g().getTrgt("cha");
 	relCHA.load();
 	for (IntTrio t : relCHA.getAry3IntTuples()) {
  	add(t.idx0,t.idx2);
 	}
 	relCHA.close();
	}
}
