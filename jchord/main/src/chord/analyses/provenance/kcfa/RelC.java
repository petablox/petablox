package chord.analyses.provenance.kcfa;

import chord.analyses.alias.DomC;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
	    name = "RelC",
	    consumes = { "C" },
	    sign = "C0:C0"
	)
public class RelC extends ProgramRel{

	@Override
	public void fill() {
		DomC domC = (DomC) doms[0];
        for(int i = 0;i < domC.size();i ++){
        	this.add(i);
        }
	}
	
}
