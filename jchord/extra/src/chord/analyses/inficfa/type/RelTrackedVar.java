package chord.analyses.inficfa.type;

import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(name = "trackedVar", consumes = { "V" }, sign = "V0")
public class RelTrackedVar extends ProgramRel {
	@Override
	public void fill() {
		DomV domV = (DomV) doms[0];
		for (int vIdx = 0; vIdx < domV.size(); vIdx++) {
			add(vIdx);
		}
	}
}
