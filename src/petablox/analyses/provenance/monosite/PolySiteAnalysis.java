package petablox.analyses.provenance.monosite;

import petablox.analyses.invk.DomI;
import petablox.project.ClassicProject;
import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;
import petablox.project.analyses.ProgramRel;
import soot.Unit;

@Petablox(name="polySite-java")
public class PolySiteAnalysis extends JavaAnalysis {

	@Override
	public void run() {
		ClassicProject.g().runTask("polysite-dlog");
		ProgramRel polySiteRel = (ProgramRel) ClassicProject.g().getTrgt(
				"polySite");
		polySiteRel.load();
		DomI domI = (DomI)ClassicProject.g().getTrgt("I");
		Iterable<Unit> iter = polySiteRel.getAry1ValTuples();
		for(Unit i: iter)
			System.out.println(domI.indexOf(i)+", "+i);
	}

}
