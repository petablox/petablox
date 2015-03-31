package chord.slicer;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.program.QuadToJasmin;

@Chord(name="jslice-java")
public class JasminSlice extends JavaAnalysis {
	public void run() {
		ClassicProject.g().runTask("qslice-java");
		(new QuadToJasmin()).run();
	}
}
