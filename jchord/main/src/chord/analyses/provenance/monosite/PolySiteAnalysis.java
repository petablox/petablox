package chord.analyses.provenance.monosite;

import joeq.Compiler.Quad.Quad;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

@Chord(name="polySite-java")
public class PolySiteAnalysis extends JavaAnalysis {

	@Override
	public void run() {
		ClassicProject.g().runTask("polysite-dlog");
		ProgramRel polySiteRel = (ProgramRel) ClassicProject.g().getTrgt(
				"polySite");
		polySiteRel.load();
		DomI domI = (DomI)ClassicProject.g().getTrgt("I");
		Iterable<Quad> iter = polySiteRel.getAry1ValTuples();
		for(Quad i: iter)
			System.out.println(domI.indexOf(i)+", "+i);
	}

}
