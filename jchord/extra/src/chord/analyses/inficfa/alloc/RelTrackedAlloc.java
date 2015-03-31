package chord.analyses.inficfa.alloc;

import chord.analyses.alloc.DomH;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(name = "trackedAlloc", consumes = { "H" }, sign = "H0")
public class RelTrackedAlloc extends ProgramRel {
	@Override
	public void fill() {
		DomH domH = (DomH) doms[0];
		for (int hIdx = 0; hIdx < domH.size(); hIdx++) {
			add(hIdx);
		}
	}
}
