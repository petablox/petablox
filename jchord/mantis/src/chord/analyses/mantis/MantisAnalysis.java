package chord.analyses.mantis;

import chord.project.analyses.JavaAnalysis;
import chord.instr.OfflineTransformer;
import chord.project.Chord;

@Chord(
	name="mantis-java"
)
public class MantisAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		MantisInstrumentor instrumentor = new MantisInstrumentor();
		(new OfflineTransformer(instrumentor)).run();
		instrumentor.done();
	}
}

